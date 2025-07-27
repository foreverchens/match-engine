package icu.model.base;

import icu.model.Order;
import icu.model.Trade;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
public abstract class BaseOrderBook {

	/**
	 * 添加订单
	 * 添加前尝试撮合
	 *
	 * @param o
	 * @return
	 */
	protected abstract List<Trade> push(Order o);


	/**
	 * 删除订单
	 *
	 * @param orderId id
	 * @param price 用于定位在哪个槽
	 * @return
	 */
	protected abstract Order remove(Long orderId, BigDecimal price);


	/**
	 * 撮合
	 *
	 * @param order
	 * @return
	 */
	protected abstract List<Trade> match(Order order);

}
