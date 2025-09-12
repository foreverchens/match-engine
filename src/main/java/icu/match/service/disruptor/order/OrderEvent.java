package icu.match.service.disruptor.order;

import icu.match.common.OrderEventType;
import icu.match.core.model.OrderInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

	private byte eventTypeCode;

	private OrderInfo orderInfo;


	public void reset() {
		eventTypeCode = OrderEventType.NEW_ORDER.code;
		orderInfo.setSymbol(0);
		orderInfo.setOrderId(0);
	}

}
