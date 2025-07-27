package icu.service.disruptor;

import com.lmax.disruptor.RingBuffer;

import org.springframework.stereotype.Component;

import icu.model.Order;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Component
public class OrderService {

	@Resource
	private RingBuffer<OrderEvent> ringBuffer;

	public void publishOrder(Order order) {
		long seq = ringBuffer.next();
		OrderEvent event = ringBuffer.get(seq);
		event.setOrder(order);
		ringBuffer.publish(seq);
	}
}
