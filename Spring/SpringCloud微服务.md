# Spring Cloud 微服务

Spring Cloud 是一整套**分布式系统解决方案的全家桶**——注册配置中心、服务调用、网关、限流、分布式事务等都是独立的组件。P7 面试重点是**选型理由 + CAP 权衡 + 生产踩坑**。

---

## 一、注册与配置中心：Nacos / Eureka / Consul / Zookeeper 选型

### CAP 理论回顾

| CAP | 含义 | 对应场景 |
|:---|:---|:---|
| **C**onsistency（一致性） | 同一时刻所有节点看到的数据相同 | 下单后所有副本查余额必须一致 |
| **A**vailability（可用性） | 每个请求都能收到响应（不一定最新数据） | 用户能一直浏览商品页比看到实时库存更重要 |
| **P**artition Tolerance（分区容错） | 节点间网络断开仍能工作 | 分布式系统**必须有 P**，因为网络永远不可靠 |

> **核心结论**：分布式系统 P 是必须的（不选就不是分布式），所以只能**在 C 和 A 之间做 trade-off**，不存在同时满足 CAP 的系统。所以注册中心你要么选 **CP**（注册信息强一致，但集群脑裂时服务不能注册/发现），要么选 **AP**（集群脑裂时两边独立工作，牺牲短暂一致性）。

### 注册中心选型对比

| 维度 | **Nacos**（阿里，**首推**） | Eureka（Netflix） | Consul（HashiCorp） | Zookeeper（Hadoop 生态） |
|:---|:---|:---|:---|:---|
| **CAP** | **AP + CP 可切换**（默认 AP，临时实例 AP / 持久实例 CP） | **纯 AP** | **CP**（Raft 强一致） | **CP**（ZAB 强一致） |
| 集群脑裂时 | AP 模式两边都可提供注册；CP 模式少数派拒绝服务 | 自我保护模式（保留过期实例 90 秒），宁可旧可用也不盲删 | 少数派分区直接拒绝读写，不可用 | Leader 挂了重新选举期间不可用（通常 200ms） |
| **持久化存储** | Derby 内嵌 / MySQL 外置 / PostgreSQL | **无**（纯内存） | Raft snapshot | ZK 数据树 + 快照日志 |
| 健康检查方式 | TCP / HTTP / MySQL 心跳 / gRPC 客户端心跳 | **客户端心跳续约**（30s 发一次；90s 不续约剔除） | 服务端主动 TCP + HTTP 健康检查 | 客户端临时节点 Session 心跳，断连自动移除 |
| **配置中心** | ✅ **自带配置中心**（配置持久化、历史版本、命名空间/Group/tenant 三级隔离、灰度发布） | ❌ 不提供（通常用 Spring Cloud Config） | ✅ 自带 KV 存储（但功能弱，无版本管理） | ⚠️ 可以做但很别扭，官方不推荐（ZK 是协调服务，不适合存大配置文本） |
| 控制台 UI | ✅ 官方免费（配置管理、服务列表、权重配置、容量管理） | ❌ 只有基础 dashboard | ✅ 官方 UI（服务 + KV + ACL） | ❌ 只有第三方 zkui |
| 社区活跃度 | 国内非常活跃（Spring Cloud Alibaba 主流）、持续迭代 | 停止维护（Spring Cloud Netflix 2018 起停更，Archaius/Turbine/Eureka 1.x 死了） | 中等（国外大厂用得多，国内较少） | 老牌稳定（已非云原生方向） |
| **生产规模支持** | 阿里双 11 级实测（百万实例级） | Netflix 级别（但因不可持久化 + 停更，新项目不推荐） | 千级实例 OK，十万级压力 Raft 日志收敛慢 | 支持中等，但 ZK 做配置时 watcher 数量有限 |
| 对接方式 | `spring-cloud-starter-alibaba-nacos-discovery` + 配置 | `spring-cloud-starter-netflix-eureka-client` | `spring-cloud-starter-consul-discovery` | `spring-cloud-starter-zookeeper-discovery` |

### 为什么新项目首推 Nacos（面试加分项）

1. **AP + CP 切换**：默认 AP（服务宁可旧可用也不要注册不可用），但对订单/支付这种需要精确知道每个服务实例是否在线的业务切 CP，灵活；
2. **注册 + 配置二合一**：省掉 Consul + Spring Cloud Config 两套独立组件的运维成本；
3. **配置三级隔离**：命名空间（隔离环境 dev/stg/prod）+ Group（隔离业务线）+ DataId（单文件），完美匹配多环境多租户结构；
4. **国内生态**：和 Sentinel、Seata、RocketMQ 等阿里系中间件天然集成；
5. **控制台直接做权重路由 + 灰度发布**：新版本注册时 weight=0→ 少量请求验证 → 慢慢调大权重，不用网关改配置。

