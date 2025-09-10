package icu.match.service.disruptor;

import com.lmax.disruptor.RingBuffer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import icu.match.service.disruptor.order.OrderEvent;
import icu.match.service.disruptor.order.OrderEventDisruptorProvider;
import icu.match.service.disruptor.trade.TradeEvent;
import icu.match.service.disruptor.trade.TradeEventDisruptorProvider;

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
	public RingBuffer<TradeEvent> tradeEventRingBuffer(TradeEventDisruptorProvider provider) {
		return provider.ringBuffer();
	}
}
