package icu.match.service.match.model;

import org.junit.Assert;
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
public class PriceZoneTest {


	BigDecimal price;

	PriceZone priceZone;

	Order bid1 = new Order("BTCUSDT", 10, 10, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(99), BigDecimal.ONE);

	Order bid2 = new Order("BTCUSDT", 11, 11, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(99), BigDecimal.ONE);


	@Before
	public void before() {
		price = BigDecimal.valueOf(100);
		priceZone = new PriceZone(price);
	}

	@Test
	public void simplePushTest() {
		priceZone.submit(bid1);
		List<Long> orderIdList = priceZone.print();
		Assert.assertEquals(orderIdList.size(), 1);
	}

	@Test
	public void simplecancelTest() {
		Order order = priceZone.cancel(10L);
		Assert.assertNull(order);

		priceZone.submit(bid1);
		order = priceZone.cancel(10L);
		Assert.assertEquals(10L, order.orderId);
	}

	@Test
	public void simplePeekTest() {
		Order order = priceZone.peek();
		Assert.assertNull(order);

		priceZone.submit(bid1);
		order = priceZone.peek();
		Assert.assertEquals(10L, order.orderId);
	}

	@Test
	public void simplePrintTest() {
		List<Long> list = priceZone.print();
		Assert.assertEquals(list.size(), 0);

		priceZone.submit(bid1);
		list = priceZone.print();

		Assert.assertEquals(10L, (long) list.get(0));
	}

	/**
	 * 多次push 穿插删除检查id顺序
	 */
	@Test
	public void multiPushTest() {
		Assert.assertTrue(priceZone.isEmpty());
		priceZone.submit(bid1);
		Assert.assertEquals(priceZone.print().toString(), "[10]");
		Assert.assertFalse(priceZone.isEmpty());

		Assert.assertEquals(10, priceZone.peek().orderId);

		priceZone.submit(bid2);
		Assert.assertEquals(priceZone.print().toString(), "[10, 11]");

		priceZone.submit(
				new Order("BTCUSDT", 12, 12, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(99), BigDecimal.ONE));
		Assert.assertEquals(priceZone.print().toString(), "[10, 11, 12]");
		Assert.assertEquals(10, priceZone.peek().orderId);

		priceZone.cancel(11);
		Assert.assertEquals(priceZone.print().toString(), "[10, 12]");

		priceZone.cancel(10);
		Assert.assertEquals(priceZone.print().toString(), "[12]");
		Assert.assertEquals(12, priceZone.peek().orderId);

		priceZone.submit(bid1);
		Assert.assertEquals(priceZone.print().toString(), "[12, 10]");

		priceZone.cancel(10);
		Assert.assertEquals(priceZone.print().toString(), "[12]");
		Assert.assertFalse(priceZone.isEmpty());

		priceZone.cancel(12);
		Assert.assertEquals(priceZone.print().toString(), "[]");
		Assert.assertTrue(priceZone.isEmpty());

	}

}
