# Redisson 面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——**Redis 服务端看 redis/ 目录，Java 端怎么用、怎么锁、怎么缓存、怎么调优，全在这里**。Redisson 不是又一个 Jedis，它是「**基于 Netty NIO 的 Redis 高级客户端 + 分布式工具集**」：把 40+ 种 Java 并发对象（ReentrantLock、ReadWriteLock、Semaphore、CountDownLatch、BlockingQueue、ConcurrentMap…）搬到 Redis 上实现，是 Spring 生态的事实标准。

按「**定位与架构 → 分布式锁全家桶 → 分布式对象与集合 → Spring 集成与生产调优**」的主线组织，每篇均含对比表、ASCII 图、Lua 脚本/Java 代码、易错点、一句话总结。回答默认以 **Redisson 3.25+ / Redis 7.x** 为准。

---

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 0 | 🚀 入门指南（5 分钟上手） | [入门指南.md](入门指南.md) | 加依赖→3 种模式 YAML 配置抄作业→Hello World（纯Java+SpringBoot）→**最常用 API 速查表**（锁/Map/队列/限流 20 个）→**10 个入门必踩坑 FAQ** |
| 1 | 🏛️ 核心架构与使用方式 | [核心架构与使用方式.md](核心架构与使用方式.md) | **Jedis vs Lettuce vs Redisson 三选一**、Netty NIO 线程模型、连接池、4 种部署模式（单/哨兵/集群/主从）YAML 配置 |
| 2 | 🔒 分布式锁全家桶原理 | [分布式锁全家桶原理.md](分布式锁全家桶原理.md) | **可重入锁 RLock（Hash 计数 Lua）**、**看门狗 1/3 TTL 续期源码**、公平锁（List 排队）、读写锁分模式、联锁 MultiLock、红锁 RedLock（N/2+1）、信号量 RSemaphore、闭锁 RCountDownLatch |
| 3 | 🗂️ 分布式对象与集合 API | [分布式对象与集合API.md](分布式对象与集合API.md) | **RLocalCachedMap 本地缓存（一级缓存一致性）**、RMapCache 带 TTL 字段、RBloomFilter 布隆过滤器、RDelayedQueue 延迟队列、RRateLimiter 令牌桶限流、RAtomicLong、RSortedSet |
| 4 | 🌱 Spring 集成与生产调优 | [Spring集成与生产调优.md](Spring集成与生产调优.md) | **Spring Cache @Cacheable 注解接入**、`@Transactional + 分布式锁` 顺序坑（事务外加锁）、Starter 自动配置、序列化选型（Kryo vs Jackson vs JDK）、Netty 线程/连接池调优、生产三大红线 |

---

## P7 必背清单（速查）

### 一、架构与选型
- **Redisson vs Jedis vs Lettuce**：Jedis=同步阻塞 BIO、无内置分布式工具、连接池瓶颈；Lettuce=异步响应式 Netty，但只做「基础客户端」无高级并发对象；**Redisson=Netty NIO + 40+ 分布式并发对象**，Spring Boot 3.x / Spring Cache 默认推荐
- **Redisson 线程模型**：底层走 **Netty NIO 事件循环组**（默认线程数=CPU核数×2），命令发送 pipeline 化；业务线程把命令丢到队列就返回，Netty 线程负责真正网络 IO
- **连接池不是 Jedis 那种阻塞池**：Redisson 的 `连接池` 其实是 **Netty Channel 池**（每个 Redis 节点建立多条长连接），空闲连接有心跳；Cluster 模式下对每个主节点都建独立连接池
- **4 种部署模式**：`singleServerConfig`（单机）、`sentinelServersConfig`（哨兵）、`clusterServersConfig`（Cluster）、`masterSlaveServersConfig`（主从，手动指定主从）；YAML 文件或 Config 类配置

