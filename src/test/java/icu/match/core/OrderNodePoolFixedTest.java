package icu.match.core;/**
 *
 * @author 中本君
 * @date 2025/8/16
 */

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16 
 */
public class OrderNodePoolFixedTest {

	@Test
	public void allocWithinPoolAndOverflowThenFree() {
		OrderNodePoolFixed pool = new OrderNodePoolFixed(2);

		OrderNode n1 = pool.alloc(1, 11, false, 100);
		OrderNode n2 = pool.alloc(2, 22, true, 200);
		// 第三个触发溢出 new
		OrderNode n3 = pool.alloc(3, 33, false, 300);

		assertEquals(2, pool.getInUsePooled());
		assertEquals(1, pool.getOverflowAlloc());

		// 将 n2 加入队列模拟“已链接”，free 应该因 prev/next 非空而抛异常
		OrderQueue q = new OrderQueue();
		q.push(n2);
		q.push(n3);
		assertThrows(IllegalStateException.class, () -> pool.free(n3));

		// 正确摘链再 free
		q.remove(n2.orderId);
		pool.free(n2);
		pool.free(n1);
		pool.free(n3); // 溢出对象，丢 GC

		assertEquals(0, pool.getInUsePooled());
		assertEquals(1, pool.getReleasedOverflowCount());
		assertTrue(pool.getPeakInUse() >= 3);
	}

	@Test
	public void freeDoubleShouldFailIfUsedFlagSetByCaller() {
		// 当前实现 used 默认 false；这里手动标记以验证防御逻辑
		OrderNodePoolFixed pool = new OrderNodePoolFixed(1);
		OrderNode n = pool.alloc(1, 1, true, 1);
		n.used = false; // 模拟外部错误
		assertThrows(IllegalStateException.class, () -> pool.free(n));
	}

}
