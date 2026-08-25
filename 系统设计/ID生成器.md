# ID 生成器

> **P7 面试核心**：发号器是分布式系统的「基础设施中的基础设施」——订单号、用户 ID、消息 ID、短链 ID 全靠它。面试常作为「你们系统里发号器怎么做的」开场题，**3 种主流方案对比 + Snowflake 时钟回拨 + 号段模式**是 P7 必答。

---

## 一、需求澄清

### 1.1 功能性需求

| # | 需求 | 说明 |
|:---:|:---|:---|
| 1 | 全局唯一 | 跨机房跨服务不重复 |
| 2 | 趋势递增 | 让 DB 主键索引不会频繁页分裂（InnoDB 聚簇索引）|
| 3 | 高性能 | 单机 ≥ 10w QPS，集群可线性扩 |
| 4 | 可读 | 长度合理（≤ 64bit），最好含业务含义 |

### 1.2 非功能性需求

| 维度 | 目标 | 说明 |
|:---|:---|:---|
| **高可用** | 99.99% | 发号器挂 = 全站没法下单/创建任何实体 |
| **低延迟** | P99 < 1ms | 同步调用，阻塞业务 |
| **单调性** | 同一机器 ID 严格递增 | 满足 DB 范围查询优化 |
| **无外部依赖** | 尽量不依赖 DB/ZK | 避免单点 |

### 1.3 范围澄清

- 是否暴露业务信息？→ 不能（订单号被人遍历/推断规模是安全事故）
- 是否需要可读短码？→ 这是另一个问题（如短链 Base62），发号器只产 ID
- 多机房？→ 是，需保证全局唯一

---

## 二、容量估算

- 单服务每秒 10w ID（订单/用户/消息全用）
- 集群 10 台 = 100w ID/s
- 单机 10w QPS = 单线程 100us 一个 → 必须内存计算，**不能每次查 DB**

---

## 三、四种方案全景对比 ⭐⭐⭐

| # | 方案 | 唯一性 | 有序性 | 性能 | 长度 | 缺点 | 用谁 |
|:---:|:---|:---:|:---:|:---:|:---:|:---|:---|
| 1 | **UUID** | 全局唯一 | ❌ 无序 | 高（本地生成）| 36 字符 | 无序做 DB 主键页分裂、占空间大 | ❌ 不推荐做主键 |
| 2 | **DB 自增** | 唯一 | 严格递增 | 低（DB 瓶颈）| 8 字节 | 单点、性能瓶颈 | ❌ 仅小流量 |
| 3 | **号段模式（Leaf-Segment）** ⭐ | 全局唯一 | 趋势递增 | **极高**（DB 批量取号）| 8 字节 | DB 挂了短暂不可用（双 buffer 缓解）| ✅ 主流方案之一 |
| 4 | **Snowflake** ⭐⭐ | 全局唯一 | 趋势递增 | **极高**（本地时钟）| 8 字节 | **时钟回拨**问题 | ✅ 主流方案之一 |

> 💡 **生产首选**：**Snowflake（主）+ 号段（兜底）双轨**。Snowflake 性能最高但有时钟回拨；号段模式稳定但依赖 DB。两者互为兜底。

---

## 四、方案一：UUID（了解即可，不推荐）

### 4.1 原理

UUID v4 = 随机生成的 128 位 = 16 字节 = 36 字符（带连字符）。

```java
String id = UUID.randomUUID().toString();  // 如 "f47ac10b-58cc-4372-a567-0e02b2c3d479"
```

### 4.2 为什么不推荐做 DB 主键

| 问题 | 说明 |
|:---|:---|
| **无序** | InnoDB 主键是聚簇索引，无序插入会让 B+ 树频繁页分裂，**写放大 3~5 倍** |
| **空间大** | 36 字符 vs 8 字节 BIGINT，索引体积涨 4 倍 |
| **不可读** | UUID 完全随机，没法人工识别 |
| **索引性能差** | InnoDB 二级索引存主键，主键越长索引越大 |

### 4.3 适用场景

- 离线/无 DB 场景（日志 traceId、临时 token）
- 不做主键、不参与排序的场景

---

## 五、方案二：DB 自增（小流量可用）

### 5.1 原理

