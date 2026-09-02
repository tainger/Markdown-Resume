# Redisson 分布式对象与集合 API 面试题

Redisson 的野心远不止「分布式锁」。它把 Java 里常用的 **Map / Set / List / Queue / AtomicLong / BitSet / BloomFilter / RateLimiter / DelayedQueue …** 全部搬到 Redis 上实现，接口名和 JUC 几乎一样（前面加 `R`），开发体验无缝。这部分是 **P7 加分项**（面试官问到「除了锁你还用 Redisson 做过啥」，答出下面任意 3~4 个就是亮点）。

---

## 一、分布式 Map 四兄弟（⭐⭐⭐ 重点）

Redisson 提供了 4 种 Map，别混淆：

| # | 接口 | 本地缓存？ | TTL 粒度 | 典型场景 |
|:---:|:---|:---|:---|:---|
| **① RMap** | 基础分布式 Map | ❌ 只有 Redis 端 | 整个 key 级 TTL | 和 Redis Hash 一样，最基础的分布式 HashMap |
| **② RMapCache** | 扩展版 RMap | ❌ 只有 Redis 端 | **每个 field 独立 TTL** ⭐ | 每个用户/每个商品的过期时间不一样（临时 token、验证码） |
| **③ RLocalCachedMap** | 二级缓存 | ✅ JVM 本地 + Redis | 整个 key 级 TTL | **高频热点读**：商品详情、配置参数；能降 Redis 70%+ QPS |
| **④ RLocalCachedMapCache** | 本地+远端+字段过期 | ✅ JVM 本地 + Redis | 每个 field 独立 TTL | 以上两个的合体（少见，内存压力大） |

---

## 二、RLocalCachedMap 二级缓存（⭐⭐⭐⭐⭐ P7 必考场景）

这是 Redisson 最实用的 API 之一，面试经常作为「你有没有用 Redis 做过极致性能优化」的抓手题。

### 2.1 问题背景

纯 RMap 每次 get 都走一次 Redis 网络 IO（0.5~2ms），如果 10w QPS 就是 10w 次网络往返——Redis 和应用网卡都吃紧。

**RLocalCachedMap 解法：本地 JVM 存一份热点副本 + Redis 远端存全量**

```
┌──────────────────────────────────────────────────────────────┐
│                    应用 A（JVM 本地）                          │
│   Map<String, Product> localCache = ConcurrentHashMap        │
│      ├── "P1001" → Product{name:iPhone, price:5999}           │  ← 本地缓存
│      └── "P1002" → ...                                       │
│                    ▲                                    │     │
│     读命中（本地拿）  │                                    │ 更新时
│              0.1ms  │                                    │ 1. 先写 Redis
│                    │                                    ▼     │
│                 GET                                    PUT    │
└────────────────────┼────────────────────────────────────┬─────┘
                     │ 本地 miss 才查 Redis              │ 2. 广播 invalid Pub/Sub
                     ▼                                    ▼
                ┌──────────────────────────────────────────────────┐
                │              Redis（远端全量数据）                  │
                │   RMap key="product"                              │
                │      field=P1001  value=序列化后的Product         │
                └──────────────────────┬───────────────────────────┘
                                       │ 3. SUBSCRIBE 了 invalid 频道的
                                       │    实例都收到「删 P1001 本地副本」
                                       ▼
                ┌──────────────────────────────────────────────────┐
                │          应用 B、C、D（其他 JVM 实例）              │
                │   收到 invalid 消息 → 删除自己本地的 P1001         │
                │   下一次读就 miss → 重新从 Redis 拉最新值 ✅         │
                └──────────────────────────────────────────────────┘
```

### 2.2 本地缓存同步策略（三种）

| 策略 | 说明 | 一致性 | 性能 |
|:---|:---|:---|:---|
| **`INVALIDATE`（默认）** | A 更新后 PUBLISH 消息，其他实例**删除本地对应 key**，下次读再查 | 高（不会传具体数据，只传删除指令） | 中（删后需要再拉一次） |
| **`UPDATE`** | A 更新后把**最新 value 直接广播**给所有实例，其他实例本地直接覆盖 | 最高（直接推新值） | 低（大 value 广播带宽大） |
| **`NONE`** | 不推送任何消息，完全依赖 **TTL 过期** 兜底 | 最低（可能读到几秒钟旧值） | 最高（无网络广播开销） |

