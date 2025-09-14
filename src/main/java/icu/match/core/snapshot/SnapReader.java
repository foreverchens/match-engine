package icu.match.core.snapshot;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import icu.match.core.model.SnapshotView;
import lombok.SneakyThrows;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32C;
import java.util.zip.CheckedInputStream;

import static java.nio.file.StandardOpenOption.READ;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class SnapReader {
	private static final byte[] MAGIC = new byte[]{'O', 'B', 'S', 'N', 'A', 'P'}; // "OBSNAP"

	private static final int VERSION_SUPPORTED = 1;

	private static final String SUFFIX = ".snap";

	@SneakyThrows
	public static void main(String[] args) {
		Path dir = Paths.get("./data/snapshots");

		// 读“最新”的快照
		SnapReader.SnapshotReadResult r1 = SnapReader.readLatest(dir, "orderbook");
		System.out.println(r1.view.view(10, 24));

		// 或者指定文件读取
		// Path file = dir.resolve("orderbook-1757779498.snap");
		// SnapReader.SnapshotReadResult r2 = SnapReader.readSnapshot(file);
		// System.out.println(r2.view.view());
	}

	/** 读取目录中某个 baseName 的最新快照（文件名形如 baseName-epochSec.snap） */
	public static SnapshotReadResult readLatest(Path dir, String baseName) throws IOException {
		Path latest = findLatest(dir, baseName);
		if (latest == null) {
			throw new FileNotFoundException("No snapshot found for baseName=" + baseName + " in " + dir);
		}
		return readSnapshot(latest);
	}

	/** 找到最新的快照文件（按文件名中的 epochSec 排序；不做 I/O 排序） */
	private static Path findLatest(Path dir, String baseName) throws IOException {
		if (!Files.isDirectory(dir)) {
			return null;
		}
		Path latest = null;
		long bestTs = Long.MIN_VALUE;

		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, p -> Files.isRegularFile(p) && p.getFileName()
																									  .toString()
																									  .startsWith(
																											  baseName +
																											  "-") &&
																		   p.getFileName()
																			.toString()
																			.endsWith(SUFFIX))) {
			for (Path p : ds) {
				String name = p.getFileName()
							   .toString();
				// 形如 baseName-1726230000.snap
				int dash = name.lastIndexOf('-');
				int dot = name.lastIndexOf('.');
				if (dash < 0 || dot < dash) {
					continue;
				}
				try {
					long ts = Long.parseLong(name.substring(dash + 1, dot));
					if (ts > bestTs) {
						bestTs = ts;
						latest = p;
					}
				} catch (NumberFormatException ignore) {
				}
			}
		}
		return latest;
	}

	/* ================= helpers ================ */

	/** 读取单个快照文件，做 MAGIC/VERSION/CRC 校验，返回视图与元数据 */
	public static SnapshotReadResult readSnapshot(Path file) throws IOException {
		if (!Files.isRegularFile(file)) {
			throw new FileNotFoundException("Snapshot not found: " + file);
		}

		try (FileChannel ch = FileChannel.open(file, READ)) {
			long size = ch.size();
			if (size < (6 + 4 + 8 + 8 + 4)) { // 基本头部 + 尾部CRC 的最小长度
				throw new EOFException("Snapshot too small/corrupted: " + file + " size=" + size);
			}

			// 用 CheckedInputStream 对“头+体”（最后4字节除外）做 CRC32C
			long payloadSize = size - 4; // 最后4字节是尾部CRC
			CRC32C crc = new CRC32C();
			InputStream baseIn = Channels.newInputStream(ch);
			InputStream limited = new BoundedInputStream(baseIn, payloadSize); // 只读到 payload 末尾
			try (CheckedInputStream cis = new CheckedInputStream(limited, crc);
				 DataInputStream in = new DataInputStream(new BufferedInputStream(cis, 1 << 20))) {

				// ===== 头 =====
				byte[] magic = new byte[6];
				in.readFully(magic);
				if (!Arrays.equals(magic, MAGIC)) {
					throw new IOException("Bad MAGIC in snapshot: " + file);
				}

				int version = in.readInt();
				if (version != VERSION_SUPPORTED) {
					throw new IOException("Unsupported snapshot version: " + version);
				}

				long createdAtMs = in.readLong();
				long lastAppliedLsn = in.readLong();

				// ===== 体（8个 List<Long>）=====
				SnapshotView v = new SnapshotView();
				v.bidPrices = readLongList(in);
				v.bidUserIds = readLongList(in);
				v.bidOrderIds = readLongList(in);
				v.bidQtyList = readLongList(in);

				v.askPrices = readLongList(in);
				v.askOrderIds = readLongList(in);
				v.askUserIds = readLongList(in);
				v.askQtyList = readLongList(in);

				// CheckedInputStream 在此处已计算出 payload 的 CRC 值
				long calcCrc = crc.getValue();

				// ===== 尾（独立再读4字节 CRC）=====
				DataInputStream tail = new DataInputStream(Channels.newInputStream(ch));
				int fileCrc = tail.readInt();
				if ((int) calcCrc != fileCrc) {
					throw new IOException("CRC mismatch: calc=" + (int) calcCrc + " file=" + fileCrc + " path=" + file);
				}

				return new SnapshotReadResult(v, createdAtMs, lastAppliedLsn, version, file);
			}
		}
	}

	private static List<Long> readLongList(DataInput in) throws IOException {
		int n = in.readInt();
		if (n < 0) {
			throw new IOException("Negative list length: " + n);
		}
		// 可按需做上限保护以防 OOM，例如 if (n > 10_000_000) throw ...
		List<Long> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			list.add(in.readLong());
		}
		return list;
	}

	/** 限制从某个 InputStream 读取的最大字节数，避免越过 payload 读到 CRC 尾部 */
	static final class BoundedInputStream extends FilterInputStream {
		private long remaining;

		BoundedInputStream(InputStream in, long limit) {
			super(in);
			this.remaining = limit;
		}

		@Override
		public int read() throws IOException {
			if (remaining <= 0) {
				return -1;
			}
			int b = super.read();
			if (b >= 0) {
				remaining--;
			}
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (remaining <= 0) {
				return -1;
			}
			len = (int) Math.min(len, remaining);
			int n = super.read(b, off, len);
			if (n > 0) {
				remaining -= n;
			}
			return n;
		}

		@Override
		public long skip(long n) throws IOException {
			n = Math.min(n, remaining);
			long s = super.skip(n);
			remaining -= s;
			return s;
		}

		@Override
		public int available() throws IOException {
			int avail = super.available();
			return (int) Math.min(avail, remaining);
		}
	}

	/* ===== 读取结果封装 ===== */
	public static final class SnapshotReadResult {
		public final SnapshotView view;

		public final long createdAtMs;

		public final long lastAppliedLsn;

		public final int version;

		public final Path path;

		public SnapshotReadResult(SnapshotView view, long createdAtMs, long lastAppliedLsn, int version, Path path) {
			this.view = view;
			this.createdAtMs = createdAtMs;
			this.lastAppliedLsn = lastAppliedLsn;
			this.version = version;
			this.path = path;
		}

		@Override
		public String toString() {
			return "SnapshotReadResult{" + "path=" + path + ", version=" + version + ", createdAtMs=" + createdAtMs +
				   ", lastAppliedLsn=" + lastAppliedLsn + ", bids=" + (view.bidPrices == null
																	   ? 0
																	   : view.bidPrices.size()) + ", asks=" +
				   (view.askPrices == null
					? 0
					: view.askPrices.size()) + '}';
		}
	}
}