### 二、分布式锁全家桶（⭐ 面试重灾区）
- **RLock 可重入锁底层不是 SETNX**：用 **Hash 结构存`field=UUID:ThreadId value=重入次数`** + Lua 脚本保证判断+写入原子；默认 TTL=lockWatchdogTimeout=30s
- **看门狗续期机制三条铁律**：① 只有 `leaseTime=-1`（即不传 leaseTime 或显式传 -1）才启动看门狗；② 默认每 **lockWatchdogTimeout / 3 ≈ 10s** 续一次；③ 续期是重新 `PEXPIRE key 30000`，线程死了定时任务线程也没了自然不续
- **看门狗源码入口**：`RedissonLock#tryAcquireAsync` → 加锁成功后调用 `scheduleExpirationRenewal` → 用 `Timeout newTimeout = timer.newTimeout(task, 10s)` 递归延期（Netty HashedWheelTimer 时间轮）
- **公平锁 RFairLock 怎么实现**：额外维护一个 **Redis List 做等待队列**（`{lockName}:threadsQueue`），新来的线程 `RPUSH` 入队，加锁时只允许 `LINDEX 0`（队头）的线程抢，先到先得；解锁后 `LPOP` 出下一个
- **读写锁 RReadWriteLock 分模式**：ReadLock=共享锁（多个读线程可同时拿，只要没写锁）、WriteLock=排他锁；用 Hash 的两个独立 field 分别记数，**读锁存在时写锁排队、写锁存在时读写都阻塞**
- **联锁 RMultiLock**：同时对 N 把独立锁加锁，「**全部拿到才算成功**」；适用「扣库存 + 扣余额 + 生成订单」这三段要原子联动的场景，底层循环对每把锁发 tryLock
- **红锁 RRedLock 是 MultiLock 的子类**：N 个独立 Redis Master（无复制关系），加锁成功条件 = **「实际拿到数量 ≥ N/2+1」且「总耗时 < TTL」**；拿不到就对所有节点 unlock 再重试
- **RedLock 争议要答务实**：时钟漂移/GC STW 导致理论仍不完美；**生产极少用（性能折半+运维N套独立集群）**，正确做法 = Redisson 单集群 + 幂等/乐观锁兜底
- **信号量 RSemaphore**：底层是 String 计数 key，`acquire()` 用 Lua 先 DECR 再判断，≥0 就拿到、<0 就订阅 Pub/Sub 等别人 release 再抢
- **闭锁 RCountDownLatch**：Hash 存计数，`countDown()` 计数-1 到 0 时 PUBLISH 通知所有 await 的线程；Redis 7.x 之前可配合 Pub/Sub

### 三、分布式对象与集合（P7 加分点）
- **RLocalCachedMap = 本地缓存 + Redis 远端缓存（二级缓存）**：Map 数据同时存在「JVM 本地」和「Redis」；本地缓存一致靠 **Redis Pub/Sub 推送失效事件**：某实例改了数据 → 广播 invalidate 消息 → 其他实例收到后删除本地对应 key；或者配置 `timeToLive` 定时过期
- **RLocalCachedMap 三大同步策略**：`INVALIDATE`（默认，推 invalid 消息让别人删）、`UPDATE`（推新 value 给别人）、`NONE`（完全靠 TTL，不推消息）；`NONE` 性能最好但一致性最差
- **RMapCache vs RLocalCachedMap**：RMapCache=只有 Redis 远端、**每个 field 单独设置 TTL**（Hash 每个键独立过期，Redis 原生 Hash 做不到，Redisson 额外维护一个过期排序集合）；RLocalCachedMap=有本地缓存、只能整体 Map 级过期
- **RBloomFilter 布隆过滤器**：底层是 Redis Bitmap + 多个 hash 函数；支持 `tryInit(expectedInsertions, falseProbability)` 初始化预期容量和误判率
- **RDelayedQueue 延迟队列**：底层是 **ZSet（score=过期时间戳）+ BlockingQueue**；`offer(element, delay, unit)` 写到 ZSet，后台定时任务把到点的从 ZSet 搬到目标 BlockingQueue；消费者 take() 从队列取
- **RRateLimiter 限流器**：支持令牌桶/漏桶/滑动窗口；`trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit)` 设置速率；`tryAcquire()` 拿令牌；比手写 `INCR+EXPIRE` 省事且支持集群级限流
- **RAtomicLong / RAtomicDouble**：对应 JUC AtomicLong，Redis String 存值 + Lua CAS 实现 `compareAndSet`；分布式计数器 ID 生成场景

