package icu.match.common;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public enum OrderType {


	LIMIT, MARKET;

	private static final OrderType[] VALS = new OrderType[]{LIMIT, MARKET};


	public boolean isMarket() {
		return this == MARKET;
	}

	public boolean isLimit() {
		return this == LIMIT;
	}

	public static OrderType get(int idx) {
		return VALS[idx];
	}
}