### 2.3 本地缓存淘汰（控制 JVM 内存）

| 配置 | 说明 |
|:---|:---|
| `cacheSize` | 本地最多缓存多少个 key（默认 0 = 不限制，⚠️ 生产一定要设上限！不然内存爆） |
| `timeToLive` | 本地副本最长存活时间（不管怎样到点就删，防脏数据） |
| `maxIdle` | 本地副本多久没被读就删 |
| `EvictionMode.LFU` / `LRU` | 超过 cacheSize 后按什么策略淘汰 |

### 2.4 Java 使用示例

```java
LocalCachedMapOptions<String, Product> options = LocalCachedMapOptions
    .<String, Product>defaults()
    .cacheSize(1000)                    // 本地最多 1000 个 key（必设）
    .timeToLive(10, TimeUnit.MINUTES)   // 本地最多活 10 分钟
    .maxIdle(3, TimeUnit.MINUTES)       // 3 分钟没读就删
    .evictionPolicy(EvictionPolicy.LFU)  // 超了按 LFU 淘汰
    .syncStrategy(SyncStrategy.INVALIDATE) // 同步策略：invalid 默认
    .reconnectionStrategy(ReconnectionStrategy.NONE); // 实例重启后不预热全量

// 用 options 创建（就是 RMap 的用法，完全一样 get/put）
RLocalCachedMap<String, Product> productCache =
    redisson.getLocalCachedMap("product:cache", options);

// 第一次 get → Redis 查，写本地；第 2~N 次 get → JVM 本地 ConcurrentHashMap，0.1ms 返回！
Product p = productCache.get("P1001");

productCache.put("P1001", newProduct);
// 上面这行 put 干了三件事：① 写 Redis Hash；② 删自己本地副本；③ PUBLISH invalid 给其他实例
```

> **面试 P7 加分**：我们项目把商品详情和配置字典从 `RMap` 切到 `RLocalCachedMap` 后，**Redis GET 命令 QPS 从 8w 降到 2w（降了 75%），RT 从 1.8ms 降到 0.1ms（降了 94%）**——**有数字、有场景，这就是 P7 的回答方式。**

---

## 三、RMapCache（每个 field 单独 TTL）

原生 Redis Hash 的**痛点**：`HSET` 只能给整个 `key` 设 TTL，**不能给单个 field 设过期时间**。

RMapCache 是 Redisson 的扩展实现（额外维护一个 `ZSet` 记过期时间戳 + 定时清理线程）：

```java
RMapCache<String, String> verifyCode = redisson.getMapCache("verify:code");

// 每个手机号的验证码，独立过期（字段级 TTL ⭐）
verifyCode.put("13800000001", "482913", 5, TimeUnit.MINUTES);   // 5 分钟过期
verifyCode.put("13900000002", "102938", 10, TimeUnit.MINUTES);  // 10 分钟过期
// 两个互相完全独立：第一个 5 分钟后自动被清（field 级），不影响第二个
```

**适用**：验证码、临时 token、用户登录 session（每个用户过期时间不一样，或 session 最后活跃刷新过期）。

---

## 四、RBloomFilter 布隆过滤器

Redis 没有原生布隆过滤器命令（RedisBloom 是另一个模块）。Redisson 在普通 Redis 上**用 Bitmap + N 个 hash 函数自己实现了一个**，开箱即用不用装 RedisBloom 插件。

```java
RBloomFilter<String> userIdFilter = redisson.getBloomFilter("bf:activeUser");

// 【初始化一次即可】预期插入 100w，误判率 0.1%
// Redisson 会按这两个参数自动算 hash 函数个数和 Bitmap 需要的位数组大小
userIdFilter.tryInit(1_000_000, 0.001);

userIdFilter.add("u1001");   // 加一个元素
userIdFilter.add("u1002");

userIdFilter.contains("u1001");  // ✅ true (一定存在 或 误判)
userIdFilter.contains("u9999");  // ✅ false (一定不存在)
```

**核心特性要背**：

