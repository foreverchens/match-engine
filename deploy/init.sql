CREATE TABLE origin_order
(
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    order_id   BIGINT      NOT NULL COMMENT '订单ID',
    symbol     VARCHAR(50) NOT NULL COMMENT '交易对符号',
    side       VARCHAR(10) NOT NULL COMMENT '买卖方向: BUY / SELL',
    type       VARCHAR(20) NOT NULL COMMENT '订单类型: LIMIT / MARKET / STOP',
    orig_qty   BIGINT      NOT NULL COMMENT '原始数量',
    price      BIGINT      NOT NULL COMMENT '价格',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id),
    KEY idx_user (user_id),
    KEY idx_symbol (symbol)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

