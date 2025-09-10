package icu.match.core.interfaces;/**
 *
 * @author 中本君
 * @date 2025/8/16
 */


import icu.match.core.model.MatchTrade;

/**
 * @author 中本君
 * @date 2025/8/16 
 */
public interface MatchEventProcessor {

	/**
	 * 撮合成交事件
	 */
	void onTraded(MatchTrade matchTrade);

	/**
	 * 主动撤单和 部分成交IOC策略撤单
	 */
	void onOrderCancelled(String symbol, long orderId, long qty);

	/**
	 * 订单被拒绝 FOK策略不满足
	 */
	void onOrderRejected(String symbol, long orderId);
}
