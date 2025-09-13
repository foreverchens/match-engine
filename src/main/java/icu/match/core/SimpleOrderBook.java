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
	private final int symbol;

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
	public BestLiqView bestLiq(byte takerSide) {
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
	public BestLiqView bestLiq(byte takerSide, long takerLimitPrice) {
		PriceLevel liq;
		bestLiqView.clear();

		// 获取最优流动性 设置好最优价格和头节点数量
		liq = ring.getBestLevel(takerSide);
		if (liq == null) {
			return bestLiqView;
		}
		bestLiqView.setPrice(liq.getPrice());
		bestLiqView.setHeadQty(liq.getFirst().qty);
		bestLiqView.setTotalQty(ring.getTotalQty(takerSide, takerLimitPrice));
		return bestLiqView;
	}

	@Override
	public MatchTrade matchHead(byte takerSideCode, long takerQty) {
		PriceLevel bestPriceLevel = ring.getBestLevel(takerSideCode);
		if (bestPriceLevel == null) {
			throw new IllegalArgumentException("bestPriceLevel must not be null");
		}
		OrderNode makerOrder = bestPriceLevel.getFirst();

		// 计算可撮合数量 两者取小
		long matchQty = Math.min(takerQty, makerOrder.qty);
		boolean markerFilled = false;
		if (makerOrder.qty == matchQty) {
			// makerOrder 完全成交 takerOrder部分成交
			// 将makerOrder从订单簿移除
			ring.remove(bestPriceLevel.getPrice(), makerOrder.orderId);
			markerFilled = true;
			// marker被完全吃单后。如果价格当前整个流动性为空 需要检查窗口偏移情况
			recenter.checkAndRecenter();
		} else {
			// makerOrder 部分成交 takerOrder完全成交
			// 更新 makerOrder qty
			ring.patchQty(bestPriceLevel.getPrice(), makerOrder.orderId, makerOrder.qty - matchQty);
		}
		return matchTrade.fill(symbol, 0, makerOrder.userId, 0, makerOrder.orderId, takerSideCode,
							   bestPriceLevel.getPrice(), markerFilled, matchQty);
	}

	@Override
	public boolean canMatchImmediately(byte takerSide, long limitPrice) {
		return OrderSide.isAsk(takerSide)
			   ? limitPrice <= ring.bestBidPrice()
			   : limitPrice >= ring.bestAskPrice();
	}

	@Override
	public OrderStatus submit(OrderInfo orderInfo) {
		long price = orderInfo.getPrice();
		OrderNode node = pool.alloc(orderInfo.getOrderId(), orderInfo.getUserId(),
									OrderSide.isAsk(orderInfo.getSide()),
									orderInfo.getQty());
		if (ring.isWindow(price)) {
			ring.submit(price, node);
		} else {
			cold.submit(price, node);
		}
		return OrderStatus.OPEN;
	}

	@Override
	public boolean cancel(long price, long orderId) {
		OrderNode cancel;
		if (ring.isWindow(price)) {
			cancel = ring.cancel(price, orderId);
		} else {
			cancel = cold.cancel(price, orderId);
		}
		return cancel != null;
	}

	public String snapshot() {
		return ring.depth();
	}

	public String depth() {
		return ring.depth();
	}


}
