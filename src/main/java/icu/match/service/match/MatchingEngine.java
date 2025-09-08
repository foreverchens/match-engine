package icu.match.service.match;

import org.springframework.stereotype.Component;

import icu.match.common.OrderSide;
import icu.match.common.OrderStatus;
import icu.match.common.OrderTif;
import icu.match.common.OrderType;
import icu.match.core.ColdOrderBuffer;
import icu.match.core.RingOrderBuffer;
import icu.match.core.SimpleOrderBook;
import icu.match.core.interfaces.BaseOrderBook;
import icu.match.core.interfaces.MatchSink;
import icu.match.core.model.BestLiqView;
import icu.match.core.model.MatchTrade;
import icu.match.core.model.OrderInfo;
import icu.match.web.service.MatchTradeService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * 单线程撮合引擎（基于热区环形数组 + 冷区红黑树）。
 * <p>支持 LIMIT + {GTC, IOC}。只有 GTC 会入簿持久；IOC 未成交余量直接丢弃。</p>
 *
 * 设计约束：
 * <ul>
 *   <li>仅在热区窗口内撮合与落单；窗口外：GTC→落冷区，IOC→丢弃。</li>
 *   <li>成交价使用对手方价位（被动方档位）。</li>
 *   <li>撮合过程中仅通过 PriceLevel.patchQty/remove/cancel 保持聚合统计正确。</li>
 *   <li>每次撮合后调用 recenter.checkAndRecenter()，不修改 lastIdx/lastPrice 的约束由 ring 内部保证。</li>
 * </ul>
 * @author 中本君
 * @date 2025/8/16
 */
@Slf4j
@Component
public final class MatchingEngine {

	private final Map<String, BaseOrderBook> orderBookMap;

	private MatchSink matchSink;

	@Resource
	private MatchTradeService matchTradeService;

	public MatchingEngine() {
		orderBookMap = new HashMap<>();
	}

	@PostConstruct
	private void init() {
		String symbol = "BTCUSDT";
		RingOrderBuffer ring = new RingOrderBuffer(symbol, 1, 100, 110);
		ColdOrderBuffer cold = new ColdOrderBuffer();
		SimpleOrderBook orderBook = new SimpleOrderBook(ring, cold);
		orderBookMap.put(symbol, orderBook);
		OrderInfo.OrderInfoBuilder builder = OrderInfo.builder();
		builder.symbol(symbol);
		builder.userId(1000);
		builder.qty(1);

		builder.side(OrderSide.BID);
		builder.price(100);
		builder.orderId(1000);
		orderBook.submit(builder.build());
		builder.price(100);
		builder.orderId(1001);
		orderBook.submit(builder.build());
		builder.price(101);
		builder.orderId(1002);
		orderBook.submit(builder.build());
		builder.qty(2);
		builder.price(102);
		builder.orderId(1003);
		orderBook.submit(builder.build());

		builder.side(OrderSide.ASK);
		builder.price(109);
		builder.orderId(2000);
		orderBook.submit(builder.build());
		builder.price(108);
		builder.orderId(2001);
		orderBook.submit(builder.build());
		builder.price(107);
		builder.orderId(2002);
		orderBook.submit(builder.build());
		builder.qty(1);
		builder.price(108);
		builder.orderId(2003);
		orderBook.submit(builder.build());

	}


	public OrderStatus submit(OrderInfo order) {
		OrderType orderType = order.getOrderType();
		if (orderType.isMarket()) {
			// 处理市价单
			return dealMarketOrder(order);
		}
		// 处理限价单
		return dealLimitOrder(order);
	}

