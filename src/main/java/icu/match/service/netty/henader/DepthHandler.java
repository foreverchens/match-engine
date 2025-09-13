package icu.match.service.netty.henader;/**
 *
 * @author 中本君
 * @date 2025/9/8
 */

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import icu.match.service.match.MatchEngine;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import java.time.Duration;

/**
 * @author 中本君
 * @date 2025/9/8 
 */
@Slf4j
@Component
public class DepthHandler implements WebSocketHandler {

	@Resource
	private MatchEngine matchEngine;

	@Override
	@NonNull
	public Mono<Void> handle(WebSocketSession session) {
		// 每秒推送一次当前时间
		Flux<String> timeFlux = Flux.interval(Duration.ofSeconds(1))
									.map(e -> matchEngine.depth());
		return session.send(timeFlux.map(session::textMessage));
	}
}
