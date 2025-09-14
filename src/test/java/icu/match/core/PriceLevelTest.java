package icu.match.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16
 */
class PriceLevelTest {

	private final OrderNodePoolFixed pool = new OrderNodePoolFixed(64);


	@Test
	void submitGetFirstPatchCancelRemove() {
		long p = 100;
		PriceLevel lvl = new PriceLevel(p);
		OrderNodePoolFixed pool = new OrderNodePoolFixed(8);

		OrderNode o1 = pool.alloc(201, 1, true, 5);
		OrderNode o2 = pool.alloc(202, 2, true, 7);

		lvl.submit(o1);
		lvl.submit(o2);

		assertTrue(lvl.isAsk());
		assertEquals(2, lvl.size());
		assertEquals(12, lvl.totalQty());
		assertEquals(o1, lvl.getFirst());

		assertTrue(lvl.patchQty(201, 3));
		assertEquals(10, lvl.totalQty());

		OrderNode removed = lvl.remove(201);
		assertEquals(1, lvl.size());
		assertEquals(7, lvl.totalQty());
		pool.free(removed);

		OrderNode canceled = lvl.cancel(202);
		assertEquals(0, lvl.size());
		assertEquals(0, lvl.totalQty());
		pool.free(canceled);
	}

	// ----------------------- 工具：完整性与可视化校验 -----------------------

	@Test
	void submit_fifo_patch_remove_cancel_and_dump_visual() {
		long price = 12345L;
		PriceLevel lvl = new PriceLevel(price);

		// 三笔 ASK，FIFO: a -> b -> c
		OrderNode a = pool.alloc(1001L, 11L, /*ask*/true, 10);
		OrderNode b = pool.alloc(1002L, 22L, true, 20);
		OrderNode c = pool.alloc(1003L, 33L, true, 30);

		lvl.submit(a);
		lvl.submit(b);
		lvl.submit(c);

		assertTrue(lvl.isAsk(), "direction should follow first submit");
		assertEquals(3, lvl.size());
		assertEquals(60L, lvl.totalQty());
		assertSame(a, lvl.getFirst());

		// 可视化顺序：a -> b -> c
		assertLevelIntegrity(lvl);

		// patch 数量：b: 20 -> 25
		assertTrue(lvl.patchQty(1002L, 25L));
		assertEquals(65L, lvl.totalQty());
		assertLevelIntegrity(lvl);

		// remove 头部 a（完全成交）
		OrderNode ra = lvl.remove(1001L);
		assertNotNull(ra);
		assertEquals(2, lvl.size());
		assertEquals(55L, lvl.totalQty());
		// 回收前确保断链
		ra.prev = ra.next = null;
		pool.free(ra);
		assertLevelIntegrity(lvl);

		// cancel 尾部 c（撤单）
		OrderNode rc = lvl.cancel(1003L);
		assertNotNull(rc);
		assertEquals(1, lvl.size());
		assertEquals(25L, lvl.totalQty());
		rc.prev = rc.next = null;
		pool.free(rc);
		assertLevelIntegrity(lvl);

		// 仅剩 b
		assertSame(b, lvl.getFirst());
		assertTrue(lvl.patchQty(1002L, 1L));
		assertEquals(1L, lvl.totalQty());
		assertLevelIntegrity(lvl);

		// 移除最后一个
		OrderNode rb = lvl.remove(1002L);
		assertNotNull(rb);
		rb.prev = rb.next = null;
		pool.free(rb);

		assertTrue(lvl.isEmpty());
		assertEquals(0, lvl.size());
		assertEquals(0L, lvl.totalQty());
		assertLevelIntegrity(lvl);
	}


	// ----------------------- 用例：常规路径 -----------------------

	private void assertLevelIntegrity(PriceLevel lvl) {
		int expectedSize = lvl.size();
		long expectedQty = lvl.totalQty();

		// 正向遍历：从头到尾
		int count = 0;
		long sumQty = 0L;
		OrderNode cur = lvl.getFirst();
		OrderNode last = null;
		while (cur != null) {
			count++;
			sumQty += cur.qty;

			if (cur.next != null) {
				assertSame(cur, cur.next.prev, "next.prev symmetry broken at orderId=" + cur.orderId);
			}
			if (cur.prev != null) {
				assertSame(cur, cur.prev.next, "prev.next symmetry broken at orderId=" + cur.orderId);
			}
			last = cur;
			cur = cur.next;
			assertTrue(count <= expectedSize + 1, "possible cycle on forward traversal");
		}
		assertEquals(expectedSize, count, "size mismatch on forward");
		assertEquals(expectedQty, sumQty, "totalQty mismatch on forward");

		// 反向遍历：从尾回头（根据 last 指针）
		int backCount = 0;
		long backSum = 0L;
		cur = last;
		while (cur != null) {
			backCount++;
			backSum += cur.qty;
			cur = cur.prev;
			assertTrue(backCount <= expectedSize + 1, "possible cycle on backward traversal");
		}
		assertEquals(expectedSize, backCount, "size mismatch on backward");
		assertEquals(expectedQty, backSum, "totalQty mismatch on backward");
	}

	@Test
	void nonExisting_cancel_remove_are_safe() {
		PriceLevel lvl = new PriceLevel(777L);
		assertNull(lvl.cancel(42L));
		assertNull(lvl.remove(42L));

		// 放入一单后撤另一个 ID 仍然安全
		OrderNode n = pool.alloc(1L, 1L, false, 5L);
		lvl.submit(n);
		assertNull(lvl.cancel(2L));
		assertNull(lvl.remove(3L));

		// 清理
		OrderNode r = lvl.remove(1L);
		assertNotNull(r);
		r.prev = r.next = null;
		pool.free(r);
	}

	@Test
	void patchQty_must_be_positive() {
		PriceLevel lvl = new PriceLevel(100L);
		OrderNode n = pool.alloc(9L, 9L, false, 9L);
		lvl.submit(n);

		assertThrows(IllegalArgumentException.class, () -> lvl.patchQty(9L, 0L));
		assertThrows(IllegalArgumentException.class, () -> lvl.patchQty(9L, -1L));

		OrderNode r = lvl.remove(9L);
		r.prev = r.next = null;
		pool.free(r);
	}

	// ----------------------- 用例：方向标记的基础校验 -----------------------

	@Test
	void direction_flag_follows_first_submit() {
		PriceLevel lvl = new PriceLevel(321L);

		OrderNode firstAsk = pool.alloc(2001L, 1L, /*ask*/true, 3L);
		lvl.submit(firstAsk);
		assertTrue(lvl.isAsk(), "level direction should align to first submit");

		// 清理
		OrderNode r = lvl.remove(2001L);
		r.prev = r.next = null;
		pool.free(r);
		assertTrue(lvl.isEmpty());
	}
}
