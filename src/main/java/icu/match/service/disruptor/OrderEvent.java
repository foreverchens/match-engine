package icu.match.service.disruptor;

import icu.match.core.model.OrderInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

	private OrderInfo orderInfo;


	public void reset() {
		log.info("reset OrderInfo");
	}

}
