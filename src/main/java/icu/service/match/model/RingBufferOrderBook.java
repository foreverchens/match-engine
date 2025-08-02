package icu.service.match.model;

import cn.hutool.core.util.ObjUtil;

import icu.common.OrderSide;
import icu.common.OrderType;
import icu.service.disruptor.OrderEventHandler;
import icu.service.match.model.base.OrderBook;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 订单簿实现
 * 基于环形数组实现
 *
 * @author 中本君
 * @date 2025/07/27
 */
public class RingBufferOrderBook extends OrderBook {


	private String symbol;

	/**
	 * 热区环形数组长度
	 */
	private Integer length;

	/**
	 * 热区环形数组中 价格槽价格最低和最高索引
	 */
	private Integer left, right;


	/**
	 * 最近一次访问的索引位置 和最近一次处理的价格
	 */
	private Integer lastIdx;

	private BigDecimal lastP;


	/**
	 * 价格最小步长
	 */
	private BigDecimal step;

	/**
	 *  重新平衡的阈值
	 *  以买单所占比率为准 初始50% 低于时重新平衡
	 */
	private Integer reBalanceThreshold;

	private PriceZone[] hotZone;

	private TreeMap<BigDecimal, PriceZone> lowZone;

	private TreeMap<BigDecimal, PriceZone> highZone;

	private final OrderEventHandler orderEventHandler;

	public RingBufferOrderBook(Params params) {
		this.init(params.getSymbol(), params.getLen(), params.getCurP(), params.getStep(), params.getReBalId());
		orderEventHandler = params.getOrderEventHandler();
	}

	/**
	 *
	 * @param len 数组长度
	 * @param curPrice 当前价格
	 * @param step  价格步长
	 * @param balanceThreshold 重新平衡阈值
	 */
	private void init(String symbol, Integer len, BigDecimal curPrice, BigDecimal step, Integer balanceThreshold) {
		this.symbol = symbol;
		this.length = len;

		this.lastIdx = (this.length / 2);
		this.lastP = curPrice;
		this.step = step;

		this.left = lastIdx;
		this.right = lastIdx;
		this.reBalanceThreshold = balanceThreshold;

		// 初始化数据结构
		this.hotZone = new PriceZone[length];
		this.lowZone = new TreeMap<>();
		this.highZone = new TreeMap<>();

		// 初始化价格槽
		this.hotZone[lastIdx] = new PriceZone(curPrice);
		while (left > 0) {
			left = getLeftIdx(left);
			hotZone[left] = new PriceZone(hotZone[getRightIdx(left)].price.subtract(step));
		}
		while (right < length - 1) {
			right = getRightIdx(right);
			hotZone[right] = new PriceZone(hotZone[getLeftIdx(right)].price.add(step));
		}
	}

	/**
	 * 添加订单到结构中
	 * 市价单直接撮合
	 * 价格位于冷区 直接提交到对应价格槽
	 * 价格位于热区
	 * 	溢价买和折价卖 直接撮合
	 * 检查能否原价原地撮合
	 * 否则提交到对应价格槽
	 * @param o
	 */
	@Override
	public List<Trade> submit(Order o) {
		if (OrderType.isMarket(o.type)) {
			marketMatch(o);
			return null;
		}


		BigDecimal price = o.price;
		if (price.compareTo(hotZone[left].price) < 0) {
			// 追加到 low zone
			PriceZone priceZone = lowZone.get(price);
			if (Objects.isNull(priceZone)) {
				priceZone = new PriceZone(price);
				lowZone.put(price, priceZone);
			}
			priceZone.submit(o);
			return Collections.emptyList();
		}

		if (price.compareTo(hotZone[right].price) > 0) {
			// 追加到 high zone
			PriceZone priceZone = highZone.get(price);
			if (Objects.isNull(priceZone)) {
				priceZone = new PriceZone(price);
				highZone.put(price, priceZone);
			}
			priceZone.submit(o);
			return Collections.emptyList();
		}

		// 处于当前热区
		if (o.side.isAsk() && lastP.compareTo(o.price) > 0) {
			// 低于市价的限价卖单、便宜卖场合
			return match(o);
		}
		if (!o.side.isAsk() && lastP.compareTo(o.price) < 0) {
			// 高于市价的限价买单、溢价买场合
			return match(o);
		}
		int targetIdx = getTargetIdx(price);
		PriceZone targetZone = hotZone[targetIdx];
		if (targetIdx == lastIdx) {
			// 等于市价的限价单、若方向相反、原地撮合
			if (!targetZone.isEmpty() && !Objects.equals(o.side, targetZone.head.next.side)) {
				// 当前槽 有订单 且 两者订单方向相反 原地撮合
				return match(o);
			}
		}
		targetZone.submit(o);
		return Collections.emptyList();
	}

