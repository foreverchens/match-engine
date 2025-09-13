package icu.match.service.match;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;

import icu.match.common.OrderStatus;
import icu.match.core.interfaces.MatchEventProcessor;
import icu.match.core.model.MatchTrade;
import icu.match.web.model.OriginOrder;
import icu.match.web.service.MatchTradeService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/9/10 
 */
@Slf4j
@Component
public class SimpleMatchEventProcess implements MatchEventProcessor {


	@Resource
	private MatchTradeService matchTradeService;

	@Resource
	private R2dbcEntityTemplate template;

	/**
	 *  先入库 二阶段提交后在发布
	 */
	@Override
	public void onTraded(MatchTrade trade) {
		log.info("onTraded :{}", trade.getMatchSeq());
		matchTradeService.saveTrade(trade)
						 .block();
	}

	@Override
	public void onFilled(int symbol, long orderId) {
		log.info("onFilled symbol :{} orderId :{}", symbol, orderId);
		deal(symbol, orderId, OrderStatus.FILLED.val).block();

	}


	/**
	 *  撤单
	 *  订单模块 改状态
	 *  账户模块 释放余额
	 */
	@Override
	public void onOrderCancelled(int symbol, long orderId, long qty) {
		log.info("onOrderCancelled symbol :{} orderId :{}", symbol, orderId);
		this.deal(symbol, orderId, qty > 0
								   ? OrderStatus.PARTIALLY_FILLED_CANCELED.val
								   : OrderStatus.CANCELED.val)
			.block();

	}

	/**
	 * 据单
	 *  订单模块 改状态
	 *  账户模块 释放余额
	 */
	@Override
	public void onOrderRejected(int symbol, long orderId) {
		log.info("onOrderRejected symbol :{} orderId :{}", symbol, orderId);
		this.deal(symbol, orderId, OrderStatus.REJECTED.val)
			.block();
	}

	@Override
	public void onCompleted() {

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
