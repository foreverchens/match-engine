package icu.service.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
public class OrderEventFactory implements EventFactory<OrderEvent> {
	@Override
	public OrderEvent newInstance() {
		return new OrderEvent();
	}
}