```sql
-- 建表用 AUTO_INCREMENT
CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);

-- 或独立发号器表
CREATE TABLE id_generator (
    biz_tag VARCHAR(64) PRIMARY KEY,    -- 业务标识
    max_id  BIGINT NOT NULL DEFAULT 0,  -- 当前已分配最大 ID
    step    INT NOT NULL DEFAULT 1000   -- 每次取的步长
);

-- 取号（每次 UPDATE 后返回新 max_id）
UPDATE id_generator SET max_id = max_id + step WHERE biz_tag = 'order';
SELECT max_id FROM id_generator WHERE biz_tag = 'order';
```

### 5.2 致命缺陷

| 缺陷 | 说明 |
|:---|:---|
| **性能瓶颈** | 每次取号 UPDATE 行锁，单 DB QPS 上限 ~3000 |
| **单点** | DB 挂了发号器就停 |
| **扩容难** | 多机房多 DB 时需要预分片步长，扩容要重新分配 |

> 仅适合小流量场景（< 1000 QPS），生产基本不用。

---

## 六、方案三：Snowflake ⭐⭐（性能之王）

### 6.1 64bit 位段分配（Twitter 经典版）

```
 0 | 0000000000000000000000000000000000000000000000000000000000 0000 000000000000
 ─┬─ ─────────────────────┬─────────────────────────────────────────┬────┬─────────┬──
  │                       │                                         │    │         │
  │      41bit 毫秒时间戳 │              10bit 工作机ID              │5bit│ 12bit   │1bit
  │  (可用约 69 年)        │     (1024 台机器/机房段)               │DB  │ 序列号  │符号
  │                       │                                         │ID  │(同ms内) │(0)
  │                       │                                         │    │         │
```

| 段 | bit 数 | 容量 | 说明 |
|:---:|:---:|:---|:---|
| 符号位 | 1 | — | 固定 0，保证正数 |
| 时间戳 | 41 | 2.4 万亿毫秒 ≈ **69 年** | 从自定义纪元起算（如 2020-01-01）|
| 工作机 ID | 10 | **1024** 台 | 5bit 机房 + 5bit 机器（可自定义拆分）|
| 序列号 | 12 | **4096**/ms | 同一毫秒内最多 4096 个 ID |

**单机理论性能**：4096/ms × 1000ms = **409.6w ID/s**（实际因 GC、网络等打折扣到 100~400w/s）

### 6.2 Java 实现（核心逻辑）

```java
public class Snowflake {
    // 起始纪元（2020-01-01 00:00:00 UTC）的毫秒时间戳
    private static final long EPOCH = 1577836800000L;

    // 各段 bit 数
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);   // 1023
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);    // 4095

    // 各段左移位数（用于拼接）
    private static final long SEQUENCE_SHIFT = 0;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;            // 12
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;  // 22

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID)
            throw new IllegalArgumentException("workerId 超出范围 [0, 1023]");
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long now = System.currentTimeMillis();

        // ① 时钟回拨检查（核心！详见 §6.3）
        if (now < lastTimestamp) {
            return handleClockBackwards(now);
        }

        if (now == lastTimestamp) {
            // ② 同一毫秒内，序列号递增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // ③ 序列号耗尽（4096 个用完了），等下一毫秒
                now = tilNextMillis(lastTimestamp);
            }
        } else {
            // ④ 新的一毫秒，序列号归零
            sequence = 0L;
        }

        lastTimestamp = now;

        // ⑤ 拼接 64bit ID
        return ((now - EPOCH) << TIMESTAMP_SHIFT)
             | (workerId << WORKER_ID_SHIFT)
             | (sequence << SEQUENCE_SHIFT);
    }

    private long tilNextMillis(long last) {
        long t = System.currentTimeMillis();
        while (t <= last) t = System.currentTimeMillis();
        return t;
    }

    private long handleClockBackwards(long now) {
        long offset = lastTimestamp - now;
        if (offset <= 5) {
            // ⑤ 小回拨：等待回拨毫秒数后再发（轻量，业务无感）
            try { Thread.sleep(offset + 1); } catch (InterruptedException e) {}
            return nextId();  // 重新拿时间戳
        }
        // ⑥ 大回拨：报错或走备用机房发号
        throw new IllegalStateException("时钟回拨 " + offset + "ms，超过阈值，拒绝发号");
    }
}
```

### 6.3 时钟回拨问题 ⚠️⭐⭐（必考）

**为什么会回拨**：NTP 时钟同步、虚拟机迁移、容器时钟漂移都会让 `System.currentTimeMillis()` **倒退**。回拨后用旧时间戳发号，**会和之前发的号重复**。

