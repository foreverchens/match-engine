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
	 * 中间状态 新入订单簿 部分成交
	 */
	OPEN(110), PARTIALLY_FILLED(111),
	/**
	 * 终止状态 完全成交 被撤单取消 FOK不满足被拒绝 IOC部分成交取消
	 */
	FILLED(120), CANCELED(121), REJECTED(122), PARTIALLY_FILLED_CANCELED(123);

	public final int val;

	OrderStatus(int val) {
		this.val = val;
	}


}