| 特性 | 说明 |
|:---|:---|
| **误判率** | 说「存在」的可能是误判（有一定概率），说「不存在」就一定不存在（不会漏判） |
| **空间效率** | 100w × 0.1% 误判率 ≈ 1.8MB 位数组；比 HashSet 省几百倍内存 |
| **删除麻烦** | 普通 Bloom Filter 不支持删除（多个元素 hash 到同一位，删了会影响别人）；Redisson 有另一个 `RClusteredBloomFilter` 也不行；要删除的场景用**计数布隆过滤器**或用 CMS |

**场景**：缓存穿透前置拦截（先过 BF，不存在直接打回不用查 DB）、海量 UV 去重判断。

---

## 五、RDelayedQueue 延迟队列（⭐ 面试常问：不用 MQ 怎么做延迟任务）

**场景**：
- 订单 30 分钟未支付自动取消
- 用户下单 15 分钟没发优惠券自动补发
- 设备离线 10 分钟没心跳自动报警

不用 RocketMQ 延迟消息也能做——Redisson 的 `RDelayedQueue`：

```java
// 1. 先拿一个「目标阻塞队列」（到点的元素会被搬到这里）
RBlockingQueue<OrderCancelTask> destQueue =
    redisson.getBlockingQueue("order:cancel:queue");

// 2. 包装成「延迟队列」（destQueue 做参数）
RDelayedQueue<OrderCancelTask> delayedQueue =
    redisson.getDelayedQueue(destQueue);

// ========== 生产者：下单时放一个延迟任务 ==========
Order order = createOrder(...);
OrderCancelTask task = new OrderCancelTask(order.getId(), "待取消");
delayedQueue.offer(task, 30, TimeUnit.MINUTES);   // 30 分钟后才出现在 destQueue

// ========== 消费者：一直从 destQueue 拿，到点自然有元素 ==========
while (true) {
    OrderCancelTask task = destQueue.take();      // 没到点就阻塞，到点了就返回
    cancelIfNotPaid(task.getOrderId());           // 取消订单
}
```

**底层实现原理（P7 追问）**：
```
内部有两个 Redis 结构：
  1. ZSet（{队列名}:delayed）    score=过期时间戳（毫秒），member=序列化元素
  2. BlockingQueue（目标队列）   真正消费者 take 的地方

后台有一个「转移线程」：
  每隔固定时间（~1 秒）ZRANGEBYSCORE ZSet -inf ~ 当前时间戳，
  把到点的元素批量 ZPOPMIN 出来 → LPUSH 到目标 BlockingQueue
```

---

## 六、RRateLimiter 分布式限流器（⭐ 实用，高并发必备）

比自己手写 `INCR key EXPIRE`（计数器限流）高级：支持**令牌桶 / 漏桶 / 滑动窗口**三种模式，集群级多实例共享限流。

```java
RRateLimiter limiter = redisson.getRateLimiter("api:rate:createOrder");

// 【配置一次即可】OVERALL 全局限流：每秒最多 1000 个令牌
// RateType.OVERALL = 所有实例总 QPS 限制 1000
// RateType.PER_CLIENT = 每个实例各自限 1000
limiter.trySetRate(RateType.OVERALL, 1000, 1, RateIntervalUnit.SECONDS);

// ========== 每次请求前检查 ==========
if (limiter.tryAcquire(1)) {   // 拿 1 个令牌，拿到就执行
    createOrder(...);          // 正常业务
} else {
    return "系统繁忙，请稍后重试";  // 被限流
}
```

三种算法（trySetRate 内部实际默认是**令牌桶**）：

| 算法 | 说明 | 适用 |
|:---|:---|:---|
| **令牌桶**（默认） | 固定速率往桶里放令牌，来请求拿令牌，桶满丢弃放的令牌；**允许短时间突发流量** | 电商高峰：允许瞬间 2000 QPS（桶里积的令牌），但长时间平均仍 ≤1000 |
| **漏桶** | 流入速度任意，流出严格匀速；**削峰填谷但不允许突发** | 下游系统怕压垮：对下游调用严格均匀 |
| **滑动窗口** | 把时间分小块计数（如每秒一段），解决计数器限流的「突刺问题」 | 对「1 秒最多 100 次」要求严格精确的场景 |

---

## 七、其他高常用但非重灾区 API（了解，能举例加分）

