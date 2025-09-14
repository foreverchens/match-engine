package icu.match.core.snapshot;


import icu.match.core.model.SnapshotView;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.CRC32C;
import java.util.zip.CheckedOutputStream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * @author 中本君
 * @date 2025/9/13
 */
public final class SnapWriter {

	private static final int VERSION = 1;

	private static final byte[] MAGIC = new byte[]{'O', 'B', 'S', 'N', 'A', 'P'}; // "OBSNAP"

	private static final String SUFFIX = ".snap";

	/** 写入 v 到 dir 下：baseName-epochSec.snap；成功后仅保留这一个快照 */
	public static Path writeSnapshotAtomic(SnapshotView v, Path dir, String baseName) throws IOException {
		if (v == null) {
			throw new IllegalArgumentException("view == null");
		}
		Files.createDirectories(dir);

		long nowMs = System.currentTimeMillis();
		long epochSec = nowMs / 1000;
		Path finalFile = dir.resolve(baseName + "-" + epochSec + SUFFIX);

		// 在同目录创建临时文件，保证 ATOMIC_MOVE 有效
		Path tmp = Files.createTempFile(dir, baseName + "-", ".tmp");

		try (FileChannel ch = FileChannel.open(tmp, CREATE, WRITE, TRUNCATE_EXISTING)) {

			// 不让 close() 关掉底层 FileChannel，避免 force 时 channel 已被关闭
			OutputStream base = new NoCloseOutputStream(Channels.newOutputStream(ch));

			// 计算 payload（头+体）的 CRC32C
			CRC32C crc = new CRC32C();
			try (CheckedOutputStream cos = new CheckedOutputStream(base, crc);
				 DataOutputStream out = new DataOutputStream(new BufferedOutputStream(cos, 1 << 20))) {

				// ===== 头 =====
				out.write(MAGIC);              // 6
				out.writeInt(VERSION);         // 4
				out.writeLong(nowMs);          // 8
				out.writeLong(-1L);            // 8 预留 lastAppliedLsn（暂写 -1）

				// ===== 体：8个 List<Long> =====
				writeLongList(out, v.bidPrices);
				writeLongList(out, v.bidUserIds);
				writeLongList(out, v.bidOrderIds);
				writeLongList(out, v.bidQtyList);

				writeLongList(out, v.askPrices);
				writeLongList(out, v.askOrderIds);
				writeLongList(out, v.askUserIds);
				writeLongList(out, v.askQtyList);

				out.flush(); // 刷用户态缓冲
			}

			// ===== 尾：写入 CRC（避免经过 cos，否则会把尾也算进去）=====
			try (DataOutputStream tail = new DataOutputStream(base)) {
				tail.writeInt((int) crc.getValue());
				tail.flush();
			}

			// 强制落盘（含元数据）
			ch.force(true);

		} catch (IOException e) {
			safeDelete(tmp);
			throw e;
		}

		// 原子替换为最终文件
		try {
			Files.move(tmp, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

			// 可选：fsync 目录（有的平台不支持，吞异常）
			try (FileChannel dirCh = FileChannel.open(dir, READ)) {
				dirCh.force(true);
			} catch (Exception ignore) {
			}

		} catch (IOException e) {
			safeDelete(tmp);
			throw e;
		}

		// 删除旧快照（同 baseName 的 .snap，除了当前文件）
		deleteOldSnapshots(dir, baseName, finalFile);

		return finalFile;
	}

	/* ================= helpers ================= */

	private static void writeLongList(DataOutput out, List<Long> list) throws IOException {
		if (list == null) {
			out.writeInt(0);
			return;
		}
		int n = list.size();
		out.writeInt(n);
		for (int i = 0; i < n; i++) {
			out.writeLong(list.get(i)); // 自动拆箱
		}
	}

	private static void safeDelete(Path p) {
		try {
			Files.deleteIfExists(p);
		} catch (IOException ignore) {
		}
	}

	private static void deleteOldSnapshots(Path dir, String baseName, Path keep) {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, p -> Files.isRegularFile(p) && p.getFileName()
																									  .toString()
																									  .startsWith(
																											  baseName +
																											  "-") &&
																		   p.getFileName()
																			.toString()
																			.endsWith(SUFFIX) && !p.equals(keep))) {
			for (Path p : ds) safeDelete(p);
		} catch (IOException ignore) {
		}
	}

	/** 关闭时仅 flush，不关闭底层 OutputStream（从而不关闭 FileChannel） */
	static final class NoCloseOutputStream extends FilterOutputStream {
		NoCloseOutputStream(OutputStream out) {super(out);}

		@Override
		public void close() throws IOException {flush();}
	}
}
