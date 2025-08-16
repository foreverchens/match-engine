package icu.match.web.service;

import org.springframework.stereotype.Service;

import icu.match.util.ModelUtil;
import icu.service.disruptor.DisruptorService;
import icu.service.global.MonoSinkManage;
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
		log.info("原始订单:{}", originOrder);

		return orderRepository.save(originOrder)
							  .flatMap(savedOrder -> Mono.<OrderResult>create(sink -> {
								  // 注册sink，后续由撮合线程调用 sink.success() / sink.error()
								  MonoSinkManage.put(savedOrder.getOrderId(), sink);

								  // 提交到撮合队列
								  disruptorService.publish(ModelUtil.originOrderToOrder(savedOrder));

								  // 可选：增加超时清理，避免永远挂起
								  sink.onCancel(() -> MonoSinkManage.remove(savedOrder.getOrderId()));
								  sink.onDispose(() -> MonoSinkManage.remove(savedOrder.getOrderId()));
							  }))
							  .doOnError(e -> log.error("订单提交失败", e));
	}


}
