package icu.match.core;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 热区环形价格缓冲（单线程）。
 * 数组长度为 2 的幂；每个槽位固定映射一个 price（price = initLow + i * step）。
 * 热窗口由 [lowIdx, highIdx] 描述，仅移动边界，不改变槽位 price。
 *
 * 左移/右移时：
 * - 返回被挤出窗口一侧的 PriceLevel；
 * - 将传入的冷区 PriceLevel 放到“被挤出对象原来的索引”上（满足你的交换语义）；
 * - 同时更新 lowIdx/highIdx 与 lowPrice/highPrice。
 * @author 中本君
 * @date 2025/8/12
 */
public final class RingOrderBuffer {

	@Getter
	private final String symbol;

	/**
	 * 价格步长（tick，>0）
	 */
	private final long step;

	/**
	 *  数组长度（2 的幂）
	 */
	@Getter
	private final int length;

	/**
	 * 环形掩码（length - 1）
	 */
	private final int mask;

	/**
	 * 槽位数组（构造时按 index→price 全量初始化）
	 */
	private final PriceLevel[] levels;

	/**
	 * 环形数组的左右边界索引。
	 */
	@Getter
	private int lowIdx;

	@Getter
	private int highIdx;

	/**
	 * 环形数组的左右边界价格。
	 */
	@Getter
	private long lowPrice;

	@Getter
	private long highPrice;

	/**
	 * 最近一次成交/访问位置
	 */
	@Getter
	private int lastIdx;

	@Getter
	private long lastPrice;

	/**
	 * 构造热区环形缓冲。
	 * 最终：数组长度为 2 的幂；lowIdx=0，highIdx=len-1，lastIdx=lowIdx。
	 */
	public RingOrderBuffer(String symbol, long step, long lowPrice, long highPrice) {
		if (symbol == null || symbol.isEmpty()) {
			throw new IllegalArgumentException("symbol must not be empty");
		}
		if (step <= 0) {
			throw new IllegalArgumentException("step must be > 0");
		}
		if (highPrice < lowPrice) {
			throw new IllegalArgumentException("highPrice must be >= lowPrice");
		}
		long span = highPrice - lowPrice;
		if (span % step != 0) {
			throw new IllegalArgumentException("price range must align to step");
		}

		this.symbol = symbol;
		this.step = step;

		int slots = Math.toIntExact(span / step + 1);
		int len = ceilPow2(slots);
		this.length = len;
		this.mask = len - 1;

		this.lowIdx = 0;
		this.highIdx = mask;

		// 全量初始化：index → price = lowPrice + i * step
		this.levels = new PriceLevel[len];
		for (int i = 0; i < len; i++) {
			long priceAtIdx = lowPrice + (long) i * step;
			levels[i] = new PriceLevel(priceAtIdx);
		}

		// 初始化窗口价格与最近访问
		this.lowPrice = levels[lowIdx].getPrice();
		this.highPrice = levels[highIdx].getPrice();
		this.lastIdx = this.lowIdx;
		this.lastPrice = this.lowPrice;
	}

	// ------------------------------------------------------------------
	// 基本操作
	// ------------------------------------------------------------------

	private static int ceilPow2(int n) {
		int x = 1;
		while (x < n) {
			x <<= 1;
		}
		return x;
	}

	/**
	 * 将订单提交到指定价格的价位桶（尾插）。
	 * 价格是否位于热区 由上层检查
	 */
	public void submit(long price, OrderNode node) {
		int idx = priceToIdx(price);
		levels[idx].submit(node);
	}

	/**
	 * 将价格映射到当前热区内的数组索引。
	 * <p>仅在“价格已保证位于热区 [lowPrice, highPrice] 且按 step 对齐”的场景下使用。</p>
	 *
	 * @param price 目标价格
	 * @return 对应的数组索引（环绕到 [0, length-1]）
	 * @throws IllegalArgumentException 当价格未按 step 对齐时抛出
	 */
	private int priceToIdx(long price) {
		assert price >= lowPrice && price <= highPrice : "price out of hot window";
		long delta = price - this.lowPrice;

		// 若你完全信任上层，这个对齐检查也可以移除以进一步提速
		if (delta % this.step != 0) {
			throw new IllegalArgumentException("price not aligned to step: " + price + ", step=" + step);
		}

		int ofs = (int) (delta / this.step);

		// 价格保证在热区范围内时，ofs ∈ [0, windowSize-1]
		// 当前实现窗口覆盖全数组，或滑窗后仍固定窗口宽度，因此直接环绕即可
		return (this.lowIdx + ofs) & this.mask;
	}

