package icu.service.match;

import cn.hutool.core.util.ObjectUtil;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import icu.common.OrderSide;
import icu.common.OrderType;
import icu.service.match.interfac.MatchEngine;
import icu.service.match.model.Order;
import icu.service.match.model.RingBufferOrderBook;
import icu.service.match.model.Trade;
import icu.service.web.MonoSinkManage;
import icu.service.web.model.OrderResult;
import icu.util.CzClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class SimpleMatchEngineImpl implements MatchEngine, InitializingBean {

	private Map<String, RingBufferOrderBook> orderBookMap;

	@Resource
	private CzClient czClient;

	@Override
	public void submit(Order order) {
		log.info("处理订单:{}", order);
		if (ObjectUtil.isNotNull(order)) {
			MonoSink<OrderResult> sink = MonoSinkManage.getSink(order.getOrderId());
			if (ObjectUtil.isNotNull(sink)) {
				sink.success(OrderResult.builder().orderId(1L).type(OrderType.LIMIT).userId(1L).status(1).symbol("bu")
										.side(OrderSide.ASK).price(BigDecimal.ONE).origQty(BigDecimal.ONE).build());
			}
			return;
		}
		String symbol = order.getSymbol();
		List<Trade> trades = orderBookMap.get(symbol).submit(order);
		if (!trades.isEmpty()) {
			trades.forEach(e -> log.info(e.toString()));
		}
	}

	@Override
	public void afterPropertiesSet() {
		orderBookMap = new HashMap<>(16);
		String symbol = "BTCUSDT";
		BigDecimal curP = czClient.getPrice(symbol).setScale(0, RoundingMode.DOWN);
		BigDecimal step = BigDecimal.ONE;
		RingBufferOrderBook.Params params = RingBufferOrderBook.Params.builder().symbol(symbol).len(1024).curP(curP)
																	  .step(step).reBalId(20).build();
		orderBookMap.put(symbol, new RingBufferOrderBook(params));
	}
}
