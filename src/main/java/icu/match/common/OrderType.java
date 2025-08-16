package icu.match.common;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public interface OrderType {

	String LIMIT = "LIMIT";

	String MARKET = "MARKET";

	/**
	 *  判断是否为市价订单
	 *
	 * @param type type
	 * @return isMarket
	 */
	static boolean isMarket(String type) {
		return MARKET.equalsIgnoreCase(type);
	}
}
