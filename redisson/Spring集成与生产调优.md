# Redisson Spring 集成与生产调优面试题

Redisson 在实际项目里几乎不会单独用，都是和 **Spring Boot + Spring Cache + @Transactional** 全家桶一起上。这部分是 **P7 工程能力的主战场**——面试官不需要你会写 Starter，但要你**踩过坑、知道为什么踩、怎么避坑、怎么调参**。核心三件事：**Spring Cache 注解缓存 + @Transactional 锁顺序坑 + 序列化与 Netty 调优参数**。

---

## 一、Spring Boot Starter 集成（5 分钟搞定）

### 1.1 依赖坐标（⚠️ 版本对应关系必考）

Redisson Starter 的版本号和 Spring Boot 大版本**严格对应**，搞混会启动报错：

| Spring Boot 版本 | Redisson Starter 依赖坐标 |
|:---|:---|
| **Spring Boot 3.x**（Jakarta EE，JDK 17+） | `org.redisson:redisson-spring-boot-starter:3.25.x`（3.x 最新版） |
| **Spring Boot 2.x**（JDK 8~11） | `org.redisson:redisson-spring-boot-starter:3.25.x`（同样是 3.25.x，内部自动适配 2.x） |
| Spring Boot 1.x | `org.redisson:redisson-spring-boot-starter:2.15.x`（老版本，别用了） |

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
</dependency>
```

> 引入后 **Spring Data Redis 的 RedisTemplate 也能用**（Redisson Starter 会同时装配 `RedisConnectionFactory` 和 `RedissonClient` 两个 Bean），两套 API 混用完全没问题。

### 1.2 配置方式（三种）

```yaml
# ===== 方式一：最简版（用 Spring 默认 spring.redis 前缀，单节点即可用）=====
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: null
    database: 0
# 不用写额外配置，RedissonAutoConfiguration 会自动读取上面的地址
# 缺点：复杂模式（哨兵/集群）和高级参数（序列化/连接池/线程数）配不了
```

```yaml
# ===== 方式二：⭐ 推荐生产方式（file: 引用独立 Redisson YAML 配置）=====
spring:
  redis:
    redisson:
      # 指向类路径下或文件系统中的 redisson.yml
      config: classpath:redisson-config.yml

# 然后 resources/redisson-config.yml 写完整配置
# （上一篇「核心架构与使用方式.md」写了哨兵/集群4种模式的完整 YAML，直接复用）
```

```yaml
# ===== 方式三：config: 后面直接内嵌 Redisson YAML 字符串=====
spring:
  redis:
    redisson:
      config: |
        clusterServersConfig:
          nodeAddresses:
            - "redis://node1:6379"
            - "redis://node2:6379"
          password: "xxx"
        threads: 16
        codec: !<org.redisson.codec.Kryo5Codec> {}
# 不推荐，长 YAML 内嵌可读性差
```

### 1.3 装配出来的 Bean

```java
@RestController
public class DemoController {

    // ✅ 自动装配（Starter 里的 RedissonAutoConfiguration 干的）
    @Autowired
    private RedissonClient redisson;

    // ✅ 同时也能拿到 Spring 标准的 RedisTemplate
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
}
```

---

## 二、Spring Cache 注解整合（@Cacheable / @CacheEvict / @CachePut） ⭐⭐⭐⭐⭐

Redisson 是 **Spring Cache 官方指定默认实现**（Spring Boot 3.x 起，有 `redisson-spring-cache-provider`）。比 Spring Data Redis 自带的 `RedisCacheManager` 有两大优势：① **每个 key 可以独立过期**（RedisCacheManager 只能每个 cacheName 一个过期）；② **sync=true 背后用 RLock 分布式互斥锁，击穿零代码**。

### 2.1 配置 RedissonSpringCacheManager

```java
@Configuration
@EnableCaching                         // 启动类或配置类加这个
public class RedissonCacheConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        // 创建每个缓存的配置（TTL + 最大空闲）
        Map<String, CacheConfig> config = new HashMap<>();

        // 缓存名 = "product"：TTL 30 分钟、maxIdle 10 分钟
        config.put("product", new CacheConfig(30 * 60 * 1000, 10 * 60 * 1000));
        // 缓存名 = "user"：TTL 1 小时
        config.put("user", new CacheConfig(60 * 60 * 1000, 0));

        // ① 基础版：远端 Redis 缓存（RMapCache 背）
        return new RedissonSpringCacheManager(redissonClient, config);

        // ② ⭐ 进阶版（本地缓存二级）：RLocalCachedMap 背
        // LocalCachedCacheManager 每个 cacheName 一份本地缓存配置
        // LocalCacheConfig localCfg = LocalCacheConfig.defaults()
        //     .cacheSize(1000).timeToLive(10, TimeUnit.MINUTES);
        // return new RedissonSpringLocalCachedCacheManager(redissonClient, config, localCfg);
    }
}
```

### 2.2 业务代码：@Cacheable（一行搞定缓存穿透/击穿）

```java
@Service
public class ProductService {

