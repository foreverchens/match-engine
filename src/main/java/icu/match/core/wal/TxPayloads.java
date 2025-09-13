package icu.match.core.wal;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class TxPayloads {

	// COMMIT payload = [txId]
	public static byte[] encodeCommit(long txId) {
		ByteBuffer buf = ByteBuffer.allocate(8)
								   .order(ByteOrder.LITTLE_ENDIAN);
		buf.putLong(txId);
		return buf.array();
	}

	public static long decodeCommit(byte[] bytes) {
		return ByteBuffer.wrap(bytes)
						 .order(ByteOrder.LITTLE_ENDIAN)
						 .getLong();
	}

	// ORDER payload = [txId][Order fields...]
	public static byte[] encodeOrder(long txId, long userId, long orderId, int symbol, byte side, byte type, byte tif,
									 long price, long qty, long time) {
		ByteBuffer buf = ByteBuffer.allocate(8 + 8 + 8 + 4 + 1 + 1 + 1 + 8 + 8 + 8)
								   .order(ByteOrder.LITTLE_ENDIAN);
		buf.putLong(txId);
		buf.putLong(userId);
		buf.putLong(orderId);
		buf.putInt(symbol);
		buf.put(side);
		buf.put(type);
		buf.put(tif);
		buf.putLong(price);
		buf.putLong(qty);
		buf.putLong(time);
		return buf.array();
	}

	public static byte[] encodeCancel(long txId, int symbol, long orderId, long price) {
		ByteBuffer buf = ByteBuffer.allocate(8 + 4 + 8 + 8)
								   .order(ByteOrder.LITTLE_ENDIAN);
		buf.putLong(txId);
		buf.putInt(symbol);
		buf.putLong(orderId);
		buf.putLong(price);
		return buf.array();
	}
}
