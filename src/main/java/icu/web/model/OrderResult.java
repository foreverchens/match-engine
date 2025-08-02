package icu.web.model;

import icu.common.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {
	Long orderId;

	Long userId;

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

	/**
	 * 原始订单数量 已填充数量 剩余数量
	 */
	BigDecimal origQty;

	BigDecimal filledQty;

	BigDecimal overQty;

	Integer status;

	LocalDateTime createdAt;


}
