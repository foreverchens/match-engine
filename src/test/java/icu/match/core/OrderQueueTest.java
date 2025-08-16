

package icu.match.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16
 */
class OrderQueueTest {

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
		OrderNodePoolFixed pool = new OrderNodePoolFixed(4);
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

}