	/**
	 * 移除订单
	 * @param orderId id
	 * @param price 用于定位在哪个槽
	 * @return
	 */
	@Override
	public Order cancel(Long orderId, BigDecimal price) {
		Order rlt;
		if (price.compareTo(hotZone[left].price) < 0) {
			// low zone
			PriceZone priceZone = lowZone.get(price);
			if (Objects.isNull(priceZone)) {
				return null;
			}
			rlt = priceZone.cancel(orderId);
			if (priceZone.isEmpty()) {
				lowZone.remove(price);
			}
			return rlt;
		}

		if (price.compareTo(hotZone[right].price) > 0) {
			// high zone
			PriceZone priceZone = highZone.get(price);
			if (Objects.isNull(priceZone)) {
				return null;
			}
			rlt = priceZone.cancel(orderId);
			if (priceZone.isEmpty()) {
				lowZone.remove(price);
			}
			return rlt;
		}
		// 最小步长
		return hotZone[getTargetIdx(price)].cancel(orderId);
	}

	/**
	 * 以下订单直接市价撮合
	 * 1.主动市价单
	 * 2.限价价格充分满足订单簿价格、对满足部分进行市价撮合、不满足按照限价处理
	 *
	 * 尝试撮合
	 * 市价场合
	 * 1.不看价格全力撮合
	 *
	 * 限价场合
	 * 1.获取当前订单价格和方向
	 * 2.获取待撮合数量 循环检查是否大于0
	 * 3.获取目标方向最优价格槽
	 * 4.检查价格能否匹配 不能匹配则提交到订单簿 结束循环
	 * 5.进行匹配 生成trade
	 *
	 * @param order
	 * @return
	 */
	@Override
	public List<Trade> match(Order order) {
		return order.side.isAsk()
			   ? matchForAsk(order)
			   : matchForBid(order);
	}

	private void marketMatch(Order o) {
		if (canFillMarketOrder(o.side, o.origQty)) {
			match(o);
		}
		else {
			// 无法全部撮合
		}
	}

	private List<Trade> matchForAsk(Order askOrder) {
		long askUserId = askOrder.userId;
		long askOrderId = askOrder.orderId;

		List<Trade> rlt = new ArrayList<>();
		while (askOrder.overQty.compareTo(BigDecimal.ZERO) > 0) {
			// 订单剩余量 大于 0
			PriceZone priceZone = bestLevel(askOrder.side);
			if (Objects.isNull(priceZone)) {
				// 没有最优的
				break;
			}
			Order peek = priceZone.peek();
			if (ObjUtil.isNull(peek) || !matchable(askOrder, peek)) {
				// 限价价格不匹配
				break;
			}
			// 吃掉流动性
			long bidUserId = peek.orderId;
			long bidOrderId = peek.orderId;
			BigDecimal exeQty = askOrder.overQty.compareTo(peek.origQty) > 0
								? peek.origQty
								: askOrder.overQty;

			// 更新流动性
			priceZone.patch(peek.orderId, exeQty);

			// 更新订单
			askOrder.overQty = askOrder.overQty.subtract(exeQty);
			askOrder.filledQty = askOrder.filledQty.add(exeQty);

			// 生成trade
			Trade trade = new Trade(bidUserId, askUserId, bidOrderId, askOrderId, peek.price, exeQty,
									LocalDateTime.now());
			rlt.add(trade);
		}
		if (askOrder.overQty.compareTo(BigDecimal.ZERO) > 0) {
			// 部分撮合 提交到订单簿
			int targetIdx = getTargetIdx(askOrder.price);
			PriceZone targetZone = hotZone[targetIdx];
			targetZone.submit(askOrder);
		}
		return rlt;
	}

