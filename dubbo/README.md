# Dubbo 面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——从架构执行流程到通信线程模型，成体系地理解 Dubbo。

按「**架构与流程 → SPI 扩展 → 注册发现 → 集群容错 → 通信线程模型**」的主线组织，每篇均含对比表、图解、易错点、一句话总结。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🏗️ 核心架构与执行流程 | [核心架构与执行流程.md](核心架构与执行流程.md) | 分层架构、**Invoker 核心模型**、服务暴露/引用、一次 RPC 调用全链路、**Dubbo3 应用级服务发现**、vs Spring Cloud/gRPC |
| 2 | 🔌 SPI机制与自适应扩展 | [SPI机制与自适应扩展.md](SPI机制与自适应扩展.md) | Java SPI vs Dubbo SPI、**`@SPI/@Adaptive/@Activate`**、Adaptive 生成类原理、IOC 与 AOP（Wrapper+Filter 链）、自定义扩展 |
| 3 | 🧭 服务注册发现与配置 | [服务注册发现与配置.md](服务注册发现与配置.md) | **URL 统一模型**、ZooKeeper vs Nacos（CP/AP）、订阅推送、**注册中心挂了还能调吗**、配置优先级、check 启动检查 |
| 4 | 🛡️ 集群容错与负载均衡 | [集群容错与负载均衡.md](集群容错与负载均衡.md) | 六大容错策略、四种负载均衡（含**一致性哈希虚拟节点**）、**超时重试与幂等**、Mock 降级、优雅上下线 |
| 5 | 📡 通信协议与线程模型 | [通信协议与线程模型.md](通信协议与线程模型.md) | **单一长连接 + NIO 的原因**、16 字节协议头、**请求 id 异步配对**、Dispatcher 派发、线程池打满排查、hessian2 vs protobuf |

## P7 必背清单（速查）

- **一次 RPC 调用链**：代理接住调用 → Cluster（Directory→Router→LoadBalance）选机器 → 协议序列化 → Netty 发送 → 服务端 IO 线程解码 → Dispatcher 派发业务线程池 → 反射调用 → id 配对异步回包
- **Invoker 是核心模型**：可执行抽象，掩盖本地/远程差异；ClusterInvoker 把整个集群伪装成一个 Invoker
- **分层架构**：Service→Proxy→Registry→Cluster→Protocol→Remoting（Exchange/Transport/Serialize），每层都是 SPI 扩展点
- **Dubbo SPI vs Java SPI**：key-value 配置 + 懒加载 + IOC（setter 注入）+ AOP（Wrapper/Filter 链）；Java SPI 一次性全量实例化
- **`@Adaptive` 原理**：启动生成 Adaptive 类，「从 URL 取参数值当 key → getExtension(key) → 委托执行」三步；自适应方法必须有 URL 参数
- **一切皆 URL**：配置/协议/注册/路由都是 URL 的增删改；`dubbo://ip:port/接口?group=&version=&timeout=`
- **注册中心挂了还能调**：Consumer 本地 `RegistryDirectory` 缓存列表照常调用，只是失去上下线感知（AP 取向）
- **ZK vs Nacos**：ZK=CP（ZAB、临时节点+Watch）；Nacos=AP（心跳+主动探测、推送更友好），主流首选
- **Dubbo3 应用级服务发现**：注册中心只存应用实例，接口→应用映射进元数据中心，解决接口级注册数据膨胀（云原生）
- **六大容错**：Failover（默认，retries=2，读）、Failfast（写防重复）、Failsafe（旁路）、Failback（定时重发）、Forking（并行取最快）、Broadcast（广播）
- **四种负载均衡**：Random（默认加权）、RoundRobin（平滑加权）、LeastActive（活跃数最少，慢机少接活）、ConsistentHash（虚拟节点 160、默认哈希第一参数）
- **重试必须配幂等**：timeout 1s + retries 2 = 最多 3 次请求；写接口 retries=0 或唯一键/状态机兜底
- **timeout 配置优先级**：方法级 > 接口级 > 全局级；**Provider > Consumer**（提供方最懂自己的耗时）
- **dubbo 协议 = 单一长连接 + NIO**：每 Provider 一条长连接，8 字节请求 id 异步配对实现「一条连接高并发」；16 字节定长头 + body 长度解决粘包
- **线程模型**：Netty IO 线程只编解码；Dispatcher 默认 `all` 派发到业务线程池（fixed 200 + SynchronousQueue 快速失败）；打满报 `EXHAUSTED` 查下游慢调用
- **序列化**：默认 hessian2；JDK 序列化慢/大/不安全被禁；Dubbo3 triple = HTTP/2 + protobuf，兼容 gRPC 支持流式
- **Mock 降级**：`mock=fail:return null` / `force:return`；配合动态配置中心实现不重启降级

## 学习/复习建议

1. 先按 1→5 顺序建立体系：**一次调用的全链路是纲**，SPI/注册/容错/通信都是这条链上的展开。
2. 必须能白板画的三张图：分层架构图、一次 RPC 调用时序图、16 字节协议头。
3. 「注册中心挂了还能调用吗」「dubbo 协议为什么单长连接」「@Adaptive 怎么选实现」是三大追问题，要能答到源码级别。
4. 每篇「一句话总结」当口述提纲，能复述即过关；「易错点」都是真实生产事故点（重试重复写、线程池打满、大报文）。
5. 结合项目讲：自定义 Filter 透传 traceId、Nacos 迁移双注册、按 LeastActive 解决机器性能不均，都是 P7 加分案例。

## 相关笔记

- 分布式基础（CAP、一致性、分布式锁）→ [../分布式/README.md](../分布式/README.md)
- 系统设计中的 RPC 与服务治理 → [../系统设计/README.md](../系统设计/README.md)
- 消息中间件对比（RocketMQ 可靠性与高可用）→ [../rocketMq/可靠性与高可用.md](../rocketMq/可靠性与高可用.md)
- 网络与通信底层（TCP、HTTP/2）→ [../计算机网络/TCP.md](../计算机网络/TCP.md)、[../计算机网络/HTTP.md](../计算机网络/HTTP.md)
- Spring 集成与 IOC 容器 → [../Spring/IOC与Bean生命周期.md](../Spring/IOC与Bean生命周期.md)