    /**
     * @Cacheable：先查缓存，命中直接返回；没命中就查 DB，结果写回缓存
     * - cacheNames = "product" → Redis 中的 key 前缀 = "product:"
     * - key = "#skuId"          → 最终 key = "product:1001"（SpEL 表达式）
     * - sync = true             ⭐⭐⭐【击穿神器】同一 skuId 并发过来时，
     *                                   Redisson 会加分布式互斥锁（RLock），
     *                                   只让第一个线程查 DB+写缓存，其余线程等结果。
     *                                   不用自己写 SETNX 重建缓存逻辑！
     */
    @Cacheable(cacheNames = "product", key = "#skuId", sync = true)
    public Product getProduct(Long skuId) {
        // 下面这段只有在【缓存完全没命中】时才会执行（而且只一个线程执行，sync=true）
        log.info("查 DB，skuId={}", skuId);
        return productMapper.selectById(skuId);
    }

    // 更新后让缓存失效（Cache Aside 模式：先更 DB 再删缓存）
    @CacheEvict(cacheNames = "product", key = "#product.id")
    public void updateProduct(Product product) {
        productMapper.updateById(product);
        // @CacheEvict 标注的方法执行完后，自动删 Redis 中的 product:id
        // 注意：是先执行方法（更新 DB）→ 后删缓存，正确顺序！✅
    }

    // 写后直接更新缓存（不常用，适合实时性极高的）
    @CachePut(cacheNames = "product", key = "#result.id")
    public Product createProduct(Product product) {
        productMapper.insert(product);
        return product;  // 返回值会被写进缓存
    }
}
```

### 2.3 sync=true 的底层原理（P7 追问 ⭐）

```
sync=true 时 RedissonSpringCache 自动加锁流程：

 Thread-A/B/C 三个并发 getProduct(1001)
    ↓ 都查缓存 product:1001 → 没命中
    ↓
    ├─ A 抢到 RLock(product:1001:lock)
    │     ├─ 再查一次缓存（双检，避免 A 拿锁期间有人已经写好了）
    │     ├─ 还是没 → 查 DB + put 进缓存
    │     └─ 解锁
    │
    ├─ B/C 没抢到锁，就 park 等待（等锁释放 + Pub/Sub 通知缓存好了）
    └─ 醒来后再查缓存 → 已命中 → 直接返回 ✅
        ↑ 全程不用碰 DB，击穿零代码
```

---

## 三、🔴🔴🔴 @Transactional + 分布式锁的「顺序坑」（P7 必背，面试翻车率 80%）

### 3.1 错误写法（绝大多数人第一反应，生产会出并发脏写）

```java
@Service
public class StockService {

    @Autowired private RedissonClient redisson;
    @Autowired private StockMapper stockMapper;

