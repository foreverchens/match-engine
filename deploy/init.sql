CREATE TABLE origin_order
(
    order_id   BIGINT UNSIGNED  NOT NULL COMMENT '订单Id（与订单模块一致）',
    user_id    BIGINT UNSIGNED  NOT NULL COMMENT '用户Id',
    symbol     INT UNSIGNED     NOT NULL COMMENT 'SymbolId',
    side       TINYINT UNSIGNED NOT NULL COMMENT '订单方向 0=BID,1=ASK',
    type       TINYINT UNSIGNED NOT NULL COMMENT '订单类型 0=LIMIT,1=MARKET',
    tif        TINYINT UNSIGNED NOT NULL COMMENT '订单有效时间 0=GTC,1=IOC,2=FOK',

    price      BIGINT UNSIGNED  NOT NULL COMMENT '价格',
    qty        BIGINT UNSIGNED  NOT NULL COMMENT '原始数量',

    status     TINYINT UNSIGNED NOT NULL DEFAULT 100 COMMENT '100 NEW,110 OPEN,111 PART,120 FILLED,121 CANCELED,122 REJECTED',

    created_at TIMESTAMP                 DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id),

    CHECK (status IN (100, 110, 111, 120, 121, 122, 123))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单';

CREATE TABLE match_trade
(
    match_seq      BIGINT UNSIGNED  NOT NULL COMMENT '成交序列号，全局唯一或分片内唯一',
    symbol         INT UNSIGNED     NOT NULL COMMENT '交易对，如BTCUSDT',
    taker_user_id  BIGINT UNSIGNED  NOT NULL COMMENT '吃单用户ID',
    maker_user_id  BIGINT UNSIGNED  NOT NULL COMMENT '挂单用户ID',
    taker_order_id BIGINT UNSIGNED  NOT NULL COMMENT '吃单订单ID',
    maker_order_id BIGINT UNSIGNED  NOT NULL COMMENT '挂单订单ID',
    taker_side     TINYINT UNSIGNED NOT NULL COMMENT '吃单方向',
    price          BIGINT UNSIGNED  NOT NULL COMMENT '成交价，整数tick',
    qty            BIGINT UNSIGNED  NOT NULL COMMENT '成交量，整数最小单位',
    trade_time     BIGINT           NOT NULL COMMENT '成交时间戳（毫秒或纳秒）',

    PRIMARY KEY (match_seq)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='撮合成交流水';