> **Eureka 不推荐但面试常问**：Eureka 经典的「**自我保护模式**（Self-Preservation Mode）」——15 分钟内 85% 的实例心跳失败，Eureka 认为网络分区导致正常实例被误剔除，于是**停止剔除所有过期实例**，把注册表锁定为当前状态，等网络恢复。这是 Eureka 纯 AP 设计的精髓——宁可不删错，也不要让客户端空注册表连不上服务。

---

## 二、服务调用：Feign 与 Ribbon / LoadBalancer

### Feign 工作流程图

```
OrderController.orderService.createOrder(...)
    │
    ▼
@FeignClient("inventory-service") 声明接口
InventoryClient.deduct(sku, num):
    │
    ▼
Feign 动态代理（Contract 解析注解 → MethodHandler）
    │
    ├─ RequestInterceptor（统一加 traceId、token、灰度 Header）
    ▼
Spring Cloud LoadBalancer（原 Ribbon）：
    ├─ 从 Nacos（DiscoveryClient）拿 inventory-service 实例列表
    ├─ 负载均衡策略：轮询 / 随机 / 加权轮询（Nacos 自带） / 一致性 Hash
    └─ 选中 instance=10.0.0.12:8080
    │
    ▼
Client（默认 Apache HttpClient / OkHttp3，不要用默认 JDK URLConnection）：
    ├─ 拼接完整 URL：http://10.0.0.12:8080/inventory/deduct?sku=1&num=2
    ├─ 加 Header（Content-Type / Accept / Trace-Id / X-Gray）
    ├─ 连接超时 connectTimeout=1s、读取超时 readTimeout=3s（★ 默认 60s 大坑！）
    └─ 发送 HTTP 请求
         │  成功 → Decoder（Spring MVC HttpMessageConverter 反序列化）
         │  失败 → Feign Exception + Retryer（默认重试 5 次！★ 不要用默认重试策略！）
         │                ↑ 这是 P0 级大坑：POST 创建订单被调用了 5 次，扣了 5 次钱
         └─ 超过重试次数 → 传播异常给调用方（或触发 fallback）
```

### 必须记住的 4 个配置点（生产事故重灾区）

| 配置 | 推荐值 | 踩坑记录 |
|:---|:---|:---|
| **connectTimeout** / **readTimeout** | 连 1s / 读 3s | 默认读超时是 `60 秒`！下游服务 hang 住，上游线程池被 60s 的请求占满，连锁反应上游雪崩 |
| **重试策略** | **关闭重试**（`Retryer.NEVER_RETRY`），**或者只针对 GET/幂等接口重试** | 默认是 `new Retryer.Default(100, SECONDS.toMillis(1), 5)` —— 最多重试 5 次，间隔最长 1s。POST 下单接口**一定不要重试**，否则多扣钱多下单。 |
| **HTTP 客户端** | Apache HttpClient 5.x / OkHttp 3.x | **默认 JDK URLConnection 没有连接池**——每次请求都新建 TCP 连接（三次握手），QPS 稍微高点就打爆。Feign 支持 Apache HttpClient 和 OkHttp，加依赖 + `feign.httpclient.enabled=true` / `feign.okhttp.enabled=true` 即可开启。 |
| **Sentinel 兜底降级** | 每个 Feign Client 要配置 FallbackFactory（不是 Fallback，Factory 能拿到 Exception 分情况降级） | 库存服务不可用时，Fall back 到"下单成功但库存稍后扣"（异步补偿链路），而不是给用户看 500。 |

### 重试的正确姿势

**原则**：**幂等才能重试**。GET 查数据是幂等，POST 下单不是幂等。

```java
@Configuration
public class FeignConfig {
    @Bean
    public Retryer retryer() {
        // 生产推荐：完全关闭 Feign 自己的重试。
        // 如果真的要重试：用 Spring Retry + 幂等键，在 Service 层做，不要让 Feign 层重试
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(
            1, TimeUnit.SECONDS,   // connectTimeout
            3, TimeUnit.SECONDS,   // readTimeout
            true                   // followRedirects
        );
    }
}
```

---

## 三、API 网关：Spring Cloud Gateway

