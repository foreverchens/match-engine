package icu.service.disruptor;

import com.lmax.disruptor.RingBuffer;

import org.springframework.stereotype.Component;

import icu.service.match.model.Order;
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

	public void publish(Order order) {
		log.info("处理订单:{}", order);
		long seq = ringBuffer.next();
		OrderEvent event = ringBuffer.get(seq);
		event.setOrder(order);
		ringBuffer.publish(seq);
	}
}
