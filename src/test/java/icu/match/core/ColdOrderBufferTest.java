package icu.match.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16
 */
class ColdOrderBufferTest {

	@Test
	void submitBestPopCancelRemoveDump() {
		ColdOrderBuffer cold = new ColdOrderBuffer();
		OrderNodePoolFixed pool = new OrderNodePoolFixed(16);

		// 落冷区：BID 99, 98；ASK 105
		OrderNode b1 = pool.alloc(401, 1, false, 10);
		OrderNode b2 = pool.alloc(402, 2, false, 20);
		OrderNode a1 = pool.alloc(403, 3, true, 5);

		cold.submit(99, b1);
		cold.submit(98, b2);
		cold.submit(105, a1);

		assertEquals(2, cold.sizeBids());
		assertEquals(1, cold.sizeAsks());

		assertEquals(99, cold.bestBid()
							 .getPrice());
		assertEquals(105, cold.bestAsk()
							  .getPrice());

		// pop 最优
		PriceLevel pb = cold.popBestBid();
		assertNotNull(pb);
		assertEquals(99, pb.getPrice());

		PriceLevel pa = cold.popBestAsk();
		assertNotNull(pa);
		assertEquals(105, pa.getPrice());

		// 撤单（剩余的 98 档）
		OrderNode canceled = cold.cancel(98, 402, false);
		assertNotNull(canceled);
		pool.free(canceled);

		assertEquals(0, cold.sizeAsks());
		assertEquals(0, cold.sizeBids());

		String snapshot = cold.dump(10);
		assertNotNull(snapshot);
		assertTrue(snapshot.contains("ColdOrderBuffer"));
	}
}
