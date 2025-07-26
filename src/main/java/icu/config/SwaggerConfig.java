package icu.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * @author 中本君
 * @date 2025/07/26 
 */
@Configuration
public class SwaggerConfig {
	@Bean
	public OpenAPI baseOpenAPI() {
		return new OpenAPI()
				.info(new Info()
							  .title("撮合引擎 API 文档")
							  .version("1.0")
							  .description("基于 WebFlux + Redis 的响应式撮合引擎"));
	}

	@Bean
	public GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder()
							 .group("default")
							 .pathsToMatch("/**")
							 .build();
	}
}
