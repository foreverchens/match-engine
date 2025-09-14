

package icu.match.core;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author 中本君
 * @date 2025/8/16
 */
@Slf4j
class OrderQueueTest {

	private final OrderNodePoolFixed pool = new OrderNodePoolFixed(32);


	@Test
	void pushPeekPatchRemoveClear() {
		OrderNodePoolFixed pool = new OrderNodePoolFixed(8);
		OrderQueue q = new OrderQueue();

		OrderNode a = pool.alloc(101, 1, false, 10);
		OrderNode b = pool.alloc(102, 2, false, 20);
		OrderNode c = pool.alloc(103, 3, false, 30);

		q.push(a);
		q.push(b);
		q.push(c);

		assertEquals(3, q.getSize());
		assertEquals(60, q.getTotalQty());
		assertEquals(a, q.peek());

		// patch
		assertTrue(q.patchQty(102, 25));
		assertEquals(65, q.getTotalQty());

		// remove middle
		OrderNode rb = q.remove(102);
		assertNotNull(rb);
		assertEquals(2, q.getSize());
		assertEquals(40, q.getTotalQty());
		pool.free(rb);

		// clear
		OrderNode first = q.clear();
		assertNull(q.peek());
		assertEquals(0, q.getSize());
		assertEquals(0, q.getTotalQty());

		// 回收剩余
		// 遍历链：first -> next
		OrderNode cur = first;
		while (cur != null) {
			OrderNode next = cur.next;
			// 清空后 n.prev/next 已被置空；这里是旧引用链，不影响 pool.free 的 prev/next 校验
			cur.prev = null;
			cur.next = null;
			pool.free(cur);
			cur = next;
		}
	}

	@Test
	void duplicateOrderIdShouldFail() {
		OrderQueue q = new OrderQueue();
		OrderNode a = pool.alloc(1, 1, false, 10);
		OrderNode b = pool.alloc(1, 2, false, 20); // 重复ID
		q.push(a);
		assertThrows(IllegalStateException.class, () -> q.push(b));
		// 回收
		q.remove(1);
		pool.free(a);
		pool.free(b);
	}


	// ----------- 工具函数：可重复使用的完整性校验 -----------

	@Test
	void push_peek_patch_remove_visualize() {
		OrderQueue q = new OrderQueue();

		OrderNode a = pool.alloc(101, 1, false, 10);
		OrderNode b = pool.alloc(102, 2, false, 20);
		OrderNode c = pool.alloc(103, 3, false, 30);

		q.push(a);
		q.push(b);
		q.push(c);

		// 基本状态
		assertEquals(3, q.getSize());
		assertEquals(60, q.getTotalQty());
		assertSame(a, q.peek());


		// patch b: 20 -> 25
		assertTrue(q.patchQty(102, 25));
		assertEquals(65, q.getTotalQty());
		assertQueueIntegrity(q);

		// 移除中间 b
		OrderNode rb = q.remove(102);
		assertNotNull(rb);
		assertEquals(2, q.getSize());
		assertEquals(40, q.getTotalQty());
		assertQueueIntegrity(q);

		// 回收被移除的节点（先确保彻底摘链）
		rb.prev = rb.next = null;
		pool.free(rb);

		// 移除头 a
		OrderNode ra = q.remove(101);
		assertNotNull(ra);
		ra.prev = ra.next = null;
		pool.free(ra);
		assertEquals(1, q.getSize());
		assertEquals(30, q.getTotalQty());
		assertQueueIntegrity(q);

		// 移除尾 c
		OrderNode rc = q.remove(103);
		assertNotNull(rc);
		rc.prev = rc.next = null;
		pool.free(rc);
		assertEquals(0, q.getSize());
		assertEquals(0, q.getTotalQty());
		assertNull(q.peek());
		assertQueueIntegrity(q);
	}


	// ----------- 用例：常规路径 -----------

