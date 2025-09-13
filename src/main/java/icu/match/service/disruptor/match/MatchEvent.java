package icu.match.service.disruptor.match;/**
 *
 * @author 中本君
 * @date 2025/9/10
 */

import icu.match.core.model.MatchTrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 中本君
 * @date 2025/9/10 
 */
@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvent {

	private MatchTrade matchTrade;

}
