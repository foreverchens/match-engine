package icu.service.match.interfac;

import icu.service.match.model.MatchResultEvent;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
public interface MatchResultEventHandler {


	/**
	 * 处理撮合成功的订单
	 *
	 * @param event
	 */
	void handle(MatchResultEvent event);
}
