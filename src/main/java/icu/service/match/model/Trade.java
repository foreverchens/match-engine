package icu.service.match.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 撮合成交原始信息
 *
 * @author 中本君
 * @date 2025/07/27
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

	BigDecimal qty;

	LocalDateTime time;
}
