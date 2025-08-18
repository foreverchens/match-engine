package icu.match.core;

import org.springframework.stereotype.Component;

import icu.match.common.CallResult;
import icu.match.common.OrderSide;
import icu.match.common.OrderStatus;
import icu.match.core.interfaces.BaseOrderBook;
import icu.match.core.interfaces.MatchSink;
import icu.match.core.model.MatchedTrade;
import icu.match.core.model.OrderInfo;
import icu.match.service.match.model.Order;

import javax.annotation.PostConstruct;

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
@Component
public final class MatchingEngine {

	private final Map<String, BaseOrderBook> orderBookMap;


	public MatchingEngine() {
		orderBookMap = new HashMap<>();
	}

	@PostConstruct
	private void init() {
		String symbol = "BTCUSDT";
		RingOrderBuffer ring = new RingOrderBuffer(symbol, 1, 100, 110);
		ColdOrderBuffer cold = new ColdOrderBuffer();
		SimpleOrderBook orderBook = new SimpleOrderBook(ring, cold, new MatchSink() {
			@Override
			public void onTrade(MatchedTrade t) {

			}

			@Override
			public void onOrderAccepted(Order o) {

			}

			@Override
			public void onOrderRested(OrderNode o) {

			}

			@Override
			public void onOrderCancelled(long orderId, String reason) {

			}

			@Override
			public void onOrderRejected(OrderNode o, String reason) {

			}
		});
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


	public CallResult<OrderStatus> submit(OrderInfo order) {
		return CallResult.suc(orderBookMap.get(order.getSymbol())
										  .submit(order));
	}

	public String dump() {
		return orderBookMap.get("BTCUSDT")
						   .dump();
	}

	public String snapshot() {
		return orderBookMap.get("BTCUSDT")
						   .snapshot();
	}
}
