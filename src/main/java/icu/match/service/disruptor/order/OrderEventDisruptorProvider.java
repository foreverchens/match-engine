package icu.match.service.disruptor.order;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.common.OrderEventType;
import icu.match.core.model.OrderInfo;
import icu.match.service.disruptor.AbstractDisruptorProvider;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/9/10 
 */
@Component
public class OrderEventDisruptorProvider extends AbstractDisruptorProvider<OrderEvent> {

	@Resource
	private OrderEventHandler orderEventHandler;

	@Override
	protected EventFactory<OrderEvent> eventFactory() {
		return () -> new OrderEvent(OrderEventType.NEW_ORDER, new OrderInfo());
	}

	@Override
	protected String threadName() {
		return "disruptor-order-event-consumer";
	}

	@Override
	protected EventHandler<OrderEvent>[] eventHandlers() {
		return new EventHandler[]{orderEventHandler};
	}
}
