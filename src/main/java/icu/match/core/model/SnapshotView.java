package icu.match.core.model;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
@Data
public class SnapshotView {

	public List<Long> bidPrices;

	public List<Long> bidUserIds;

	public List<Long> bidOrderIds;

	public List<Long> bidQtyList;


	public List<Long> askPrices;

	public List<Long> askOrderIds;

	public List<Long> askUserIds;

	public List<Long> askQtyList;

	public SnapshotView() {
		this.bidPrices = new ArrayList<>();
		this.bidUserIds = new ArrayList<>();
		this.bidOrderIds = new ArrayList<>();
		this.bidQtyList = new ArrayList<>();

		this.askPrices = new ArrayList<>();
		this.askOrderIds = new ArrayList<>();
		this.askUserIds = new ArrayList<>();
		this.askQtyList = new ArrayList<>();

	}


	public void appendBid(long price, long userId, long orderId, long qty) {
		bidPrices.add(price);
		bidUserIds.add(userId);
		bidOrderIds.add(orderId);
		bidQtyList.add(qty);
	}

	public void appendAsk(long price, long userId, long orderId, long qty) {
		askPrices.add(price);
		askUserIds.add(userId);
		askOrderIds.add(orderId);
		askQtyList.add(qty);
	}

	/** 默认展示前 10 档 */
	public String view() {
		return view(10, 24);
	}

	/**
	 * @param levels 展示买卖各前多少档（价位聚合后）
	 * @param barWidth ASCII 深度条宽度
	 */
	public String view(int levels, int barWidth) {
		Objects.requireNonNull(bidPrices, "bidPrices");
		Objects.requireNonNull(bidQtyList, "bidQtyList");
		Objects.requireNonNull(askPrices, "askPrices");
		Objects.requireNonNull(askQtyList, "askQtyList");

		// 1) 按价位聚合数量（买/卖）
		Map<Long, Long> bidLevels = aggregateByPrice(bidPrices, bidQtyList);
		Map<Long, Long> askLevels = aggregateByPrice(askPrices, askQtyList);

		// 2) 排序：买降、卖升
		List<Map.Entry<Long, Long>> bid = sortLevels(bidLevels, true);
		List<Map.Entry<Long, Long>> ask = sortLevels(askLevels, false);

		// 3) 取前 N 档 & 计算可视化比例
		bid = bid.subList(0, Math.min(levels, bid.size()));
		ask = ask.subList(0, Math.min(levels, ask.size()));

		long maxQty = 1;
		for (Map.Entry<Long, Long> e : bid) maxQty = Math.max(maxQty, e.getValue());
		for (Map.Entry<Long, Long> e : ask) maxQty = Math.max(maxQty, e.getValue());

		// 4) 头部信息：最优价、spread、mid
		StringBuilder sb = new StringBuilder(1024);
		Long bestBid = bid.isEmpty()
					   ? null
					   : bid.get(0)
							.getKey();
		Long bestAsk = ask.isEmpty()
					   ? null
					   : ask.get(0)
							.getKey();
		sb.append("=== OrderBook Snapshot (levels=")
		  .append(levels)
		  .append(") ===\n");
		sb.append("Bids: ")
		  .append(bidLevels.size())
		  .append(" levels, ")
		  .append("Asks: ")
		  .append(askLevels.size())
		  .append(" levels\n");

		if (bestBid != null) {
			sb.append("BestBid: ")
			  .append(bestBid);
		}
		if (bestAsk != null) {
			if (bestBid != null) {
				sb.append("  |  ");
			}
			sb.append("BestAsk: ")
			  .append(bestAsk);
		}
		if (bestBid != null && bestAsk != null) {
			long spread = bestAsk - bestBid;
			double mid = (bestAsk + bestBid) / 2.0;
			sb.append("  |  Spread: ")
			  .append(spread)
			  .append("  Mid: ")
			  .append(mid);
		}
		sb.append('\n');

		// 5) 表头
		sb.append(String.format("%-12s %-12s %-" + barWidth + "s | %-" + barWidth + "s %-12s %-12s%n", "BidQty",
								"BidPrice", "BID-DEPTH", "ASK-DEPTH", "AskPrice", "AskQty"));

		// 6) 行渲染：左右对齐（买在左、卖在右）
		int rows = Math.max(bid.size(), ask.size());
		for (int i = 0; i < rows; i++) {
			Map.Entry<Long, Long> b = (i < bid.size())
									  ? bid.get(i)
									  : null;
			Map.Entry<Long, Long> a = (i < ask.size())
									  ? ask.get(i)
									  : null;

			String bidQty = b == null
							? ""
							: String.valueOf(b.getValue());
			String bidPrice = b == null
							  ? ""
							  : String.valueOf(b.getKey());
			String askPrice = a == null
							  ? ""
							  : String.valueOf(a.getKey());
			String askQty = a == null
							? ""
							: String.valueOf(a.getValue());

			String bidBar = b == null
							? ""
							: bar(b.getValue(), maxQty, barWidth, /*leftToRight*/true);
			String askBar = a == null
							? ""
							: bar(a.getValue(), maxQty, barWidth, /*leftToRight*/false);

			sb.append(String.format("%-12s %-12s %-" + barWidth + "s | %-" + barWidth + "s %-12s %-12s%n", bidQty,
									bidPrice, bidBar, askBar, askPrice, askQty));
		}

		return sb.toString();
	}

	// ======= helpers =======

	private static Map<Long, Long> aggregateByPrice(List<Long> prices, List<Long> qtys) {
		int n = Math.min(prices.size(), qtys.size());
		Map<Long, Long> map = new HashMap<>(Math.max(16, n * 2));
		for (int i = 0; i < n; i++) {
			long p = prices.get(i);
			long q = qtys.get(i);
			if (q <= 0) {
				continue;
			}
			map.merge(p, q, Long::sum);
		}
		return map;
	}

	private static List<Map.Entry<Long, Long>> sortLevels(Map<Long, Long> map, boolean bids) {
		List<Map.Entry<Long, Long>> list = new ArrayList<>(map.entrySet());
		list.sort((e1, e2) -> bids
							  ? Long.compare(e2.getKey(), e1.getKey())
							  // 买：价高在前
							  : Long.compare(e1.getKey(), e2.getKey())); // 卖：价低在前
		return list;
	}

	/** 画一个等比柱条；左侧买从左向右填充，右侧卖从右向左填充 */
	private static String bar(long qty, long maxQty, int width, boolean leftToRight) {
		if (maxQty <= 0 || width <= 0) {
			return "";
		}
		int filled = (int) Math.max(1, Math.round((qty * 1.0 / maxQty) * width));
		filled = Math.min(filled, width);
		char block = '█';
		char space = ' ';
		StringBuilder sb = new StringBuilder(width);
		if (leftToRight) {
			for (int i = 0; i < filled; i++) sb.append(block);
			for (int i = filled; i < width; i++) sb.append(space);
		} else {
			for (int i = 0; i < width - filled; i++) sb.append(space);
			for (int i = 0; i < filled; i++) sb.append(block);
		}
		return sb.toString();
	}
}
