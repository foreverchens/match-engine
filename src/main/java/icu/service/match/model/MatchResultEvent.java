package icu.service.match.model;

import lombok.Data;

import java.util.List;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Data
public class MatchResultEvent {
	private Order order;

	private List<Trade> tradeList;
}
