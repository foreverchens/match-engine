package icu.service.disruptor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import icu.common.OrderSide;
import icu.common.OrderType;
import icu.model.Order;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Component
public class Test implements CommandLineRunner {

	@Resource
	OrderService orderService;

	@Override
	public void run(String... args) {

		Order bid1 = new Order("BTCUSDT", 10, 10, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000),
							   BigDecimal.ONE);

		Order bid2 = new Order("BTCUSDT", 11, 11, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000),
							   BigDecimal.ONE);

		Order bid3 = new Order("BTCUSDT", 12, 12, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000),
							   BigDecimal.ONE);
		List<Order> orders = Arrays.asList(bid1, bid2, bid3);
		orders.forEach(orderService::publishOrder);
	}
}
