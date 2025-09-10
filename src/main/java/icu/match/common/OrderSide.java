package icu.match.common;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public enum OrderSide {
	/**
	 * bid ask
	 */
	BID, ASK;

	private static final OrderSide[] VALS = new OrderSide[]{BID, ASK};


	public boolean isAsk() {
		return "ASK".equalsIgnoreCase(this.name());
	}

	public boolean eq(OrderSide side) {
		return this.name().equalsIgnoreCase(side.name());
	}

	public static OrderSide get(int idx) {
		return VALS[idx];
	}
}