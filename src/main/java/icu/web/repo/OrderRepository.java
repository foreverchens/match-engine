package icu.web.repo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import icu.web.model.OriginOrder;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
public interface OrderRepository extends ReactiveCrudRepository<OriginOrder, Long> {}