| 回拨程度 | 解法 | 代价 |
|:---|:---|:---|
| **小回拨（< 5ms）** | `Thread.sleep(回拨毫秒 + 1)` 等到追上再发 | 几毫秒延迟，业务无感 |
| **中回拨（5ms ~ 3s）** | 用「序列号延展位」借用之前的时间戳继续发 | 需要记录上次序列号 |
| **大回拨（> 3s）** | 报错 + 告警 + 走备用机房号段 | 业务短时不可用 |
| **极端回拨** | 启动时拒绝发号 + 人工介入 | 防止雪崩 |

**生产增强**：
- 启动时从 ZK/ETCD 拉取上次最大时间戳，**当前时间 < 上次时间戳 → 拒绝启动**
- 用「待发号」队列缓存请求，回拨期间走号段兜底
- Leaf-Snowflake（美团）方案：用 ZK 持久化每个 worker 的最近时间戳，启动时校验

### 6.4 workerId 分配问题

10bit workerId 怎么分配给 1024 台机器？

| 方案 | 实现 | 优缺点 |
|:---|:---|:---|
| **配置文件写死** | 每台机器配置不同 workerId | 简单，但机器迁移/扩容易配错 |
| **ZK 自动分配** ⭐ | 启动时去 ZK 注册临时节点，拿序号当 workerId | 自动化，机器宕了自动释放 workerId |
| **DB 分配** | 启动时 INSERT ON DUPLICATE KEY 拿机器 IP 对应 workerId | 不依赖 ZK，但 DB 是单点 |
| **机房+机器位拆分** | 5bit 机房 + 5bit 机器，跨机房天然不冲突 | 路由清晰，推荐 |

---

## 七、方案四：号段模式 Leaf-Segment（美团）⭐⭐

### 7.1 核心思想

**不要每次取号都查 DB**，而是一次取**一批号（号段）**到本地内存，用完了再取。把 DB 的 QPS 从「每号一次」降到「每号段一次」。

```
DB: max_id = 1000, step = 1000
   ↓ 启动时取号段
本地：current = 0, max = 1000（用 0~999）
   ↓ 业务取号
本地：current++ → 0, 1, 2, ..., 999
   ↓ 用到 800（80%）时触发异步取下一号段
本地：current=800, max=1000, next_max=2000（预加载）
   ↓ 用到 1000 时无缝切到 next
本地：current=0, max=1000（来自 2000 号段）
```

### 7.2 双 Buffer 优化（防 DB 抖动）

```
本地内存：[当前号段 1000-1999] [下一号段 2000-2999 预加载]
                              ↑
                              当前用到 80% 时，异步线程去 DB 取下一号段
                              即使 DB 暂时挂了，也有 20% 缓冲去重试
```

```java
public class LeafSegment {
    private final String bizTag;
    private volatile Segment current;     // 当前号段
    private volatile Segment next;        // 预加载的下一号段
    private final DataSource db;
    private volatile boolean loading = false;

    public synchronized long nextId() {
        if (current == null || !current.hasNext()) {
            // 当前号段用完，切换到 next
            if (next == null) {
                loadSegment();  // 同步加载（首次启动）
            }
            current = next;
            next = null;
        }
        long id = current.next();

        // 用到 10%（默认阈值）时，异步预加载 next
        if (!loading && current.getUsage() < 0.1 && next == null) {
            loading = true;
            executor.submit(this::preloadNext);
        }
        return id;
    }

    private void preloadNext() {
        try {
            Segment seg = loadSegmentFromDB();
            this.next = seg;
        } finally {
            loading = false;
        }
    }
}

class Segment {
    long current;  // 当前已发
    long max;      // 号段上界
    long step;
    boolean hasNext() { return current < max; }
    long next() { return ++current; }
    double getUsage() { return (double)(max - current) / step; }
}
```

### 7.3 DB 取号 SQL（原子）

```sql
UPDATE id_generator
SET max_id = max_id + step
WHERE biz_tag = 'order';
SELECT max_id, step FROM id_generator WHERE biz_tag = 'order';
-- 假设原 max_id=1000, step=1000 → 更新后 max_id=2000
-- 本地拿到 [1001, 2000] 这个号段
```

### 7.4 优缺点

| ✅ 优点 | ❌ 缺点 |
|:---|:---|
| 性能极高（本地内存，无网络）| DB 挂了 → 号段用完即不可用（双 buffer 缓解）|
| ID 趋势递增（DB 主键友好）| 多机房需分配不同 step 段，扩容麻烦 |
| 无时钟回拨问题 | ID 可读性差（连续数字，暴露业务规模）|
| 实现简单 | 严格单调性弱（多实例同时取号，ID 顺序乱）|

