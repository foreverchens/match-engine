package icu.match.core.model;/**
 *
 * @author 中本君
 * @date 2025/8/17
 */

import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import icu.match.util.SnowFlakeIdUtil;
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
@Table("match_trade")
@NoArgsConstructor
@AllArgsConstructor
public final class MatchTrade {

	long matchSeq;

	int symbol;

	long takerUserId;

	long makerUserId;

	long takerOrderId;

	long makerOrderId;

	byte takerSide;

	long price;

	long qty;

	long tradeTime;

	@Transient
	boolean makerFilled = false;


	/** 复用前清空（可选） */
	public void clear() {
		matchSeq = 0;
		symbol = 0;
		takerUserId = 0L;
		makerUserId = 0L;
		takerOrderId = 0L;
		makerOrderId = 0L;
		price = 0L;
		qty = 0L;
		tradeTime = 0L;
	}

	public MatchTrade fill(int symbol, long tUid, long mUid, long tOid, long mOid, byte takerSide, long price,
						   boolean makerFilled, long qty) {
		this.matchSeq = SnowFlakeIdUtil.nextId();
		this.symbol = symbol;
		this.takerUserId = tUid;
		this.makerUserId = mUid;
		this.takerOrderId = tOid;
		this.makerOrderId = mOid;
		this.takerSide = takerSide;
		this.price = price;
		this.qty = qty;
		this.makerFilled = makerFilled;
		this.tradeTime = System.currentTimeMillis();
		return this;
	}

}
