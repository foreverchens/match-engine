package icu;

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
		log.info("api doc :  http://localhost:8080/webjars/swagger-ui/index.html");
		// while (true) {
		// 	try {
		// 		TimeUnit.SECONDS.sleep(3);
		// 	} catch (InterruptedException e) {
		// 		throw new RuntimeException(e);
		// 	}
		// 	// 这里写你的定时任务逻辑
		// 	Instant time = Instant.now();
		// 	log.info("定时任务执行：" + time);
		// 	MonoSink<OrderResult> sink = MonoSinkManage.getSink(1L);
		// 	if (ObjectUtil.isNotNull(sink)) {
		// 		sink.success(OrderResult.builder().orderId(1L).type(OrderType.LIMIT).userId(1L).status(1).symbol("bu")
		// 		.side(OrderSide.ASK).price(BigDecimal.ONE).origQty(BigDecimal.ONE).build());
		// 	}
		// }
	}


	@Bean
	public WebClient webClient() {
		HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(5));
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();

	}
}
