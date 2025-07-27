package icu.service.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Configuration
public class DisruptorConfig {
	@Bean
	public Disruptor<OrderEvent> disruptor(@Autowired OrderEventHandler orderEventHandler) {
		Disruptor<OrderEvent> disruptor = new Disruptor<>(new OrderEventFactory(), 1024,
														  Executors.defaultThreadFactory());
		disruptor.handleEventsWith(orderEventHandler);
		disruptor.start();
		return disruptor;
	}

	@Bean
	public RingBuffer<OrderEvent> ringBuffer(Disruptor<OrderEvent> disruptor) {
		return disruptor.getRingBuffer();
	}

}
