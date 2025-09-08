package icu.match.service.disruptor;

import com.lmax.disruptor.RingBuffer;

import org.springframework.stereotype.Component;

import icu.match.common.OrderEventType;
import icu.match.core.model.OrderInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class DisruptorService {

	@Resource
	private RingBuffer<OrderEvent> ringBuffer;

	public void publish(OrderInfo order) {
		this.publish(OrderEventType.NEW_ORDER, order);
	}

	public void publish(OrderEventType orderEventType, OrderInfo order) {
		log.info("deal order event:{}", order.getOrderId());
		long seq = ringBuffer.next();
		OrderEvent event = ringBuffer.get(seq);
		event.setOrderEventType(orderEventType);
		event.setOrderInfo(order);
		ringBuffer.publish(seq);
	}
}