	/**
	 * 获取最优Bid挂单
	 */
	public PriceLevel getBidBestLevel() {
		int i = lastIdx;
		while (true) {
			PriceLevel lvl = levels[i];
			if (!lvl.isEmpty() && !lvl.isAsk()) {
				// 第一个非空 BID 即为最优（价格单调递增）
				return lvl;
			}
			if (i == lowIdx) {
				break;
			}
			i = (i - 1) & mask;
		}
		return null;
	}

	/**
	 * 获取最优Ask挂单
	 */
	public PriceLevel getAskBestLevel() {
		int i = lastIdx;
		while (true) {
			PriceLevel lvl = levels[i];
			if (!lvl.isEmpty() && lvl.isAsk()) {
				// 第一个非空 ASK 即为最优
				return lvl;
			}
			if (i == highIdx) {
				break;
			}
			i = (i + 1) & mask;
		}
		return null;
	}

	/**
	 * 撤单（不回收）：按价格与订单 ID 从对应价位摘除
	 * @param price 用于定位PriceLevel
	 * @param orderId 用于定位OrderNode
	 * @return OrderNode
	 */
	public OrderNode cancel(long price, long orderId) {
		int idx = priceToIdx(price);
		return levels[idx].cancel(orderId);
	}

	// ------------------------------------------------------------------
	// 窗口滑动 & 冷热交换（单步）
	// ------------------------------------------------------------------

	/**
	 * 完全成交删除（不回收）：按价格与订单 ID 从对应价位摘除
	 * @param price 用于定位PriceLevel
	 * @param orderId 用于定位OrderNode
	 * @return OrderNode
	 */
	public OrderNode remove(long price, long orderId) {
		int idx = priceToIdx(price);
		return levels[idx].remove(orderId);
	}

	/**
	 * 更新lastIdx和lastPrice
	 */
	public void recordTradePrice(long tradedPrice) {
		this.lastIdx = priceToIdx(tradedPrice);
		this.lastPrice = tradedPrice;
	}

	/**
	 * 让窗口一次性迁移到把 incoming（冷区档位）纳入热区的状态。
	 *
	 * 规则：
	 * - incoming.price < lowPrice  → 向左移动 k = (lowPrice - incoming.price)/step 步；
	 * - incoming.price > highPrice → 向右移动 k = (incoming.price - highPrice)/step 步；
	 * - 若落在热区内则不迁移，返回空列表。
	 *
	 * 过程中：
	 * - 每一步都会有一个热区边缘价位被挤出，按顺序加入返回集合；
	 * - 中间步（尚未到达 incoming.price）用“该步进入索引的【空 PriceLevel】”作为参数调用 shift*；
	 * - 最后一步用你传入的 incoming 作为参数（被放到“被挤出对象原索引”）。
	 *
	 * @param incoming 来自冷区的价位桶（不可为 null）
	 * @return 被挤出的 PriceLevel 列表（按逐出顺序）
	 */
	public List<PriceLevel> migrateToInclude(PriceLevel incoming) {
		if (incoming == null) {
			throw new IllegalArgumentException("incoming must not be null");
		}
		final long p = incoming.getPrice();
		final List<PriceLevel> evicted = new ArrayList<>();

		if (p < lowPrice) {
			long delta = lowPrice - p;
			if (delta % step != 0) {
				throw new IllegalArgumentException("incoming price not aligned to step: " + p);
			}
			int k = (int) (delta / step);
			// 前 k-1 步：用“该步的目标被挤出索引”的价格创建空档，作为 shiftLeft 的参数
			for (int i = 1; i < k; i++) {
				int evictIdx = highIdx;
				// 价格等于 evictIdx 的空 PriceLevel
				PriceLevel filler = newEmptyAtIndex(evictIdx);
				evicted.add(shiftLeft(filler));
			}
			// 第 k 步：把 incoming 放到“被挤出索引”
			evicted.add(shiftLeft(incoming));
		}
		else if (p > highPrice) {
			long delta = p - highPrice;
			if (delta % step != 0) {
				throw new IllegalArgumentException("incoming price not aligned to step: " + p);
			}
			int k = (int) (delta / step);
			for (int i = 1; i < k; i++) {
				int evictIdx = lowIdx;
				PriceLevel filler = newEmptyAtIndex(evictIdx);
				evicted.add(shiftRight(filler));
			}
			evicted.add(shiftRight(incoming));
		}
		return evicted;
	}

	/** 基于给定数组索引创建一个“空的 PriceLevel”（价格正确但队列为空）。 */
	private PriceLevel newEmptyAtIndex(int idx) {
		return new PriceLevel(indexToPrice(idx));
	}


	// ------------------------------------------------------------------
	// 打印
	// ------------------------------------------------------------------

