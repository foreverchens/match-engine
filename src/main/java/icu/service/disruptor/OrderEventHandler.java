package icu.service.disruptor;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.service.match.interfac.MatchEngine;
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
	private MatchEngine matchEngine;

	@Override
	public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
		matchEngine.submit(event.getOrder());
	}
}
