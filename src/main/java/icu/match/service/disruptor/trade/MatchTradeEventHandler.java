package icu.match.service.disruptor.trade;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.core.model.MatchTrade;
import icu.match.web.service.MatchTradeService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class MatchTradeEventHandler implements EventHandler<TradeEvent> {

	private final Sinks.Many<MatchTrade> sink = Sinks.many()
													 .unicast()
													 .onBackpressureBuffer();

	@Resource
	private MatchTradeService matchTradeService;

	@PostConstruct
	private void init() {
		sink.asFlux()
			.concatMap(t -> matchTradeService.saveTrade(t)
											 .doOnSuccess(saved -> log.info("tradeId: {}", saved.getMatchSeq()))
											 .then(Mono.defer(() -> publish(t))), 1)
			.doOnError(e -> log.error("FATAL stream error", e))
			.retry()
			.subscribe();
	}

	private Mono<Void> publish(MatchTrade t) {
		// 成功后发布到 MQ/Redis
		return Mono.empty();
	}

	@Override
	public void onEvent(TradeEvent tradeEvent, long sequence, boolean endOfBatch) {
		MatchTrade matchTrade = tradeEvent.getMatchTrade();
		log.info("received trade event :{}", matchTrade.getMatchSeq());
		// 深拷贝/不可变，避免 Disruptor 复用对象被改
		var r = sink.tryEmitNext(matchTrade);
		if (r.isFailure()) {
			log.error("emit failed: {}", r);
		}
	}
}
