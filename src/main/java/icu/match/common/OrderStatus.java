package icu.match.common;

/**
 * @author 中本君
 * @date 2025/08/03 
 */

public enum OrderStatus {
	/**
	 * 初始状态
	 */
	NEW(100),
	/**
	 * 正常情况 入订单簿 部分成交 完成成交
	 */
	PENDING(110), PARTIALLY_FILLED(111), FILLED(112),
	/**
	 * 异常情况 被测单 被拒绝
	 */
	CANCELED(120), REJECTED(121);

	public final int val;

	OrderStatus(int val) {
		this.val = val;
	}


}
