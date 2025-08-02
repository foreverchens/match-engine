package icu.service.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author 中本君
 * @date 2025/08/02 
 */
@Data
@Builder
@Schema(description = "提交撮合订单参数")
public class OrderParam {


	@Schema(description = "订单Id")
	private Long orderId;


	@Schema(description = "订单数量", example = "100")
	private BigDecimal qty;

	@Schema(description = "订单价格", example = "100")
	private BigDecimal price;

}
