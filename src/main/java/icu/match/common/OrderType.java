package icu.match.common;

/**
 * 订单类型
 * 0->LIMIT
 * 1->MARKET
 * @author yyy
 * @tg t.me/ychen5325
 */
public enum OrderType {


	LIMIT((byte) 0), MARKET((byte) 1);


	public final byte code;

	OrderType(byte code) {
		this.code = code;
	}


	private static final OrderType[] VALS = new OrderType[]{LIMIT, MARKET};


	public static boolean isMarket(byte code) {
		return code == MARKET.code;
	}


	public static OrderType get(byte code) {
		return VALS[code];
	}
}
