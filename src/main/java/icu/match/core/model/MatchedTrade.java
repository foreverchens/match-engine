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

/**
 * @author 中本君
 * @date 2025/8/17 
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MatchedTrade {

	long takerUserId;

	long makerUserId;

	long takerOrderId;

	long makerOrderId;

	long price;

	long qty;

	long time;
}
