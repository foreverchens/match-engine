# 1.基本设计

## 1.用户下单链路图

```mermaid
sequenceDiagram
    participant U as User
    participant O as Order Service
    participant A as Account Service
    participant M as Match Service

    U->>+O: Submit order information
    O->>O: Save order to database
    O->>+A: Request balance lock
    A-->>-O: Balance locked result
    O->>M: Send order to Match Service (via MQ)
    O-->>-U: Return submission success

```

## 2.撮合流程图

1. 订单服务通过MQ将订单推送到撮合服务
2. 撮合服务消费订单信息将其提交到Disruptor同步队列
3. Disruptor队列消费处理器处理将订单提交到订单簿
4. 订单簿内部撮合后产生Order和Trade数据
   1. 将撮合后数据通过撮合回调处理器处理
   2. 将Order和Trade组装为撮合事件通过MQ推送到账户服务更新余额
   3. 将Order和Trade组装为撮合事件通过MQ推送到订单服务更新订单状态

```mermaid
sequenceDiagram
    participant O as Order Service
    participant M as Match Service
    participant D as Disruptor
    participant B as Order Book
    participant H as CallbackHandler
    participant A as Account Service

    Note over O,M: 1. Order Service → Match Service via MQ
    O->>M: Publish order message (via MQ)

    Note over M,D: 2. Match Service → Disruptor
    M->>D: Submit order to Disruptor queue

    Note over D,B: 3. Disruptor → Order Book
    D->>B: Dispatch order

    Note over B: 4. Order Book internal matching
    B->>B: Execute matching engine

    Note over B,H: 4.i Trigger callback handler
    B->>H: onMatchFinished(Order, Trade[])

    Note over H,A: 4.ii Publish TradeEvent
    H-->>A: TradeEvent (via MQ)

    Note over H,O: 4.iii Publish OrderStatusEvent
    H-->>O: OrderStatusEvent (via MQ)

```

## 3.余额自动释放机制

1. 订单服务向账户服务提交锁余额请求
2. 账户服务接收到锁余额请求
3. 生成预锁事件 将余额短时锁定固定秒数
4. 返回预锁定结果
5. 将预锁事件提交到延时队列 延时处理
6. 如果到期未接收到订单服务提交的锁定通知 释放锁定的余额
7. 订单服务锁定通知
   1. 订单服务在做完余额锁定和订单入库后
   2. 需要向账户服务通过MQ发送一条通知 要求长期锁定该部分余额知道订单被撮合或取消

```mermaid
sequenceDiagram
    participant O as Order Service
    participant A as Account Service
    participant Q as Delay Queue

    O->>+A: 1. Pre-lock balance request
    A->>A: 2. Create pre-lock event & lock balance locally (fixed X seconds)
    A-->>-O: 3. Return pre-lock result
    A->>Q: 4. Submit pre-lock event to delay queue

    alt Confirmation received before timeout
        O->>A: 7. Lock confirmation notification (via MQ)
        A->>Q: Cancel delayed release event
    else Timeout expired without confirmation
        Q-->>A: 6. Delay event triggered
        A->>A: Release pre-locked balance
    end

```

## 4.订单簿数据结构

订单簿数据具有以下特点

订单数量和访问频率大致以当前价格为中心呈正态分布，且靠近市价的订单簿价格具有连续性

顾可将数据分为靠近市价的热区和远离市价的冷区

冷区用两个红黑树

热区用环形数组+双向链表+哈希存储

环形数组每个元素以价格为索引 价格最小步长为间距 并指向一个双向链表

每个双向链表存储当前价格下的所有订单

只需维护头节点用于get 尾节点用于插入

其他操作通过orderId->Node 的hash来进行

冷热区数据具有自平衡机制

![ringArr](./deploy/ringArr.svg)

# 2.详细设计

