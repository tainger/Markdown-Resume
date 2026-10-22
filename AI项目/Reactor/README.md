# Reactor 3 从入门到精通

> 阅读主线：数据与信号 → 创建与订阅 → 操作符组合 → 线程与背压 → 可靠性 → Context 与生命周期 → 测试 → AgentScope 实战。

## 资料与版本

- 主资料：[Reactor 官方参考文档](https://projectreactor.io/docs/core/release/reference/aboutDoc.html)，2026-10-22 阅读时页面标示 **3.8.7**。`release` 是浮动入口，后续阅读可能显示其他版本。
- 本系列是中文讲解与原创练习，不是官方文档逐字翻译；每章给出官方对应章节。
- **可运行实验固定 Java 17 + Reactor Core/Test 3.8.2**，与本库 AgentScope `v1.0.12` 导入的 Reactor BOM `2025.0.2` 一致；不把 release 文档版本当成本地依赖版本。
- 原项目材料：[[AI项目/AgentScope1.0/2.AgentScope Java特性：流式输出、结构化输出、超时与重试、执行控制]]、[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]、[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/gogo-agent/27.基于Reactor Context 实现多智能体之间的会话上下文传递]]。
- 除第 13 章实验工程外，各章代码是方法体片段或说明性示例；`loadUser`、`save` 等业务方法需由应用实现，不宣称可整段独立运行。

## 目录与学习目标

| 章节 | 学完能做什么 |
|:---|:---|
| [[AI项目/Reactor/01.响应式基础与环境准备]] | 分清响应式、异步、非阻塞、并发与并行 |
| [[AI项目/Reactor/02.Mono与Flux的创建和订阅]] | 知道何时执行、谁来订阅、怎样取结果 |
| [[AI项目/Reactor/03.转换过滤与聚合操作符]] | 正确使用 map、filter、then、collectList |
| [[AI项目/Reactor/04.异步组合顺序与并发]] | 选择 flatMap、concatMap、zip、when，控制并发 |
| [[AI项目/Reactor/05.线程模型与Scheduler]] | 解释 subscribeOn/publishOn，接入阻塞调用 |
| [[AI项目/Reactor/06.背压与流量控制]] | 理解 request、prefetch、队列和过载策略 |
| [[AI项目/Reactor/07.异常处理超时与重试]] | 设计超时层次、恢复范围和幂等边界 |
| [[AI项目/Reactor/08.冷热流共享与Sinks]] | 识别重复执行、缓存范围和手动推送风险 |
| [[AI项目/Reactor/09.Context与资源生命周期]] | 传递请求身份，处理取消与资源释放 |
| [[AI项目/Reactor/10.StepVerifier测试与调试]] | 验证时序、背压、错误、Context 与订阅分支 |
| [[AI项目/Reactor/11.AgentScope与WebFlux实战]] | 读懂 reasoning 链，把 Agent 接到响应式接口 |
| [[AI项目/Reactor/12.批处理并行与生产排错]] | 使用 buffer/window/groupBy，定位积压与重复请求 |
| [[AI项目/Reactor/13.可执行实验与面试自测]] | 运行无外部服务的练习，检验关键语义 |

## 三种阅读路线

1. **先能写业务**：01 → 02 → 03 → 04 → 05 → 07 → 13。
2. **看懂 AgentScope**：02 → 03 → 04 → 09 → 11，再回到原项目源码。
3. **准备工程面试**：顺序读完，重点口述 06、07、09、12 的边界，再完成 13 的自测。

## 先建立这六个判断

| 看见什么 | 先问什么 |
|:---|:---|
| `Mono<T>` / `Flux<T>` | 这是结果对象，还是产生结果的流程？最多几个值？ |
| `map` / `flatMap` | 回调返回普通值，还是另一个 Publisher？ |
| `subscribe()` / `block()` | 谁拥有这次执行？是否又订阅了一次？ |
| `subscribeOn` / `publishOn` | 源头在哪执行？后续处理在哪执行？ |
| `timeout` / `retryWhen` | 限制哪一段？重新订阅会重复什么副作用？ |
| `contextWrite` / `deferContextual` | 写入位于读取的哪个方向？有没有独立订阅边界？ |

## 学完后的验收

- 能解释同一个冷 Mono 两次订阅为何可能触发两次请求。
- 能把“查用户 → 查订单 → 返回详情”组合为一条链，没有嵌套订阅。
- 能说明“按序输出”与“按序执行副作用”的差别。
- 能让阻塞 SDK 离开非阻塞事件线程，并控制并发和超时。
- 能为取消、空结果、重试、慢消费者写出行为测试。
- 能用类型与信号解释 ReActAgent 的 `Flux<ChatResponse> → Mono<Msg>` 转换。

## 实验入口

完整工程在 `examples/`，**28 项测试已全部通过**；运行方式及验证记录见 [[AI项目/Reactor/13.可执行实验与面试自测]]。实验不访问模型、数据库或网络服务；它验证 Reactor 语义，不代表 AgentScope 端到端或生产压测已经通过。

复习总入口：[[wiki/synthesis/Reactor3学习与源码主线]]。
