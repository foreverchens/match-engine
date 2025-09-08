CREATE TABLE origin_order
(
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    order_id   BIGINT      NOT NULL COMMENT '订单ID',
    symbol     VARCHAR(50) NOT NULL COMMENT '交易对符号',
    side       VARCHAR(10) NOT NULL COMMENT '买卖方向: BUY / SELL',
    type       VARCHAR(20) NOT NULL COMMENT '订单类型: LIMIT / MARKET / STOP',
    tif VARCHAR(20) NOT NULL COMMENT '有效时间GTC IOC FOK',
    orig_qty   BIGINT      NOT NULL COMMENT '原始数量',
    price      BIGINT      NOT NULL COMMENT '价格',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id),
    KEY idx_user (user_id),
    KEY idx_symbol (symbol)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

CREATE TABLE match_trade
(
    match_seq      BIGINT UNSIGNED NOT NULL COMMENT '成交序列号，全局唯一或分片内唯一',
    symbol         VARCHAR(32)     NOT NULL COMMENT '交易对，如BTCUSDT',
    taker_user_id  BIGINT UNSIGNED NOT NULL COMMENT '吃单用户ID',
    maker_user_id  BIGINT UNSIGNED NOT NULL COMMENT '挂单用户ID',
    taker_order_id BIGINT UNSIGNED NOT NULL COMMENT '吃单订单ID',
    maker_order_id BIGINT UNSIGNED NOT NULL COMMENT '挂单订单ID',
    taker_side     VARCHAR(16)     NOT NULL COMMENT '吃单方向',
    price          BIGINT          NOT NULL COMMENT '成交价，整数tick',
    qty            BIGINT          NOT NULL COMMENT '成交量，整数最小单位',
    trade_time     BIGINT          NOT NULL COMMENT '成交时间戳（毫秒或纳秒）',

    PRIMARY KEY (match_seq),
    KEY idx_symbol_time (symbol, trade_time),
    KEY idx_taker_user (taker_user_id),
    KEY idx_maker_user (maker_user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='撮合成交流水';


