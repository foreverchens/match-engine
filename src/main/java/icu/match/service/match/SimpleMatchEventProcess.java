package icu.match.service.match;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import com.lmax.disruptor.RingBuffer;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import icu.match.common.OrderStatus;
import icu.match.core.interfaces.MatchEventProcessor;
import icu.match.core.model.MatchTrade;
import icu.match.service.disruptor.match.MatchEvent;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/9/10 
 */
@Slf4j
@Component
public class SimpleMatchEventProcess implements MatchEventProcessor {


	@Resource
	private RingBuffer<MatchEvent> ringBuffer;

	/**
	 *  先入库 二阶段提交后在发布
	 */
	@Override
	public void onTraded(MatchTrade trade) {
		log.info("onTraded :{}", trade.getMatchSeq());
		long next = ringBuffer.next();
		MatchEvent matchEvent = ringBuffer.get(next);
		BeanUtils.copyProperties(trade, matchEvent.getMatchTrade());
		ringBuffer.publish(next);
	}

	@Override
	public void onFilled(int symbol, long orderId) {
		log.info("onFilled symbol :{} orderId :{}", symbol, orderId);
		this.publish(symbol, orderId, OrderStatus.FILLED);

	}


	/**
	 *  撤单
	 *  订单模块 改状态
	 *  账户模块 释放余额
	 */
	@Override
	public void onOrderCancelled(int symbol, long orderId, long qty) {
		log.info("onOrderCancelled symbol :{} orderId :{}", symbol, orderId);
		this.publish(symbol, orderId, qty > 0
									  ? OrderStatus.PARTIALLY_FILLED_CANCELED
									  : OrderStatus.CANCELED);

	}

	/**
	 * 据单
	 *  订单模块 改状态
	 *  账户模块 释放余额
	 */
	@Override
	public void onOrderRejected(int symbol, long orderId) {
		log.info("onOrderRejected symbol :{} orderId :{}", symbol, orderId);
		this.publish(symbol, orderId, OrderStatus.REJECTED);
	}

	private void publish(int symbol, long orderId, OrderStatus status) {
		long next = ringBuffer.next();
		MatchEvent matchEvent = ringBuffer.get(next);
		matchEvent.setSymbol(symbol);
		matchEvent.setOrderId(orderId);
		matchEvent.setOrderStatus(status);
		ringBuffer.publish(next);
	}

}