	/**
	 *  市价撮合流程
	 *  1.获取待撮合数量remainingQty
	 *  2.进入循环remainingQty > 0
	 *  3.获取对手方最优流动性层
	 *  4.不断对该层进行头节点撮合 直到完全撮合
	 */
	private OrderStatus dealMarketOrder(OrderInfo order) {
		String symbol = order.getSymbol();
		BaseOrderBook orderBook = orderBookMap.get(symbol);
		long remainingQty = order.getQty();
		OrderSide side = order.getSide();
		while (remainingQty > 0) {
			BestLiqView bestLiqView = orderBook.bestLiq(side);
			if (bestLiqView.getTotalQty() <= 0) {
				// 整个对手订单簿为空 不可能事件呢
				break;
			}
			long totalMatchedQty = 0;
			while (totalMatchedQty < bestLiqView.getTotalQty()) {
				MatchTrade matchTrade = orderBook.matchHead(order.getSide(), order.getQty(), order.getUserId(),
															order.getOrderId());
				long matchedQty = matchTrade.getQty();
				remainingQty -= matchedQty;
				totalMatchedQty += matchedQty;
				order.setQty(remainingQty);
				//　todo matchTradeRlt结果的发布 写wal日志 通知订单 账户模块等等
				matchTradeService.saveTrade(matchTrade)
								 .block();
				if (remainingQty == 0) {
					break;
				}
			}
		}
		if (remainingQty > 0) {
			// 未完全市价撮合 走IOC策略 自动取消
			// matchSink.onOrderCancelled();
			log.info("remaining:{}", remainingQty);
			return OrderStatus.PARTIALLY_FILLED;
		}
		return OrderStatus.FILLED;
	}

	private OrderStatus dealLimitOrder(OrderInfo order) {
		String symbol = order.getSymbol();
		BaseOrderBook orderBook = orderBookMap.get(symbol);

		OrderSide takerSide = order.getSide();
		long takerPrice = order.getPrice();
		OrderTif tif = order.getTif();

		// 1. 先检查能否立即限价撮合
		if (!orderBook.canMatchImmediately(takerSide, takerPrice)) {
			// 1.1不能立即限价撮合
			// 1.2在检查订单TIF IOC策略则取消 FOK策略则拒单 GTC策略则提交到订单簿
			switch (tif) {
				case FOK:
					return OrderStatus.REJECTED;
				case IOC:
					return OrderStatus.CANCELED;
				case GTC:
					// 1.3 不能立即撮合的GTC单子 提交到订单簿
					return orderBook.submit(order);
				default:
					throw new IllegalArgumentException("Unsupported TIF: " + tif);
			}
		}
		// 2.能立即限价撮合
		// 2.1 如果是FOK检查能否立即完全成交
		long takerQty = order.getQty();
		if (OrderTif.FOK.equals(order.getTif())) {
			BestLiqView bestLiq = orderBook.bestLiq(takerSide, takerPrice);
			if (bestLiq.getTotalQty() < takerQty) {
				// 不能完全成交
				return OrderStatus.REJECTED;
			}
		}
		// 能完全成交的FOK订单和IOC、GTC订单 进入撮合
		// 2.2 获取满足撮合条件的流动性视图
		BestLiqView bestLiqView = orderBook.bestLiq(takerSide, takerPrice);
		// 2.3 计算可撮合数量和剩余数量
		long canMatchQty = Math.min(takerQty, bestLiqView.getTotalQty());
		long remainingQty = takerQty - canMatchQty;
		// 2.4 对可撮合数量部分进行撮合
		while (canMatchQty > 0) {
			// 2.4.1 不断进行头节点撮合 同时更新可撮合数量
			MatchTrade matchTrade = orderBook.matchHead(order.getSide(), order.getQty(), order.getUserId(),
														order.getOrderId());
			long matchedQty = matchTrade.getQty();
			canMatchQty -= matchedQty;
			order.setQty(canMatchQty);
			//　todo matchTradeRlt结果的发布 写wal日志 通知订单 账户模块等等
			log.info("matchTradeRlt:{}", matchTrade);
			// todo 换成事件队列写
			matchTradeService.saveTrade(matchTrade)
							 .block();
		}
		// 2.5 末尾处理
		if (remainingQty == 0) {
			// 完全撮合 FOK策略订单肯定已经完全撮合
			return OrderStatus.FILLED;
		}
		// 未完全撮合
		if (OrderTif.GTC.equals(tif)) {
			order.setQty(remainingQty);
			return orderBook.submit(order);
		} else {
			// IOC策略
			return OrderStatus.CANCELED;
		}
	}

	public void cancel(OrderInfo orderInfo) {
		String symbol = orderInfo.getSymbol();
		BaseOrderBook orderBook = orderBookMap.get(symbol);
		orderBook.cancel(orderInfo);
	}

	public String snapshot() {
		return orderBookMap.get("BTCUSDT")
						   .snapshot();
	}


}
