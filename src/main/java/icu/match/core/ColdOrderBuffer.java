package icu.match.core;/**
 *
 * @author 中本君
 * @date 2025/8/16
 */

import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *  * 冷区价格簿：ASK/BID 各一棵红黑树（TreeMap）。
 *  * <p>只存“非空”的价位桶；空档不入树，避免膨胀。</p>
 *  *
 *  * 能力边界：
 *  * <ul>
 *  *   <li>submit/cancel/remove：当价格不在热区时由撮合层调用，落到冷区。</li>
 *  *   <li>best/peek/pop：获取或弹出最优档（红黑树头部）。</li>
 *  *   <li>takeExact / takeExactOrEmpty：按精确价格取出（删除），无则返回空价位，满足迁移参数非 null。</li>
 *  *   <li>migrateIncludeToRing：用传入的冷区档位驱动热区滑窗（ring.migrateToInclude），并接收被逐出的热档。</li>
 *  *   <li>dump/统计：可视化与监控。</li>
 *  * </ul>
 *
 * @author 中本君
 * @date 2025/8/16 
 */
public class ColdOrderBuffer {

	/** ASK 按升序，bestAsk = firstEntry()。 */
	private final NavigableMap<Long, PriceLevel> asks = new TreeMap<>();

	/** BID 按降序，bestBid = firstEntry()。 */
	private final NavigableMap<Long, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());


	// ----------------------------------------------------------------------
	// 基础：落单 / 撤单 / 完全成交
	// ----------------------------------------------------------------------

	/**
	 * 提交订单到冷区（价格在热区外时由撮合层调用）。
	 * <p>若该价位不存在则创建一个新的 PriceLevel（方向由首单决定）。</p>
	 */
	public void submit(long price, OrderNode node) {
		if (node == null) {
			throw new IllegalArgumentException("node must not be null");
		}
		PriceLevel lvl = getOrCreate(price, node.ask);
		lvl.submit(node);
		// 新建或从空转非空时，需要放入树
		if (lvl.size() == 1) {
			putInternal(lvl);
		}
	}

	/** 若该价位不存在则创建一个空价位（首次提交时确定方向）；存在则返回已有。 */
	private PriceLevel getOrCreate(long price, boolean ask) {
		PriceLevel lvl = ask
						 ? asks.get(price)
						 : bids.get(price);
		if (lvl != null) {
			return lvl;
		}
		// 不存在则新建（暂未入树，等首次 submit 后 size=1 时 putInternal）
		return new PriceLevel(price);
	}

	/** 把一个非空价位插入树（按方向/价格放到正确位置）。 */
	private void putInternal(PriceLevel level) {
		if (level == null) {
			throw new IllegalArgumentException("level must not be null");
		}
		final long price = level.getPrice();
		if (level.isAsk()) {
			PriceLevel prev = asks.put(price, level);
			if (prev != null) {
				throw new IllegalStateException("ASK level already exists at price=" + price);
			}
		}
		else {
			PriceLevel prev = bids.put(price, level);
			if (prev != null) {
				throw new IllegalStateException("BID level already exists at price=" + price);
			}
		}
	}

	// ----------------------------------------------------------------------
	// 取/放价位桶（对象所有权转移）
	// ----------------------------------------------------------------------

	/**
	 * 冷区撤单：按方向与价格定位到价位桶后撤单；若价位桶转空则从树中移除。
	 */
	public OrderNode cancel(long price, long orderId, boolean ask) {
		PriceLevel lvl = (ask
						  ? asks.get(price)
						  : bids.get(price));
		if (lvl == null) {
			return null;
		}
		OrderNode n = lvl.cancel(orderId);
		if (n != null && lvl.isEmpty()) {
			removeLevel(price, ask);
		}
		return n;
	}

	/** 从树中移除一个价位桶（价位已为空）。 */
	private void removeLevel(long price, boolean ask) {
		if (ask) {
			asks.remove(price);
		}
		else {
			bids.remove(price);
		}
	}

	// ----------------------------------------------------------------------
	// 最优价：查询 / 弹出
	// ----------------------------------------------------------------------

	/**
	 * 冷区完全成交删除：与 {@link #cancel} 一致，语义区分。
	 */
	public OrderNode remove(long price, long orderId, boolean ask) {
		PriceLevel lvl = (ask
						  ? asks.get(price)
						  : bids.get(price));
		if (lvl == null) {
			return null;
		}
		OrderNode n = lvl.remove(orderId);
		if (n != null && lvl.isEmpty()) {
			removeLevel(price, ask);
		}
		return n;
	}

	/** 放回一个价位桶到冷区（非空才入树；空则忽略）。 */
	public void put(PriceLevel level) {
		if (level == null) {
			throw new IllegalArgumentException("level must not be null");
		}
		if (level.isEmpty()) {
			return; // 不存空档
		}
		putInternal(level);
	}

	/** 精确取出并删除（不存在返回 null）。 */
	public PriceLevel takeExact(long price, boolean ask) {
		return ask
			   ? asks.remove(price)
			   : bids.remove(price);
	}

	public PriceLevel bestAsk() {
		var e = asks.firstEntry();
		return e != null
			   ? e.getValue()
			   : null;
	}

	public PriceLevel bestBid() {
		var e = bids.firstEntry();
		return e != null
			   ? e.getValue()
			   : null;
	}


	// ----------------------------------------------------------------------
	// 与热区协作：一次迁移并将被逐出价位回灌冷区
	// ----------------------------------------------------------------------


	// ----------------------------------------------------------------------
	// 工具 / 维护
	// ----------------------------------------------------------------------

	/** 弹出最优 ASK（删除并返回，可能为 null）。 */
	public PriceLevel popBestAsk() {
		var e = asks.pollFirstEntry();
		if (e == null) {
			return null;
		}
		return e.getValue();
	}

	/** 弹出最优 BID（删除并返回，可能为 null）。 */
	public PriceLevel popBestBid() {
		var e = bids.pollFirstEntry();
		if (e == null) {
			return null;
		}
		return e.getValue();
	}

	// ColdOrderBuffer
	public void putAll(Collection<PriceLevel> levels) {
		if (levels == null) {
			return;
		}
		for (PriceLevel lvl : levels) {
			if (lvl != null && !lvl.isEmpty()) {
				putInternal(lvl);
			}
		}
	}

	/** 清理：把内部已转空的价位（如果外部绕过 cancel/remove 导致）从树里剔除。 */
	public void vacuum() {
		asks.entrySet()
			.removeIf(e -> {
				return e.getValue()
						.isEmpty();
			});
		bids.entrySet()
			.removeIf(e -> {
				return e.getValue()
						.isEmpty();
			});
	}


	public int sizeAsks() {return asks.size();}

	public int sizeBids() {return bids.size();}

	/** 简便重载：默认每侧展示前 32 档。 */
	public String dump() {return dump(32);}

	/** 冷区快照（仅列出前若干最优档，避免过长）。 */
	public String dump(int limitPerSide) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("ColdPriceBook{")
		  .append("asks=")
		  .append(asks.size())
		  .append(", bids=")
		  .append(bids.size())
		  .append("}\n  ASKS: ");
		int shown = 0;
		for (var e : asks.entrySet()) {
			PriceLevel lvl = e.getValue();
			sb.append(e.getKey())
			  .append("(n=")
			  .append(lvl.size())
			  .append(",qty=")
			  .append(lvl.totalQty())
			  .append(") ");
			if (++shown >= limitPerSide) {
				break;
			}
		}
		sb.append("\n  BIDS: ");
		shown = 0;
		for (var e : bids.entrySet()) {
			PriceLevel lvl = e.getValue();
			sb.append(e.getKey())
			  .append("(n=")
			  .append(lvl.size())
			  .append(",qty=")
			  .append(lvl.totalQty())
			  .append(") ");
			if (++shown >= limitPerSide) {
				break;
			}
		}
		return sb.toString();
	}

}
