package icu.model;

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

	Order bid1 = new Order(10, 10, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(99), BigDecimal.ONE);

	Order bid2 = new Order(11, 11, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(99), BigDecimal.ONE);

	Order ask3 = new Order(20, 20, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(111), BigDecimal.ONE);

	Order ask4 = new Order(21, 21, OrderSide.ASK, OrderType.LIMIT, BigDecimal.valueOf(111), BigDecimal.ONE);

	@Before
	public void before() {
		price = BigDecimal.valueOf(100);
		priceZone = new PriceZone(price);
	}

	@Test
	public void simplePushTest() {
		priceZone.push(bid1);
		List<Long> orderIdList = priceZone.print();
		Assert.assertEquals(orderIdList.size(), 1);
	}

	@Test
	public void simpleRemoveTest() {
		Order order = priceZone.remove(10L);
		Assert.assertNull(order);

		priceZone.push(bid1);
		order = priceZone.remove(10L);
		Assert.assertEquals(10L, order.orderId);
	}

	@Test
	public void simplePeekTest() {
		Order order = priceZone.peek();
		Assert.assertNull(order);

		priceZone.push(bid1);
		order = priceZone.peek();
		Assert.assertEquals(10L, order.orderId);
	}

	@Test
	public void simplePrintTest() {
		List<Long> list = priceZone.print();
		Assert.assertEquals(list.size(), 0);

		priceZone.push(bid1);
		list = priceZone.print();

		Assert.assertEquals(10L, (long) list.get(0));
	}

	/**
	 * 多次push 穿插删除检查id顺序
	 */
	@Test
	public void multiPushTest() {
		Assert.assertTrue(priceZone.isEmpty());
		priceZone.push(bid1);
		Assert.assertEquals(priceZone.print().toString(), "[10]");
		Assert.assertFalse(priceZone.isEmpty());

		Assert.assertEquals(10, priceZone.peek().orderId);

		priceZone.push(bid2);
		Assert.assertEquals(priceZone.print().toString(), "[10, 11]");

		priceZone.push(new Order(12, 12, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(99), BigDecimal.ONE));
		Assert.assertEquals(priceZone.print().toString(), "[10, 11, 12]");
		Assert.assertEquals(10, priceZone.peek().orderId);

		priceZone.remove(11);
		Assert.assertEquals(priceZone.print().toString(), "[10, 12]");

		priceZone.remove(10);
		Assert.assertEquals(priceZone.print().toString(), "[12]");
		Assert.assertEquals(12, priceZone.peek().orderId);

		priceZone.push(bid1);
		Assert.assertEquals(priceZone.print().toString(), "[12, 10]");

		priceZone.remove(10);
		Assert.assertEquals(priceZone.print().toString(), "[12]");
		Assert.assertFalse(priceZone.isEmpty());

		priceZone.remove(12);
		Assert.assertEquals(priceZone.print().toString(), "[]");
		Assert.assertTrue(priceZone.isEmpty());

	}

}
