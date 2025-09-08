package icu.match.web.service;/**
 *
 * @author 中本君
 * @date 2025/9/8
 */

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;

import icu.match.core.model.MatchTrade;
import icu.match.service.match.repo.MatchTradeRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import java.util.List;

/**
 * @author 中本君
 * @date 2025/9/8 
 */
@Slf4j
@Service
public class MatchTradeService {

	private final MatchTradeRepository repository;

	@Resource
	private R2dbcEntityTemplate template;

	public MatchTradeService(MatchTradeRepository repository) {
		this.repository = repository;
	}

	public Mono<MatchTrade> saveTrade(MatchTrade trade) {
		return template.insert(MatchTrade.class)
					   .using(trade);
	}

	public Flux<MatchTrade> saveAllTrades(List<MatchTrade> trades) {
		return repository.saveAll(trades);
	}


	public Flux<MatchTrade> listAllTrades() {
		return template.select(MatchTrade.class)
					   .matching(Query.empty()
									  .sort(Sort.by(Sort.Direction.DESC, "tradeTime")))
					   .all();
	}

}
