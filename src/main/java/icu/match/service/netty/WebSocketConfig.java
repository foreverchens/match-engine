package icu.match.service.netty;/**
 *
 * @author 中本君
 * @date 2025/8/18
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import icu.match.service.netty.henader.DepthHandler;

import javax.annotation.Resource;

import java.util.Map;

/**
 * @author 中本君
 * @date 2025/8/18 
 */
@Configuration
public class WebSocketConfig {

	@Resource
	private DepthHandler depthHandler;

	@Bean
	public HandlerMapping webSocketMapping() {
		return new SimpleUrlHandlerMapping(Map.of("/ws/depth", depthHandler), 10);
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}
}