package icu.match.util;

import org.springframework.beans.BeanUtils;

import icu.match.common.OrderStatus;
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

	public static Order originOrderToOrder(OriginOrder originOrder) {
		Order order = new Order();
		BeanUtils.copyProperties(originOrder, order);
		order.setOverQty(originOrder.getOrigQty());
		order.setFilledQty(BigDecimal.ZERO);
		order.setStatus(OrderStatus.PENDING.val);
		return order;
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
