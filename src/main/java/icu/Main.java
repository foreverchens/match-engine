package icu;

public class Main {
	// public static void main(String[] args) {
	// 	// 初始化中间价为100，步长为1，数组大小为101（中心索引50）
	// 	long initialMid = 100;
	// 	int zoneSize = 101;
	// 	long priceStep = 1;
	//
	// 	PriceBook buyBook = new PriceBook(initialMid, priceStep, zoneSize);
	// 	PriceBook sellBook = new PriceBook(initialMid, priceStep, zoneSize);
	//
	// 	MatchingEngine engine = new MatchingEngine(buyBook, sellBook);
	//
	// 	// 示例：添加几个订单并撮合
	// 	engine.addOrder(new Order(1L, OrderSide.BUY, 102, 5));
	// 	engine.addOrder(new Order(2L, OrderSide.SELL, 101, 3));
	// 	engine.addOrder(new Order(3L, OrderSide.SELL, 102, 4));
	// 	engine.addOrder(new Order(4L, OrderSide.BUY, 101, 2));
	//
	// 	// 手动滑动价格至102
	// 	buyBook.adjustMidPrice(102);
	// 	sellBook.adjustMidPrice(102);
	//
	// 	// 添加更多订单
	// 	engine.addOrder(new Order(5L, OrderSide.BUY, 103, 1));
	// 	engine.addOrder(new Order(6L, OrderSide.SELL, 103, 1));
	// }
	//
	//
	//
	// /**
	//  * 价格级别：管理该价格下的所有订单，使用双向链表，并通过hash表快速定位
	//  */
	//
	// /**
	//  * 热区+远端价格结构：环形数组+两棵红黑树
	//  */
	//
	// /**
	//  * 核心撮合逻辑
	//  */
	// static class MatchingEngine {
	// 	private PriceBook buyBook;
	// 	private PriceBook sellBook;
	// 	private Map<Long, Order> allOrders = new HashMap<>();
	//
	// 	public MatchingEngine(PriceBook buyBook, PriceBook sellBook) {
	// 		this.buyBook = buyBook;
	// 		this.sellBook = sellBook;
	// 	}
	//
	// 	public void addOrder(Order o) {
	// 		allOrders.put(o.orderId, o);
	// 		// 先撮合
	// 		match(o);
	// 		// 剩余挂单
	// 		if (o.quantity > 0) {
	// 			if (o.type == OrderSide.BUY) {
	// 				buyBook.addOrder(o);
	// 			}
	// 			else {
	// 				sellBook.addOrder(o);
	// 			}
	// 		}
	// 	}
	//
	// 	private void match(Order incoming) {
	// 		PriceBook opposite = incoming.type == OrderSide.BUY ? sellBook : buyBook;
	// 		boolean isBuy = incoming.type == OrderSide.BUY;
	// 		while (incoming.quantity > 0) {
	// 			PriceLevel best = opposite.bestLevel(isBuy);
	// 			if (best == null) {
	// 				break;
	// 			}
	// 			// 价格判断
	// 			if ((isBuy && best.price > incoming.price) || (!isBuy && best.price < incoming.price)) {
	// 				break;
	// 			}
	// 			// 链表头撮合
	// 			Order head = best.head;
	// 			long traded = Math.min(incoming.quantity, head.quantity);
	// 			incoming.quantity -= traded;
	// 			head.quantity -= traded;
	// 			System.out.printf("Trade: %d @ price %d between %d and %d\n", traded, best.price, incoming.orderId, head.orderId);
	// 			if (head.quantity == 0) {
	// 				best.removeOrder(head);
	// 				allOrders.remove(head.orderId);
	// 			}
	// 			if (best.isEmpty()) {
	// 				opposite.removeLevel(best);
	// 			}
	// 		}
	// 	}
	// }
}
