package icu.match.service.disruptor;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.core.MatchingEngine;
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
		log.info("订单处理:{}", event.getOrder());
		// matchEngine.submitLimit(event.getOrder());
	}
}
