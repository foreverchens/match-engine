package icu.match.core;

import icu.match.common.OrderSide;
import lombok.Getter;

/**
 * 价位桶：维护单一价格与单一方向的一组订单。
 * <p>内部以 OrderQueue 管理同价位 FIFO 队列，并透出常用的撮合操作。</p>
 * <p>本对象不负责 OrderNode 的分配与回收；节点回收由上层撮合逻辑处理。</p>
 *
 * <h3>不变式</h3>
 * <ul>
 *   <li>仅代表单一价格 {@code price} 与单一方向 {@code ask}。</li>
 *   <li>同价位内订单以 FIFO 顺序撮合，尾插、头取。</li>
 *   <li>聚合统计 {@code size}/{@code totalQty} 由内部队列维护，读操作 O(1)。</li>
 * </ul>
 *  @author 中本君
 *  @date 2025/8/12
 */
public final class PriceLevel {

	/** 该层对应的价格 */
	@Getter
	private final long price;

	/**
	 * 同价位订单的 FIFO 队列与聚合统计
	 */
	private final OrderQueue queue;

	/**
	 * 方向标记，true 表示卖单侧（ask），false 表示买单侧（bid）。
	 * 仅在队列为空时由首单决定；非空时必须与既有方向一致。
	 */
	@Getter
	private boolean ask;

	public PriceLevel(long price) {
		this.price = price;
		this.queue = new OrderQueue();
	}

	/**
	 * 尾插法 提交订单
	 * <p>节点必须由上层分配且当前未被链接；本方法不负责回收。</p>
	 *
	 * @param node 订单节点
	 */
	public void submit(OrderNode node) {
		if (node == null) {
			throw new IllegalArgumentException("node must not be null");
		}
		if (node.qty <= 0) {
			throw new IllegalArgumentException("newQty must be greater than 0");
		}

		// 队列为空 → 由首单设定本价位方向；非空 → 必须一致
		if (queue.isEmpty()) {
			this.ask = node.ask;
		}
		else if (this.ask != node.ask) {
			throw new IllegalStateException("side mismatch for price " + price);
		}

		this.queue.push(node);
	}

	/**
	 * 获取队头订单节点（不移除）
	 */
	public OrderNode getFirst() {return queue.peek();}

	/**
	 * 修改指定订单的数量
	 */
	public boolean patchQty(long orderId, long newQty) {return queue.patchQty(orderId, newQty);}

	public boolean isEmpty() {return queue.isEmpty();}

	/**
	 * 撤单：按订单 ID 摘除（不回收）
	 */
	public OrderNode cancel(long orderId) {return queue.remove(orderId);}

	/**
	 * 完全成交删除：按订单 ID 摘除（不回收）
	 */
	public OrderNode remove(long orderId) {return queue.remove(orderId);}

	public int size() {return queue.getSize();}

	/**
	 * 当前价位聚合数量
	 */
	public long totalQty() {return queue.getTotalQty();}

	/**
	 * 生成快照
	 */
	public String dump() {
		String side = ask
					  ? OrderSide.ASK.toString()
					  : OrderSide.BID.toString();
		return "PriceLevel{price=" + price + ", side=" + side + ", size=" + queue.getSize() + ", totalQty=" +
			   queue.getTotalQty() + "}\n" + queue.dump();
	}
}