	/**
	 * 校验队列的内部一致性：
	 * 1) 从 head (peek) 正向遍历到 null，计数与 totalQty/size 一致；
	 * 2) 从最后一个节点反向回到 null，计数一致；
	 * 3) 无越界、无断链（next/prev 对称）。
	 */
	private void assertQueueIntegrity(OrderQueue q) {
		int expectSize = q.getSize();
		long expectQty = q.getTotalQty();

		// 正向
		int count = 0;
		long sumQty = 0L;
		OrderNode cur = q.peek();
		OrderNode last = null;
		while (cur != null) {
			count++;
			sumQty += cur.qty;

			// next.prev 必须指回自己（除非 next 为 null）
			if (cur.next != null) {
				assertSame(cur, cur.next.prev, "next.prev symmetry broken at orderId=" + cur.orderId);
			}
			// prev.next 必须指回自己（除非 prev 为 null）
			if (cur.prev != null) {
				assertSame(cur, cur.prev.next, "prev.next symmetry broken at orderId=" + cur.orderId);
			}

			last = cur;
			cur = cur.next;

			// 简单环保护：不应超过 size 次
			assertTrue(count <= expectSize + 1,
					   "possible cycle detected on forward traversal (count=" + count + ", size=" + expectSize + ")");
		}

		assertEquals(expectSize, count, "size mismatch on forward traversal");
		assertEquals(expectQty, sumQty, "totalQty mismatch on forward traversal");

		// 反向
		int backCount = 0;
		long backSumQty = 0L;
		cur = last;
		while (cur != null) {
			backCount++;
			backSumQty += cur.qty;
			cur = cur.prev;

			assertTrue(backCount <= expectSize + 1,
					   "possible cycle detected on backward traversal (count=" + backCount + ", size=" + expectSize +
					   ")");
		}
		assertEquals(expectSize, backCount, "size mismatch on backward traversal");
		assertEquals(expectQty, backSumQty, "totalQty mismatch on backward traversal");
	}

	@Test
	void duplicate_id_should_throw_and_dump_stable() {
		OrderQueue q = new OrderQueue();
		OrderNode a = pool.alloc(1, 10, false, 5);
		OrderNode b = pool.alloc(1, 11, false, 7); // duplicate id

		q.push(a);
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> q.push(b));
		assertTrue(ex.getMessage()
					 .contains("duplicate"), ex.getMessage());

