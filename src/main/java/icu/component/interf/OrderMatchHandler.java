package icu.component.interf;

import icu.service.Order;
import icu.service.Trade;

import java.util.List;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public interface OrderMatchHandler {

	/**
	 * 
	 * @param order
	 * @return
	 */
	List<Trade>  handle(Order order);
}
