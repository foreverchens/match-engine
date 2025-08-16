package icu.match.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16
 */
class PriceLevelTest {

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
}