### 为什么不选 Zuul（面试问 Zuul vs Gateway）

| 维度 | Zuul 1.x（Netflix 旧方案） | **Spring Cloud Gateway**（现行推荐） |
|:---|:---|:---|
| **底层** | Servlet 2.5（阻塞式）、同步 IO | **Spring WebFlux + Netty**、异步非阻塞、Reactor |
| 线程模型 | 每个请求分配一个 worker 线程，阻塞等下游响应 → 高并发时线程池耗尽 | **EventLoop（Netty NIO）**，少量线程就能支撑数万并发连接 |
| 性能 | 低（阻塞 IO + 同步） | **高 1.6~2.2 倍**（Spring 官方 benchmark，单实例 5 万 vs 2 万 QPS） |
| 过滤器类型 | pre / route / post（简单） | 更细的 GlobalFilter / GatewayFilter + order 顺序 + WebFilter 链 |
| 维护状态 | **停更**（Spring Cloud 2020.0.0 移除 Zuul 1，Zuul 2 Netflix 开源但 Spring 官方不集成） | **活跃维护** |
| 限流能力 | 自己实现 → 一般用 Bucket4j + Redis 写 | **内置 RequestRateLimiter 过滤器**，可选 Redis RateLimiter（Lua 脚本令牌桶） |

### Gateway 工作流程（9 步）

```
客户端请求 http://api.example.com/order/create
           │
           ▼
   Netty HttpServer 接收 → 编解码成 ServerHttpRequest
           │
           ▼
   ★ Route Predicate Factory 匹配路由：
   - Path=/order/**   → 匹配成功
   - -H Host=order.* → 也可组合（多 Predicate 都满足才算命中）
           │
           ▼
   命中 Route=order-service-route → uri=lb://order-service
           │
           ▼
   Filter Chain（GlobalFilter 全局 + 路由专属 GatewayFilter，按 @Order 排序）:
     1. RemoveCachedBodyFilter （清理请求体缓存）
     2. AdaptCachedBodyGlobalFilter
     3. ★ GatewayMetricsFilter （Micrometer 指标）
     4. ★ RequestRateLimiterGatewayFilterFactory  ← Redis 令牌桶限流在这里
     5. ★ TokenRelay / AddRequestHeader / StripPrefix
     6. ★ GlobalCacheRequestBodyFilter （缓存请求体，防止 Netty 读一次就丢）
     7. ★ NettyRoutingFilter  ★  ← 真正发起下游 HTTP 请求（lb:// 转真实 IP:PORT）
           │
           ▼  ↓ 请求去了 order-service（通过 LoadBalancer 选实例）
     8. NettyWriteResponseFilter  ← 下游响应写回给客户端
     9. ★ 后置：TraceWebFilter 记录 traceId
           │
           ▼
   客户端收到响应
```

### 典型 Gateway Filter 场景化使用表

| 业务诉求 | 推荐 Filter / 方式 | 配置示例 |
|:---|:---|:---|
| 网关统一加 Header（灰度、TraceId） | `AddRequestHeader` / `AddResponseHeader` | `spring.cloud.gateway.routes[].filters[0].AddRequestHeader=X-Gray, v2` |
| 路径重写（`/api/order/xxx` → `/order/xxx`） | `StripPrefix=1` / `RewritePath` | `- StripPrefix=1`（去掉第一级 `/api`） |
| 接口限流（防恶意刷） | `RequestRateLimiter`（默认 Redis 令牌桶） + 按 IP KeyResolver | 单 IP 每分钟最多 100 次请求 |
| 鉴权 / JWT 校验 | 自定义 `GlobalFilter implements Ordered`，比 NettyRoutingFilter 先执行，401 直接 return `ServerHttpResponse` 截断 | 检查 Authorization Bearer token，签名不对就 401，过了就把 userId 放 Header 传给下游 |
| 跨域 CORS | Gateway 层统一配置 | `spring.cloud.gateway.globalcors.cors-configurations[/].allowed-origin-patterns` |
| 黑白名单 IP | `RemoteAddrRoutePredicate` + 自定义黑名单 GlobalFilter | 按请求 remote address 拦截黑名单 IP |
| 熔断降级（下游挂了） | **整合 Sentinel / Resilience4j CircuitBreaker** | 下游 5 秒内 50% 错误率 → 熔断 30 秒，30 秒内直接 Fallback 返回友好错误 |

---

## 四、服务容错：Sentinel vs Hystrix（面试对比）

