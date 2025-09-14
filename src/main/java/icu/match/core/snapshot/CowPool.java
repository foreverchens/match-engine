package icu.match.core.snapshot;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import icu.match.core.OrderNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class CowPool {

	private final Map<Long, OrderNode> POOL = new HashMap<>();

	public OrderNode get(long orderId) {
		return POOL.get(orderId);
	}

	public void put(long orderId, OrderNode orderNode) {
		POOL.putIfAbsent(orderId, orderNode);
	}

}
