package icu.service.web.model;

import icu.common.OrderSide;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Data
@Builder
@ToString
public class OrderResult {
	long orderId;

	long userId;

	String symbol;

	/**
	 * OrderSide
	 * BID ｜ ASK
	 */
	OrderSide side;

	/**
	 * OrderType
	 * LIMIT ｜ MARKET
	 */
	String type;

	BigDecimal price;

	Integer status;

	/**
	 * 原始订单数量 已填充数量 剩余数量
	 */
	BigDecimal origQty;
}