### 四、Spring 集成 & 生产调优（P7 工程能力）
- **Spring Boot Starter 坐标**：`redisson-spring-boot-starter`（注意和 Spring Boot 版本匹配：3.x 对应 `redisson-spring-boot-starter:3.25.x`，2.x 对应 `redisson-spring-boot-starter:3.25.x:2x`）；自动装配类 `RedissonAutoConfiguration`
- **Spring Cache 三种缓存管理器**：`RedissonSpringCacheManager`（每把 Cache = 一个 RMapCache，每个 key 独立 TTL）、`RedissonSpringLocalCachedCacheManager`（二级缓存，RLocalCachedMap 背）、`RedissonNativeCacheManager`（存二进制、兼容多个应用实例共享）
- **⚠️ @Cacheable + 异步查询坑**：`sync=true` 背后就是 Redisson 的可重入锁（同一个 key 的并发查询只有一个真正查 DB，其他等结果）；高并发热点缓存场景直接用，不用自己写互斥锁
- **🔴 @Transactional + 分布式锁 顺序必背坑**：**必须在事务外锁**（先加锁 → 再开事务 → 执行业务 → 事务提交 → 再解锁），如果锁在@Transactional方法内部会出现：事务还没 commit 就解锁了 → 其他线程拿到锁查到旧数据 → 并发脏写；解法：AOP 切面（加锁@Around 在事务切面外层，order 配置比事务小）或者把锁写在 Controller 层
- **序列化选 Kryo，别用默认 JDK**：默认 JDK 序列化体积大、慢、类版本兼容差；生产配 `codec = new Kryo5Codec()`（二进制紧凑、跨语言差但Java内部够用）；如果跨语言就 `JacksonJsonCodec`
- **Netty 调优三参数**：`threads`（Netty EventLoop 线程数，默认 CPU×2，IO 密集型可大）、`nettyThreads`（同上）、`connectionMinimumIdleSize`（每个节点最小空闲连接，避免冷启动建连接）、`connectionPoolSize`（每个节点连接池上限，默认 64，够了别乱调大）
- **生产三大红线**：① 禁止 `lock()` 无参永久等（必须 `tryLock(wait, lease, unit)`，lease 不传就开看门狗，二选一）；② 禁止 finally 外 unlock（锁泄漏=等30s）；③ **禁止业务长事务内持有锁**（DB事务秒级、锁应该毫秒级就放）

---

## 学习/复习建议

1. 按 1→4 顺序建立体系：**「为什么选 Redisson 而不是 Jedis」是开场第一问**，接着进入面试主战场——分布式锁的原理。
2. 必须能白板画的三张图：看门狗续期时间线（10s→30s 循环）、公平锁 List 排队机制、RLocalCachedMap Pub/Sub 失效广播链路。
3. P7 必追问链要背熟：「SETNX 简陋版 3 坑」→「Redisson RLock 可重入 Hash Lua」→「看门狗怎么续、什么时候不续」→「主从切换锁丢失」→「RedLock 为什么不用」→「事务 + 锁的顺序坑」→ 答到这里基本满分。
4. 反套路点：别上来就说 RedLock 完美；要讲「工程上更务实的是锁 + 幂等双保险、极端场景不追求 100% 锁完美」——这是 P7 和应届生最大的差别。
5. 结合项目讲：二级缓存（RLocalCachedMap 降低 Redis 70% QPS）、延迟队列（支付 30 分钟未支付取消订单，不用 MQ）、Spring事务外加锁切面（防超卖），都是加分项目案例。
6. 速背节奏：先把 `分布式/分布式锁.md` 的 Redisson 部分背熟（那个讲了 SETNX→Redisson 的演进），再来本目录看「Redisson 扩展的 6 种锁 + 集合 + Spring 坑」。

---

## 相关笔记

- 分布式锁顶层（三种方案选型矩阵 + 手写正确版 + 基础 RLock Lua）→ [../分布式/分布式锁.md](../分布式/分布式锁.md)
- Redis 服务端机制（底层数据结构、持久化、内存淘汰、高可用）→ [../redis/README.md](../redis/README.md)
- Spring 事务（事务外锁切面的事务基础）→ [../Spring/事务.md](../Spring/事务.md)
- Spring Boot 自动配置原理（Starter 装配机制）→ [../Spring/自动配置与启动流程.md](../Spring/自动配置与启动流程.md)
- Java 线程与锁（ReentrantLock / ReadWriteLock / Semaphore，Redisson 分布式版本的「本地版对照」）→ [../java/线程面试题.md](../java/线程面试题.md)
- 系统设计（秒杀扣库存 / Feed流缓存 都是 Redisson 高频场景）→ [../系统设计/README.md](../系统设计/README.md)
