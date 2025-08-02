package icu.service.web;

import org.springframework.stereotype.Service;

import icu.service.disruptor.DisruptorService;
import icu.service.match.model.Order;
import icu.service.web.model.OrderParam;
import icu.service.web.model.OrderResult;
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
	private DisruptorService disruptorService;

	public Mono<OrderResult> submit(OrderParam orderParam) {
		return Mono.create(sink -> {
			MonoSinkManage.put(orderParam.getOrderId(), sink);
			disruptorService.publish(Order.builder().orderId(orderParam.getOrderId()).build());
		});
	}

}
