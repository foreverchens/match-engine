package icu.match.core;

import icu.match.core.interfaces.MatchSink;

import java.util.Objects;

/**
 * 单线程撮合引擎（基于热区环形数组 + 冷区红黑树）。
 * <p>支持 LIMIT + {GTC, IOC}。只有 GTC 会入簿持久；IOC 未成交余量直接丢弃。</p>
 *
 * 设计约束：
 * <ul>
 *   <li>仅在热区窗口内撮合与落单；窗口外：GTC→落冷区，IOC→丢弃。</li>
 *   <li>成交价使用对手方价位（被动方档位）。</li>
 *   <li>撮合过程中仅通过 PriceLevel.patchQty/remove/cancel 保持聚合统计正确。</li>
 *   <li>每次撮合后调用 recenter.checkAndRecenter()，不修改 lastIdx/lastPrice 的约束由 ring 内部保证。</li>
 * </ul>
 * @author 中本君
 * @date 2025/8/16
 */
public final class MatchingEngine {

	private final String symbol;

	private final RingOrderBuffer ring;

	private final ColdOrderBuffer cold;

	private final RecenterManager recenter;

	private final OrderNodePoolFixed pool;

	private final MatchSink matchSink;

	public MatchingEngine(RingOrderBuffer ring, ColdOrderBuffer cold, OrderNodePoolFixed pool, MatchSink matchSink) {
		this.symbol = ring.getSymbol();
		this.ring = Objects.requireNonNull(ring, "ring");
		this.cold = Objects.requireNonNull(cold, "cold");
		this.recenter = new RecenterManager(ring, cold);
		this.pool = Objects.requireNonNull(pool, "pool");
		this.matchSink = matchSink;
	}

	/**
	 * 提交一张限价单（LIMIT + {GTC, IOC}）。
	 *
	 * @param orderId  订单ID（唯一）
	 * @param userId   用户ID
	 * @param side     方向（BID=买、ASK=卖）
	 * @param price    价格（按 tick 缩放的 long）
	 * @param qty      数量（按 tick 缩放的 long，>0）
	 * @param tif      时效（GTC 入簿；IOC 仅撮合、余量丢弃）
	 * @return 剩余未成交数量（IOC 可能 >0 但不会入簿；GTC >0 则已入热/冷簿）
	 */
	public long submitLimit(long orderId, long userId, Side side, long price, long qty, TIF tif) {
		if (qty <= 0) {
			throw new IllegalArgumentException("qty must be > 0");
		}
		final boolean isAsk = (side == Side.ASK);

		long remaining = qty;

		// 先在热区尝试撮合
		if (priceInHot(price)) {
			if (isAsk) {
				remaining = matchSellAsTaker(orderId, userId, price, remaining);
			}
			else {
				remaining = matchBuyAsTaker(orderId, userId, price, remaining);
			}
		}

		// 若有剩余
		if (remaining > 0) {
			if (tif == TIF.GTC) {
				// GTC：在热区则挂单，否则落冷区
				if (priceInHot(price)) {
					OrderNode node = pool.alloc(orderId, userId, isAsk, remaining);
					ring.submit(price, node);
				}
				else {
					OrderNode node = pool.alloc(orderId, userId, isAsk, remaining);
					cold.submit(price, node);
				}
			}
			else {
				// IOC：余量丢弃
				// （也可改成：若在热区外且对手侧可成交，则先拉窗后撮合；本实现保持简单不迁移。）
			}
		}

		return remaining;
	}

	private boolean priceInHot(long price) {
		long low = ring.getLowPrice();
		long high = ring.getHighPrice();
		return price >= low && price <= high;
	}

	// ----------------------------------------------------------------------------
	// 入单入口
	// ----------------------------------------------------------------------------

