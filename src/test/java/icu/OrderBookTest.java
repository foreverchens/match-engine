// package icu;
//
// import org.junit.Test;
//
// import icu.common.OrderSide;
// import icu.common.OrderType;
// import icu.model.Order;
// import icu.model.OrderBook;
// import lombok.extern.slf4j.Slf4j;
//
// import java.math.BigDecimal;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Random;
//
// /**
//  * @author yyy
//  * @tg t.me/ychen5325
//  */
// @Slf4j
// public class OrderBookTest {
//
//
//
// 	@Test
// 	public void treeRemoveTest() {
// 		int len = 10;
// 		BigDecimal curPrice = BigDecimal.valueOf(1000);
// 		BigDecimal step = BigDecimal.valueOf(1);
// 		List<Order> dataList = new ArrayList<>();
//
// 		Random rand = new Random();
// 		int minP = 995;
// 		int maxP = 1005;
// 		long idCounter = 1;
// 		for (int i = 0; i < 10; i++) {
// 			String side = rand.nextBoolean()
// 						  ? OrderSide.BID
// 						  : OrderSide.SELL;
// 			long price = minP + rand.nextInt((int) (maxP - minP + 1));
// 			dataList.add(new Order(idCounter++, side, OrderType.LIMIT, BigDecimal.valueOf(price), null));
// 		}
// 		OrderBook orderBook = new OrderBook(len, curPrice, step, 20, dataList);
// 		orderBook.print();
//
//
// 		log.info("low zone try push new order ");
// 		// 在添加
// 		minP = 980;
// 		maxP = 985;
// 		dataList = new ArrayList<>();
// 		for (int i = 10; i < 20; i++) {
// 			String side = rand.nextBoolean()
// 						  ? OrderSide.BID
// 						  : OrderSide.SELL;
// 			long price = minP + rand.nextInt((int) (maxP - minP + 1));
// 			dataList.add(new Order(idCounter++, side, OrderType.LIMIT, BigDecimal.valueOf(price), null));
// 		}
// 		dataList.forEach(orderBook::push);
// 		orderBook.print();
//
//
// 		log.info("remove low zone data");
// 		dataList.forEach(e -> {
// 			orderBook.remove(e.getOrderId(), e.getPrice());
// 		});
// 		orderBook.print();
//
// 		log.info("high zone try push new order ");
// 		// 在添加
// 		minP = 1011;
// 		maxP = 1016;
// 		dataList = new ArrayList<>();
// 		for (int i = 20; i < 30; i++) {
// 			String side = rand.nextBoolean()
// 						  ? OrderSide.BID
// 						  : OrderSide.SELL;
// 			long price = minP + rand.nextInt((int) (maxP - minP + 1));
// 			dataList.add(new Order(idCounter++, side, OrderType.LIMIT, BigDecimal.valueOf(price), null));
// 		}
// 		dataList.forEach(orderBook::push);
// 		orderBook.print();
//
//
// 		log.info("remove high zone data");
// 		dataList.forEach(e -> {
// 			orderBook.remove(e.getOrderId(), e.getPrice());
// 		});
// 		orderBook.print();
// 	}
//
// 	@Test
// 	public void simpleMatch() {
// 		int len = 10;
// 		BigDecimal curPrice = BigDecimal.valueOf(1000);
// 		BigDecimal step = BigDecimal.valueOf(1);
// 		List<Order> dataList = new ArrayList<>();
//
// 		Random rand = new Random();
// 		int minP = 995;
// 		int maxP = 1005;
// 		long idCounter = 1;
// 		dataList.add(new Order(100, OrderSide.BID, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE));
// 		for (int i = 0; i < 10; i++) {
//
// 			long price = minP + rand.nextInt((int) (maxP - minP + 1));
// 			String side = price < curPrice.intValue()
// 						  ? OrderSide.BID
// 						  : OrderSide.SELL;
// 			dataList.add(new Order(idCounter++, side, OrderType.LIMIT, BigDecimal.valueOf(price), BigDecimal.ONE));
// 		}
// 		dataList.add(new Order(101, OrderSide.SELL, OrderType.LIMIT, BigDecimal.valueOf(1000), BigDecimal.ONE));
// 		dataList.add(new Order(102, OrderSide.SELL, OrderType.LIMIT, BigDecimal.valueOf(999), BigDecimal.ONE));
// 		OrderBook orderBook = new OrderBook(len, curPrice, step, 20, dataList);
// 		orderBook.print();
// 	}
// }