		// 清理
		OrderNode r = q.remove(1);
		r.prev = r.next = null;
		pool.free(r);
		pool.free(b);
	}

	@Test
	void clear_should_unlink_all_and_return_first_for_manual_recycle() {
		OrderQueue q = new OrderQueue();
		OrderNode a = pool.alloc(11, 1, false, 1);
		OrderNode b = pool.alloc(12, 1, false, 2);
		OrderNode c = pool.alloc(13, 1, false, 3);
		q.push(a);
		q.push(b);
		q.push(c);

		OrderNode first = q.clear();
		assertEquals(0, q.getSize());
		assertEquals(0, q.getTotalQty());
		assertNull(q.peek());

		// 遍历旧链并回收（注意：clear() 已把内部 head/tail 置空，
		// 这里的 first/next 是旧引用，不影响队列状态）
		OrderNode cur = first;
		int count = 0;
		while (cur != null) {
			OrderNode next = cur.next;
			cur.prev = cur.next = null;
			pool.free(cur);
			cur = next;
			count++;
		}
		assertTrue(count >= 1);
	}

	// ----------- 用例：安全/异常路径 -----------

	@Test
	void push_node_already_linked_should_throw() {
		OrderQueue q1 = new OrderQueue();
		OrderQueue q2 = new OrderQueue();
		OrderNode n1 = pool.alloc(77, 7, false, 7);
		OrderNode n2 = pool.alloc(78, 7, false, 7);

		q1.push(n1);
		q1.push(n2);
		// 未摘链直接向另一队列 push 应抛异常
		assertThrows(IllegalStateException.class, () -> q2.push(n1));

		// 摘链后可复用
		q1.remove(77);
		n1.prev = n1.next = null;
		q2.push(n1);

		// 清理
		OrderNode r = q2.remove(77);
		r.prev = r.next = null;
		pool.free(r);
	}

	@Test
	void patchQty_zero_or_negative_should_throw() {
		OrderQueue q = new OrderQueue();
		OrderNode n = pool.alloc(99, 9, false, 9);
		q.push(n);

		assertThrows(IllegalArgumentException.class, () -> q.patchQty(99, 0));
		assertThrows(IllegalArgumentException.class, () -> q.patchQty(99, -1));

		// 清理
		OrderNode r = q.remove(99);
		r.prev = r.next = null;
		pool.free(r);
	}

	@Test
	void dump_cycle_detection_visual_check() {
		OrderQueue q = new OrderQueue();
		OrderNode a = pool.alloc(201, 2, false, 2);
		OrderNode b = pool.alloc(202, 2, false, 2);
		q.push(a);
		q.push(b);

		// 人为制造一个环：b.next 指回 a，a.prev 指向 b（请勿在生产代码这样做）
		b.next = a;
		a.prev = b;

		// 解除环并清理
		a.prev = null;
		b.next = null;

		OrderNode rb = q.remove(202); // 此时 byId 仍管理正常
		OrderNode ra = q.remove(201);
		if (rb != null) {
			rb.prev = rb.next = null;
			pool.free(rb);
		}
		if (ra != null) {
			ra.prev = ra.next = null;
			pool.free(ra);
		}
	}

	// ---------- 用例 1：基础 push -> remove -> 再 push（复用节点） ----------
	@Test
	public void push_remove_repush_should_keep_consistency() {
		OrderQueue q = new OrderQueue();

		OrderNode a = node(1, 10);
		OrderNode b = node(2, 20);
		OrderNode c = node(3, 30);

		q.push(a);
		q.push(b);
		q.push(c);
		assertInvariant(q, 3, 60, 1, 2, 3);

		// remove 中间节点
		OrderNode removedB = q.remove(2);
		assertSame(b, removedB);
		// 被 unlink 后，prev/next 应已置空，可再次 push
		assertNull(b.prev);
		assertNull(b.next);
		assertInvariant(q, 2, 40, 1, 3);

		// 复用同一个 b 实例再次入队（append 到尾部）
		q.push(b);
		assertInvariant(q, 3, 60, 1, 3, 2);

		// 再次 remove 头、尾，检查边界
		assertSame(a, q.remove(1));
		assertInvariant(q, 2, 50, 3, 2);

		assertSame(b, q.remove(2));
		assertInvariant(q, 1, 30, 3);

		// remove 不存在的 id
		assertNull(q.remove(999));
		assertInvariant(q, 1, 30, 3);
	}

	// ---------- 工具：构造节点（按你项目的 OrderNode 定义调整） ----------
	private static OrderNode node(long id, long qty) {
		OrderNode n = new OrderNode();
		n.orderId = id;
		n.qty = qty;
		n.time = System.nanoTime();
		n.pooled = false;
		// prev/next 必须为 null，才能被 push 接受
		n.prev = null;
		n.next = null;
		return n;
	}

	// ---------- 工具：从队列正向遍历收集 id、累计 qty，并做结构不变式检查 ----------
	private static void assertInvariant(OrderQueue q, long expectedSize, long expectedTotalQty,
										long... expectedOrderIds) {
		// 1) 基础统计
		assertEquals(expectedSize, q.getSize(), "size 不一致");
		assertEquals(expectedTotalQty, q.getTotalQty(), "totalQty 不一致");
		if (expectedSize == 0) {
			assertNull(q.peek(), "空队列时 head 应为 null");
			return;
		}

		// 2) 从 head 正向遍历，验证顺序、无环、无自指针
		List<Long> seen = new ArrayList<>();
		long sumQty = 0;
		int guard = 0;

		OrderNode cur = q.peek();
		OrderNode prev = null;

		while (cur != null) {
			// 无自环
			assertNotSame(cur, cur.next, "发生自环：cur.next == cur");
			assertNotSame(cur, cur.prev, "发生自环：cur.prev == cur");

			// 前向链接一致性
			if (prev == null) {
				assertNull(cur.prev, "头结点 prev 必须为 null");
			}
			else {
				assertSame(prev, cur.prev, "双向链接断裂：prev/next 不匹配");
			}

			// byId 映射一致性（存在于链表的一定能 get 到）
			OrderNode mapped = q.get(cur.orderId);
			assertSame(cur, mapped, "byId 与链表脱节：同 id 映射到不同实例或为 null");

			seen.add(cur.orderId);
			sumQty += cur.qty;

			prev = cur;
			cur = cur.next;

			// 简单环检测：遍历次数不应超过 expectedSize
			guard++;
			if (guard > expectedSize + 1) {
				fail("疑似形成环：遍历超过期望大小");
			}
		}

		// 3) 顺序与内容核对
		assertEquals(expectedOrderIds.length, seen.size(), "遍历结果元素个数与期望不符");
		for (int i = 0; i < expectedOrderIds.length; i++) {
			assertEquals(expectedOrderIds[i], seen.get(i), "遍历顺序不符（index=" + i + "）");
		}

		// 4) 量纲核对
		assertEquals(expectedTotalQty, sumQty, "遍历累计 qty 与 totalQty 不一致");

	}

	// ---------- 用例 2：重复 remove（幂等性） ----------
	@Test
	public void repeated_remove_should_be_idempotent() {
		OrderQueue q = new OrderQueue();

		OrderNode x = node(11, 5);
		OrderNode y = node(12, 6);

		q.push(x);
		q.push(y);
		assertInvariant(q, 2, 11, 11, 12);

		assertSame(x, q.remove(11));
		assertNull(q.remove(11), "对同一 id 重复 remove 应返回 null，不改变状态");
		assertInvariant(q, 1, 6, 12);
	}

	// ---------- 用例 3：头/中/尾删除的链接正确性 ----------
	@Test
	public void remove_head_middle_tail_should_keep_links_correct() {
		OrderQueue q = new OrderQueue();

		OrderNode a = node(1, 1);
		OrderNode b = node(2, 2);
		OrderNode c = node(3, 3);
		OrderNode d = node(4, 4);

		q.push(a);
		q.push(b);
		q.push(c);
		q.push(d);
		assertInvariant(q, 4, 10, 1, 2, 3, 4);

		// 删头
		assertSame(a, q.remove(1));
		assertInvariant(q, 3, 9, 2, 3, 4);

		// 删中
		assertSame(c, q.remove(3));
		assertInvariant(q, 2, 6, 2, 4);

		// 删尾
		assertSame(d, q.remove(4));
		assertInvariant(q, 1, 2, 2);

		// 只剩一个
		assertSame(b, q.remove(2));
		assertInvariant(q, 0, 0);
	}

	// ---------- 用例 4：重复 push 同 id（应报错），push 已链接节点（应报错） ----------
	@Test
	public void duplicate_id_or_linked_node_should_fail_fast() {
		OrderQueue q = new OrderQueue();

		OrderNode a = node(1, 10);
		OrderNode b = node(1, 99); // 同 id

		q.push(a);
		assertThrows(IllegalStateException.class, () -> q.push(b), "重复 orderId 应拒绝");

		// 伪造“已链接节点”场景：手工设置 next 指针
		OrderNode c = node(3, 30);
		c.next = new OrderNode(); // 非 null 即视为已链接
		assertThrows(IllegalStateException.class, () -> q.push(c), "prev/next 非空的节点不应允许入队");
	}

	// ---------- 用例 5：patchQty 一致性 ----------
	@Test
	public void patchQty_should_update_totalQty_consistently() {
		OrderQueue q = new OrderQueue();

		OrderNode a = node(1, 10);
		OrderNode b = node(2, 20);
		q.push(a);
		q.push(b);
		assertInvariant(q, 2, 30, 1, 2);

		assertTrue(q.patchQty(1, 15)); // +5
		assertInvariant(q, 2, 35, 1, 2);

		assertTrue(q.patchQty(2, 1)); // -19
		assertInvariant(q, 2, 16, 1, 2);

		assertFalse(q.patchQty(999, 7)); // 不存在
		assertInvariant(q, 2, 16, 1, 2);

		assertThrows(IllegalArgumentException.class, () -> q.patchQty(1, 0), "newQty<=0 应拒绝");
	}

	// ---------- 用例 6：clear 之后复用节点 ----------

	// ---------- 用例 7：高频 push/remove 序列（随机化可扩展） ----------
	@Test
	public void stress_like_push_remove_sequences_should_hold() {
		OrderQueue q = new OrderQueue();

		OrderNode a = node(101, 5);
		OrderNode b = node(102, 6);
		OrderNode c = node(103, 7);
		OrderNode d = node(104, 8);

		// 一串交替操作
		q.push(a);                         // [a]
		q.push(b);                         // [a,b]
		assertInvariant(q, 2, 11, 101, 102);

		q.remove(101);                     // [b]
		assertInvariant(q, 1, 6, 102);

		q.push(c);                         // [b,c]
		q.push(d);                         // [b,c,d]
		assertInvariant(q, 3, 21, 102, 103, 104);

		q.remove(104);                     // [b,c]
		q.remove(102);                     // [c]
		assertInvariant(q, 1, 7, 103);

		// 复用 a，再 push 回来
		assertNull(a.prev);
		assertNull(a.next);
		q.push(a);                         // [c,a]
		assertInvariant(q, 2, 12, 103, 101);

		// 再复用 b
		assertNull(b.prev);
		assertNull(b.next);
		q.push(b);                         // [c,a,b]
		assertInvariant(q, 3, 18, 103, 101, 102);

		// 全清
		q.remove(103);
		q.remove(101);
		q.remove(102);
		assertInvariant(q, 0, 0);
	}

}
