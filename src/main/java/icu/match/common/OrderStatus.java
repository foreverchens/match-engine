package icu.match.common;

/**
 * @author 中本君
 * @date 2025/08/03 
 */

public enum OrderStatus {
	/**
	 * 初始状态
	 */
	NEW(0),
	/**
	 * 正常情况 入订单簿 部分成交 完成成交
	 */
	PENDING(10), PARTIALLY_FILLED(11), FILLED(12),
	/**
	 * 异常情况 被测单 被拒绝
	 */
	CANCELED(20), REJECTED(21);

	public final int val;

	OrderStatus(int val) {
		this.val = val;
	}


}
