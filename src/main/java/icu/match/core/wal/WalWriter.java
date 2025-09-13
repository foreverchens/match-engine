package icu.match.core.wal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author 中本君
 * @date 2025/9/13
 */
public final class WalWriter implements AutoCloseable {

	private final WalAppender appender;

	private final AtomicLong txSeq = new AtomicLong(1);

	public WalWriter(WalAppender appender) {
		this.appender = appender;
	}

	/** 开始事务，返回 TxContext */
	public TxContext beginTx() throws IOException {
		long txId = txSeq.getAndIncrement();
		Lsn lsn = appender.append(RecordType.BEGIN_TX, longToBytes(txId), System.currentTimeMillis(), false);
		return new TxContext(txId, lsn);
	}

	private static byte[] longToBytes(long v) {
		return ByteBuffer.allocate(8)
						 .order(ByteOrder.LITTLE_ENDIAN)
						 .putLong(v)
						 .array();
	}

	/** 下单请求，挂在某个 Tx 下 */
	public void logOrder(TxContext tx, long userId, long orderId, int symbol, byte side, byte type, byte tif,
						 long price, long qty, long time) throws IOException {
		byte[] payload = TxPayloads.encodeOrder(tx.txId, userId, orderId, symbol, side, type, tif, price, qty, time);
		appender.append(RecordType.ORDER_REQ, payload, System.currentTimeMillis(), false);
	}

	/** 撤单请求 */
	public void logCancel(TxContext tx, int symbol, long orderId, long price) throws IOException {
		byte[] payload = TxPayloads.encodeCancel(tx.txId, symbol, orderId, price);
		appender.append(RecordType.CANCEL_REQ, payload, System.currentTimeMillis(), false);
	}

	/** 提交事务：写一条 COMMIT，force 落盘 */
	public void commitTx(TxContext tx) throws IOException {
		byte[] payload = TxPayloads.encodeCommit(tx.txId);
		appender.append(RecordType.COMMIT_TX, payload, System.currentTimeMillis(), true);
	}

	@Override
	public void close() throws IOException {
		appender.close();
	}
}