	private List<Trade> matchForBid(Order bidOrder) {
		long bidUserId = bidOrder.userId;
		long bidOrderId = bidOrder.orderId;

		List<Trade> rlt = new ArrayList<>();
		while (bidOrder.overQty.compareTo(BigDecimal.ZERO) > 0) {
			// 订单剩余量 大于 0
			PriceZone priceZone = bestLevel(bidOrder.side);
			if (Objects.isNull(priceZone)) {
				// 没有最优的
				break;
			}
			Order peek = priceZone.peek();
			if (ObjUtil.isNull(peek) || !matchable(bidOrder, peek)) {
				// 限价价格不匹配
				break;
			}
			// 吃掉流动性
			long askUserId = peek.orderId;
			long askOrderId = peek.orderId;
			BigDecimal exeQty = bidOrder.overQty.compareTo(peek.overQty) > 0
								? peek.overQty
								: bidOrder.overQty;

			// 更新流动性
			priceZone.patch(peek.orderId, exeQty);

			// 更新订单
			bidOrder.overQty = bidOrder.overQty.subtract(exeQty);
			bidOrder.filledQty = bidOrder.filledQty.add(exeQty);

			// 生成trade
			Trade trade = new Trade(bidUserId, askUserId, bidOrderId, askOrderId, peek.price, exeQty,
									LocalDateTime.now());
			rlt.add(trade);
		}
		if (bidOrder.overQty.compareTo(BigDecimal.ZERO) > 0) {
			// 部分撮合 提交到订单簿
			int targetIdx = getTargetIdx(bidOrder.price);
			PriceZone targetZone = hotZone[targetIdx];
			targetZone.submit(bidOrder);
		}
		return rlt;
	}

	private boolean matchable(Order order, Order peek) {
		if (Objects.equals(order.side, peek.side)) {
			return false;
		}
		if (OrderType.isMarket(order.type)) {
			return true;
		}
		if (order.side.isAsk()) {
			// ask
			return order.price.compareTo(peek.price) <= 0;
		}
		else {
			// bid
			return order.price.compareTo(peek.price) >= 0;
		}
	}

	/**
	 * 检查当前深度能否满足市价订单要求
	 *  仅检查除了left和right两点的热区
	 */
	boolean canFillMarketOrder(OrderSide side, BigDecimal marketQty) {
		BigDecimal totalQty = BigDecimal.ZERO;
		PriceZone lastZone = hotZone[lastIdx];
		if (lastZone.isAsk() && !side.isAsk()) {
			// 不同向 可匹配
			totalQty = totalQty.add(lastZone.totalQty);
		}
		if (side.isAsk()) {
			// buy
			int idx = getLeftIdx(lastIdx);
			while (idx != left && totalQty.compareTo(marketQty) < 0) {
				totalQty = totalQty.add(hotZone[idx].totalQty);
				idx = getLeftIdx(idx);
			}
		}
		else {
			// sell
			int idx = getRightIdx(lastIdx);
			while (idx != right && totalQty.compareTo(marketQty) < 0) {
				totalQty = totalQty.add(hotZone[idx].totalQty);
				idx = getRightIdx(idx);
			}
		}
		return totalQty.compareTo(marketQty) >= 0;
	}

	/**
	 *  获取最优流动性
	 * @param sourceSide
	 * @return
	 */
	private PriceZone bestLevel(OrderSide sourceSide) {
		PriceZone priceZone = hotZone[lastIdx];
		Order peek = priceZone.peek();
		if (ObjUtil.isNotNull(peek)) {
			if (!Objects.equals(sourceSide, peek.side)) {
				// 当前价格槽 方向相反
				return priceZone;
			}
		}
		if (!sourceSide.isAsk()) {
			// 买场合 找卖1 价
			int r = getRightIdx(lastIdx);
			while (r != right && ObjUtil.isNull(hotZone[r].peek())) {
				r = getRightIdx(r);
			}
			return ObjUtil.isNull(hotZone[r].peek())
				   ? null
				   : hotZone[r];
		}
		else {
			int l = getLeftIdx(lastIdx);
			while (l != left && ObjUtil.isNull(hotZone[l].peek())) {
				l = getLeftIdx(l);
			}
			return ObjUtil.isNull(hotZone[l].peek())
				   ? null
				   : hotZone[l];

		}
	}

