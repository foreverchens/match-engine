package icu.match.core.wal;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public class TxContext {
	public final long txId;

	public final Lsn lsnBegin;

	public TxContext(long txId, Lsn lsnBegin) {
		this.txId = txId;
		this.lsnBegin = lsnBegin;
	}
}
