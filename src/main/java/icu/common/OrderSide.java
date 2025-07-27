package icu.common;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public interface OrderSide {
	String BID = "BID";

	String ASK = "ASK";

	/**
	 *  判断是否为卖单
	 *
	 * @param side side
	 * @return isMarket
	 */
	static boolean isAsk(String side) {
		return ASK.equalsIgnoreCase(side);
	}
}