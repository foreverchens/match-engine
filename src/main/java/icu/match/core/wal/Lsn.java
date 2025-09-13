package icu.match.core.wal;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class Lsn {
	public final int segmentId;

	public final long index; // in-segment

	public Lsn(int segmentId, long index) {
		this.segmentId = segmentId;
		this.index = index;
	}

	public static Lsn ofLong(long v) {return new Lsn((int) (v >>> 32), v & 0xffffffffL);}

	public long toLong() {return ((long) segmentId << 32) | (index & 0xffffffffL);}

	@Override
	public String toString() {return segmentId + ":" + index;}
}
