package icu.match.service.match;

import cn.hutool.core.util.ObjectUtil;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import icu.match.service.global.MonoSinkManage;
import icu.match.service.match.interfac.MatchEngine;
import icu.match.service.match.model.Order;
import icu.match.service.match.model.RingBufferOrderBook;
import icu.match.util.CzClient;
import icu.match.util.ModelUtil;
import icu.match.web.model.OrderResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.util.HashMap;
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
		String symbol = order.getSymbol();
		orderBookMap.get(symbol)
					.submit(order);

		MonoSink<OrderResult> sink = MonoSinkManage.getSink(order.getOrderId());
		if (ObjectUtil.isNotNull(sink)) {
			sink.success(ModelUtil.orderToOrderResult(order));
		}
	}

	@Override
	public void afterPropertiesSet() {
		orderBookMap = new HashMap<>(16);
		String symbol = "BTCUSDT";
		// BigDecimal curP = czClient.getPrice(symbol).setScale(0, RoundingMode.DOWN);
		BigDecimal curP = BigDecimal.valueOf(100);
		BigDecimal step = BigDecimal.ONE;
		RingBufferOrderBook.Params params = RingBufferOrderBook.Params.builder()
																	  .symbol(symbol)
																	  .len(5)
																	  .curP(curP)
																	  .step(step)
																	  .reBalId(20)
																	  .matchResultEventHandler(
																			  new DefaultMatchResultEventHandler())
																	  .build();
		orderBookMap.put(symbol, new RingBufferOrderBook(params));
	}
}
