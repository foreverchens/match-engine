package icu.match.web.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import icu.match.common.CallResult;
import icu.match.core.MatchingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * @author 中本君
 * @date 2025/07/26 
 */
@Validated
@RestController
@RequestMapping("/api/test")
@Tag(name = "测试接口", description = "测试接口相关操作")
public class TestController {

	@PostMapping
	@Operation(summary = "创建用户", description = "接收用户信息并返回创建结果")
	public Mono<CallResult<String>> createUser(@RequestBody @Validated Mono<User> userMono) {
		return userMono.map(user -> {
			// 模拟保存逻辑
			return CallResult.suc("创建成功：" + user.getUname());
		});
	}

	@Resource
	private MatchingEngine matchingEngine;

	@GetMapping("/dump")
	public Mono<String> dump() {
		return Mono.just(matchingEngine.dump());
	}

	@GetMapping("/snap")
	public Mono<String> snap() {
		return Mono.just(matchingEngine.snapshot());
	}



	@Data
	@Schema(description = "创建用户请求参数")
	public static class User {
		@NotBlank
		@Schema(description = "用户名", example = "nakamoto")
		private String uname;

		@Min(18)
		@Max(60)
		@Schema(description = "年龄", example = "30")
		private Integer age;

	}

}