	private int getTargetIdx(BigDecimal price) {
		int idxStep = price.subtract(lastP).divide(this.step).intValue();
		return lastIdx + idxStep;
	}

	private int getLeftIdx(int idx) {
		return (idx - 1 + length) % length;
	}

	private int getRightIdx(int idx) {
		return (idx + 1) % length;
	}

	public List<String> print() {
		List<String> rlt = new ArrayList<>();
		rlt.addAll(treePrint(lowZone));

		int l = left;
		do {
			PriceZone priceZone = this.hotZone[l];
			if (priceZone.isEmpty()) {
				continue;
			}
			List<Long> orderIdList = priceZone.print();
			rlt.add(String.format("price:[%s],  orderList:%s", priceZone.price, orderIdList));
		}
		while ((l = getRightIdx(l)) != left);

		rlt.addAll(treePrint(highZone));
		return rlt;
	}

	private List<String> treePrint(TreeMap<BigDecimal, PriceZone> treeMap) {
		Iterator<Map.Entry<BigDecimal, PriceZone>> iterator = treeMap.entrySet().iterator();
		List<String> rlt = new ArrayList<>();
		while (iterator.hasNext()) {
			Map.Entry<BigDecimal, PriceZone> next = iterator.next();
			PriceZone priceZone = next.getValue();
			List<Long> orderIdList = priceZone.print();
			rlt.add(String.format("price:[%s],  orderList:%s", priceZone.price, orderIdList));
		}
		return rlt;
	}

	//
	// // 根据新中间价滑动环形数组并平衡
	// public void adjustMidPrice(long newMid) {
	// 	int offset = (int) ((newMid - midPrice) / priceStep);
	// 	midPrice = newMid;
	// 	if (offset > 0) {
	// 		// 向上滑动
	// 		for (int k = 0; k < offset; k++) {
	// 			rotateUp();
	// 		}
	// 	}
	// 	else if (offset < 0) {
	// 		// 向下滑动
	// 		for (int k = 0; k < -offset; k++) {
	// 			rotateDown();
	// 		}
	// 	}
	// }
	//
	// // 向上平衡一次
	// private void rotateUp() {
	// 	// 弹出最底端热区
	// 	PriceLevel dropped = hotZone[0];
	// 	System.arraycopy(hotZone, 1, hotZone, 0, size - 1);
	// 	hotZone[size - 1] = null;
	// 	if (dropped != null) {
	// 		lowZone.put(dropped.price, dropped);
	// 	}
	// 	// 引入上远端最优
	// 	if (!highZone.isEmpty()) {
	// 		Map.Entry<Long, PriceLevel> e = highZone.pollFirstEntry();
	// 		hotZone[size - 1] = e.getValue();
	// 	}
	// }
	//
	// // 向下平衡一次
	// private void rotateDown() {
	// 	PriceLevel dropped = hotZone[size - 1];
	// 	System.arraycopy(hotZone, 0, hotZone, 1, size - 1);
	// 	hotZone[0] = null;
	// 	if (dropped != null) {
	// 		highZone.put(dropped.price, dropped);
	// 	}
	// 	if (!lowZone.isEmpty()) {
	// 		Map.Entry<Long, PriceLevel> e = lowZone.pollLastEntry();
	// 		hotZone[0] = e.getValue();
	// 	}
	// }

	/**
	 * 构建参数
	 */
	@Data
	@Builder
	@ToString
	public static class Params {
		// }
		private String symbol;

		private Integer len;

		private BigDecimal curP;

		private BigDecimal step;

		private Integer reBalId;

		private OrderEventHandler orderEventHandler;

	}
}
