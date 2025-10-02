package icu.match.web.route;/**
 *
 * @author 中本君
 * @date 2025/9/9
 */

import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import icu.match.web.route.handler.OrderHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 中本君
 * @date 2025/9/9 
 */
@Slf4j
@Configuration
public class WebRouteConfig {

	@Bean
	@RouterOperation(
			path = "/api/order.bin",
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
			method = RequestMethod.POST,
			beanClass = OrderHandler.class,
			beanMethod = "placeOrder",
			operation = @Operation(
					summary = "Place order (binary)",
					description = "提交字节流订单，39字节定长",
					requestBody = @RequestBody(
							content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
					),
					responses = {@ApiResponse(
							responseCode = "202", description = "Accepted"
					), @ApiResponse(responseCode = "400", description = "Bad request")}
			)
	)
	public RouterFunction<ServerResponse> routes(OrderHandler h) {
		return RouterFunctions.route()
							  .POST("/api/order.bin",
									RequestPredicates.contentType(MediaType.APPLICATION_OCTET_STREAM),
									h::placeOrder)
							  .build();
	}

	@Bean
	public RouterFunction<ServerResponse> homepage() {
		return RouterFunctions.route()
							  .GET("/", request -> ServerResponse.ok()
																 .bodyValue(new ClassPathResource(
																		 "static/trade" + ".html")))
							  .build();
	}
}
