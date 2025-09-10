package icu.match.core.interfaces;/**
 *
 * @author 中本君
 * @date 2025/8/16
 */


import icu.match.core.OrderNode;
import icu.match.core.model.MatchTrade;

/**
 * @author 中本君
 * @date 2025/8/16 
 */
public interface MatchSink {

	void onTraded(MatchTrade matchTrade);

	void onOrderRested(OrderNode o);

	void onOrderCancelled(long orderId, String reason);

	void onOrderRejected(OrderNode o, String reason);
}