	/**
	 * 卖单撮合：与最优 BID 对手逐价撮合，直到价格不再可成交或数量耗尽。
	 * 成交条件：bestBidPrice >= askPrice
	 */
	private long matchSellAsTaker(long takerOrderId, long takerUserId, long askPrice, long qty) {
		while (qty > 0) {
			PriceLevel bestBid = ring.getBidBestLevel();
			if (bestBid == null) {
				break; // 无对手
			}
			long bidPrice = bestBid.getPrice();
			if (bidPrice < askPrice) {
				break; // 不可成交
			}

			// 取价位头部订单（先进先出）
			OrderNode maker = bestBid.getFirst();
			if (maker == null) {
				break; // 理论不应发生
			}

			long tradeQty = Math.min(qty, maker.qty);
			long newMakerQty = maker.qty - tradeQty;

			// 回调成交（taker=卖）

			// 记录最新成交价以利下一次扫描起点
			ring.recordTradePrice(bidPrice);

			// 修改 maker 数量或摘除
			if (newMakerQty == 0) {
				// 完全成交：按 orderId 删除并可回收节点
				OrderNode removed = bestBid.remove(maker.orderId);
				if (removed != null) {
					pool.free(removed);
				}
			}
			else {
				// 部分：patch 数量（保持聚合统计一致）
				boolean ok = bestBid.patchQty(maker.orderId, newMakerQty);
				if (!ok) {
					throw new IllegalStateException("patch maker failed: " + maker.orderId);
				}
			}

			// 扣减 taker
			qty -= tradeQty;

			// 每次成交后做一次轻量再平衡
			recenter.checkAndRecenter();
		}
		return qty;
	}

	/**
	 * 买单撮合：与最优 ASK 对手逐价撮合，直到价格不再可成交或数量耗尽。
	 * 成交条件：askPrice <= bidPrice（此处写作 bestAskPrice <= bidLimitPrice）
	 */
	private long matchBuyAsTaker(long takerOrderId, long takerUserId, long bidPrice, long qty) {
		while (qty > 0) {
			PriceLevel bestAsk = ring.getAskBestLevel();
			if (bestAsk == null) {
				break; // 无对手
			}
			long askPrice = bestAsk.getPrice();
			if (askPrice > bidPrice) {
				break; // 不可成交
			}

			OrderNode maker = bestAsk.getFirst();
			if (maker == null) {
				break;
			}

			long tradeQty = Math.min(qty, maker.qty);
			long newMakerQty = maker.qty - tradeQty;

			// 回调成交（taker=买）
			ring.recordTradePrice(askPrice);

			if (newMakerQty == 0) {
				OrderNode removed = bestAsk.remove(maker.orderId);
				if (removed != null) {
					pool.free(removed);
				}
			}
			else {
				boolean ok = bestAsk.patchQty(maker.orderId, newMakerQty);
				if (!ok) {
					throw new IllegalStateException("patch maker failed: " + maker.orderId);
				}
			}

			qty -= tradeQty;

			recenter.checkAndRecenter();
		}
		return qty;
	}

	// ----------------------------------------------------------------------------
	// 内部：撮合实现
	// ----------------------------------------------------------------------------

	/**
	 * 撤单：优先尝试热区；未命中再尝试冷区。
	 *
	 * @return 被撤的节点（热区或冷区），若不存在返回 null；若存在，调用方需决定是否回收 node。
	 */
	public OrderNode cancel(long price, long orderId, Side side) {
		if (priceInHot(price)) {
			OrderNode n = ring.cancel(price, orderId);
			if (n != null) {
				return n;
			}
		}
		// 冷区需要方向来定位树
		boolean ask = (side == Side.ASK);
		return cold.cancel(price, orderId, ask);
	}

	/** 撮合侧别（true=ASK 卖，false=BID 买）。 */
	public enum Side {ASK, BID}

	// ----------------------------------------------------------------------------
	// 工具
	// ----------------------------------------------------------------------------

	/** 时效策略。 */
	public enum TIF {GTC, IOC}
}
