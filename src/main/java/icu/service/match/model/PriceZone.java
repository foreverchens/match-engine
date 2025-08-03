package icu.service.match.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 环形数组中 单个价格槽 存储相同价格的所有订单
 * @author 中本君
 * @date 2025/07/27
 */
@Data
public class PriceZone {

	BigDecimal price;

	BigDecimal totalQty;

	/**
	 * head.next 永远指定最高优先级的订单
	 */
	Order head;

	/**
	 * tail.prev 永远指向最新追加的订单
	 */
	Order tail;

	Map<Long, Order> orderIdMap;

	public PriceZone(BigDecimal price) {
		this.price = price;
		head = new Order();
		tail = new Order();
		head.next = tail;
		tail.prev = head;
		orderIdMap = new HashMap<>();
		totalQty = BigDecimal.ZERO;
	}


	/**
	 * 添加订单至尾部链表
	 * @param o
	 */
	public void submit(Order o) {
		orderIdMap.put(o.orderId, o);
		totalQty = totalQty.add(o.origQty);
		o.prev = tail.prev;
		o.next = tail;
		tail.prev.next = o;
		tail.prev = o;
	}

	public Order peek() {
		if (isEmpty()) {
			return null;
		}
		return head.next;
	}

	boolean isEmpty() {
		return orderIdMap.isEmpty();
	}

	Order patch(long orderId, BigDecimal filledQty) {
		Order order = orderIdMap.get(orderId);
		if (Objects.isNull(order)) {
			return null;
		}
		order.filledQty = order.filledQty.add(filledQty);
		order.overQty = order.overQty.subtract(filledQty);
		totalQty = totalQty.subtract(filledQty);
		return order;
	}

	/**
	 * 移除订单
	 * @param orderId
	 * @return
	 */
	Order cancel(long orderId) {
		Order order = orderIdMap.remove(orderId);
		if (Objects.isNull(order)) {
			return null;
		}
		totalQty = totalQty.subtract(order.overQty);
		order.prev.next = order.next;
		order.next.prev = order.prev;
		return order;
	}

	/**
	 * 判断当前价格槽方向
	 */
	boolean isAsk() {
		if (isEmpty()) {
			return true;
		}
		return head.next.side.isAsk();
	}

	List<Long> print() {
		List<Long> rlt = new ArrayList<>();
		Order tmp = head.next;
		while (tmp != tail) {
			rlt.add(tmp.orderId);
			tmp = tmp.next;
		}
		return rlt;
	}

	@Override
	public String toString() {
		return "PriceLevel{" + "price=" + price + ", orderIdMap=" + orderIdMap + '}';
	}
}
