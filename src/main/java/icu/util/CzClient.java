package icu.util;

import com.alibaba.fastjson.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Component
public class CzClient {

	@Resource
	private WebClient webClient;


	public BigDecimal getPrice(String symbol) {
		Mono<JSONObject> responseMono = webClient.get().uri(
				"https://api.binance.com/api/v3/ticker/price?symbol=" + symbol).retrieve().bodyToMono(JSONObject.class);

		JSONObject resp = responseMono.block();
		if (Objects.isNull(resp)) {
			return null;
		}
		return resp.getBigDecimal("price");
	}
}
