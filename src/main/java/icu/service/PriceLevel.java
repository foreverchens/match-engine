package icu.service;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
public class PriceLevel {

	BigDecimal price;

	BigDecimal totalQty;

	Order head;

	Order tail;

	Map<Long, Order> orderIdMap;

	public PriceLevel(BigDecimal price) {
		this.price = price;
		this.head = this.tail = new Order();
		this.orderIdMap = new HashMap<>();
		this.totalQty = BigDecimal.ZERO;
	}


	/**
	 * 添加订单至尾部链表
	 * @param o
	 */
	public void push(Order o) {
		orderIdMap.put(o.orderId, o);
		tail.next = o;
		o.prev = tail;
		tail = o;
		totalQty = totalQty.add(o.origQty);
	}

	public Order peek() {
		return head.next;
	}

	/**
	 * 移除订单
	 * @param orderId
	 * @return
	 */
	Order remove(Long orderId) {
		Order order = orderIdMap.remove(orderId);
		if (Objects.isNull(order)) {
			return null;
		}
		order.prev.next = order.next;
		if (!Objects.isNull(order.next)) {
			order.next.prev = order.prev;
		}
		totalQty = totalQty.subtract(order.overQty);
		return order;
	}

	Order patch(Long orderId, BigDecimal filledQty) {
		Order order = orderIdMap.get(orderId);
		if (Objects.isNull(order)) {
			return null;
		}
		order.filledQty = order.filledQty.add(filledQty);
		order.overQty = order.overQty.subtract(filledQty);
		if (order.filledQty.compareTo(order.origQty) >= 0) {
			return remove(orderId);
		}
		return order;
	}

	public boolean isEmpty() {
		return orderIdMap.isEmpty();
	}

	public String print() {
		StringBuilder sb = new StringBuilder();
		Order tmp = head.next;
		while (!Objects.isNull(tmp)) {
			sb.append(tmp.orderId);
			sb.append("->");
			tmp = tmp.next;
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "PriceLevel{" + "price=" + price + ", orderIdMap=" + orderIdMap + '}';
	}
}