---

## 八、选型决策树

```
                       ┌──────────────────────────┐
                       │ 你的业务场景是什么？     │
                       └────────────┬─────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
    「极高并发，单机 10w+ QPS」   「中等并发，要稳定」      「日志 traceId 等无 DB」
            │                       │                       │
            ▼                       ▼                       ▼
       Snowflake             Leaf-Segment 号段              UUID
       (需处理时钟回拨)       (DB 批量取号)              (本地生成)
            │                       │
            │                       │
            └─────┬─────────────────┘
                  ▼
        「两者互为兜底」
        ─ Snowflake 主，号段兜底（时钟回拨时切到号段）
        ─ 号段模式主，Snowflake 兜底（DB 挂时切到 Snowflake）
```

---

## 九、Snowflake vs 号段模式对比（P7 必背）

| 维度 | Snowflake | Leaf-Segment |
|:---|:---|:---|
| **性能** | 单机 400w ID/s | 单机 ~100w ID/s（本地内存）|
| **依赖** | 仅本地时钟 + workerId 分配 | DB（双 buffer 缓解单点）|
| **有序性** | 同机严格递增 | 趋势递增（多实例可能乱序）|
| **时钟回拨** | **必须处理** ⚠️ | 无此问题 |
| **可读性** | 64bit 数字，可反解时间 | 纯数字，无业务含义 |
| **扩展性** | 加机器即可 | 加 DB 分片或扩 step |
| **适合场景** | 高并发、长 ID、订单号 | 中等并发、要求稳定 |
| **生产案例** | Twitter、百度 UidGenerator | 美团 Leaf、滴滴 Tinyid |

---

## 十、易错点 ⚠️

| # | 易错点 | 正确理解 |
|:---:|:---|:---|
| 1 | 「Snowflake 单机性能 400w QPS」 | ⚠️ 理论上限是 4096/ms = 409.6w/s，但**实际受 GC、锁竞争、网络**影响，生产实测 100~400w/s。同步锁是瓶颈，可用 RingBuffer（如百度 UidGenerator）异步预生成 |
| 2 | 「时钟回拨 sleep 等就行」 | ⚠️ 仅小回拨（<5ms）可 sleep；大回拨（>3s）sleep 几秒业务早超时了。必须**报错 + 走备用号段兜底** |
| 3 | 「workerId 写死配置最简单」 | ❌ 机器迁移/扩容/重装易配错，导致**两台机器用同一 workerId → ID 必然重复**。必须用 ZK/DB 自动分配 |
| 4 | 「号段模式 DB 挂了立即不可用」 | ⚠️ 双 buffer 设计下，DB 短时挂（<1 分钟）本地还有号段可用，**不是立即停摆**。但号段用完仍未恢复就挂了 |
| 5 | 「Snowflake 的 41bit 时间戳够用 69 年」 | ✅ 从 2020 起算可用到 2089 年。但**起始纪元不能改**（改了所有 ID 含义变化），生产要预先规划 |
| 6 | 「号段模式 ID 连续会被遍历」 | ✅ 业务侧要加扰或拼接业务前缀（如 `order_id = "ORD" + snowflake + checkCode`），**不要直接暴露连续号** |
| 7 | 「UUID 完全不能用」 | ❌ 离线场景（traceId、临时 token）UUID 简单可靠，只是不适合做 DB 主键 |
| 8 | 「Snowflake ID 严格全局递增」 | ❌ 同机严格递增，**跨机不保证**（不同机器时钟不同步）。如果业务要严格全局递增，得加全局序列号（性能差）|
| 9 | 「序列号 12bit 同毫秒内够 4096 个」 | ⚠️ 极端高并发单机超过 4096/ms 会**等下一毫秒**，导致 P99 抖动。生产前要评估单机峰值 |

---

## 十一、一句话总结

> ID 生成器的本质是「**唯一 + 有序 + 高性能**」三角，UUID 失在无序、DB 自增失在性能、Snowflake 胜在性能但有时钟回拨坑、号段模式胜在稳定但依赖 DB。P7 生产首选 **Snowflake + 号段双轨互兜底**，面试核心讲清 Snowflake 位段分配、时钟回拨三档处理、workerId 自动分配、号段双 buffer 优化。
