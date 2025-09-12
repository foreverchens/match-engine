package icu.match.common;

import lombok.Data;

/**
 * @author 中本君
 * @date 2025/07/26 
 */
@Data
public class CallResult<T> {

	private static final int CODE_FAIL = 400;

	private static final int CODE_SUC = 200;

	private boolean suc;

	private int code;

	public String msg;

	private T data;

	public CallResult(boolean suc, int code, String msg, T data) {
		this.suc = suc;
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public static <T> CallResult<T> suc() {
		return new CallResult<>(true, CODE_SUC, "suc", null);
	}

	public static <T> CallResult<T> suc(T data) {
		return new CallResult<>(true, CODE_SUC, "suc", data);
	}

	public static <T> CallResult<T> fail() {
		return new CallResult<>(false, CODE_FAIL, "fail", null);
	}

	public static <T> CallResult<T> fail(String msg) {
		return new CallResult<>(false, CODE_FAIL, msg, null);
	}

	public static <T> CallResult<T> fail(int code, String msg) {
		return new CallResult<>(false, code, msg, null);
	}

	@Override
	public String toString() {
		return "CallResult(success=" + this.suc + ", code=" + this.code + ", msg" + "=" + this.msg + ", data=" +
			   this.data + ")";
	}

}

