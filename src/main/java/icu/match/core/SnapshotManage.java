package icu.match.core;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import icu.match.core.model.SnapshotView;
import icu.match.core.snapshot.CowPool;
import icu.match.core.snapshot.SnapWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class SnapshotManage {

	public static final CowPool COW_POOL = new CowPool();

	public static volatile boolean enabled = true;

	public static volatile long seq = System.nanoTime();

	public static volatile boolean writing = false;

	public static volatile boolean prepare = false;

	private final RingOrderBuffer ring;

	private final ColdOrderBuffer cold;


	public SnapshotManage(RingOrderBuffer ring, ColdOrderBuffer cold) {
		if (ring == null) {
			throw new IllegalArgumentException("ring must not be null");
		}
		if (cold == null) {
			throw new IllegalArgumentException("cold must not be null");
		}
		this.ring = ring;
		this.cold = cold;
	}

	public static OrderNode get(long orderId) {
		return COW_POOL.get(orderId);
	}

	public static void put(long orderId, OrderNode orderNode) {
		COW_POOL.put(orderId, orderNode);
	}


	public void start() {
		while (enabled) {
			prepare = true;
			seq = System.nanoTime();
			while (writing) {
				Thread.yield();
			}

			this.deal();

			prepare = false;
			seq = System.nanoTime();
			try {
				TimeUnit.SECONDS.sleep(30);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void deal() {
		try {
			SnapshotView snapshot = ring.snapshot();
			System.out.println(snapshot.view());
			Path dir = Paths.get("./data/snapshots");
			Path written = SnapWriter.writeSnapshotAtomic(snapshot, dir, "orderbook");
			System.out.println(written);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
