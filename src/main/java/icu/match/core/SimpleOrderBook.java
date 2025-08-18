package icu.match.core;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import icu.match.common.OrderSide;
import icu.match.common.OrderStatus;
import icu.match.core.interfaces.BaseOrderBook;
import icu.match.core.interfaces.MatchSink;
import icu.match.core.model.BestLiqView;
import icu.match.core.model.MatchedTrade;
import icu.match.core.model.OrderInfo;
import icu.match.core.model.StepMatchResult;
import icu.match.service.match.model.Order;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

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

	private final MatchSink matchSink;

	public SimpleOrderBook(RingOrderBuffer ring, ColdOrderBuffer cold, MatchSink matchSink) {
		this.symbol = ring.getSymbol();
		this.ring = Objects.requireNonNull(ring, "ring must not be null");
		this.cold = Objects.requireNonNull(cold, "cold must not be null");
		this.recenter = new RecenterManager(ring, cold);
		this.pool = new OrderNodePoolFixed(64);
		this.matchSink = matchSink;
	}

	@Override
	public Optional<BestLiqView> bestLiq(OrderSide takerSide) {
		PriceLevel liq;
		if (takerSide.isAsk()) {
			liq = ring.getBidBestLevel();

		}
		else {
			liq = ring.getAskBestLevel();
		}
		assert liq != null;
		return Optional.of(new BestLiqView(liq.getPrice(), liq.totalQty(), liq.getFirst().qty));
	}

	@Override
	public Optional<BestLiqView> bestLiq(OrderSide takerSide, long limitPrice) {
		boolean flag;
		PriceLevel liq;
		BestLiqView bestLiqView = new BestLiqView();
		do {
			if (takerSide.isAsk()) {
				liq = ring.getBidBestLevel();
			}
			else {
				liq = ring.getAskBestLevel();
			}
			assert liq != null;
			if (bestLiqView.getPrice() == 0) {
				bestLiqView.setPrice(liq.getPrice());
				bestLiqView.setHeadQty(liq.getFirst().qty);
			}
			bestLiqView.setTotalQty(bestLiqView.getTotalQty() + liq.totalQty());
			flag = takerSide.isAsk()
				   ? bestLiqView.getPrice() > limitPrice
				   : bestLiqView.getPrice() < limitPrice;
		}
		while (flag);
		return Optional.of(bestLiqView);
	}

	@Override
	public StepMatchResult matchHead(long takerQty, Order takerOrder) {
		// 前置条件极简保护（生产代码可改为抛异常或断言）
		if (takerQty <= 0 || takerOrder == null) {
			throw new IllegalArgumentException("takerQty must greater than 0");
		}
		OrderSide side = takerOrder.getSide();

		PriceLevel bestPriceLevel = side.isAsk()
									? ring.getBidBestLevel()

									: ring.getAskBestLevel();
		if (bestPriceLevel == null) {
			throw new IllegalArgumentException("bestPriceLevel must greater than 0");
		}
		OrderNode makerOrder = bestPriceLevel.getFirst();

		// 本步撮合量：两者最小
		long traded = Math.min(takerQty, makerOrder.qty);
		makerOrder.qty -= traded;
		if (makerOrder.qty <= 0) {
			// makerOrder 完全成交 takerOrder部分成交


		}
		else {
			// makerOrder 部分成交 takerOrder完全成交
		}
		return new StepMatchResult(false, takerOrder.getPrice()
													.longValue(), takerQty, new MatchedTrade());
	}

	@Override
	public OrderStatus submit(OrderInfo orderInfo) {
		long price = orderInfo.getPrice();
		OrderNode node = pool.alloc(orderInfo.getOrderId(), orderInfo.getUserId(), orderInfo.getSide()
																							.isAsk(),
									orderInfo.getQty());
		if (ring.isWindow(price)) {
			ring.submit(price, node);
		}
		else {
			cold.submit(price, node);
		}
		return OrderStatus.PENDING;
	}


	public String dump() {
		return ring.dump();
	}

	public String snapshot() {
		return ring.snapshot();
	}
}
