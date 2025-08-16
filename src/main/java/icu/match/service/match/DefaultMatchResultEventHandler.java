package icu.match.service.match;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;

import icu.match.service.match.interfac.MatchResultEventHandler;
import icu.match.service.match.model.MatchResultEvent;
import icu.match.service.match.model.Order;
import icu.match.service.match.model.Trade;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Slf4j
public class DefaultMatchResultEventHandler implements MatchResultEventHandler {
	@Override
	public void handle(MatchResultEvent event) {
		if (Objects.isNull(event)) {
			return;
		}
		Order order = event.getOrder();
		if (ObjectUtil.isNotNull(order)) {
			log.info("matched order is {}", order);
		}
		List<Trade> tradeList = event.getTradeList();
		if (CollectionUtil.isNotEmpty(tradeList)) {
			for (Trade trade : tradeList) {
				log.info("matched trade is {}", trade.toString());
			}
		}
	}
}
