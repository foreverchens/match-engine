package icu.service.match.model;

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
public class RingBufferOrderBookTest {

	int len = 8;

	BigDecimal curP = BigDecimal.valueOf(1000);

	BigDecimal step = BigDecimal.ONE;


	Order bid1 = new Order("BTCUSDT", 10, 10, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE);

	Order bid2 = new Order("BTCUSDT", 11, 11, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE);

	Order bid3 = new Order("BTCUSDT", 12, 12, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE);

	Order bid4 = new Order("BTCUSDT", 13, 13, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000),
						   BigDecimal.ONE);

	Order ask1 = new Order("BTCUSDT", 20, 20, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000),
						   BigDecimal.ONE);

	Order ask2 = new Order("BTCUSDT", 21, 21, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1001),
						   BigDecimal.ONE);

	Order ask3 = new Order("BTCUSDT", 22, 22, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1001),
						   BigDecimal.ONE);


	RingBufferOrderBook ringBufferOrderBook;

	@Before
	public void before() {
		RingBufferOrderBook.Params btcusdt = RingBufferOrderBook.Params.builder().symbol("BTCUSDT").len(len).curP(curP)
																	   .step(step).reBalId(10).build();
		ringBufferOrderBook = new RingBufferOrderBook(btcusdt);
	}

	@Test
	public void simplePushTest() {
		ringBufferOrderBook.submit(bid1);
		ringBufferOrderBook.submit(bid2);
		ringBufferOrderBook.submit(bid3);
		assert ringBufferOrderBook.print().get(0).equals("price:[999],  orderList:[10, 11, 12]");

		ringBufferOrderBook.submit(ask2);
		ringBufferOrderBook.submit(ask3);
		assert ringBufferOrderBook.print().get(1).equals("price:[1001],  orderList:[21, 22]");
	}

	@Test
	public void simpleRemoveTest() {
		ringBufferOrderBook.submit(bid1);
		ringBufferOrderBook.submit(bid2);
		ringBufferOrderBook.submit(bid3);
		assert ringBufferOrderBook.print().get(0).equals("price:[999],  orderList:[10, 11, 12]");

		ringBufferOrderBook.cancel(bid1.getOrderId(), bid1.getPrice());
		assert ringBufferOrderBook.print().get(0).equals("price:[999],  orderList:[11, 12]");
		ringBufferOrderBook.cancel(bid2.getOrderId(), bid2.getPrice());
		ringBufferOrderBook.cancel(bid3.getOrderId(), bid3.getPrice());
		assert ringBufferOrderBook.print().size() == 0;
	}

	@Test
	public void simpleBidMatchTest() {
		ringBufferOrderBook.submit(ask1);
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[20]");
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000),
						  BigDecimal.ONE));
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[20, 30]");
		// match
		List<Trade> trades = ringBufferOrderBook.submit(
				new Order("BTCUSDT", 31, 31, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000),
						  BigDecimal.ONE));
		assert trades.size() == 1;
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[30]");
	}

	@Test
	public void simpleAskMatchTest() {
		ringBufferOrderBook.submit(bid4);
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[13]");
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000),
						  BigDecimal.ONE));
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[13, 30]");
		// match
		List<Trade> trades = ringBufferOrderBook.submit(
				new Order("BTCUSDT", 31, 31, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(1000),
						  BigDecimal.ONE));
		assert trades.size() == 1;
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[30]");
	}

	@Test
	public void simpleMarketMatchTest() {
		ringBufferOrderBook.submit(bid4);
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[13]");
		// match
		List<Trade> trades = ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.ASK, OrderType.MARKET, BigDecimal.valueOf(999),
						  BigDecimal.ONE));
		assert trades.size() == 1;
		assert ringBufferOrderBook.print().size() == 0;
	}

	@Test
	public void overLimitMatchTest() {
		ringBufferOrderBook.submit(bid4);
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[13]");
		// match
		List<Trade> trades = ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE));
		assert trades.size() == 1;
		assert ringBufferOrderBook.print().size() == 0;

		ringBufferOrderBook.submit(ask1);
		assert ringBufferOrderBook.print().get(0).equals("price:[1000],  orderList:[20]");
		// match
		trades = ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1001),
						  BigDecimal.ONE));
		assert trades.size() == 1;
		assert ringBufferOrderBook.print().size() == 0;
	}

	@Test
	public void lowTreePushRemoveTest() {
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(900), BigDecimal.ONE));
		assert ringBufferOrderBook.print().size() == 1;
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 31, 31, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(800), BigDecimal.ONE));
		assert ringBufferOrderBook.print().size() == 2;
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 32, 32, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(901), BigDecimal.ONE));
		assert ringBufferOrderBook.print().size() == 3;

		ringBufferOrderBook.cancel(30L, BigDecimal.valueOf(900));
		assert ringBufferOrderBook.print().size() == 2;
		ringBufferOrderBook.cancel(31L, BigDecimal.valueOf(800));
		assert ringBufferOrderBook.print().size() == 1;
		ringBufferOrderBook.cancel(32L, BigDecimal.valueOf(901));
		assert ringBufferOrderBook.print().size() == 0;
	}


	@Test
	public void canFillMarketOrderTest() {
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE));
		ringBufferOrderBook.submit(
				new Order("BTCUSDT", 30, 30, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.TEN));
		assert ringBufferOrderBook.canFillMarketOrder(OrderSide.ASK, BigDecimal.valueOf(11));
	}
}
