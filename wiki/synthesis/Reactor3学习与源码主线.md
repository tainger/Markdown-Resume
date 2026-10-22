---
type: synthesis
status: growing
updated: 2026-10-22
---

# Reactor 3 学习与源码主线

> 答题骨架：信号模型 → 订阅与执行 → 数据组合 → 线程/需求 → 错误/取消 → Context → 测试与项目边界。课程总入口：[[AI项目/Reactor/README]]。

## 版本与证据

用户指定官方 release 文档，阅读时标示 3.8.7；实验固定 Java 17 + Reactor Core/Test 3.8.2，对齐本库 AgentScope v1.0.12 的 Reactor BOM 2025.0.2。浮动文档版本与固定实验版本分开记录。来源：[[AI项目/Reactor/01.响应式基础与环境准备]]、[[AI项目/Reactor/13.可执行实验与面试自测]]。

## 七段主线

| 主线 | 先解释什么 | 来源 |
|:---|:---|:---|
| 基础 | Publisher/Subscriber/Subscription；Mono 0～1、Flux 0～N；空值与终止 | [[AI项目/Reactor/01.响应式基础与环境准备]] |
| 订阅 | 组装、执行、接收；just 提前求值；冷源重复订阅 | [[AI项目/Reactor/02.Mono与Flux的创建和订阅]] |
| 组合 | map/flatMap、Void/then、zip/when、顺序输出与顺序副作用 | [[AI项目/Reactor/03.转换过滤与聚合操作符]]、[[AI项目/Reactor/04.异步组合顺序与并发]] |
| 线程/需求 | subscribeOn/publishOn、阻塞适配、request/prefetch/队列 | [[AI项目/Reactor/05.线程模型与Scheduler]]、[[AI项目/Reactor/06.背压与流量控制]] |
| 可靠性 | 恢复范围、重试重新订阅、超时层次、共享缓存 | [[AI项目/Reactor/07.异常处理超时与重试]]、[[AI项目/Reactor/08.冷热流共享与Sinks]] |
| 生命周期 | Context 方向、独立订阅边界、取消、同步/异步资源释放 | [[AI项目/Reactor/09.Context与资源生命周期]] |
| 验证 | StepVerifier、虚拟时间、Probe、生产指标与排错 | [[AI项目/Reactor/10.StepVerifier测试与调试]]、[[AI项目/Reactor/12.批处理并行与生产排错]] |

## AgentScope 源码答题骨架

1. `call` 返回最终 `Mono<Msg>`，Agent 事件 `stream` 返回 `Flux<Event>`，模型 `stream` 返回 `Flux<ChatResponse>`。
2. `Mono.using` 在订阅时获取执行资格；重复订阅可重复执行，正在运行的同一实例也可能拒绝并发调用。
3. reasoning：前置事件 Mono → 模型片段 Flux → 累积器 → defer 构造最终 Msg → 后置事件及后续决策。
4. 自定义需要等待的异步步骤用 flatMap/then 接回主链；独立 subscribe 需要自己管理错误、Context 与取消。
5. HTTP 客户端取消能否真正停止 Agent/模型，必须穿透外部回调桥验证；Flux 类型不保证端到端背压。

来源与本地源码定位：[[AI项目/Reactor/11.AgentScope与WebFlux实战]]、[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]。

## 易错口径与显式更新

- “响应式等于自动多线程/非阻塞”不成立；来源实现与 Scheduler 决定执行。见 [[AI项目/Reactor/05.线程模型与Scheduler]]。
- “背压就是每秒限流”不成立；request 控制需求，队列和来源适配仍需设计。见 [[AI项目/Reactor/06.背压与流量控制]]。
- **更新**：原 Context 笔记里的“ThreadLocal 完全不可用”“独立订阅 Context 必然为空”不能作为普遍规则；已配置的传播桥、显式 Context 和框架 Hook 可改变行为。原笔记保留，修正及官方依据集中在 [[AI项目/Reactor/09.Context与资源生命周期]]，原来源为 [[AI项目/gogo-agent/27.基于Reactor Context 实现多智能体之间的会话上下文传递]]。
- `flatMapSequential` 保持输出顺序，不保证外部写入顺序；`when` 不保证事务原子性。见 [[AI项目/Reactor/04.异步组合顺序与并发]]。
- 实验结果只覆盖固定版本的本地语义；⚠️ raw 缺失，待补充：真实模型/SDK 取消、网络断连、生产压测、延迟与费用数据。见 [[AI项目/Reactor/13.可执行实验与面试自测]]。

关联：[[wiki/synthesis/AgentScope1.0面试答题主线]]、[[wiki/synthesis/gogo-agent面试答题主线]]、[[wiki/concepts/ContextEngineering]]。
