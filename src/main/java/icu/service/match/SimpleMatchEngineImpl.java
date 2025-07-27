package icu.service.match;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import icu.model.Order;
import icu.model.OrderBook;
import icu.model.Trade;
import icu.service.match.interfac.MatchEngine;
import icu.util.CzClient;
import lombok.extern.slf4j.Slf4j;

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

	private Map<String, OrderBook> orderBookMap;

	@Resource
	private CzClient czClient;

	@Override
	public void submit(Order order) {
		String symbol = order.getSymbol();
		List<Trade> trades = orderBookMap.get(symbol).push(order);
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
		orderBookMap.put(symbol, new OrderBook(symbol, 1024, curP, step, 20));
	}
}
