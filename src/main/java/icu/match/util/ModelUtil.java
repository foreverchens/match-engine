package icu.match.util;

import icu.match.core.model.OrderInfo;
import icu.match.web.model.OriginOrder;

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
			   .orderType(originOrder.getType())
			   .tif(originOrder.getTif())
			   .price(originOrder.getPrice())
			   .qty(originOrder.getOrigQty());
		return builder.build();
	}

}