	/**
	 * 向左滑动一个步长（整体价格下移）。
	 * 行为：
	 * 1) 取出当前 highIdx 的 PriceLevel（被挤出，返回给上层放入冷区树）。
	 * 2) 边界左移：lowIdx--, highIdx--（环绕）。
	 * 3) 将传入的 coldBid 放到“被挤出对象原来的索引”（旧 highIdx）上。
	 * 4) 更新 lowPrice / highPrice / lastIdx / lastPrice。
	 *
	 * @param coldBid 来自冷区的 BID 档位（可为 null；若非空需 price 与旧 highIdx 相等）
	 * @return 被挤出的最高价位（通常为 ASK 热端）
	 */
	private PriceLevel shiftLeft(PriceLevel coldBid) {
		if (coldBid == null) {
			throw new IllegalArgumentException("coldBid must not be null");
		}
		int evictIdx = highIdx;
		PriceLevel evicted = levels[evictIdx];

		// 左移窗口边界
		lowIdx = (lowIdx - 1) & mask;
		highIdx = (highIdx - 1) & mask;

		// 将传入的冷区档位放回“被挤出对象原位置”
		long expect = indexToPrice(evictIdx);
		if (coldBid.getPrice() != expect) {
			throw new IllegalArgumentException(
					"coldBid price mismatch: expect=" + expect + ", actual=" + coldBid.getPrice());
		}
		levels[evictIdx] = coldBid;

		// 仅更新窗口价格；不改 lastIdx/lastPrice（成交后另行设置）
		lowPrice = indexToPrice(lowIdx);
		highPrice = indexToPrice(highIdx);

		return evicted;
	}

	// ------------------------------------------------------------------
	// 内部工具
	// ------------------------------------------------------------------

	/**
	 * 向右滑动一个步长（整体价格上移）。
	 * 行为与 {@link #shiftLeft(PriceLevel)} 对称：
	 * 把传入的 coldAsk 放到“被挤出对象原来的索引”（旧 lowIdx）上。
	 *
	 * @param coldAsk 来自冷区的 ASK 档位（可为 null；若非空需 price 与旧 lowIdx 相等）
	 * @return 被挤出的最低价位（通常为 BID 热端）
	 */
	private PriceLevel shiftRight(PriceLevel coldAsk) {
		int evictIdx = lowIdx;
		PriceLevel evicted = levels[evictIdx];

		lowIdx = (lowIdx + 1) & mask;
		highIdx = (highIdx + 1) & mask;

		long expect = indexToPrice(evictIdx);
		if (coldAsk.getPrice() != expect) {
			throw new IllegalArgumentException(
					"coldAsk price mismatch: expect=" + expect + ", actual=" + coldAsk.getPrice());
		}
		levels[evictIdx] = coldAsk;

		lowPrice = indexToPrice(lowIdx);
		highPrice = indexToPrice(highIdx);

		return evicted;
	}

	/** index → price（固定映射）。 */
	private long indexToPrice(int idx) {
		return levels[idx].getPrice();
	}

	public String dump() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("RingOrderBuffer{symbol=")
		  .append(symbol)
		  .append(", step=")
		  .append(step)
		  .append(", length=")
		  .append(length)
		  .append(", window=[")
		  .append(lowPrice)
		  .append("..")
		  .append(highPrice)
		  .append("]")
		  .append(", lowIdx=")
		  .append(lowIdx)
		  .append(", highIdx=")
		  .append(highIdx)
		  .append(", lastIdx=")
		  .append(lastIdx)
		  .append(", lastPrice=")
		  .append(lastPrice)
		  .append("}\n");

		int i = lowIdx, shown = 0, limit = 64;
		while (true) {
			PriceLevel p = levels[i];
			if (!p.isEmpty()) {
				sb.append("  [idx=")
				  .append(i)
				  .append(", price=")
				  .append(p.getPrice())
				  .append(", side=")
				  .append(p.isAsk()
						  ? "ASK"
						  : "BID")
				  .append(", size=")
				  .append(p.size())
				  .append(", totalQty=")
				  .append(p.totalQty())
				  .append("]\n");
				if (++shown >= limit) {
					break;
				}
			}
			if (i == highIdx) {
				break;
			}
			i = (i + 1) & mask;
		}
		if (shown == 0) {
			sb.append("  <empty>\n");
		}
		return sb.toString();
	}

	/** idx 是否在当前窗口内。 */
	private boolean inWindow(int idx) {
		int i = lowIdx;
		while (true) {
			if (i == idx) {
				return true;
			}
			if (i == highIdx) {
				return false;
			}
			i = (i + 1) & mask;
		}
	}
}
