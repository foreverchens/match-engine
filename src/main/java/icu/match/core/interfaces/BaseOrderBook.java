package icu.match.core.interfaces;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import icu.match.common.OrderSide;
import icu.match.common.OrderStatus;
import icu.match.core.model.BestLiqView;
import icu.match.core.model.StepMatchResult;
import icu.match.service.match.model.Order;

import java.util.Optional;

/**
 * @author 中本君
 * @date 2025/8/17 
 */
public interface BaseOrderBook {
	/**
	 * 获取对手盘最优一档流动性（price/totalQty/headQty）
	 */
	Optional<BestLiqView> bestLiq(OrderSide takerSide);

	Optional<BestLiqView> bestLiq(OrderSide takerSide, long limitPrice);

	StepMatchResult matchHead(long takerQty, Order order);

	OrderStatus submit(Order order);
}
