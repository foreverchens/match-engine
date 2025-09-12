package icu.match.core.model;/**
 *
 * @author 中本君
 * @date 2025/8/18
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 中本君
 * @date 2025/8/18 
 */
@Slf4j
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {

	private long userId;

	private long orderId;

	private int symbol;

	private byte side;

	private byte type;

	private byte tif;

	private long price;

	private long qty;

	private long time;

}


