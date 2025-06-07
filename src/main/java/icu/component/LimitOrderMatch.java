package icu.component;

import icu.component.interf.OrderMatchHandler;
import icu.model.Order;
import icu.model.Trade;

import java.util.List;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public class LimitOrderMatch implements OrderMatchHandler {

	@Override
	public List<Trade> handle(Order order) {
		return null;
	}
}
