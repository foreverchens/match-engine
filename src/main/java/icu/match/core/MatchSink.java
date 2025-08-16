package icu.match.core;/**
 *
 * @author 中本君
 * @date 2025/8/16
 */


import icu.match.service.match.model.Order;
import icu.match.service.match.model.Trade;

/**
 * @author 中本君
 * @date 2025/8/16 
 */
public interface MatchSink {

	void onTrade(Trade t);

	void onOrderAccepted(Order o);

	void onOrderRested(OrderNode o);

	void onOrderCancelled(long orderId, String reason);

	void onOrderRejected(OrderNode o, String reason);
}
