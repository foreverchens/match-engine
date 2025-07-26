package icu.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	long orderId;


	BigDecimal price;

	BigDecimal origQty;

	BigDecimal filledQty;

	BigDecimal overQty;

	String side;

	String type;

	/**
	 * 双向链表指针
	 */
	Order next, prev;

	public Order(long orderId, String side, String type, BigDecimal price, BigDecimal origQty) {
		this.orderId = orderId;
		this.side = side;
		this.type = type;
		this.price = price;
		this.origQty = origQty;
		this.overQty = origQty;
		this.filledQty = BigDecimal.ZERO;

	}

	@Override
	public String toString() {
		return "Order{" + "orderId=" + orderId + ", price=" + price + ", origQty=" + origQty + ", filledQty=" +
			   filledQty + ", overQty=" + overQty + ", side='" + side + '\'' + ", type='" + type + '}';
	}
}
