package icu.match.web.model;

import org.springframework.data.relational.core.mapping.Table;

import icu.match.common.OrderSide;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Data
@Builder
@Table("origin_order")
@Schema(description = "提交撮合订单参数")
@NoArgsConstructor
@AllArgsConstructor
public class OriginOrder {

	private Long userId;

	private Long orderId;

	private String symbol;

	private OrderSide side;

	private String type;

	private long origQty;

	private long price;

}
