package icu.match.core.wal;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

/**
 * @author 中本君
 * @date 2025/9/13 
 */
public interface RecordType {
	byte BEGIN_TX = 1;

	byte ORDER_REQ = 10;

	byte CANCEL_REQ = 11;

	byte COMMIT_TX = 99;
}
