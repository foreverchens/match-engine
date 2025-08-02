package icu.service.match.interfac;

import icu.service.match.model.Order;

/**
 * 撮合引擎接口
 *
 * @author 中本君
 * @date 2025/07/27 
 */
public interface MatchEngine {


	/**
	 * 提交订单到订单簿
	 *
	 * @param order order
	 */
	void submit(Order order);
}
