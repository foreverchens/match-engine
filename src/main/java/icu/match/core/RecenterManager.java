package icu.match.core;

import lombok.Getter;

import java.util.List;

/**
 * 热区再平衡管理器（单线程）。
 * <p>依据 lastIdx 相对 lowIdx 的位置比（0%..100%），将热区窗口向左/右微调，维持“成交枢轴”居中。</p>
 *
 * 判定策略：
 * <ul>
 *   <li>位置比 ratio = ((lastIdx - lowIdx) & mask) / (length - 1) * 100。</li>
 *   <li>目标中心默认 50%，偏离阈值按 10% 为一个档：偏离 X% ⇒ 执行 floor(X/10) 次单步迁移，最多 4 次。</li>
 *   <li>ratio &lt; target ⇒ 窗口左移（引入更低的价，增大 ratio）；ratio &gt; target ⇒ 窗口右移（引入更高的价，减小 ratio）。</li>
 * </ul>
 *
 * 迁移细节：
 * <ul>
 *   <li>每步迁移均为一次冷热交换：取“将要进入窗口一侧”的精确价位，如冷区不存在则用同价位的空 PriceLevel 占位。</li>
 *   <li>左移一步：进入价 = 当前 highPrice（ASK）；右移一步：进入价 = 当前 lowPrice（BID）。</li>
 *   <li>调用 {@link RingOrderBuffer#migrate(PriceLevel)} 执行一步迁移，并把被逐出的价位回灌冷区。</li>
 *   <li>不修改 lastIdx/lastPrice（仍然只在成交后由撮合核调用 recordTradePrice 更新）。</li>
 * </ul>
 */
public final class RecenterManager {

	private final RingOrderBuffer ring;

	private final ColdOrderBuffer cold;

	/** 目标中心百分比（0..100），默认 50。 */
	@Getter
	private final double targetCenterPercent = 50.0;

	/** 每 10% 偏离执行 1 次迁移。 */
	@Getter
	private final double stepPercent = 10.0;

	/** 单次调用的最大迁移步数上限，默认 4。 */
	@Getter
	private final int maxStepsPerCall = 4;

	public RecenterManager(RingOrderBuffer ring, ColdOrderBuffer cold) {
		if (ring == null) {
			throw new IllegalArgumentException("ring must not be null");
		}
		if (cold == null) {
			throw new IllegalArgumentException("cold must not be null");
		}
		this.ring = ring;
		this.cold = cold;
	}


	/**
	 * 检查是否需要再平衡；若需要，则按偏离程度执行 1..4 次单步冷热交换。
	 *
	 * @return 实际执行的迁移步数
	 */
	public int checkAndRecenter() {
		final double ratio = ring.getSlopeRate();
		final double deviation = Math.abs(ratio - targetCenterPercent);
		int planned = (int) Math.floor(deviation / stepPercent);
		if (planned <= 0) {
			return 0;
		}
		if (planned > maxStepsPerCall) {
			planned = maxStepsPerCall;
		}

		// ratio < target ⇒ 左移；ratio > target ⇒ 右移
		final boolean moveLeft = ratio < targetCenterPercent;

		int done = 0;
		for (int i = 0; i < planned; i++) {
			if (moveLeft) {
				// 进入价位 = 当前 highPrice，方向 ASK
				PriceLevel incoming = cold.popBestBid();
				// 当冷区一直没数据 且热区一直偏移时
				// 此刻新订单提交时即使离市价很近 也会先添加到冷区 然后才迁移到热区
				if (incoming != null) {
					List<PriceLevel> evicted = ring.migrate(incoming);
					putBackNonEmpty(evicted);
				}
			}
			else {
				// 进入价位 = 当前 lowPrice，方向 BID
				PriceLevel incoming = cold.popBestAsk();
				if (incoming != null) {
					List<PriceLevel> evicted = ring.migrate(incoming);
					putBackNonEmpty(evicted);
				}
			}
			done++;
		}
		return done;
	}


	/**
	 * 将被逐出的热档回灌冷区（忽略空档）。
	 */
	private void putBackNonEmpty(List<PriceLevel> evicted) {
		if (evicted == null || evicted.isEmpty()) {
			return;
		}
		for (PriceLevel out : evicted) {
			if (out != null && !out.isEmpty()) {
				cold.put(out);
			}
		}
	}
}
