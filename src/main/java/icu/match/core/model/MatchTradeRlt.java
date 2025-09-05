package icu.match.core.model;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author 中本君
 * @date 2025/8/17 
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class MatchTradeRlt {

	long takerUserId;

	long makerUserId;

	long takerOrderId;

	long makerOrderId;

	long price;

	long qty;

	long time;

	/** 复用前清空（可选） */
	public void clear() {
		takerUserId = 0L;
		makerUserId = 0L;
		takerOrderId = 0L;
		makerOrderId = 0L;
		price = 0L;
		qty = 0L;
		time = 0L;
	}

	public MatchTradeRlt fill(long tUid, long mUid, long tOid, long mOid, long price, long qty, long time) {
		this.takerUserId = tUid;
		this.makerUserId = mUid;
		this.takerOrderId = tOid;
		this.makerOrderId = mOid;
		this.price = price;
		this.qty = qty;
		this.time = time;
		return this;
	}


}
