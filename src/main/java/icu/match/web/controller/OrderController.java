package icu.match.web.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import icu.match.core.model.MatchTrade;
import icu.match.web.model.OrderResult;
import icu.match.web.model.OriginOrder;
import icu.match.web.service.MatchTradeService;
import icu.match.web.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Validated
@RestController
@RequestMapping("/api/order")
@Tag(name = "订单接口", description = "订单接口相关操作")
public class OrderController {

	@Resource
	private OrderService orderService;

	@Resource
	private MatchTradeService matchTradeService;

	@PostMapping
	@Operation(summary = "提交撮合", description = "接收订单提交撮合并返回撮合结果")
	public Mono<OrderResult> submit(@RequestBody @Validated Mono<OriginOrder> orderMono) {
		return orderMono.flatMap(order -> orderService.submit(order));
	}

	@PostMapping("/cancel")
	@Operation(summary = "撤单", description = "撤单 基于symbol orderId price")
	public Mono<Void> cancel(@RequestBody Mono<OriginOrder> orderMono) {
		return orderMono.flatMap(order -> orderService.cancel(order));
	}


	@GetMapping("/trades")
	@Operation(summary = "成交列表", description = "撮合成交列表")
	public Flux<MatchTrade> trades() {
		return matchTradeService.listAllTrades();
	}
}