| 维度 | **Sentinel**（阿里，首推） | Hystrix（Netflix，已停更） |
|:---|:---|:---|
| 隔离策略 | **并发数隔离 + 线程池隔离**，默认并发数（更轻量，无线程切换） | **强制线程池隔离**（每个下游一个独立线程池，内存开销大） |
| 熔断降级策略 | 慢调用比例 / 异常比例 / 异常数（3 种规则组合） | 固定窗口：`rolling window` 百分比 |
| 限流 | **支持热点参数限流、系统自适应限流、集群限流、QPS/线程数双模式** | ❌ 几乎不限流，只管熔断 |
| 控制台 | 官方 Sentinel Dashboard（实时监控秒级、规则动态推送、热点参数 Top N 排行） | 只有 Hystrix Dashboard，且需要 Turbine 聚合，规则不能动态改 |
| 生态 | 和 Nacos/Seata/Dubbo/Feign 深度整合，Spring Cloud Alibaba 默认组件 | Spring Cloud Netflix 旧体系，2018.12 进入维护模式 |
| 启动性能 | 轻（无需预创建线程池） | 重（每个 HystrixCommandKey 一个线程池，启动慢 + 内存消耗大） |

### Sentinel 核心规则类型（场景化记忆）

| 规则类型 | 触发条件 | 典型场景 |
|:---|:---|:---|
| **Flow（流量控制）** | QPS 超过阈值或线程池并发超阈值 | 秒杀接口单实例限流 100 QPS；单用户每分钟最多 30 次请求 |
| **Degrade（熔断降级）** | 下游慢调用比例 / 异常比例 / 异常数超阈值 → 熔断一段时间 | 下游库存服务连续 10 个请求都 > 2s，熔断 30 秒，期间直接返回"库存稍后扣" |
| **ParamFlow（热点参数限流）** | 根据方法参数值精细限流（第 1 个参数 skuId=123 特别限流） | 热销商品 skuId=爆款1 单独限流 200 QPS，其他商品限流 50 QPS |
| **System（系统自适应保护）** | 根据机器 Load / CPU / 平均响应时间 / 入口 QPS / 并发线程数，全局保护不压垮机器 | 双 11 期间机器 CPU > 80% 时，自动降载入口 QPS |
| **Authority（黑白名单）** | 根据调用来源（origin）限制接口访问 | 管理后台 origin 才能访问敏感接口 |

### 熔断降级的 3 种策略（高频）

```java
// 策略1：慢调用比例 —— 当 10 秒内 80% 调用响应>1s，且最小请求数>=5，则熔断 30s
// 适合：下游依赖第三方接口，经常超时的场景
DegradeRule slowRule = new DegradeRule()
    .setGrade(CircuitBreakingRule.DEGRADE_GRADE_RT)
    .setCount(1000)           // 阈值：RT > 1000ms 算慢
    .setTimeWindow(30)        // 熔断时长 30s
    .setSlowRatioThreshold(0.8) // 慢调用比例 80%
    .setMinRequestAmount(5);

// 策略2：异常比例 —— 10 秒内异常比例 > 50% 且最小请求>=5，熔断 30s
// 适合：下游已经挂了，每次调用都抛错
DegradeRule errRatioRule = new DegradeRule()
    .setGrade(CircuitBreakingRule.DEGRADE_GRADE_EXCEPTION_RATIO)
    .setCount(0.5)
    .setTimeWindow(30)
    .setMinRequestAmount(5);

// 策略3：异常数 —— 1 分钟内异常数 >= 20，熔断 2 分钟
// 适合：统计周期长，希望触发一次熔断就保护久一点
DegradeRule errCountRule = new DegradeRule()
    .setGrade(CircuitBreakingRule.DEGRADE_GRADE_EXCEPTION_COUNT)
    .setCount(20)
    .setTimeWindow(120)
    .setStatIntervalMs(60000);
```

---

## 五、分布式事务 Seata

微服务最复杂的问题之一——多服务（多数据库）之间的事务一致性。

### 四种模式对比

