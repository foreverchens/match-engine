package icu.model;

import org.junit.Before;
import org.junit.Test;

import icu.common.OrderSide;
import icu.common.OrderType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
public class OrderBookTest {

	int len = 8;

	BigDecimal curP = BigDecimal.valueOf(1000);

	BigDecimal step = BigDecimal.ONE;


	Order bid1 = new Order(10, 10, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE);

	Order bid2 = new Order(11, 11, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE);

	Order bid3 = new Order(12, 12, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE);

	Order bid4 = new Order(13, 13, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE);

	Order ask1 = new Order(20, 20, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE);

	Order ask2 = new Order(21, 21, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1001), BigDecimal.ONE);

	Order ask3 = new Order(22, 22, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1001), BigDecimal.ONE);


	OrderBook orderBook;

	@Before
	public void before() {
		orderBook = new OrderBook("BTCUSDT", len, curP, step, 20);
		// orderBook.push(bid1);
		// orderBook.push(bid2);
		// orderBook.push(bid3);
		// orderBook.push(ask1);
		// orderBook.push(ask2);
		// orderBook.push(ask3);
	}

	@Test
	public void simplePushTest() {
		orderBook.push(bid1);
		orderBook.push(bid2);
		orderBook.push(bid3);
		assert orderBook.print().get(3).equals("price:[999],  orderList:[10, 11, 12]");

		orderBook.push(ask2);
		orderBook.push(ask3);
		assert orderBook.print().get(5).equals("price:[1001],  orderList:[21, 22]");
	}

	@Test
	public void simpleRemoveTest() {
		orderBook.push(bid1);
		orderBook.push(bid2);
		orderBook.push(bid3);
		assert orderBook.print().get(3).equals("price:[999],  orderList:[10, 11, 12]");

		orderBook.remove(bid1.getOrderId(), bid1.getPrice());
		assert orderBook.print().get(3).equals("price:[999],  orderList:[11, 12]");
		orderBook.remove(bid2.getOrderId(), bid2.getPrice());
		orderBook.remove(bid3.getOrderId(), bid3.getPrice());
		assert orderBook.print().get(3).equals("price:[999],  orderList:[]");
	}

	@Test
	public void simpleBidMatchTest() {
		orderBook.push(ask1);
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[20]");
		orderBook.push(new Order(30, 30, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE));
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[20, 30]");
		// match
		List<Trade> trades = orderBook.push(
				new Order(31, 31, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE));
		assert trades.size() == 1;
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[30]");
	}

	@Test
	public void simpleAskMatchTest() {
		orderBook.push(bid4);
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[13]");
		orderBook.push(new Order(30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE));
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[13, 30]");
		// match
		List<Trade> trades = orderBook.push(
				new Order(31, 31, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE));
		assert trades.size() == 1;
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[30]");
	}

	@Test
	public void simpleMarketMatchTest() {
		orderBook.push(bid4);
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[13]");
		// match
		List<Trade> trades = orderBook.push(
				new Order(30, 30, OrderSide.ASK, OrderType.MARKET, BigDecimal.valueOf(999), BigDecimal.ONE));
		assert trades.size() == 1;
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[]");
	}

	@Test
	public void overLimitMatchTest() {
		orderBook.push(bid4);
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[13]");
		// match
		List<Trade> trades = orderBook.push(
				new Order(30, 30, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE));
		assert trades.size() == 1;
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[]");

		orderBook.push(ask1);
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[20]");
		// match
		trades = orderBook.push(
				new Order(30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1001), BigDecimal.ONE));
		assert trades.size() == 1;
		assert orderBook.print().get(4).equals("price:[1000],  orderList:[]");
	}

	@Test
	public void lowTreePushRemoveTest() {
		orderBook.push(new Order(30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(900), BigDecimal.ONE));
		assert orderBook.print().size() == 1;
		orderBook.push(new Order(31, 31, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(800), BigDecimal.ONE));
		assert orderBook.print().size() == 2;
		orderBook.push(new Order(32, 32, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(901), BigDecimal.ONE));
		assert orderBook.print().size() == 3;

		orderBook.remove(30L, BigDecimal.valueOf(900));
		assert orderBook.print().size() == 2;
		orderBook.remove(31L, BigDecimal.valueOf(800));
		assert orderBook.print().size() == 1;
		orderBook.remove(32L, BigDecimal.valueOf(901));
		assert orderBook.print().size() == 0;
	}

}
