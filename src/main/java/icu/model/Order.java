package icu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 撮合订单信息
 *
 * @author 中本君
 * @date 2025/07/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
	long orderId;

	long userId;

	/**
	 * OrderSide
	 * BID ｜ ASK
	 */
	String side;

	/**
	 * OrderType
	 * LIMIT ｜ MARKET
	 */
	String type;

	BigDecimal price;

	/**
	 * 原始订单数量 已填充数量 剩余数量
	 */
	BigDecimal origQty;

	BigDecimal filledQty;

	BigDecimal overQty;

	int status;

	LocalDateTime createdAt;

	LocalDateTime updatedAt;

	/**
	 * 双向链表指针
	 */
	Order next, prev;

	public Order(long userId, long orderId, String side, String type, BigDecimal price, BigDecimal origQty) {
		this.userId = userId;
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