| 模式 | 全称 | 原理 | 一致性 | 对业务侵入 | 性能 | 生产推荐 |
|:---|:---|:---|:---|:---|:---|:---|
| **AT**（自动事务） | Automatic（TXC 演进） | 一阶段：解析 SQL 生成 before/after image（undo log），本地事务提交即释放锁；二阶段：commit 异步删 undo log，rollback 用 undo log 反向补偿 | **最终一致**（一阶段提交后其他事务能看到中间值，靠全局锁 + 隔离级别保护） | **低**（只要加 `@GlobalTransactional` 注解，业务代码几乎零改） | **高**（本地事务提交释放锁） | ✅ **生产首推**，90% 场景 |
| **TCC** | Try-Confirm-Cancel | 手动拆三阶段：Try 预留资源（如冻结余额）、Confirm 正式提交（扣冻结）、Cancel 回滚预留（解冻） | **最终一致** | **极高**（每个服务三个接口手动写，幂等 + 空回滚 + 悬挂必须自己防） | 高 | 适合高性能高并发、且不想暴露 SQL 细节的场景（如支付/账户） |
| **Saga** | 长事务模式 | 拆 N 个本地事务，每个本地事务成功走下一个，失败则反向调用每个前序服务的**补偿接口** | **最终一致**（无回滚，失败靠补偿） | 高（每个服务要有正向 + 补偿两个接口） | 高 | 适合服务特别多（>5）的长链路，且中间状态可被用户接受 |
| **XA** | 强一致两阶段提交 | 一阶段 prepare 所有分支（事务管理器 TM 让每个 RM 预提交），二阶段全部 commit / 全部 rollback | **强一致**（CP） | 无 | **极低**（一阶段到二阶段期间一直持有 DB 行锁，整个事务期间锁不释放，并发直接爆炸） | ❌ 几乎不用，只在银行转账等绝对必须强一致 + 并发量小的场景 |

### AT 模式原理（必考）

```
假设有 OrderService → InventoryService → AccountService（3 个服务 3 个 DB）：

@GlobalTransactional  // 开启全局事务，TM 生成 XID 全局事务 ID
public void placeOrder(Order o) {
    orderDao.insert(o);                // DB 1：订单
    inventoryClient.deduct(o.getSku()); // DB 2：扣库存（通过 Feign，XID 会被 RootContext 透传）
    accountClient.debit(o.getUserId(), o.getAmount());  // DB 3：扣余额
}
```

每个 RM（Resource Manager，包裹 DataSource 的代理）在本地事务内部会做这些：

```
一阶段（本地事务执行时）：
  1. Seata 拦截 JDBC PreparedStatement 执行
  2. 拿到 SQL：update inventory set num = num - 2 where id = 123 and num >= 2
  3. ★ before_image：select id, num from inventory where id = 123 → 得到 (123, 10)
  4. 执行真实 SQL（num = 10 - 2 = 8）
  5. ★ after_image：select id, num from inventory where id = 123 → 得到 (123, 8)
  6. 把 before/after image 写入 undo_log 表（同库同事务）
  7. ★ 获取全局锁（branchRegister，告诉 TC 我改了 inventory:123）
  8. 本地事务 commit → 行锁释放 ✅（这就是 AT 高性能的关键：本地提交即释放锁！）
  9. 本地事务完成后 report 给 TC（Transaction Coordinator，Seata Server）
      → TC 记录分支状态成功

全部分支成功：
  TC 通知所有 RM commit（异步，批量）：
    RM 收到 → 不做别的，直接删 undo_log（因为本地已经提交了，不需要真的再次 commit）
      → 速度极快

任一失败（比如扣余额失败）：
  TC 通知所有 RM rollback：
    RM 收到 → 查 undo_log 的 before_image
      → 校验 after_image 和当前 DB 值是否一致
         一致：执行反向补偿 SQL（update num = 10 where id = 123）
         不一致：脏写告警（说明有别的事务绕过 Seata 改了这条数据）
      → 删 undo_log
        → 全局事务完成（最终一致）
```

### AT 模式前提（生产必做）