| API | 类比 JUC | 典型场景 |
|:---|:---|:---|
| `RAtomicLong` | AtomicLong | 分布式自增 ID 生成器：`incrementAndGet` |
| `RAtomicDouble` | AtomicDouble | 金额类计数（不太推荐用 Redis 做精确金额，会有精度） |
| `RCountDownLatch` | CountDownLatch | 已经在锁篇讲了 |
| `RSemaphore` | Semaphore | 已经在锁篇讲了 |
| `RBlockingQueue` / `RQueue` | ArrayBlockingQueue / LinkedList | 简单消息队列（生产消息少、无 ACK 机制，生产建议 MQ） |
| `RSet / RList / RSortedSet` | HashSet / ArrayList / TreeSet | 分布式集合；去重、交并差集、排行榜（ZSet 封装） |
| `RBitSet` | BitSet | 大位集合，签到/活跃用户：`set(uid); get(uid)` |
| `RLongAdder` | LongAdder | 高并发计数器（比 RAtomicLong 高并发性能好，分段累加），分布式 JVM 版全局累加 |
| `RObject` / `RBucket<T>` | Object 引用 | 把任何一个 Java 对象作为 Redis String 存：`bucket.set(obj) / get()` |

---

## 八、易错点

| # | 易错点 | 正确理解 |
|:---:|:---|:---|
| 1 | 「RLocalCachedMap 啥也不用配直接用」 | ❌ 默认 `cacheSize=0`（不限制本地 key 数）！如果 key 极多会把 JVM 堆吃爆；生产必须设 cacheSize + TTL |
| 2 | 「RLocalCachedMap 的 UPDATE 同步策略最好，性能最高」 | ❌ UPDATE 直接推 value 本身，大对象会炸带宽；INVALIDATE 只推删除指令更常用 |
| 3 | 「Redis Hash 原生也支持 field 级 TTL」 | ❌ 不支持；RMapCache 是 Redisson 额外维护 ZSet + 清理线程模拟出来的，有一定开销 |
| 4 | 「布隆过滤器能精确判断存在」 | ❌ 有误判（说存在可能不存在）；说不存在就一定不存在 |
| 5 | 「RDelayedQueue 精度毫秒级，到点立刻出」 | ⚠️ 近似；转移线程是轮询机制（~秒级），和 RocketMQ 定时消息精度相当，够用但不做毫秒级触发 |
| 6 | 「RRateLimiter trySetRate 每调用一次都重置」 | ❌ 和 RSemaphore.trySetPermits 一样，**只有从未设置过时才生效**；动态改要先 delete 再设置 |
| 7 | 「把 RLocalCachedMap 当永久数据用」 | ❌ 它是**缓存**（本地副本会被删），永久数据应该用 RMap 或 DB；同时 Redis 也要开持久化不然重启丢数据 |

---

## 九、一句话总结

> Redisson 的分布式对象/集合 = **「把 Java 常用并发容器搬到 Redis」**：核心四件套是 **RLocalCachedMap（二级缓存，降 Redis QPS 70%+，本地缓存通过 Pub/Sub 推 invalid 消息保持一致性）**、**RMapCache（每个 field 独立 TTL，验证码/session 场景）**、**RDelayedQueue（ZSet+BlockingQueue 实现延迟队列，订单自动取消不用 MQ）**、**RRateLimiter（令牌桶分布式限流，手写 INCR+EXPIRE 的工业替代）**；P7 回答要配合「RLocalCachedMap 前后 QPS/RT 对比数字」「延迟队列实现原理 ZSet 轮询转移」等具体场景数据，而不是光背 API。

---

## 十、相关笔记

| 主题 | 笔记 |
|:---|:---|
| 分布式锁全家桶（本系列第 2 篇） | [分布式锁全家桶原理.md](分布式锁全家桶原理.md) |
| Spring 集成（Spring Cache 背后就是 RMapCache） | [Spring集成与生产调优.md](Spring集成与生产调优.md) |
| 缓存穿透三兄弟（布隆过滤器是穿透核心解法） | [../redis/缓存问题与实战.md](../redis/缓存问题与实战.md) |
| 4 种限流算法对比（计数器/滑动窗口/漏桶/令牌桶） | [../分布式/README.md](../分布式/README.md) |
| Redis 原生 ZSet（延迟队列底层）| [../redis/数据类型与底层结构.md](../redis/数据类型与底层结构.md) |
| 高并发场景落地（秒杀、Feed流等用上面的 API）| [../系统设计/README.md](../系统设计/README.md) |
