package icu.service.global;

import icu.web.model.OrderResult;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
public class MonoSinkManage {

	private static final ConcurrentHashMap<Long, MonoSink<OrderResult>> SINK_MAP = new ConcurrentHashMap<>();

	public static MonoSink<OrderResult> getSink(Long orderId) {
		return SINK_MAP.get(orderId);
	}

	public static MonoSink<OrderResult> remove(Long orderId) {
		return SINK_MAP.remove(orderId);
	}

	public static void put(Long orderId, MonoSink<OrderResult> sink) {
		SINK_MAP.put(orderId, sink);
	}
}
