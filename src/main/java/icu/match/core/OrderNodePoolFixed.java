package icu.match.core;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 固定上限的对象池
 * @author 中本君
 * @date 2025/8/16
 */
public final class OrderNodePoolFixed {

	@Getter
	private final int poolSize;

	private final Deque<OrderNode> freeStack;

	/**
	 * 当前(池内)在用数量 动态变化
	 */
	@Getter
	private int inUsePooled = 0;

	/**
	 * 直接 new 的次数 不断累加
	 */
	@Getter
	private long overflowAlloc = 0;

	/**
	 * 直接new 但是已释放的数量
	 */
	@Getter
	private long releasedOverflowCount = 0;

	/**
	 *  历史峰值（池内+溢出）
	 */
	@Getter
	private int peakInUse = 0;

	public OrderNodePoolFixed(int poolSize) {
		this.poolSize = poolSize;
		this.freeStack = new ArrayDeque<>(poolSize);
		warmup();
	}

	/**
	 * 一次性分配到上限
	 */
	private void warmup() {
		while (freeStack.size() < poolSize) {
			OrderNode n = new OrderNode();
			n.used = false;
			n.pooled = true;
			freeStack.addLast(n);
		}
	}

	/**
	 * 分配：优先复用池；池空 → 溢出 new（pooled=false）
	 */
	public OrderNode alloc(long orderId, long userId, boolean ask, long qty) {
		final OrderNode n;
		OrderNode fromPool = freeStack.pollLast();
		if (fromPool != null) {
			n = fromPool;
			n.init(orderId, userId, ask, qty, true);
			inUsePooled++;
		}
		else {
			overflowAlloc++;
			n = new OrderNode();
			n.init(orderId, userId, ask, qty, false);
		}
		final int inUseOverflow = (int) Math.max(0, overflowAlloc - releasedOverflowCount);
		final int currentInUse = inUsePooled + inUseOverflow;
		if (currentInUse > peakInUse) {
			peakInUse = currentInUse;
		}
		return n;
	}

	/**
	 * 释放：池内节点归还池；溢出节点丢给GC
	 */
	public void free(OrderNode node) {
		if (!node.used) {
			throw new IllegalStateException("double free: orderId=" + node.orderId);
		}
		node.reset();
		boolean wasPooled = node.pooled;
		if (wasPooled) {
			freeStack.addLast(node);
			inUsePooled--;
		}
		else {
			releasedOverflowCount++;
		}
	}
}