1. **每个业务库必须有 undo_log 表**（Seata 官方 SQL）：
```sql
CREATE TABLE undo_log (
    branch_id BIGINT PRIMARY KEY,
    xid VARCHAR(100),
    context VARCHAR(128),
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    KEY idx_xid (xid),
    KEY idx_branch_id (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

2. 所有要做 AT 的表必须有**主键**（before/after image 需要唯一键定位单条）
3. 不支持 SQL 中包含 `select ... for update`（Seata 自己会加全局锁，不需要再加显式行锁，加了会冲突）
4. MySQL 隔离级别建议**RC（读已提交）**，因为一阶段本地提交后其他事务能看到中间数据，RR 级别反而容易出问题

### TCC 三大坑（空回滚 / 幂等 / 悬挂）

TCC 必须自己防三类情况：

| 问题 | 现象 | 解决方案 |
|:---|:---|:---|
| **空回滚** | Try 没执行（如网络丢包没到），TC 因超时却调了 Cancel，Cancel 发现根本没有冻结记录 | Cancel 先查冻结记录，不存在 → 插入一张「空回滚占位」状态记录（避免之后迟到的 Try 真去冻结成功、但 Cancel 已经跑了） |
| **幂等** | Confirm/Cancel 被重复调用（网络重试 / TC 重试） | 每张 Try 记录有唯一 branchId，Confirm/Cancel 先查状态已 DONE 就直接 return |
| **悬挂** | Cancel 先于 Try 执行（极端网络乱序）：先解冻成功（无记录 → 空回滚），后 Try 才到，冻结成功但再也没人会取消 | 空回滚时已经在状态表写了「已回滚」标记，Try 执行前先查，若发现已回滚过 → 直接报错不冻结 |

> 口诀：**先幂等、再空回滚、后悬挂**。TCC 写不好是坑中坑，除非业务真的要极致性能，否则选 AT。

---

## 六、易错点

| 易错点 | 说明 |
|:---|:---|
| **以为 Feign 默认重试没问题** | 默认 5 次重试！下单接口一旦超时多扣 5 次钱，P0 事故。Feign 关闭重试，重试放 Service 层用 Spring Retry + 幂等键 |
| **以为 Nacos 默认 CP** | Nacos 默认 AP（临时实例心跳），AP 模式下集群脑裂两边都能注册，可能会出现客户端连上旧实例。需要精确在线状态的，切持久实例（CP） |
| **Gateway Filter 里读了请求 Body 导致下游没请求体** | Netty WebFlux 的 Body 是 `Flux(DataBuffer)` 流式数据，读一次就丢。要先用 `ServerWebExchangeUtils.cacheRequestBody` 缓存。Gateway 自带 `CacheRequestBodyFilter`，要在自定义鉴权 Filter 之前先加载 |
| **以为 Sentinel 熔断自动恢复** | 熔断窗口时间到了是「半开」，只放 1 个探测请求过去，成功则全恢复，失败重新熔断。不要以为过了时间就自动好——半开失败会再次熔断 |
| **Seata AT 模式支持所有 SQL** | 只支持：UPDATE（有主键 WHERE）、DELETE（有主键 WHERE）、INSERT（单条，或批量但 before_image 可查）。不支持复杂子查询、多表 JOIN UPDATE、INSERT ... SELECT。遇到复杂 SQL 走 TCC / Saga |
| **Nacos 配置 `refresh-enabled=true` 时 Bean 会被重新创建** | `@RefreshScope` 标注的 Bean 在配置变化时会被销毁重新实例化、重新 @Value 绑定。如果 Bean 里有 `@PostConstruct` 启动线程池，refresh 时线程池会被重复创建导致泄漏。正确姿势：线程池/连接池类的 Bean 不要标 `@RefreshScope`，让它启动一次就不变 |

---

## 七、一句话总结

Spring Cloud 体系选型首推**「Nacos（注册+配置，AP+CP 可切换） + OpenFeign（自定义超时/关闭重试+Apache HttpClient 连接池） + Spring Cloud Gateway（Netty WebFlux 非阻塞，内置限流+路由谓词） + Sentinel（并发隔离+熔断+热点限流，已替代停更的 Hystrix） + Seata AT（全局事务，before/after image + undo_log 反向补偿，90% 场景首选，SQL 复杂或极致性能再切 TCC/Saga）」**；注册中心永远先回答「CAP 权衡：P 必选，服务注册宁可旧可用不要新不可用 → 选 AP（Nacos 默认）」；每个组件都要讲一个 STAR 结构的真实踩坑（如 Feign 默认 5 次重试、Nacos RefreshScope 重复创建线程池、Gateway Filter 读 body 丢失、TCC 空回滚悬挂）。

---

## 八、相关笔记

| 主题 | 笔记 |
|:---|:---|
| 本地事务 + 传播行为（Seata AT 一阶段的本地事务仍要遵循） | [事务.md](事务.md) |
| MySQL redo/undo/binlog 两阶段提交（Seata XA 模式就是扩展 2PC 协议） | [MySQL/存储引擎与架构.md](../mysql/存储引擎与架构.md) |
| Nacos 启动时 EnvironmentPostProcessor 拉配置（Spring Boot 自动配置扩展点） | [自动配置与启动流程.md](自动配置与启动流程.md) |
