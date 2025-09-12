package icu.match.common;/**
 *
 * @author 中本君
 * @date 2025/8/18
 */

/**
 * 订单事件类别
 * 0->NEW
 * 1->CANCEL
 * 2->MODIFY
 * @author 中本君
 * @date 2025/8/18 
 */
public enum OrderEventType {
	NEW_ORDER((byte) 0), CANCEL_ORDER((byte) 1), MODIFY_ORDER((byte) 2);

	private static final OrderEventType[] VALS = new OrderEventType[]{NEW_ORDER, CANCEL_ORDER, MODIFY_ORDER};

	public final byte code;

	OrderEventType(byte code) {
		this.code = code;
	}

	public static OrderEventType get(byte code) {
		return VALS[code];
	}
}
