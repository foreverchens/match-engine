package icu.component;

import org.springframework.stereotype.Component;

import icu.component.interf.OrderMatchHandler;
import icu.model.Order;
import icu.model.Trade;

import java.util.List;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Component
public class LimitOrderMatch implements OrderMatchHandler {

	@Override
	public List<Trade> handle(Order order) {
		return null;
	}
}
