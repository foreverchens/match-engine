package icu.match.core.interfaces;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import icu.match.common.OrderStatus;
import icu.match.core.model.BestLiqView;
import icu.match.core.model.MatchTrade;
import icu.match.core.model.OrderInfo;

/**
 * @author 中本君
 * @date 2025/8/17 
 */
public interface BaseOrderBook {

	/**
	 * 获取对手盘最优一档流动性（price/totalQty/headQty）
	 */
	BestLiqView bestLiq(byte takerSideCode);


	/**
	 * 获取能与limitPrice价格撮合的流动性视图
	 */
	BestLiqView bestLiq(byte takerSideCode, long takerLimitPrice);


	/**
	 * 最小原子撮合函数 仅与最高优先级的head订单进行一次撮合
	 */
	MatchTrade matchHead(byte takerSideCode, long takerQty);

	/**
	 * 判断限价单价格能否立即撮合
	 * @param takerSideCode 限价单方向
	 * @param takerLimitPrice 限价单价格
	 */
	boolean canMatchImmediately(byte takerSideCode, long takerLimitPrice);

	OrderStatus submit(OrderInfo orderInfo);

	boolean cancel(long price, long orderId);

	String snapshot();

}
