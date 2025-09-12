package icu.match.web.route.handler;/**
 *
 * @author 中本君
 * @date 2025/9/9
 */

import com.lmax.disruptor.RingBuffer;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import icu.match.common.OrderEventType;
import icu.match.core.model.OrderInfo;
import icu.match.service.disruptor.order.OrderEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import java.util.Map;

/**
 * @author 中本君
 * @date 2025/9/9 
 */
@Slf4j
@Component
public class OrderHandler {


	private static final int DATA_LEN = 39;

	@Resource
	private DatabaseClient db;

	@Resource
	private RingBuffer<OrderEvent> ringBuffer;


	public Mono<ServerResponse> placeOrder(ServerRequest req) {
		if (req.headers()
			   .contentType()
			   .filter(mt -> mt.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
			   .isEmpty()) {
			return ServerResponse.status(415)
								 .build();
		}
		// 将数据块复制整理为一个连续的大块
		return DataBufferUtils.join(req.body(BodyExtractors.toDataBuffers()))
							  // 转换为netty字节缓存
							  .map(NettyDataBufferFactory::toByteBuf)
							  .flatMap(buf -> {
								  // 写入到字节缓存池对象
								  final ByteBuf pooled = PooledByteBufAllocator.DEFAULT.buffer(buf.readableBytes());
								  pooled.writeBytes(buf);
								  ReferenceCountUtil.safeRelease(buf);

								  return Mono.defer(() -> {
												 if (!pooled.isReadable() || pooled.capacity() != DATA_LEN) {
													 return Mono.error(new DecodingException("bad_req"));
												 }
												 /**
												  * 编码
												  * 		ByteBuf buf = Unpooled.buffer();
												  * 		buf.writeLongLE(userId);
												  * 		buf.writeLongLE(orderId);
												  * 		buf.writeIntLE(symbol);
												  * 		buf.writeByte(side);
												  * 		buf.writeByte(tif);
												  * 		buf.writeByte(type);
												  * 		buf.writeLongLE(price);
												  * 		buf.writeLongLE(qty);
												  *
												  * 		byte[] body = new byte[buf.readableBytes()];
												  * 		buf.readBytes(body);
												  */
												 // 解码
												 long userId = pooled.readLongLE();
												 long orderId = pooled.readLongLE();
												 int symbol = pooled.readIntLE();
												 byte side = pooled.readByte();
												 byte tif = pooled.readByte();
												 byte type = pooled.readByte();
												 long price = pooled.readLongLE();
												 long qty = pooled.readLongLE();

												 // 同步持久化：写入 origin order 表（幂等：order_id PK）
												 String sql = String.format(
														 "INSERT INTO origin_order  " + "(order_id,user_id,symbol," +
														 "side,tif,type," + "price,orig_qty)%n" +
														 "VALUES (:orderId,:userId,:symbol," + ":side," + ":tif," +
														 ":type," +
														 ":price," + ":orig_qty)");
												 Mono<Integer> insert = db.sql(sql)
																		  .bind("orderId", orderId)
																		  .bind("userId", userId)
																		  .bind("symbol", symbol)
																		  .bind("side", side)
																		  .bind("tif", tif)
																		  .bind("type", type)
																		  .bind("price", price)
																		  .bind("orig_qty", qty)
																		  .fetch()
																		  .rowsUpdated();
												 return insert.flatMap(n -> {
													 this.publish(symbol, userId, orderId, side, type, tif, price,
																  qty);
													 // todo 如需同步返回 需注册钩子
													 return ServerResponse.status(202)
																		  .contentType(MediaType.APPLICATION_JSON)
																		  .bodyValue(Map.of("ok", true));
												 });
											 })
											 .doFinally(sig -> ReferenceCountUtil.safeRelease(pooled));
							  })
							  .onErrorResume(DecodingException.class, e -> ServerResponse.badRequest()
																						 .bodyValue(Map.of("ok", false,
																										   "error",
																										   "bad_req")));
	}

	private void publish(int symbol, long userId, long orderId, byte side, byte type, byte tif, long price, long qty) {
		log.info("publish orderId :{}", orderId);

		long seq = ringBuffer.next();
		OrderEvent event = ringBuffer.get(seq);

		event.setEventTypeCode(OrderEventType.NEW_ORDER.code);

		OrderInfo orderInfo = event.getOrderInfo();
		orderInfo.setOrderId(orderId);
		orderInfo.setUserId(userId);
		orderInfo.setSide(side);
		orderInfo.setType(type);
		orderInfo.setTif(tif);
		orderInfo.setPrice(price);
		orderInfo.setQty(qty);
		orderInfo.setSymbol(1001);
		ringBuffer.publish(seq);
	}
}
