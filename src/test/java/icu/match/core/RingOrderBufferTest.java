package icu.match.core;

import org.junit.jupiter.api.Test;

import icu.match.common.OrderSide;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16
 */
class RingOrderBufferTest {

	@Test
	void initSubmitCancelRemoveAndBest() {
		RingOrderBuffer ring = new RingOrderBuffer("BTCUSDT", 1, 100, 107); // slots=8,len=8
		OrderNodePoolFixed pool = new OrderNodePoolFixed(16);

		// 造一笔 BID 在高价端；先把 last 定位到 high，使 getBidBestLevel 从右往左能命中
		ring.recordTradePrice(ring.getHighPrice());

		long bidPrice = 106;
		OrderNode bid = pool.alloc(301, 1, false, 10);
		ring.submit(bidPrice, bid);

		PriceLevel bestBid = ring.getBestLevel(OrderSide.ASK);
		assertNotNull(bestBid);
		assertEquals(bidPrice, bestBid.getPrice());
		assertFalse(bestBid.isAsk());

		// 撤单
		OrderNode canceled = ring.cancel(bidPrice, 301);
		assertNotNull(canceled);
		pool.free(canceled);

		// 卖单测试
		ring.recordTradePrice(ring.getLowPrice());
		long askPrice = 101;
		OrderNode ask = pool.alloc(302, 2, true, 5);
		ring.submit(askPrice, ask);

		PriceLevel bestAsk = ring.getBestLevel(OrderSide.BID);
		assertNotNull(bestAsk);
		assertEquals(askPrice, bestAsk.getPrice());
		assertTrue(bestAsk.isAsk());

		OrderNode removed = ring.remove(askPrice, 302);
		assertNotNull(removed);
		pool.free(removed);
	}

	@Test
	void migrateToIncludeMultiStepLeftAndRight() {
		RingOrderBuffer ring = new RingOrderBuffer("ETHUSDT", 1, 100, 107);
		// 初始：low=100, high=107
		long low0 = ring.getLowPrice();
		long high0 = ring.getHighPrice();

		// 向左：引入 98，需要 k=2 步
		PriceLevel coldBid98 = new PriceLevel(98);
		List<PriceLevel> ev1 = ring.migrateToInclude(coldBid98);
		assertEquals(2, ev1.size());
		assertEquals(98, ring.getLowPrice());
		assertEquals(high0 - 2, ring.getHighPrice());
		// lastIdx 不因迁移改变
		// （无法直接断言 lastIdx 值，但应保持在旧值；此处验证不抛异常且窗口正确即可）

		// 向右：引入 110，需要 k=（110 - 105）=5 步
		// 现 high=105，low=98
		PriceLevel coldAsk110 = new PriceLevel(110);
		List<PriceLevel> ev2 = ring.migrateToInclude(coldAsk110);
		assertEquals(5, ev2.size());
		assertEquals(103, ring.getLowPrice());
		assertEquals(110, ring.getHighPrice());
	}
}
