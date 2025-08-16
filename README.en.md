# Order Book Matching Engine

## Overview

This document describes an **order book data structure** based on a **Ring Buffer (Hot Zone)** and a **Red-Black Tree (
Cold Zone)**, designed for **high-throughput, low-latency matching** (e.g., crypto spot/futures).  
The design divides the price space into two storage regions:

- **Hot Zone**: Fixed-length (power of 2) array of `PriceLevel`, covering active price ranges. Emphasizes cache locality
  and O(1) lookup.
- **Cold Zone**: Red-Black Trees (`TreeMap`) for prices far from the current range. Emphasizes ordered queries and O(log
  N) operations.

By **dynamic migration** between zones and **re-centering**, the system balances performance, memory efficiency, and
robustness.

---

## Core Components

### 1) `OrderNode`

- Represents a single order, acts as a node in a linked list.
- Fields: `orderId`, `userId`, `ask` (true = ASK/sell, false = BID/buy), `qty`, `time`, `prev/next`.
- Allocated/recycled via **`OrderNodePoolFixed`** to reduce GC overhead.

### 2) `OrderNodePoolFixed`

- Fixed-capacity object pool (uses `ArrayDeque` for cache locality).
- Overflow handled by direct `new`; overflowed objects left to GC.
- Provides stats: pool usage, overflow count, peak usage.

### 3) `OrderQueue` (**Queue inside each price level**)

- **Doubly-linked list + HashMap** (`orderId → node`) for O(1) `peek/push/remove/patchQty`.
- FIFO: append at tail, consume from head → price-time priority.
- Aggregation: `size`, `totalQty`, `dump()` for inspection.

### 4) `PriceLevel`

- A single price + single side (ASK/BID).
- Contains one `OrderQueue`.
- APIs: `submit`, `getFirst`, `patchQty`, `cancel`, `remove`, `isEmpty`, `size`, `totalQty`, `dump`.
- Direction consistency ensured (no mixing ASK/BID inside).

### 5) `RingOrderBuffer` (**Hot Zone**)

- Power-of-2 length array of `PriceLevel`, index wrap-around via `mask = length-1`.
- State: `lowIdx/highIdx`, `lowPrice/highPrice`, `lastIdx/lastPrice`.
- APIs:
    - Insert/cancel/remove: `submit(price,node)`, `cancel(price,id)`, `remove(price,id)`
    - Best price: `getBidBestLevel()` (scan down from `lastIdx`), `getAskBestLevel()` (scan up)
    - Migration: `shiftLeft(level)` / `shiftRight(level)` (single step), `migrateToInclude(level)` (multi-step)
    - Diagnostics: `dump()`
- **Index ↔ Price mapping is exact**; migration always uses a non-null `PriceLevel` (possibly empty placeholder).

### 6) `ColdOrderBuffer` (**Cold Zone**)

- Backed by two `TreeMap<Long, PriceLevel>`:
    - `asks` (ascending)
    - `bids` (descending)
- Stores only **non-empty** levels.
- APIs:
    - Submit/cancel/remove: `submit`, `cancel`, `remove`
    - Best: `bestAsk/bestBid`, `popBestAsk/popBestBid`
    - Exact fetch: `takeExact(price,ask)`
    - Monitoring: `dump`, `sizeAsks/sizeBids`, `vacuum()`

### 7) `RecenterManager`

- Keeps hot zone **balanced around `lastIdx`**.
- Skew = `(lastIdx - lowIdx) / (length-1) * 100%`; ideal center = 50%.
- Threshold-based migration:
    - Deviation ≥10% → 1 step, ≥20% → 2 steps, ≥30% → 3 steps, ≥40% → 4 steps.
- Each step triggers **hot/cold migration**:
    - Left shift: import `highPrice` level, eject `lowPrice` level.
    - Right shift: import `lowPrice` level, eject `highPrice` level.

### 8) `MatchingEngine`

- Orchestrates all components; single-threaded matching.
- Supports **LIMIT + {GTC, IOC}**:
    - Match inside hot zone first.
    - Remaining GTC → insert into hot/cold zone (depending on price).
    - Remaining IOC → discarded.
- Trade price = passive side price.
- After each trade: update `ring.recordTradePrice(price)` and `recenter.checkAndRecenter()`.

---

## Component Relationship (with `OrderQueue`)

```mermaid
classDiagram
    class MatchingEngine {
        +submitLimit(...)
        +cancel(...)
    }
    class RecenterManager {
        +checkAndRecenter()
        +currentSkewPercent()
    }
    class RingOrderBuffer {
        +submit(price,node)
        +cancel(price,id)
        +remove(price,id)
        +getBidBestLevel()
        +getAskBestLevel()
        +migrateToInclude(level)
        +recordTradePrice(price)
        +dump()
    }
    class ColdOrderBuffer {
        +submit(price,node)
        +cancel(price,id,ask)
        +remove(price,id,ask)
        +bestBid()/bestAsk()
        +popBestBid()/popBestAsk()
        +takeExact(price,ask)
        +dump()
    }
    class PriceLevel {
        +submit(node)
        +getFirst()
        +patchQty(orderId,newQty)
        +cancel(orderId)
        +remove(orderId)
        +size()
        +totalQty()
        +isAsk()
        +isEmpty()
        +getPrice()
        +dump()
    }
    class OrderQueue {
        +push(node)
        +peek()
        +remove(orderId)
        +patchQty(orderId,newQty)
        +clear()
        +getSize()
        +getTotalQty()
        +dump()
    }
    class OrderNode
    class OrderNodePoolFixed

    MatchingEngine --> RingOrderBuffer
    MatchingEngine --> ColdOrderBuffer
    MatchingEngine --> RecenterManager
    RingOrderBuffer --> PriceLevel
    PriceLevel --> OrderQueue
    OrderQueue --> OrderNode
    OrderNodePoolFixed --> OrderNode
```

Hot/Cold Migration Sequence

```mermaid
sequenceDiagram
    participant Recenter as RecenterManager
    participant Ring as RingOrderBuffer
    participant Cold as ColdOrderBuffer

    Recenter->>Ring: compute skew (lastIdx vs lowIdx)
    alt skew >= threshold
        Recenter->>Ring: plan N steps shiftLeft/Right
        loop N steps
            alt left shift
                Cold-->>Ring: provide highPrice PriceLevel (or empty placeholder)
                Ring->>Recenter: migrateToInclude(incoming)
                Ring-->>Cold: eject PriceLevel(s)
                Cold->>Cold: reinsert non-empty levels
            else right shift
                Cold-->>Ring: provide lowPrice PriceLevel (or empty placeholder)
                Ring->>Recenter: migrateToInclude(incoming)
                Ring-->>Cold: eject PriceLevel(s)
                Cold->>Cold: reinsert non-empty levels
            end
        end
    else
        Recenter-->>Ring: no migration
    end

```

```mermaid
flowchart TD
    U[New Order] --> X{Inside Hot Zone?}
    X -- No --> C[ColdOrderBuffer.submit]
    X -- Yes --> M[Match in Hot Zone]
    M --> |Partial/Full| T[Emit Trade Event]
    M --> |Remainder + GTC| H[Insert into hot/cold zone]
    M --> |Remainder + IOC| D[Discard remainder]
    T --> R[ring.recordTradePrice]
    R --> RC[RecenterManager.checkAndRecenter]

```