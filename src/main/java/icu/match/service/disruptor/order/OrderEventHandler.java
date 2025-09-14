package icu.match.service.disruptor.order;

import com.lmax.disruptor.EventHandler;

import org.springframework.stereotype.Component;

import icu.match.common.OrderEventType;
import icu.match.core.SnapshotManage;
import icu.match.core.model.OrderInfo;
import icu.match.core.wal.TxContext;
import icu.match.core.wal.WalAppender;
import icu.match.core.wal.WalWriter;
import icu.match.service.global.MonoSinkManage;
import icu.match.service.match.MatchEngine;
import icu.match.web.model.OrderResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import javax.annotation.Resource;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author 中本君
 * @date 2025/07/27 
 */
@Slf4j
@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {


	private final WalWriter svc = new WalWriter(new WalAppender(Paths.get("./data/wal"), 256L * 1024 * 1024));

	@Resource
	private MatchEngine matchEngine;

	public OrderEventHandler() throws IOException {}

	@Override
	@SneakyThrows
	public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
		SnapshotManage.writing = true;
		try {
			OrderEventType orderEventType = OrderEventType.get(event.getEventTypeCode());
			OrderInfo orderInfo = event.getOrderInfo();

			log.info("submit order :{}", orderInfo.getOrderId());
			if (orderEventType == OrderEventType.NEW_ORDER) {
				dealOrderNew(orderInfo);
				return;
			}
			switch (orderEventType) {
				case CANCEL_ORDER:
					TxContext tx = svc.beginTx();
					svc.logCancel(tx, orderInfo.getSymbol(), orderInfo.getOrderId(), orderInfo.getPrice());
					matchEngine.cancel(orderInfo.getSymbol(), orderInfo.getPrice(), orderInfo.getOrderId());
					svc.commitTx(tx);
					break;
				case MODIFY_ORDER:
					break;
				default:
					throw new IllegalArgumentException("Unsupported event type: " + orderEventType);
			}
		} finally {
			event.reset();
			SnapshotManage.writing = false;
		}
	}

	@SneakyThrows
	private void dealOrderNew(OrderInfo orderInfo) {
		TxContext tx = null;
		try {
			// 阶段 1
			tx = svc.beginTx();
			svc.logOrder(tx, orderInfo.getUserId(), orderInfo.getOrderId(), orderInfo.getSymbol(), orderInfo.getSide(),
						 orderInfo.getType(), orderInfo.getTif(), orderInfo.getPrice(), orderInfo.getQty(),
						 orderInfo.getTime());
			matchEngine.submit(orderInfo);
		} finally {
			if (tx != null) {
				// 阶段 2
				svc.commitTx(tx);
				// 二阶段提交成功后 此阶段撮合数据将可发布
			}
			MonoSink<OrderResult> sink = MonoSinkManage.getSink(orderInfo.getOrderId());
			if (sink != null) {
				sink.success(new OrderResult());
			}
		}
	}
}
