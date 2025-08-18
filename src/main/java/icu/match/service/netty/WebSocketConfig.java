package icu.match.service.netty;/**
 *
 * @author 中本君
 * @date 2025/8/18
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import icu.match.core.MatchingEngine;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;

import java.time.Duration;
import java.util.Map;

/**
 * @author 中本君
 * @date 2025/8/18 
 */
@Configuration
public class WebSocketConfig {

	@Resource
	MatchingEngine matchingEngine;

	@Bean
	public HandlerMapping webSocketMapping() {
		return new SimpleUrlHandlerMapping(Map.of("/ws/depth", depthHandler()), 10);
	}

	@Bean
	public WebSocketHandler depthHandler() {
		return session -> {
			// 每秒推送一次当前时间
			Flux<String> timeFlux = Flux.interval(Duration.ofSeconds(1))
										.map(e -> matchingEngine.snapshot());

			return session.send(timeFlux.map(session::textMessage));
		};
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}
}