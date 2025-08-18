package icu.match.service.disruptor;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.common.CallResult;
import icu.match.common.OrderStatus;
import icu.match.core.MatchingEngine;
import icu.match.service.global.MonoSinkManage;
import icu.match.web.model.OrderResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {

	@Resource
	private MatchingEngine matchEngine;

	@Override
	public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
		log.info("订单处理:{}", event.getOrderInfo());
		CallResult<OrderStatus> rlt = matchEngine.submit(event.getOrderInfo());
		log.info(rlt.toString());
		MonoSinkManage.getSink(event.getOrderInfo()
									.getOrderId())
					  .success(new OrderResult());
	}
}
