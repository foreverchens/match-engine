package icu.match.web.service;

import com.lmax.disruptor.RingBuffer;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import icu.match.common.OrderEventType;
import icu.match.core.model.OrderInfo;
import icu.match.service.disruptor.order.OrderEvent;
import icu.match.service.global.MonoSinkManage;
import icu.match.web.model.OrderResult;
import icu.match.web.model.OriginOrder;
import icu.match.web.repo.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Slf4j
@Service
public class OrderService {


	@Resource
	private OrderRepository orderRepository;

	@Resource
	private RingBuffer<OrderEvent> ringBuffer;

	public Mono<OrderResult> submit(OriginOrder originOrder) {
		log.info("received orderId :{}", originOrder.getOrderId());

		return orderRepository.save(originOrder)
							  .flatMap(savedOrder -> Mono.<OrderResult>create(sink -> {
								  // 注册sink，后续由撮合线程调用 sink.success() / sink.error()
								  MonoSinkManage.put(savedOrder.getOrderId(), sink);

								  // 提交到撮合队列
								  this.publish(OrderEventType.NEW_ORDER.code, savedOrder);

								  // 可选：增加超时清理，避免永远挂起
								  sink.onCancel(() -> MonoSinkManage.remove(savedOrder.getOrderId()));
								  sink.onDispose(() -> MonoSinkManage.remove(savedOrder.getOrderId()));
							  }))
							  .doOnError(e -> log.error("订单提交失败", e));
	}

	private void publish(byte eventTypeCode, OriginOrder originOrder) {
		log.info("publish orderId :{}", originOrder.getOrderId());

		long seq = ringBuffer.next();
		OrderEvent event = ringBuffer.get(seq);

		event.setEventTypeCode(eventTypeCode);
		OrderInfo orderInfo = event.getOrderInfo();
		BeanUtils.copyProperties(originOrder, orderInfo);
		orderInfo.setSymbol(originOrder.getSymbol());
		orderInfo.setTif(originOrder.getTif());
		orderInfo.setSide(originOrder.getSide());
		orderInfo.setType(originOrder.getType());
		ringBuffer.publish(seq);
	}

	public Mono<Void> cancel(OriginOrder order) {
		this.publish(OrderEventType.CANCEL_ORDER.code, order);
		return Mono.empty();
	}

}
