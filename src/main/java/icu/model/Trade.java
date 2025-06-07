package icu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

	Long bidUserId;

	Long askUserId;

	Long bidOrderId;

	Long askOrderId;

	BigDecimal price;

	BigDecimal filledQty;


	public Trade(Long bidOrderId, Long askOrderId, BigDecimal price, BigDecimal filledQty) {
		this.bidOrderId = bidOrderId;
		this.askOrderId = askOrderId;
		this.price = price;
		this.filledQty = filledQty;
	}
}
