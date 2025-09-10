package icu.match.common;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

/**
 * @author 中本君
 * @date 2025/8/17 
 */
public enum OrderTif {

	GTC, IOC, FOK;

	private static final OrderTif[] VALS = new OrderTif[]{GTC, IOC, FOK};


	public static OrderTif get(int idx) {
		return VALS[idx];
	}
}
