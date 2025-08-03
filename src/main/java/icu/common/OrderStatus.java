package icu.common;

/**
 * @author 中本君
 * @date 2025/08/03 
 */

public interface OrderStatus {

	/**
	 * 该订单被交易引擎接受。
	 *  已入库但未提交disruptor队列
	 */
	Integer NEW = 0;

	/**
	 * 已从disruptor队列消费并提交给了撮合引擎
	 */
	Integer PENDING = 1;

	/**
	 * 完全成交
	 */
	Integer FILLED = 2;

	/**
	 * 部分成交
	 */
	Integer PARTIALLY_FILLED = 3;

	/**
	 * 撤销的
	 */
	Integer CANCELED = 4;

	/**
	 * 拒绝的
	 */
	Integer REJECTED = 5;

}
