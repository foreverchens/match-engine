package icu.match.service.global;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import icu.match.common.CallResult;

import java.net.BindException;

/**
 * @author 中本君
 * @date 2025/07/26 
 */
@RestControllerAdvice
@Order(-1)
public class GlobalExceptionHandler {
	/**
	 * 参数校验失败（@Valid）
	 */
	@ExceptionHandler(WebExchangeBindException.class)
	public CallResult<String> handleWebExchangeBindException(WebExchangeBindException ex) {
		String errorMessage = ex.getFieldErrors().stream().findFirst().map(
				err -> err.getField() + ": " + err.getDefaultMessage()).orElse("参数校验失败");
		return CallResult.fail(errorMessage);

	}

	@ExceptionHandler(BindException.class)
	public CallResult<String> handleBindException(BindException ex) {
		return CallResult.fail(ex.getMessage());
	}


	@ExceptionHandler(RuntimeException.class)
	public CallResult<String> handleRunTimeException(RuntimeException ex) {
		return CallResult.fail(ex.getMessage());
	}
}
