package icu.match.service.disruptor.match;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.service.disruptor.AbstractDisruptorProvider;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/9/10 
 */
@Component
public class MatchEventDisruptorProvider extends AbstractDisruptorProvider<MatchEvent> {

	@Resource
	private MatchEventHandler matchEventHandler;

	@Override
	protected EventFactory<MatchEvent> eventFactory() {
		return MatchEvent::new;
	}

	@Override
	protected String threadName() {
		return "disruptor-match-trade-event-consumer";
	}

	@Override
	protected EventHandler<MatchEvent>[] eventHandlers() {
		return new EventHandler[]{matchEventHandler};
	}
}