    /**
     * ❌ 典型翻车代码：@Transactional 在最外层，锁在方法内部
     *    AOP 顺序是：【事务开启】→ 【执行业务：加锁 → 扣库存 → 解锁】→ 【事务提交】
     *    问题出在：解锁 → 事务提交之间有空窗期！
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductWrong(Long skuId, int qty) {
        RLock lock = redisson.getLock("stock:" + skuId);
        try {
            lock.lock();                // ① 加锁
            Stock s = stockMapper.selectBySku(skuId);
            if (s.getStock() < qty) throw new RuntimeException("库存不足");
            s.setStock(s.getStock() - qty);
            stockMapper.updateById(s);  // ② 改库存（这一步是写 DB 的事务内 update）
        } finally {
            lock.unlock();              // ③ ⚠️ 先解锁了！但 @Transactional 还没 commit！
        }
    }                                   // ④ 方法出了大括号 → Spring AOP commit
                                        //   → ③ → ④ 之间的空窗期：锁放了、数据没提交
}
```

**空窗期并发问题**：

```
时间轴：
 ┌Thread-A: deductWrong(1001, 1)
 │ ① lock  → ② update s.stock 100→99 → ③ unlock（锁放了！）  → ④ 还没 commit（DB 还没真的改）
 │                                                                 ▲
 │                                                                 │ 就在这一刻！
 │ Thread-B: deductWrong(1001, 1)                                   │
 │   ① 加锁：没问题啊！没人持锁了  ✅                                   │
 │   查 DB：stock=100！（因为 A 还没 commit，Repeatable Read 下 B 看不到）
 │   扣 99、update、unlock、commit
 │
 └ Thread-A 终于 commit 了：stock 又被写成 99
    → 但实际扣了 2 次！应该是 98 才对！超卖 1 件！🔻🔻🔻
```

### 3.2 正确写法：**事务外锁 + 事务内执行**

两种方式选一种，**本质都是让加锁 AOP 顺序在事务 AOP 外层**：

#### 方式 A：Controller 层加锁（最直观、零配置）

```java
@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired private RedissonClient redisson;
    @Autowired private StockService stockService;

    @PostMapping("/deduct")
    public R deduct(Long skuId, Integer qty) throws Exception {
        RLock lock = redisson.getLock("stock:" + skuId);
        boolean ok = lock.tryLock(5, 30, TimeUnit.SECONDS);
        if (!ok) return R.fail("系统繁忙，请稍后");
        try {
            // ✅ 外面拿好锁 → 再进入 Service 的 @Transactional
            stockService.deduct(skuId, qty);    // 这里 deduct 方法内 @Transactional
            return R.ok();
        } finally {
            // ✅ 事务 commit/rollback 完（service 方法已经 return），才解锁
            lock.unlock();
        }
    }
}
```

#### 方式 B：切面 + Order 控制（推荐，不用每个 Controller 写）

```java
// ========== ① 自定义注解 ==========
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();             // SpEL 表达式，如 "'stock:' + #skuId"
    long waitTime() default 5;
    long leaseTime() default 30;
    TimeUnit unit() default TimeUnit.SECONDS;
}

// ========== ② AOP 切面，@Order 必须 < 事务切面的 Order！==========
// @Transactional 默认 order = Ordered.LOWEST_PRECEDENCE（int 最大值）
// 我们设 1，数字越小越先执行（外层） → 【加锁在事务外层】
@Aspect
@Component
@Order(1)
public class DistributedLockAspect {

    @Autowired private RedissonClient redisson;
    private ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock lock) throws Throwable {
        // 解析 SpEL，拿到真实锁 key（支持拼方法参数）
        String key = parseKey(lock.key(), pjp);
        RLock rLock = redisson.getLock(key);
        boolean ok = rLock.tryLock(lock.waitTime(), lock.leaseTime(), lock.unit());
        if (!ok) throw new BizException("系统繁忙，请稍后重试");
        try {
            return pjp.proceed();  // ✅ 执行业务（里面 @Transactional 才开启）
        } finally {
            rLock.unlock();        // ✅ 方法返回 → 事务早已 commit/rollback 完 → 才解锁
        }
    }
}

// ========== ③ Service 直接用注解 ==========
@Service
public class StockService {

    // 切面 Order=1（外层）→ 事务切面（外层顺序大，内层）
    // 实际执行顺序：加锁 → 开事务 → 扣库存 → 事务 commit → 解锁 ✅
    @DistributedLock(key = "'stock:' + #skuId", waitTime = 3, leaseTime = 20)
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long skuId, int qty) {
        Stock s = stockMapper.selectBySku(skuId);
        if (s.getStock() < qty) throw new RuntimeException("库存不足");
        s.setStock(s.getStock() - qty);
        stockMapper.updateById(s);
    }
}
```

> 💡 **P7 回答要求**：别只说「事务外锁」，要**说原因（为什么锁内会有空窗）**+ **说 AOP Order 控制方案**+ **说结果时序「加锁 → 开事务 → 业务 → 事务 commit → 解锁」**。

---

## 四、序列化选型（生产别用默认 JDK）

Redisson 默认序列化是 **JDK 序列化（`SerializationCodec`）**，烂大街的坑：

| 维度 | 默认 JDK SerializationCodec | ⭐推荐 **Kryo5Codec** | 跨语言推荐 **JacksonJsonCodec** |
|:---|:---|:---|:---|
| 要求类实现 | 必须 `implements Serializable` | 不需要（字段自动探测） | 不需要（反射 getter/setter） |
| 序列化体积 | 大（对象头、类信息冗余） | **小（JDK 的 1/5 ~ 1/10）** | 中（JSON 文本） |
| 速度 | 慢（反射 + IO） | **极快（字节码生成 + 二进制）** | 中（JSON 解析） |
| 类版本兼容 | ❌ 极差（加个字段都反序列化失败） | ✅ 较好（自动处理新增/缺失字段） | ✅ 很好（字段多/少都能解析） |
| 跨语言（Java 写 Python 读） | ❌ 完全不行（Java 专有格式） | ❌ 不行（Kryo 是 Java 生态） | ✅ 完全可以（JSON 通用） |
| 可读性 | ❌ 二进制瞎 | ❌ 二进制瞎 | ✅ 人眼能读 |
| **推荐场景** | 只有本地调试、小项目临时用 | **90% Java 内部微服务首选** | 跨语言/和前端/其他系统对接 |

```yaml
# 配置方法（redisson-config.yml 顶部）
codec: !<org.redisson.codec.Kryo5Codec> {}      # 生产推荐
# 或
codec: !<org.redisson.codec.JsonJacksonCodec> {}   # 跨语言
```

---

## 五、Netty + 连接池调优参数（⭐ 生产调优面试）

| 配置 | 默认值 | 调优建议 |
|:---|:---|:---|
| `threads` / `nettyThreads` | CPU 核数 × 2 | **IO 密集型（Redis 本质就是 IO）**：CPU×2~×4 都行；计算密集型别调大（线程切换开销）；16 核机器 32~64 都行 |
| `connectionPoolSize` | 64 | 单节点默认 64 条 Netty Channel；**一般够用别乱加大**（太多连接 Redis 服务端也吃不住）；大 QPS 压测时才调到 128 |
| `connectionMinimumIdleSize` | 10（单）/ 24（集群） | 生产调大到 32~48；避免冷启动/高峰期临时创建连接的延迟 |
| `keepAlive` | false | 建议 `true`；TCP Keepalive 探活，防止路由/防火墙把空闲连接断开导致超时 |
| `retryAttempts` / `retryInterval` | 3 次 / 1500ms | 慢查询/网络抖动时，重试 3×1.5s=4.5s；金融场景调小一点，宁可快速失败也不积压 |
| `timeout`（命令响应超时） | 3000ms | **生产建议 1000ms 或 1500ms**；Redis 正常命令是毫秒级，3s 超时意味着 Redis 本身已经扛不住了，早点熔断降级 |
| `idleConnectionTimeout` | 10000ms（10s） | 空闲连接回收；不要太小，不然频繁建连 |
| **`pingConnectionInterval`** | 30000ms | **建议设 30000**；每 30 秒主动发 PING，保持连接活跃 + 探测死连接，有效防「长连接突然断连首命令超时」 |

### 调优三原则
1. **大部分场景默认值够用**，别乱改；只有压测确实有瓶颈才调
2. **连接池不是越大越好**（Redis 单线程命令执行，1000 条连接也还是一个一个跑），反而会占满服务端 fd
3. **超时时间宁小勿大**，配合 Sentinel 限流降级，比等 3s 后报错用户体验好

---

## 六、生产三大红线（背下来，出了问题就是 P0 事故）

| 红线 | 错误示例 | 为什么会炸 |
|:---|:---|:---|
| **① 禁止 lock() 无参永久等** | `lock.lock();` → 不设 waitTime | 别人锁长期不释放（死循环/没解锁），你的线程永久 park，线程池耗光 = 应用雪崩 | 必须 `tryLock(wait, lease, unit)`，lease 不传就靠看门狗；至少有 wait 超时 |
| **② 禁止 unlock 不在 finally 里** | `try { lock.lock(); doBiz(); lock.unlock(); }` → 业务抛异常就没 unlock | 锁只能等 30s 看门狗过期（或默认 TTL 到点）。这期间别人都拿不到锁=业务堵死。finally 必须写，哪怕你觉得业务 100% 不会抛错 |
| **③ 禁止长事务内持有锁** | 锁内 RPC 调下游 2s + DB 慢查 1s → 持锁 3s | 锁的粒度应该是**毫秒级**（查内存 + 判断 + DB update）。长事务内持锁=锁成为整个系统的瓶颈。正确做法：锁内只做**「核心判断 + 更新」**，RPC/慢查在锁外做、传结果进来。 |

---

## 七、易错点

| # | 易错点 | 正确理解 |
|:---:|:---|:---|
| 1 | 「Spring Boot 引入 starter 就能直接用，还配什么序列化」 | 默认 JDK 序列化坑死人；生产必须改 Kryo5 或 Jackson，不然类一改就反序列化炸 + 体积超大 |
| 2 | 「Redisson 会帮我做 Spring Cache 的 cacheName 配置」 | 不配置默认 TTL=永久（0），缓存永远不失效；必须显式给每个 cacheName 配过期 |
| 3 | 「@Cacheable sync=true 就是 synchronized」 | ❌ synchronized 是单机 JVM 级；sync=true 背后是 **Redisson RLock 分布式锁**，多实例也互斥 |
| 4 | 「@Transactional + 锁，只要写在同一个方法里就行」 | ❌ 顺序错就空窗期并发；要切面 Order 在事务外层（数字更小）或显式写在 Controller 层 |
| 5 | 「连接池越大越好用，设它 1024」 | ❌ Redis 单线程处理命令，连接再多没用；Cluster 模式默认每个节点 64，3 主 3 从就是 6×64=384 条，够用了 |
| 6 | 「Redisson 和 RedisTemplate 不能混用」 | ✅ 完全可以混用，Starter 会同时装配；RedisTemplate 适合简单命令，Redisson 适合高级工具/锁 |
| 7 | 「Kryo 序列化类里加字段就反序列化失败」 | Kryo5 默认兼容新增字段（反序列化时给新字段赋默认值）、删除字段时自动忽略；比 JDK 强 100 倍 |
| 8 | 「分布式锁切面的 @Order 写成 2147483647 没问题」 | ❌ 事务切面默认就是这个值（最大），写成一样 Spring 顺序未定义；**明确写 1、2 这种小数**，保证切面在外层 |

---

## 八、一句话总结

> Redisson + Spring Boot 的正确姿势 = **Starter 依赖 + classpath 引用独立 YAML 配置 + Kryo5Codec 序列化 + RedissonSpringCacheManager 配 TTL + @Cacheable(sync=true) 防击穿**。**P7 面试翻车率最高的坑：@Transactional + 分布式锁顺序**——必须「加锁在事务外，顺序 AOP @Order 设小数在事务切面外层（或 Controller 层显式加锁）」保证时序 「锁→事务→业务→commit→解锁」。生产三大红线：tryLock 带超时、finally 必解锁、长事务别持锁；调优就是默认值 + 适当加大 idle 连接、命令超时设 1s 内+降级兜底。

---

## 九、相关笔记

| 主题 | 笔记 |
|:---|:---|
| 分布式锁全家桶（各种锁使用方式对照） | [分布式锁全家桶原理.md](分布式锁全家桶原理.md) |
| Spring @Transactional 底层原理（AOP 切面/事务边界） | [../Spring/事务.md](../Spring/事务.md) |
| Spring Boot 自动配置（RedissonAutoConfiguration 原理类似） | [../Spring/自动配置与启动流程.md](../Spring/自动配置与启动流程.md) |
| 二级缓存 RLocalCachedMap（Spring Cache 本地缓存版） | [分布式对象与集合API.md](分布式对象与集合API.md) |
| 缓存三兄弟（@Cacheable sync=true 解决的就是击穿） | [../redis/缓存问题与实战.md](../redis/缓存问题与实战.md) |
