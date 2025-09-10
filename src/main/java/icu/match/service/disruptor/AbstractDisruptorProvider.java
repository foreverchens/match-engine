package icu.match.service.disruptor;


import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author 中本君
 * @date 2025/9/10
 */
public abstract class AbstractDisruptorProvider<T> {

	private Disruptor<T> disruptor;

	private RingBuffer<T> ringBuffer;

	@PostConstruct
	protected void init() {
		disruptor = new Disruptor<>(eventFactory(), bufferSize(), namedThreadFactory(threadName()), producerType(),
									waitStrategy());
		disruptor.setDefaultExceptionHandler(exceptionHandler());

		EventHandler<T>[] eh = eventHandlers();
		if (eh == null || eh.length == 0) {
			throw new IllegalStateException("eventHandlers required");
		}
		disruptor.handleEventsWith(eh);

		disruptor.start();

		ringBuffer = disruptor.getRingBuffer();
	}

	// ---- 子类必须/可选实现的钩子 ----
	protected abstract EventFactory<T> eventFactory();

	/**
	 *   必须是2^n
	 */
	protected int bufferSize() {
		return 1 << 10;
	}

	protected ThreadFactory namedThreadFactory(String name) {
		return r -> {
			Thread t = new Thread(r, name);
			t.setDaemon(true);
			return t;
		};
	}

	protected abstract String threadName();

	/**
	 *   // SINGLE / MULTI
	 */
	protected ProducerType producerType() {
		return ProducerType.MULTI;
	}

	/**
	 *  Yielding/Blocking...
	 */
	protected WaitStrategy waitStrategy() {
		return new YieldingWaitStrategy();
	}

	protected IgnoreExceptionHandler exceptionHandler() {return new IgnoreExceptionHandler();}

	protected abstract EventHandler<T>[] eventHandlers();

	@PreDestroy
	protected void destroy() {
		if (disruptor != null) {
			try {
				disruptor.shutdown(1, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
			}
		}
	}

	// ---- 对外暴露访问器（用于@Bean方法返回）----
	public RingBuffer<T> ringBuffer() {return ringBuffer;}

	public Disruptor<T> disruptor() {return disruptor;}
}
