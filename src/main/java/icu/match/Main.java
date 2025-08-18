package icu.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 伟大的撮合引擎
 *
 * @author 中本君
 */
@Slf4j
@SpringBootApplication
public class Main {

	/**
	 *
	 */
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
		log.info("-------------suc-------------");
		log.info("api doc  : http://localhost:8080/webjars/swagger-ui/index.html");
		log.info("homepage : http://localhost:8080/trade.html");
		// Flux.interval(Duration.ofSeconds(1))
		// 	.map(e -> "" + e)
		// 	.doOnNext(log::info)
		// 	.subscribe();

	}


	@Bean
	public WebClient webClient() {
		HttpClient httpClient = HttpClient.create()
										  .responseTimeout(Duration.ofSeconds(1));
		return WebClient.builder()
						.clientConnector(new ReactorClientHttpConnector(httpClient))
						.build();
	}
}
