package icu.match.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
public class SnowFlakeIdUtil {


	private static final Snowflake INSTANCE;

	static {
		INSTANCE = IdUtil.getSnowflake();
	}

	public static long nextId() {
		return INSTANCE.nextId();
	}
}
