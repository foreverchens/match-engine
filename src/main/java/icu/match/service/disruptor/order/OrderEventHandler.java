package icu.match.service.disruptor.order;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.common.OrderEventType;
import icu.match.common.OrderStatus;
import icu.match.core.model.OrderInfo;
import icu.match.service.global.MonoSinkManage;
import icu.match.service.match.MatchingEngine;
import icu.match.web.model.OrderResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {

	@Resource
	private MatchingEngine matchEngine;

	@Override
	public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
		try {
			OrderEventType orderEventType = event.getOrderEventType();
			OrderInfo orderInfo = event.getOrderInfo();

			log.info("submit order :{}", orderInfo.getOrderId());
			if (orderEventType == OrderEventType.NEW_ORDER) {
				try {
					OrderStatus rlt = matchEngine.submit(orderInfo);
					log.info(rlt.toString());
				} finally {
					MonoSink<OrderResult> sink = MonoSinkManage.getSink(orderInfo.getOrderId());
					if (sink != null) {
						sink.success(new OrderResult());
					}
				}
				return;
			}
			switch (orderEventType) {
				case CANCEL_ORDER:
					matchEngine.cancel(orderInfo);
					break;
				case MODIFY_ORDER:
					break;
				default:
					throw new IllegalArgumentException("Unsupported event type: " + orderEventType);
			}
		} finally {
			event.reset();
		}
	}
}
