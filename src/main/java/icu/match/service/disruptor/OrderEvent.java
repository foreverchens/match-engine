package icu.match.service.disruptor;

import icu.match.service.match.model.Order;
import lombok.Data;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Data
public class OrderEvent {

	private Order order;

}
