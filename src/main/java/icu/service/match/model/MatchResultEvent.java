package icu.service.match.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultEvent {
	private Order order;

	private List<Trade> tradeList;
}
