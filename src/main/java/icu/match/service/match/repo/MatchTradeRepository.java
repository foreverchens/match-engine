package icu.match.service.match.repo;/**
 *
 * @author 中本君
 * @date 2025/9/8
 */

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import icu.match.core.model.MatchTrade;

/**
 * @author 中本君
 * @date 2025/9/8 
 */
public interface MatchTradeRepository extends ReactiveCrudRepository<MatchTrade, Long> {}
