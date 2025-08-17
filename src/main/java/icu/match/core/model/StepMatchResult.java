package icu.match.core.model;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * @author 中本君
 * @date 2025/8/17 
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StepMatchResult {

	/**
	 * true
	 * 	订单簿head订单 被完全成交 需生成makerOrder完全成交事件
	 * 	false
	 * 	订单簿head订单 仅部分成交 takerOrder被完成成交 需生成takerOrder完全成交事件
	 */
	boolean makerDepleted;

	long price;

	long qty;

	@NotNull
	MatchedTrade trade;

}
