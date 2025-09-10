package icu.match.core;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import icu.match.common.OrderSide;
import icu.match.common.OrderStatus;
import icu.match.core.interfaces.BaseOrderBook;
import icu.match.core.model.BestLiqView;
import icu.match.core.model.MatchTrade;
import icu.match.core.model.OrderInfo;
import lombok.Getter;

import java.util.Objects;

/**
 * @author 中本君
 * @date 2025/8/17 
 */
public class SimpleOrderBook implements BaseOrderBook {

	@Getter
	private final String symbol;

	private final RingOrderBuffer ring;

	private final ColdOrderBuffer cold;

	private final RecenterManager recenter;

	private final OrderNodePoolFixed pool;


	/**
	 * 单对象复用
	 */
	private final BestLiqView bestLiqView = new BestLiqView();

	// 单例对象 数据获取时需要先复制
	private final MatchTrade matchTrade = new MatchTrade();

	public SimpleOrderBook(RingOrderBuffer ring, ColdOrderBuffer cold) {
		this.symbol = ring.getSymbol();
		this.ring = Objects.requireNonNull(ring, "ring must not be null");
		this.cold = Objects.requireNonNull(cold, "cold must not be null");
		this.recenter = new RecenterManager(ring, cold);
		this.pool = new OrderNodePoolFixed(64);
	}

	@Override
	public BestLiqView bestLiq(OrderSide takerSide) {
		bestLiqView.clear();
		PriceLevel liq = ring.getBestLevel(takerSide);
		if (liq == null) {
			return bestLiqView;
		}
		bestLiqView.setPrice(liq.getPrice());
		bestLiqView.setTotalQty(liq.totalQty());
		bestLiqView.setHeadQty(liq.getFirst().qty);
		return bestLiqView;
	}

	@Override
	public BestLiqView bestLiq(OrderSide takerSide, long limitPrice) {
		PriceLevel liq;
		bestLiqView.clear();

		// 获取最优流动性 设置好最优价格和头节点数量
		liq = ring.getBestLevel(takerSide);
		if (liq == null) {
			return bestLiqView;
		}
		bestLiqView.setPrice(liq.getPrice());
		bestLiqView.setHeadQty(liq.getFirst().qty);
		bestLiqView.setTotalQty(ring.getTotalQty(takerSide, limitPrice));
		return bestLiqView;
	}

	@Override
	public MatchTrade matchHead(OrderSide takerSide, long takerQty, long takerUserId, long takerOrderId) {
		PriceLevel bestPriceLevel = ring.getBestLevel(takerSide);
		if (bestPriceLevel == null) {
			throw new IllegalArgumentException("bestPriceLevel must not be null");
		}
		OrderNode makerOrder = bestPriceLevel.getFirst();

		// 计算可撮合数量 两者取小
		long matchQty = Math.min(takerQty, makerOrder.qty);
		if (makerOrder.qty == matchQty) {
			// makerOrder 完全成交 takerOrder部分成交
			// 将makerOrder从订单簿移除
			ring.remove(bestPriceLevel.getPrice(), makerOrder.orderId);
		} else {
			// makerOrder 部分成交 takerOrder完全成交
			// 更新 makerOrder qty
			ring.patchQty(bestPriceLevel.getPrice(), makerOrder.orderId, makerOrder.qty - matchQty);
		}
		return matchTrade.fill(symbol, takerUserId, makerOrder.userId, takerOrderId, makerOrder.orderId, takerSide,
							   bestPriceLevel.getPrice(), matchQty);
	}

	@Override
	public boolean canMatchImmediately(OrderSide takerSide, long limitPrice) {
		return takerSide.isAsk()
			   ? limitPrice <= ring.bestBidPrice()
			   : limitPrice >= ring.bestAskPrice();
	}

	@Override
	public OrderStatus submit(OrderInfo orderInfo) {
		long price = orderInfo.getPrice();
		OrderNode node = pool.alloc(orderInfo.getOrderId(), orderInfo.getUserId(), orderInfo.getSide()
																							.isAsk(),
									orderInfo.getQty());
		if (ring.isWindow(price)) {
			ring.submit(price, node);
		} else {
			cold.submit(price, node);
		}
		return OrderStatus.PENDING;
	}

	@Override
	public void cancel(OrderInfo orderInfo) {
		long price = orderInfo.getPrice();
		long orderId = orderInfo.getOrderId();
		if (ring.isWindow(price)) {
			ring.cancel(price, orderId);
		} else {
			cold.cancel(price, orderId, orderInfo.getSide()
												 .isAsk());
		}
	}

	public String snapshot() {
		return ring.snapshot();
	}
}
