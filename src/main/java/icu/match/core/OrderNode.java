package icu.match.core;

/**
 * @author 中本君
 * @date 2025/8/12
 */
public final class OrderNode {

	// 核心字段（撮合态）
	long orderId;

	long userId;

	boolean ask;

	/**
	 * 剩余数量（乘以 tick 的 long）
	 */
	long qty;

	/**
	 * 到达时间
	 */
	long time;

	/**
	 * 通过对象池初始化的
	 */
	boolean pooled;

	/**
	 * 运行时状态 被初始化的
	 */
	boolean used;


	OrderNode prev;

	OrderNode next;

	/**
	 * 仅池内调用
	 */
	void init(long orderId, long userId, boolean ask, long qty, boolean pooled) {
		this.orderId = orderId;
		this.userId = userId;
		this.ask = ask;
		this.qty = qty;
		this.time = System.nanoTime();
		this.used = true;
		this.pooled = pooled;
		this.prev = null;
		this.next = null;
	}

	/**
	 * 回收前由池调用
	 */
	void reset() {
		this.prev = null;
		this.next = null;
		this.qty = 0L;
		this.used = false;
	}

	@Override
	public String toString() {
		return "OrderNode{orderId=" + orderId + ", userId=" + userId + ", qty=" + qty + '}';
	}
}
