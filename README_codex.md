# 撮合引擎 Match Engine

面向普通开发者与交易从业者的轻量级撮合引擎示例项目。系统基于 Spring Boot WebFlux，内置订单簿、撮合逻辑、WAL/Snapshot
持久化与一个简单的前端演示页面。你可以用它快速体验撮合流程，或作为二次开发的起点。

> 需要英文版？请查看 [README.en.md](README.en.md)。

## 项目亮点

- 单机撮合内核：支持限价单（LIMIT）与市价单（MARKET），TIF 包含 GTC、IOC（FOK 暂未启用）。
- 高性能数据结构：热区使用 RingBuffer，冷区使用 TreeMap，根据行情动态迁移价位。
- 响应式接口：REST API + Swagger 文档，返回值全部是 JSON，方便接入任何前端或脚本。
- 可视化体验：`trade.html` 提供最小化的下单与行情查看页面，默认端口 `8080`。
- 可扩展的事件流：撮合线程通过 Disruptor 环形队列运行，预留 MQ/Redis 钩子便于集成生产系统。

## 快速开始

### 1. 准备环境

- JDK 11（或更高版本）。
- Maven 3.8+。
- MySQL 8.x：默认连接 `r2dbc:pool:mysql://localhost:3306/test`，账号密码均为 `test`。
- Redis（可选）：默认指向 `localhost:6379`，目前主要作为扩展点，缺省不影响启动。

> 如果你已经有不同的数据库/Redis 地址，请修改 `src/main/resources/application.yml` 后再启动。

MySQL 初始化示例：

```sql
CREATE DATABASE IF NOT EXISTS test CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'test'@'%' IDENTIFIED BY 'test';
GRANT ALL PRIVILEGES ON test.* TO 'test'@'%';
FLUSH PRIVILEGES;
```

### 2. 启动服务

```bash
mvn clean package            # 可选：编译与单元测试
mvn spring-boot:run          # 启动应用（或 java -jar target/match-engine-*.jar）
```

启动成功后终端会提示：

```
api doc  : http://localhost:8080/webjars/swagger-ui/index.html
homepage : http://localhost:8080/trade.html
```

### 3. 验证服务

- 浏览器访问 `http://localhost:8080/trade.html` 体验下单与撤单。
- 开发者可在 `http://localhost:8080/webjars/swagger-ui/index.html` 查看/调试 API。
- 成交记录会写入 `data/wal`，快照存放在 `data/snapshots`，方便排查与回放。

## 常用 API

| 方法   | 路径                  | 说明                                |
|------|---------------------|-----------------------------------|
| POST | `/api/order`        | 提交订单并返回撮合结果。                      |
| POST | `/api/order/cancel` | 根据 `symbol + orderId + price` 撤单。 |
| GET  | `/api/order/trades` | 查看最新成交列表。                         |

### 下单示例

```bash
curl -X POST http://localhost:8080/api/order \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1001,
    "orderId": 90000001,
    "symbol": 1,
    "side": 0,
    "type": 0,
    "tif": 0,
    "qty": 100000,
    "price": 300000
  }'
```

字段含义：

- `side`：0=买单（BID），1=卖单（ASK）。
- `type`：0=限价单（LIMIT），1=市价单（MARKET）。
- `tif`：0=GTC，1=IOC，2=FOK（当前只对 0/1 生效）。
- `qty`、`price` 采用整数，通常表示「交易单位 * 1e4/1e8」，按需自定义精度。

撤单示例：

```bash
curl -X POST http://localhost:8080/api/order/cancel \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1001,
    "orderId": 90000001,
    "symbol": 1,
    "side": 0,
    "type": 0,
    "tif": 0,
    "qty": 0,
    "price": 300000
  }'
```

## 项目结构速览

```
match-engine/
├── src/main/java/icu/match
│   ├── Main.java                # Spring Boot 启动入口
│   ├── core/                    # 撮合内核（订单簿、撮合算法、WAL、Snapshot）
│   ├── service/                 # Disruptor 事件、撮合线程绑定
│   └── web/                     # WebFlux 控制器与接口模型
├── src/main/resources
│   ├── application.yml          # 服务与数据源配置
│   └── static/trade.html        # 简易前端
├── data/
│   ├── wal/                     # 写前日志（订单流）
│   └── snapshots/               # 订单簿快照
└── README.md
```

## 常见问题

- **MySQL 连不上？** 请确认服务已启动、端口开放，或改用本地 Docker。也可以把 `spring.r2dbc` 配置指向你自己的库。
- **接口返回 415/400？** 记得在请求头携带 `Content-Type: application/json`，并保证字段类型均为整数。
- **IOC 没有落单？** 设计如此：IOC 未成交的部分会直接丢弃，这是撮合引擎的默认行为。

## 研发建议

- 使用 `mvn test` 运行现有单元测试，验证核心撮合逻辑。
- 需要集成 MQ、Redis、风控等外围系统时，可以在 `service/disruptor` 模块中订阅事件。
- 如果计划部署到生产环境，建议配合独立的订单存储、风控与监控系统。

欢迎基于本项目进行二次开发或提交 Issue/PR 改进功能。
