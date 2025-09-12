package icu.match.common;

/**
 * 订单方向
 * 0->BID
 * 1->ASK
 * @author yyy
 * @tg t.me/ychen5325
 */
public enum OrderSide {
	/**
	 * bid ask
	 */
	BID((byte) 0), ASK((byte) 1);

	public final byte code;

	OrderSide(byte code) {
		this.code = code;
	}

	private static final OrderSide[] VALS = new OrderSide[]{BID, ASK};


	public static boolean isAsk(byte code) {
		return code == ASK.code;
	}

	public static OrderSide get(int code) {
		return VALS[code];
	}
}