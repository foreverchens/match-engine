package icu.match.core.model;/**
 *
 * @author 中本君
 * @date 2025/8/18
 */

import icu.match.common.OrderEventType;
import icu.match.common.OrderSide;
import icu.match.common.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 中本君
 * @date 2025/8/18 
 */
@Slf4j
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {

	private OrderEventType orderEventType;

	private long userId;

	private long orderId;

	private String symbol;

	private OrderSide side;

	private OrderType orderType;

	private long price;

	private long qty;

	private long time;

}


