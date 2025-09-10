package icu.match.core;

import com.alibaba.fastjson2.JSON;

import icu.match.common.OrderSide;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@Slf4j
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
	private int lowIdx;

	private int highIdx;

	/**
	 * 环形数组的左右边界价格。
	 */
	@Getter
	private long lowPrice;

	@Getter
	private long highPrice;

	/**
	 * 最近一次maker被成交 或最优买1卖价位置
	 * maker被成交 将调用remove 或patchQty函数
	 * 最优价更新 submit
	 */
	private int lastIdx;

	private long lastPrice;

	private int bestBidIdx;

	private long bestBidPrice;

	private int bestAskIdx;

	private long bestAskPrice;

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
		// 保持初始数组的价格升序排序
		this.levels = new PriceLevel[len];
		for (int i = 0; i < len; i++) {
			long priceAtIdx = lowPrice + (long) i * step;
			levels[i] = new PriceLevel(priceAtIdx);
		}

		// 初始化窗口价格与最近访问
		this.lowPrice = levels[lowIdx].getPrice();
		this.highPrice = levels[highIdx].getPrice();

		// 初始最优bidIdx为 highIdx  在第一次get bestBidLevel时 会从高往低查找
		this.bestBidIdx = highIdx;
		this.bestBidPrice = Long.MIN_VALUE;

		// 初始最优AskIdx为 lowIdx  在第一次get bestAskLevel时 会从低往高查找
		this.bestAskIdx = lowIdx;
		this.bestAskPrice = Long.MAX_VALUE;
	}

	// ------------------------------------------------------------------
	// 核心接口
	// ------------------------------------------------------------------

	private int ceilPow2(int n) {
		int x = 1;
		while (x < n) {
			x <<= 1;
		}
		return x;
	}

	/**
	 * 订单提交
	 * 将订单提交到指定价格的价位桶（尾插）。
	 * 价格是否位于热区 由上层检查
	 */
	public void submit(long price, OrderNode node) {
		int idx = getIdxByPrice(price);
		PriceLevel level = levels[idx];
		if (level.isEmpty()) {
			// 当前level 开始有数据 尝试更新买1卖1价
			updateBestOnAdded(node.ask, price);
		}
		level.submit(node);
	}

	/**
	 * 基于price获取其idx
	 *
	 * 将价格映射到当前热区内的数组索引。
	 * <p>仅在“价格已保证位于热区 [lowPrice, highPrice] 且按 step 对齐”的场景下使用。</p>
	 *
	 * @param price 目标价格
	 * @return 对应的数组索引（环绕到 [0, length-1]）
	 * @throws IllegalArgumentException 当价格未按 step 对齐时抛出
	 */
	private int getIdxByPrice(long price) {
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
	 * 更新最优买1卖1
	 */
	private void updateBestOnAdded(boolean ask, long price) {
		int idx = getIdxByPrice(price);
		if (ask && price < this.bestAskPrice) {
			// 新增订单为ask 且卖价更优 尝试更新卖1价格
			log.info("updateBestOnAdded, bestAskIdx [{} -> {}] bestAskPrice [{} -> {}] ", bestAskIdx, idx,
					 bestAskPrice,
					 price);
			this.bestAskPrice = price;
			this.bestAskIdx = idx;
		} else if (!ask && price > this.bestBidPrice) {
			// 新增订单为bid 且买价更优 尝试更新买1价格
			log.info("updateBestOnAdded, bestBidIdx [{} -> {}] bestBidPrice [{} -> {}] ", bestBidIdx, idx,
					 bestBidPrice,
					 price);
			this.bestBidPrice = price;
			this.bestBidIdx = idx;
		}
	}

	/**
	 * 撤单
	 * 按价格与订单 ID 从对应价位摘除
	 * @param price 用于定位PriceLevel
	 * @param orderId 用于定位OrderNode
	 * @return OrderNode
	 */
	public OrderNode cancel(long price, long orderId) {
		int idx = getIdxByPrice(price);
		OrderNode cancel = levels[idx].cancel(orderId);
		if (cancel != null && levels[idx].isEmpty()) {
			// 成功撤单后 该槽为空 尝试更新最优买1卖1价格
			updateBestOnRemoved(cancel.ask, idx);
		}
		return cancel;
	}

	/**
	 * 获取最优流动性层
	 * 基于订单方向 获取对手方最优流动性层
	 * @param side take订单方向
	 * @return make方向最优流动性层
	 */
	public PriceLevel getBestLevel(OrderSide side) {
		return side.isAsk()
			   ? getBestBidLevel()
			   : getBestAskLevel();
	}

	/**
	 * 获取最优Bid挂单
	 */
	private PriceLevel getBestBidLevel() {
		int i = bestBidIdx;
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

	// ------------------------------------------------------------------
	// 工具接口
	// ------------------------------------------------------------------

	/**
	 * 获取最优Ask挂单
	 */
	private PriceLevel getBestAskLevel() {
		int i = bestAskIdx;
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
	 * 获取可与taker价格立即撮合的总数量
	 * @param takerSide taker方向
	 * @param takerPrice taker价格
	 * @return
	 */
	public long getTotalQty(OrderSide takerSide, long takerPrice) {
		long rlt = 0;
		if (takerSide.isAsk()) {
			// ask
			int idx = bestBidIdx;
			while (getLeftIdx(idx) != bestAskIdx) {
				PriceLevel level = levels[idx];
				if (level.getPrice() < takerPrice) {
					break;
				}
				rlt += level.totalQty();
				idx = getLeftIdx(idx);
			}
		} else {
			// bid
			int idx = bestAskIdx;
			while (getLeftIdx(idx) != bestBidIdx) {
				PriceLevel level = levels[idx];
				if (level.getPrice() > takerPrice) {
					break;
				}
				rlt += levels[idx].totalQty();
				idx = getRightIdx(idx);
			}
		}
		return rlt;
	}

	/**
	 * 当最优流动性的最后一个挂单删除时 更新索引
	 * 被撤单 被吃单
	 *
	 */
	private void updateBestOnRemoved(boolean ask, int idx) {
		long price = getPriceByIdx(idx);
		if (ask && idx == bestAskIdx) {
			// 撤的是最优卖单价格档位的最后一个挂单
			// 向右 尝试找到第一个不为空的卖单价格槽
			while (levels[idx].isEmpty() && idx != highIdx) {
				idx = getRightIdx(idx);
			}
			log.info("updateBestOnRemoved, bestAskIdx [{} -> {}] bestAskPrice [{} -> {}] ", bestAskIdx, idx,
					 bestAskPrice, price);
			this.bestAskIdx = idx;
			this.bestAskPrice = price;
		} else if (!ask && idx == bestBidIdx) {
			// 撤的是最优买单价格档位的最后一个挂单
			// 向左 尝试找到第一个不为空的买单价格槽
			while (levels[idx].isEmpty() && idx != lowIdx) {
				idx = getLeftIdx(idx);
			}
			log.info("updateBestOnRemoved, bestBidIdx [{} -> {}] bestBidPrice [{} -> {}] ", bestBidIdx, idx,
					 bestBidPrice, price);
			this.bestBidIdx = idx;
			this.bestBidPrice = price;
		}
	}


	// ------------------------------------------------------------------
	// 核心私有函数
	// ------------------------------------------------------------------

	/**
	 * 完全成交删除
	 * 按价格与订单 ID 从对应价位摘除
	 * @param price 用于定位PriceLevel
	 * @param orderId 用于定位OrderNode
	 * @return OrderNode
	 */
	public OrderNode remove(long price, long orderId) {
		int idx = getIdxByPrice(price);
		OrderNode remove = levels[idx].remove(orderId);
		if (remove != null && levels[idx].isEmpty()) {
			// 成功删除后 该槽为空 尝试更新最优买1卖1价格
			updateBestOnRemoved(remove.ask, idx);
		}
		return remove;
	}

	/**
	 * 修改订单数量
	 *
	 * @param price 定位订单槽
	 * @param orderId 订单Id
	 * @param newQty 新数量
	 */
	public boolean patchQty(long price, long orderId, long newQty) {
		return levels[getIdxByPrice(price)].patchQty(orderId, newQty);
	}

	/**
	 * 检查价格是否位于热区
	 */
	public boolean isWindow(long price) {
		return lowPrice <= price && price <= highPrice;
	}

	/**
	 * 获取最优bid ask价格
	 */
	public long bestBidPrice() {
		PriceLevel bestLevel = this.getBestBidLevel();
		if (bestLevel == null) {
			return Long.MIN_VALUE;
		}
		return bestLevel.getPrice();
	}

	public long bestAskPrice() {
		PriceLevel bestLevel = this.getBestAskLevel();
		if (bestLevel == null) {
			return Long.MAX_VALUE;
		}
		return bestLevel.getPrice();
	}

	/**
	 * 计算当前Bid区所占百分比
	 */
	public double getSlopeRate() {
		final int forward = (bestBidIdx - lowIdx) & mask;
		return (forward * 100.0) / mask;
	}


	// ------------------------------------------------------------------
	// 窗口滑动和快照相关接口
	// ------------------------------------------------------------------

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
	public List<PriceLevel> migrate(PriceLevel incoming) {
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
				// 将lowP 到 incoming.price之间 的空隙
				PriceLevel filler = new PriceLevel(lowPrice - step);
				evicted.add(shiftLeft(filler));
			}
			// 第 k 步：把 incoming 放到“被挤出索引”
			evicted.add(shiftLeft(incoming));
		} else if (p > highPrice) {
			long delta = p - highPrice;
			if (delta % step != 0) {
				throw new IllegalArgumentException("incoming price not aligned to step: " + p);
			}
			int k = (int) (delta / step);
			for (int i = 1; i < k; i++) {
				// 将incoming.price到lowP之间 的空隙
				PriceLevel filler = new PriceLevel(highPrice + step);
				evicted.add(shiftRight(filler));
			}
			evicted.add(shiftRight(incoming));
		}
		return evicted;
	}

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

		levels[evictIdx] = coldBid;

		// 仅更新窗口价格；不改 lastIdx/lastPrice（成交后另行设置）
		lowPrice = getPriceByIdx(lowIdx);
		highPrice = getPriceByIdx(highIdx);

		return evicted;
	}

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

		levels[evictIdx] = coldAsk;

		lowPrice = getPriceByIdx(lowIdx);
		highPrice = getPriceByIdx(highIdx);

		return evicted;
	}

	/**
	 * 基于idx获取其price
	 */
	private long getPriceByIdx(int idx) {
		return levels[idx].getPrice();
	}

	/**
	 * 更新lastIdx和lastPrice
	 */
	@Deprecated
	public void recordTradePrice(long tradedPrice) {
		this.lastIdx = getIdxByPrice(tradedPrice);
		this.lastPrice = tradedPrice;
	}

	/**
	 * 快照
	 */
	public String snapshot() {

		Map<String, Long> bids = new HashMap<>();
		Map<String, Long> asks = new HashMap<>();

		for (int i = lowIdx; i != highIdx; i = getRightIdx(i)) {
			PriceLevel lvl = levels[i];
			if (lvl.isEmpty()) {
				continue;
			}
			long price = lvl.getPrice();
			long qty = lvl.totalQty();
			if (lvl.isAsk()) {
				asks.put(String.valueOf(price), qty);
			} else {
				bids.put(String.valueOf(price), qty);
			}
		}
		return JSON.toJSONString(Arrays.asList(bids, asks));
	}

	private int getRightIdx(int idx) {
		return (idx + 1) & mask;
	}

	private int getLeftIdx(int idx) {
		return (idx - 1) & mask;
	}
}
