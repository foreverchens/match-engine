package icu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

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
	}
}
