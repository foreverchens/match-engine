package icu.web.service;

import org.springframework.stereotype.Service;

import icu.service.disruptor.DisruptorService;
import icu.service.global.MonoSinkManage;
import icu.util.ModelUtil;
import icu.web.model.OrderResult;
import icu.web.model.OriginOrder;
import icu.web.repo.OrderRepository;
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

	@Resource
	private OrderRepository orderRepository;

	public Mono<OrderResult> submit(OriginOrder originOrder) {
		return Mono.create(sink -> {
			log.info("原始订单:{}", originOrder);
			// 持久化
			orderRepository.save(originOrder)
						   .doOnError(sink::error)
						   .subscribe(savedOrder -> {
							   try {
								   log.info("originOrder saved");
								   // 注册sink
								   MonoSinkManage.put(originOrder.getOrderId(), sink);

								   // 提交事件队列
								   disruptorService.publish(ModelUtil.originOrderToOrder(originOrder));
							   } catch (Exception e) {
								   sink.error(e);
							   }
						   }, sink::error);
		});
	}

}
