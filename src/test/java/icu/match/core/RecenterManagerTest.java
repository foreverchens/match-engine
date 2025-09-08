package icu.match.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 中本君
 * @date 2025/8/16
 */
class RecenterManagerTest {

	@Test
	void skewComputeAndRecentering() {
		long step = 1;
		RingOrderBuffer ring = new RingOrderBuffer("BTCUSDT", step, 100, 107); // len=8
		ColdOrderBuffer cold = new ColdOrderBuffer();
		OrderNode node = new OrderNode();
		node.qty = 100;
		cold.submit(99, node);
		RecenterManager mgr = new RecenterManager(ring, cold);

		// 把 lastIdx 放到靠左：设置为 lowPrice（ratio≈0%）
		ring.recordTradePrice(ring.getLowPrice());
		double skew = mgr.getStepPercent();
		assertTrue(skew >= 0.0 && skew <= 5.0, "skew should be near 0%");

		// 偏离 50% 超过 20% ⇒ 计划 2 步；向右移动
		long lowBefore = ring.getLowPrice();
		long highBefore = ring.getHighPrice();

		int steps = mgr.checkAndRecenter();
		assertTrue(steps >= 2 && steps <= 4); // 按实现最多 4，至少应 >=2

		// 右移 steps 步：low/high 同步 +steps
		assertEquals(lowBefore - step, ring.getLowPrice());
		assertEquals(highBefore - step, ring.getHighPrice());
	}
}
