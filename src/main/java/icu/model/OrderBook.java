package icu.model;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import icu.common.OrderSide;
import icu.common.OrderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public class OrderBook {


	private Integer left, right;

	private Integer length;

	private Integer balanceThreshold;

	private Integer lastIdx;

	private BigDecimal lastP;

	private BigDecimal step;

	private PriceLevel[] hotZone;

	private TreeMap<BigDecimal, PriceLevel> lowZone;

	private TreeMap<BigDecimal, PriceLevel> highZone;

	public OrderBook(int len, BigDecimal curPrice, BigDecimal step, int balanceThreshold) {
		init(len, curPrice, step, balanceThreshold);
	}


	public OrderBook(int len, BigDecimal curPrice, BigDecimal step, int balanceThreshold, List<Order> orderList) {
		this(len, curPrice, step, balanceThreshold);
		List<Trade> rlt = new ArrayList<>();
		for (Order o : orderList) {
			rlt.addAll(this.push(o));
		}
		rlt.forEach(System.out::println);
	}

	private void init(int len, BigDecimal curPrice, BigDecimal step, int balanceThreshold) {
		this.length = len * 2 + 1;
		this.lastIdx = (this.length / 2);
		this.lastP = curPrice;
		this.left = lastIdx;
		this.right = lastIdx;
		this.balanceThreshold = balanceThreshold;
		this.step = step;
		this.hotZone = new PriceLevel[length];
		this.lowZone = new TreeMap<>();
		this.highZone = new TreeMap<>();
		hotZone[lastIdx] = new PriceLevel(curPrice);
		while (left > 0) {
			left = getLeftIdx(left);
			right = getRightIdx(right);
			// 左指针的左边不是右指针
			hotZone[left] = new PriceLevel(hotZone[getRightIdx(left)].price.subtract(step));
			hotZone[right] = new PriceLevel(hotZone[getLeftIdx(right)].price.add(step));
		}
	}

	/**
	 * 添加订单到结构中
	 * @param o
	 */
	public List<Trade> push(Order o) {
		// todo
		if (OrderType.MARKET.equals(o.side)) {
			return match(o);
		}

		BigDecimal price = o.price;
		if (price.compareTo(hotZone[left].price) < 0) {
			// low zone
			PriceLevel priceLevel = lowZone.get(price);
			if (Objects.isNull(priceLevel)) {
				priceLevel = new PriceLevel(price);
				lowZone.put(price, priceLevel);
			}
			priceLevel.push(o);
			return Collections.emptyList();
		}

		if (price.compareTo(hotZone[right].price) > 0) {
			// high zone
			PriceLevel priceLevel = highZone.get(price);
			if (Objects.isNull(priceLevel)) {
				priceLevel = new PriceLevel(price);
				highZone.put(price, priceLevel);
			}
			priceLevel.push(o);
			return Collections.emptyList();
		}

		int targetIdx = getTargetIdx(price);
		if (lastP.compareTo(o.price) > 0 && StrUtil.equals(OrderSide.SELL, o.side)) {
			// 低于市价的限价卖单、便宜卖场合
			return match(o);
		}
		if (lastP.compareTo(o.price) < 0 && StrUtil.equals(OrderSide.BUY, o.side)) {
			// 高于市价的限价买单、溢价买场合
			return match(o);
		}
		PriceLevel priceLevel = hotZone[targetIdx];
		if (targetIdx == lastIdx) {
			// 等于市价的限价单、若方向相反、原地撮合
			if (ObjUtil.isNotNull(priceLevel.head.next) && !StrUtil.equals(o.side, priceLevel.head.next.side)) {
				return match(o);
			}
		}
		priceLevel.push(o);
		return Collections.emptyList();
	}

	/**
	 * 移除订单
	 * @return
	 */
	public Order remove(Long orderId, BigDecimal price) {
		Order rlt = null;
		if (price.compareTo(hotZone[left].price) < 0) {
			// low zone
			PriceLevel priceLevel = lowZone.get(price);
			if (!Objects.isNull(priceLevel)) {
				rlt = priceLevel.remove(orderId);
				if (priceLevel.isEmpty()) {
					lowZone.remove(price);
				}
			}
			return rlt;
		}

		if (price.compareTo(hotZone[right].price) > 0) {
			// high zone
			PriceLevel priceLevel = highZone.get(price);
			if (!Objects.isNull(priceLevel)) {
				rlt = priceLevel.remove(orderId);
				if (priceLevel.isEmpty()) {
					lowZone.remove(price);
				}
			}
			return rlt;
		}

		return hotZone[getTargetIdx(price)].remove(orderId);
	}

	/**
	 * 以下订单直接市价撮合
	 * 1.主动市价单
	 * 2.限价价格充分满足订单簿价格、对满足部分进行市价撮合、不满足按照限价处理
	 * @param order
	 * @return
	 */
	public List<Trade> match(Order order) {
		List<Trade> rlt = new ArrayList<>();
		long orderId = order.orderId;
		BigDecimal price = order.price;
		String side = order.side;
		while (BigDecimal.ZERO.compareTo(order.overQty) < 0) {
			PriceLevel priceLevel = bestLevel(side);
			if (Objects.isNull(priceLevel)) {
				break;
			}
			Order peek = priceLevel.peek();
			while (ObjUtil.isNotNull(peek) && peek.overQty.compareTo(BigDecimal.ZERO) > 0) {
				if (order.overQty.compareTo(peek.overQty) >= 0) {
					// 吃掉流动性
					Long bidUserId = StrUtil.equals(OrderSide.BUY, side)
									 ? orderId
									 : peek.orderId;
					Long askUserId = StrUtil.equals(OrderSide.SELL, side)
									 ? orderId
									 : peek.orderId;
					Trade trade = new Trade(bidUserId, askUserId, price, peek.overQty);
					order.overQty = order.overQty.subtract(peek.overQty);
					order.filledQty = order.filledQty.add(peek.overQty);
					rlt.add(trade);
					priceLevel.patch(peek.orderId, peek.overQty);
				}
				else {
					// 已全部撮合
					priceLevel.patch(peek.orderId, order.overQty);
					order.overQty = order.overQty.subtract(order.overQty);
					order.filledQty = order.filledQty.add(order.overQty);
					return rlt;
				}
			}
		}
		return rlt;
	}

	private PriceLevel bestLevel(String sourceSide) {
		PriceLevel priceLevel = hotZone[lastIdx];
		Order peek = priceLevel.peek();
		if (ObjUtil.isNotNull(peek)) {
			if (!StrUtil.equals(sourceSide, peek.side)) {
				return priceLevel;
			}
		}
		if (Objects.equals(sourceSide, OrderSide.BUY)) {
			// 找卖1 价
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

	public void print() {
		treePrint(lowZone);

		int l = left;
		while (l < right) {
			PriceLevel priceLevel = this.hotZone[l];
			String str = priceLevel.print();
			if (!StrUtil.isEmpty(str)) {
				System.out.printf("price:[%s],  orderList:[%s]\n", priceLevel.price, str);
			}
			l = getRightIdx(l);
		}

		treePrint(highZone);
	}

	private void treePrint(TreeMap<BigDecimal, PriceLevel> treeMap) {
		Iterator<Map.Entry<BigDecimal, PriceLevel>> iterator = treeMap.entrySet().iterator();
		System.out.println("~~~~~~~~~~~~~~~~~~");
		while (iterator.hasNext()) {
			Map.Entry<BigDecimal, PriceLevel> next = iterator.next();
			PriceLevel priceLevel = next.getValue();
			String str = priceLevel.print();
			if (!StrUtil.isEmpty(str)) {
				System.out.printf("price:[%s],  orderList:[%s]\n", priceLevel.price, str);
			}
		}
		System.out.println("~~~~~~~~~~~~~~~~~~");
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
}
