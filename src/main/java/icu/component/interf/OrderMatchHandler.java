package icu.component.interf;

import icu.model.Order;
import icu.model.Trade;

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
