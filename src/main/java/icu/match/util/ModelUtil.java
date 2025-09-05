package icu.match.util;

import org.springframework.beans.BeanUtils;

import icu.match.common.OrderType;
import icu.match.core.model.OrderInfo;
import icu.match.service.match.model.Order;
import icu.match.web.model.OrderResult;
import icu.match.web.model.OriginOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
public class ModelUtil {

	private ModelUtil() {}

	public static OrderInfo originOrderToOrder(OriginOrder originOrder) {
		OrderInfo.OrderInfoBuilder builder = OrderInfo.builder();
		builder.userId(originOrder.getUserId())
			   .orderId(originOrder.getOrderId())
			   .symbol(originOrder.getSymbol())
			   .side(originOrder.getSide())
			   .orderType(OrderType.valueOf(originOrder.getType()))
			   .price(originOrder.getPrice())
			   .qty(originOrder.getOrigQty());
		return builder.build();
	}

	public static OrderResult orderToOrderResult(Order order) {
		OrderResult result = new OrderResult();
		BeanUtils.copyProperties(order, result);
		if (order.getOverQty()
				 .compareTo(BigDecimal.ZERO) <= 0) {
			result.setStatus(2);
		}
		result.setCreatedAt(LocalDateTime.now());
		return result;
	}
}
