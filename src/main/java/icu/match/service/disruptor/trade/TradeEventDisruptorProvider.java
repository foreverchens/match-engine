package icu.match.service.disruptor.trade;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.core.model.MatchTrade;
import icu.match.service.disruptor.AbstractDisruptorProvider;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/9/10 
 */
@Component
public class TradeEventDisruptorProvider extends AbstractDisruptorProvider<TradeEvent> {

	@Resource
	private MatchTradeEventHandler matchTradeEventHandler;

	@Override
	protected EventFactory<TradeEvent> eventFactory() {
		return () -> new TradeEvent(new MatchTrade());
	}

	@Override
	protected String threadName() {
		return "disruptor-match-trade-event-consumer";
	}

	@Override
	protected EventHandler<TradeEvent>[] eventHandlers() {
		return new EventHandler[]{matchTradeEventHandler};
	}
}
