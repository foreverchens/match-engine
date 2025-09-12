package icu.match.service.match;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import com.lmax.disruptor.RingBuffer;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import icu.match.core.interfaces.MatchEventProcessor;
import icu.match.core.model.MatchTrade;
import icu.match.service.disruptor.trade.TradeEvent;
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
	private RingBuffer<TradeEvent> tradeEventRingBuffer;

	/**
	 * 结果的发布 通知订单 账户模块等等
	 */
	@Override
	public void onTraded(MatchTrade source) {
		log.info("onTraded :{}", source.getMatchSeq());
		long next = tradeEventRingBuffer.next();
		TradeEvent tradeEvent = tradeEventRingBuffer.get(next);
		MatchTrade target = tradeEvent.getMatchTrade();
		BeanUtils.copyProperties(source, target);
		tradeEventRingBuffer.publish(next);
	}

	/**
	 *  撤单
	 *  订单模块 改状态
	 *  账户模块 释放余额
	 */
	@Override
	public void onOrderCancelled(int symbol, long orderId, long qty) {
		log.info("onOrderCancelled symbol :{} orderId :{}  qty :{}", symbol, orderId, qty);
	}

	/**
	 * 据单
	 *  订单模块 改状态
	 *  账户模块 释放余额
	 */
	@Override
	public void onOrderRejected(int symbol, long orderId) {
		log.info("onOrderRejected symbol :{} orderId :{}", symbol, orderId);
	}
}
