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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class WalAppender implements AutoCloseable {

	private static final short MAGIC = (short) 0xBEEF;

	private static final byte VER = 1;

	private static final int HEADER_FIXED = 2 + 1 + 1 + 4 + 4 + 8 + 8 + 4;

	private static final OpenOption[] APPEND_OPTS = {StandardOpenOption.CREATE, StandardOpenOption.WRITE,
			StandardOpenOption.READ};

	private final Path dir;

	private final long maxSegmentBytes;

	private FileChannel ch;

	private int segmentId;

	// 段内自增
	private long nextIndex;

	// 当前已写字节
	private long fileSize;

	public WalAppender(Path dir, long maxSegmentBytes) throws IOException {
		this.dir = dir;
		this.maxSegmentBytes = maxSegmentBytes;
		Files.createDirectories(dir);
		// 恢复：定位最新段并扫描
		this.segmentId = findLatestSegmentId();
		openSegment(Math.max(segmentId, 0), true);
	}

	private int findLatestSegmentId() throws IOException {
		int max = -1;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "wal-*.bin")) {
			for (Path p : ds) {
				String name = p.getFileName()
							   .toString(); // wal-000123.bin
				try {
					int v = Integer.parseInt(name.substring(4, 10));
					if (v > max) {
						max = v;
					}
				} catch (Exception ignore) {
				}
			}
		}
		return max;
	}

	private void openSegment(int segId, boolean truncateBadTail) throws IOException {
		Path p = segPath(segId);
		boolean exists = Files.exists(p);
		ch = FileChannel.open(p, APPEND_OPTS);
		segmentId = segId;

		if (!exists) {
			nextIndex = 0;
			fileSize = 0;
			return;
		}
		// 扫描复原
		ScanResult r = scanAndTruncateBadTail(ch, truncateBadTail);
		nextIndex = r.nextIndex;
		fileSize = r.fileSize;
		if (truncateBadTail && r.truncated) {
			ch.truncate(fileSize);
		}
		// 定位写指针
		ch.position(fileSize);
	}

	private Path segPath(int segId) {
		return dir.resolve(String.format("wal-%06d.bin", segId));
	}

	// ---------- 恢复 & 段管理 ----------

	private ScanResult scanAndTruncateBadTail(FileChannel ch, boolean truncate) throws IOException {
		long pos = 0, index = 0;
		ByteBuffer hdr = ByteBuffer.allocate(HEADER_FIXED)
								   .order(ByteOrder.LITTLE_ENDIAN);
		while (true) {
			hdr.clear();
			int r = ch.read(hdr, pos);
			if (r == -1) {
				break;
			}
			if (r < HEADER_FIXED) {
				// 半条头 -> 截断
				return new ScanResult(index, pos, true);
			}
			hdr.flip();
			short magic = hdr.getShort();
			byte ver = hdr.get();
			byte type = hdr.get();
			int len = hdr.getInt();
			int segIdFile = hdr.getInt();
			long idxFile = hdr.getLong();
			long wall = hdr.getLong();
			int crcOnFile = hdr.getInt();

			if (magic != MAGIC || ver != VER || segIdFile != this.segmentId || idxFile != index || len < 0) {
				return new ScanResult(index, pos, true);
			}
			long nextPos = pos + HEADER_FIXED + len;
			if (nextPos > ch.size()) {
				return new ScanResult(index, pos, true);
			}

			// 验证CRC
			CRC32 crc = new CRC32();
			byte[] headNoCrc = new byte[2 + 1 + 1 + 4 + 4 + 8 + 8];
			ByteBuffer tmp = ByteBuffer.wrap(headNoCrc)
									   .order(ByteOrder.LITTLE_ENDIAN);
			tmp.putShort(magic)
			   .put(ver)
			   .put(type)
			   .putInt(len)
			   .putInt(segIdFile)
			   .putLong(idxFile)
			   .putLong(wall);
			crc.update(headNoCrc);
			if (len > 0) {
				ByteBuffer payload = ByteBuffer.allocate(len);
				ch.read(payload, pos + HEADER_FIXED);
				payload.flip();
				crc.update(payload);
			}
			if (((int) crc.getValue()) != crcOnFile) {
				return new ScanResult(index, pos, true);
			}
			// 这条OK，推进
			pos = nextPos;
			index++;
		}
		return new ScanResult(index, pos, false);
	}

	public Lsn append(byte type, byte[] payload, long wallClockMillis, boolean force) throws IOException {
		if (payload == null) {
			payload = new byte[0];
		}
		int len = payload.length;
		int recBytes = HEADER_FIXED + len;

		// 滚段
		if (fileSize + recBytes > maxSegmentBytes) {
			rotate();
		}

		ByteBuffer buf = ByteBuffer.allocate(recBytes)
								   .order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort(MAGIC);
		buf.put(VER);
		buf.put(type);
		buf.putInt(len);
		buf.putInt(segmentId);
		buf.putLong(nextIndex);
		buf.putLong(wallClockMillis);

		CRC32 crc = new CRC32();
		// 先对 header(不含crc)做一遍；为了算crc，把前面内容备份
		byte[] headerNoCrc = new byte[2 + 1 + 1 + 4 + 4 + 8 + 8];
		ByteBuffer tmp = ByteBuffer.wrap(headerNoCrc)
								   .order(ByteOrder.LITTLE_ENDIAN);
		tmp.putShort(MAGIC)
		   .put(VER)
		   .put(type)
		   .putInt(len)
		   .putInt(segmentId)
		   .putLong(nextIndex)
		   .putLong(wallClockMillis);
		crc.update(headerNoCrc);
		if (len > 0) {
			crc.update(payload);
		}
		int crc32 = (int) crc.getValue();
		buf.putInt(crc32);
		if (len > 0) {
			buf.put(payload);
		}
		buf.flip();

		while (buf.hasRemaining()) {
			ch.write(buf);
		}
		fileSize += recBytes;
		if (force) {
			ch.force(true);
		}

		return new Lsn(segmentId, nextIndex++);
	}

	private void rotate() throws IOException {
		if (ch != null) {
			ch.close();
		}
		openSegment(segmentId + 1, false);
	}

	public void force() throws IOException {
		ch.force(true);
	}

	@Override
	public void close() throws IOException {
		if (ch != null) {
			ch.close();
		}
	}

	private static final class ScanResult {
		final long nextIndex;

		final long fileSize;

		final boolean truncated;

		ScanResult(long nextIndex, long fileSize, boolean truncated) {
			this.nextIndex = nextIndex;
			this.fileSize = fileSize;
			this.truncated = truncated;
		}
	}
}
