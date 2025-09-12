package icu.match.common;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

/**
 * 订单有效策略
 * 0->GTC
 * 1->IOC
 * 2->FOK
 * @author 中本君
 * @date 2025/8/17 
 */
public enum OrderTif {

	GTC((byte) 0), IOC((byte) 1), FOK((byte) 2);

	public final byte code;

	OrderTif(byte code) {
		this.code = code;
	}


	private static final OrderTif[] VALS = new OrderTif[]{GTC, IOC, FOK};


	public static OrderTif get(byte code) {
		return VALS[code];
	}
}
