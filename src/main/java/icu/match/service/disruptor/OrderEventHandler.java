package icu.match.service.disruptor;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.common.OrderEventType;
import icu.match.common.OrderStatus;
import icu.match.core.MatchingEngine;
import icu.match.core.model.OrderInfo;
import icu.match.service.global.MonoSinkManage;
import icu.match.web.model.OrderResult;
import lombok.extern.slf4j.Slf4j;

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
		OrderEventType orderEventType = event.getOrderEventType();
		OrderInfo orderInfo = event.getOrderInfo();
		switch (orderEventType) {
			case NEW_ORDER:
				try {
					OrderStatus rlt = matchEngine.submit(orderInfo);
					log.info(rlt.toString());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					MonoSinkManage.getSink(orderInfo.getOrderId())
								  .success(new OrderResult());
				}
				break;
			case CANCEL_ORDER:
				matchEngine.cancel(orderInfo);
				break;
			default:
				throw new IllegalArgumentException("Unsupported event type: " + orderEventType);
		}

	}
}
