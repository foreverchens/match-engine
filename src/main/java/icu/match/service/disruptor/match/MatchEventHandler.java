package icu.match.service.disruptor.match;

import com.lmax.disruptor.EventHandler;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;

import icu.match.core.model.MatchTrade;
import icu.match.web.model.OriginOrder;
import icu.match.web.service.MatchTradeService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class MatchEventHandler implements EventHandler<MatchEvent> {

	@Resource
	private R2dbcEntityTemplate template;

	@Resource
	private MatchTradeService matchTradeService;


	@Override
	public void onEvent(MatchEvent matchEvent, long sequence, boolean endOfBatch) {
		if (matchEvent.getOrderStatus() == null) {
			// 成交事件
			MatchTrade matchTrade = matchEvent.getMatchTrade();
			log.info("received trade event :{}", matchTrade.getMatchSeq());
			matchTradeService.saveTrade(matchTrade)
							 .doOnSuccess(s -> {
								 // 成功后发布 成交事件 到 MQ/Redis
							 })
							 .doOnError(e -> log.error("FATAL stream error", e))
							 .retry()
							 // 异步执行有脏读风险 可对数据进行克隆
							 // .subscribe()
							 .block();
		} else {
			int symbol = matchEvent.getSymbol();
			long orderId = matchEvent.getOrderId();
			int status = matchEvent.getOrderStatus().val;
			matchEvent.reset();
			log.info("received status update event orderId :{}", orderId);
			this.deal(symbol, orderId, status)
				.doOnSuccess(saved -> {
					// 成功后发布 订单状态变更事件 到 MQ/Redis
				})
				.doOnError(e -> log.error("FATAL stream error", e))
				.retry()
				.then(Mono.defer(Mono::empty))
				// .subscribe()
				.block();
		}

	}

	private Mono<Integer> deal(int symbol, long orderId, int status) {
		return template.update(OriginOrder.class)
					   .matching(Query.query(Criteria.where("order_id")
													 .is(orderId)
													 .and("symbol")
													 .is(symbol)))
					   .apply(Update.update("status", status));
	}
}
