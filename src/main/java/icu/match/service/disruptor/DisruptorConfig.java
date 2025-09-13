package icu.match.service.disruptor;

import com.lmax.disruptor.RingBuffer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import icu.match.service.disruptor.match.MatchEvent;
import icu.match.service.disruptor.match.MatchEventDisruptorProvider;
import icu.match.service.disruptor.order.OrderEvent;
import icu.match.service.disruptor.order.OrderEventDisruptorProvider;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Configuration
public class DisruptorConfig {

	@Bean
	public RingBuffer<OrderEvent> orderEventRingBuffer(OrderEventDisruptorProvider provider) {
		return provider.ringBuffer();
	}


	@Bean
	public RingBuffer<MatchEvent> matchEventRingBuffer(MatchEventDisruptorProvider provider) {
		return provider.ringBuffer();
	}
}
