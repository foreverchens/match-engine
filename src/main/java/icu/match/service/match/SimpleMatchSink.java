package icu.match.service.match;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import com.lmax.disruptor.RingBuffer;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import icu.match.core.OrderNode;
import icu.match.core.interfaces.MatchSink;
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
public class SimpleMatchSink implements MatchSink {

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

	@Override
	public void onOrderRested(OrderNode o) {

	}

	@Override
	public void onOrderCancelled(long orderId, String reason) {

	}

	@Override
	public void onOrderRejected(OrderNode o, String reason) {

	}
}
