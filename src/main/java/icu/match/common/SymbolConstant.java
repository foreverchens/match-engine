package icu.match.common;/**
 *
 * @author 中本君
 * @date 2025/9/12
 */

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 1xxx symbol号段
 * btcusdt 1001
 * ethusdt 1002
 *
 * @author 中本君
 * @date 2025/9/12 
 */
public enum SymbolConstant {

	BTCUSDT(1001), ETHUSDT(1002);

	private static final Map<Integer, SymbolConstant> MAP = new HashMap<>();

	static {
		for (SymbolConstant symbol : SymbolConstant.values()) {
			MAP.put(symbol.symbolId, symbol);
		}
	}

	@Getter
	final int symbolId;

	SymbolConstant(int symbolId) {
		this.symbolId = symbolId;
	}

	public static SymbolConstant get(int symbolId) {
		return MAP.get(symbolId);
	}
}
