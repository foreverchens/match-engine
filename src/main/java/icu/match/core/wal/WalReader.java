package icu.match.core.wal;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class WalReader {

	private static final short MAGIC = (short) 0xBEEF;

	private static final byte VER = 1;

	private static final int HEADER_FIXED = 2 + 1 + 1 + 4 + 4 + 8 + 8 + 4;

	public static void main(String[] args) throws Exception {
		/**
		 * == Replay segment: ./data/wal/wal-000000.bin
		 * // 下单
		 * 0:0 BEGIN txId=1
		 * 0:1 ORDER txId=1 userId=1 orderId=98873039 symbol=1001 side=0 type=0 tif=0 price=104 qty=1 time=0
		 * 0:2 COMMIT txId=1
		 * // 撤单
		 * 0:3 BEGIN txId=2
		 * 0:4 CANCEL txId=2 symbol=1001 orderId=98873039 price=104
		 * 0:5 COMMIT txId=2
		 */
		WalReader.replayDir("./data/wal");
	}

	public static void replayDir(String dir) throws IOException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(dir), "wal-*.bin")) {
			for (Path p : ds) {
				System.out.println("== Replay segment: " + p);
				replaySegment(p);
			}
		}
	}

	private static void replaySegment(Path p) throws IOException {
		try (FileChannel ch = FileChannel.open(p, StandardOpenOption.READ)) {
			long pos = 0;
			ByteBuffer hdr = ByteBuffer.allocate(HEADER_FIXED)
									   .order(ByteOrder.LITTLE_ENDIAN);
			while (true) {
				hdr.clear();
				int r = ch.read(hdr, pos);
				if (r == -1) {
					break;
				}
				if (r < HEADER_FIXED) {
					break;
				}
				hdr.flip();

				short magic = hdr.getShort();
				byte ver = hdr.get();
				byte type = hdr.get();
				int len = hdr.getInt();
				int segId = hdr.getInt();
				long index = hdr.getLong();
				long wall = hdr.getLong();
				int crcOnFile = hdr.getInt();

				if (magic != MAGIC || ver != VER || len < 0) {
					break;
				}
				long nextPos = pos + HEADER_FIXED + len;
				if (nextPos > ch.size()) {
					break;
				}

				byte[] payload = new byte[len];
				if (len > 0) {
					ByteBuffer pb = ByteBuffer.wrap(payload);
					ch.read(pb, pos + HEADER_FIXED);
				}

				// CRC 校验
				CRC32 crc = new CRC32();
				byte[] headNoCrc = new byte[2 + 1 + 1 + 4 + 4 + 8 + 8];
				ByteBuffer tmp = ByteBuffer.wrap(headNoCrc)
										   .order(ByteOrder.LITTLE_ENDIAN);
				tmp.putShort(magic)
				   .put(ver)
				   .put(type)
				   .putInt(len)
				   .putInt(segId)
				   .putLong(index)
				   .putLong(wall);
				crc.update(headNoCrc);
				if (len > 0) {
					crc.update(payload);
				}
				if (((int) crc.getValue()) != crcOnFile) {
					break;
				}

				Lsn lsn = new Lsn(segId, index);

				switch (type) {
					case RecordType.BEGIN_TX: {
						long txId = ByteBuffer.wrap(payload)
											  .order(ByteOrder.LITTLE_ENDIAN)
											  .getLong();
						System.out.printf("%s BEGIN txId=%d%n", lsn, txId);
						break;
					}
					case RecordType.ORDER_REQ: {
						ByteBuffer buf = ByteBuffer.wrap(payload)
												   .order(ByteOrder.LITTLE_ENDIAN);
						long txId = buf.getLong();
						long userId = buf.getLong();
						long orderId = buf.getLong();
						int symbol = buf.getInt();
						byte side = buf.get();
						byte otype = buf.get();
						byte tif = buf.get();
						long price = buf.getLong();
						long qty = buf.getLong();
						long time = buf.getLong();
						System.out.printf(
								"%s ORDER txId=%d userId=%d orderId=%d symbol=%d side=%d type=%d tif=%d price=%d " +
								"qty=%d time=%d%n", lsn, txId, userId, orderId, symbol, side, otype, tif, price, qty,
								time);
						break;
					}
					case RecordType.CANCEL_REQ: {
						ByteBuffer buf = ByteBuffer.wrap(payload)
												   .order(ByteOrder.LITTLE_ENDIAN);
						long txId = buf.getLong();
						int symbol = buf.getInt();
						long orderId = buf.getLong();
						long price = buf.getLong();
						System.out.printf("%s CANCEL txId=%d symbol=%d orderId=%d price=%d%n", lsn, txId, symbol,
										  orderId, price);
						break;
					}
					case RecordType.COMMIT_TX: {
						long txId = ByteBuffer.wrap(payload)
											  .order(ByteOrder.LITTLE_ENDIAN)
											  .getLong();
						System.out.printf("%s COMMIT txId=%d%n", lsn, txId);
						break;
					}
					default:
						System.out.printf("%s UNKNOWN type=%d%n", lsn, type);
				}

				pos = nextPos;
			}
		}
	}
}
