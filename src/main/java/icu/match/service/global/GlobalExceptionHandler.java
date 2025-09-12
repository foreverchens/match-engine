package icu.match.service.global;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import icu.match.common.CallResult;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;

/**
 * @author 中本君
 * @date 2025/07/26 
 */
@Slf4j
@Order(-1)
@RestControllerAdvice
public class GlobalExceptionHandler {
	/**
	 * 参数校验失败（@Valid）
	 */
	@ExceptionHandler(WebExchangeBindException.class)
	public CallResult<String> handleWebExchangeBindException(WebExchangeBindException ex) {
		String errorMessage = ex.getFieldErrors().stream().findFirst().map(
				err -> err.getField() + ": " + err.getDefaultMessage()).orElse("参数校验失败");
		log.info(errorMessage);
		return CallResult.fail(errorMessage);

	}

	@ExceptionHandler(BindException.class)
	public CallResult<String> handleBindException(BindException ex) {
		log.info(ex.getMessage());
		return CallResult.fail(ex.getMessage());
	}


	@ExceptionHandler(RuntimeException.class)
	public CallResult<String> handleRunTimeException(RuntimeException ex) {
		log.info(ex.getMessage());
		return CallResult.fail(ex.getMessage());
	}
}
