package icu.match.service.match.model.base;

import icu.match.service.match.model.Order;
import icu.match.service.match.model.Trade;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
public abstract class OrderBook {


	/**
	 * 添加订单
	 * 如果发生撮合 将返回成交列表 同时order发生变化
	 * @param o
	 * @return
	 */
	protected abstract List<Trade> submit(Order o);


	/**
	 * 删除订单
	 *
	 * @param orderId id
	 * @param price 用于定位在哪个槽
	 * @return
	 */
	protected abstract Order cancel(Long orderId, BigDecimal price);


	/**
	 * 撮合
	 *
	 * @param order
	 * @return
	 */
	protected abstract List<Trade> match(Order order);

}
