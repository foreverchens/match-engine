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

	public boolean isAsk() {
		return "ASK".equalsIgnoreCase(this.name());
	}

	public boolean eq(OrderSide side) {
		return this.name().equalsIgnoreCase(side.name());
	}
}