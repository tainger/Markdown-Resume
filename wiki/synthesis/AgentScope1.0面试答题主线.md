---
type: synthesis
status: growing
updated: 2026-10-22
---

# AgentScope 1.0 面试答题主线

> 答题骨架：框架定位 → call 主链 → 工具/记忆 → 挂起恢复 → 工程边界。题库入口：[[AI项目/AgentScope1.0/interview/README]]。

## 版本先行

原系列以 1.0.12 为例，本地源码已按用户要求切到 `v1.0.12`（`b9080f27`，detached HEAD），题库均按该标签核对；初始 main 的 2.0.3-SNAPSHOT 不混入答案。原文入口：[[AI项目/AgentScope1.0/1.AgentScope初探]]；源码版本及 14 组口径核对见 [[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]]。

## 五条深挖线

| 主线 | 按什么顺序回答 | 题库 |
|:---|:---|:---|
| ReAct 执行 | 订阅 call → PreCall → doCall → reasoning/acting → PostCall → 释放 | [[AI项目/AgentScope1.0/interview/01.框架架构与ReAct执行原理]] |
| 事件与工具 | 增量/累积 → Hook 顺序 → Schema/上下文 → 并行 → 超时/重试 | [[AI项目/AgentScope1.0/interview/02.流式输出与Hook机制]]、[[AI项目/AgentScope1.0/interview/03.工具调用与执行治理]] |
| 记忆与压缩 | Memory/Session → 保存范围 → 长期记忆 → 六级压缩 → 原文重载 | [[AI项目/AgentScope1.0/interview/04.会话持久化与长期记忆]]、[[AI项目/AgentScope1.0/interview/05.自动上下文压缩原理]] |
| 扩展与协作 | MCP → Knowledge → RAG 模式 → Skill → 计划/子 Agent/Pipeline | [[AI项目/AgentScope1.0/interview/06.MCP接入与RAG机制]]、[[AI项目/AgentScope1.0/interview/07.Skill计划与多Agent协作]] |
| HITL 与可靠性 | 动作前/后暂停 → ToolSuspend → ID 回填 → 中断/关机 → 故障窗口 | [[AI项目/AgentScope1.0/interview/08.HITL中断恢复与生产追问]] |

来源：[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/AgentScope1.0/9.AgentScope Java特性：工具集成]]、[[AI项目/AgentScope1.0/3.AgentScope Java特性：多轮会话&会话持久化]]、[[AI项目/AgentScope1.0/5.AgentScope Java特性：智能上下文压缩]]、[[AI项目/AgentScope1.0/13.AgentScope Java进阶：Human-in-the-Loop]]。

## 答题前必须修正的口径

- PendingToolRecoveryHook 在此标签 Builder 中默认关闭；显式启用或手动注册才接入。原文与实现差异：[[AI项目/AgentScope1.0/13.AgentScope Java进阶：Human-in-the-Loop]]、[[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]]。
- Toolkit 复制不等于所有用户工具对象深拷贝；Hook 排序也不等于每类回调都串行完成。来源：[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]；源码追问见 Q08/Q14。
- 静态长期记忆与 Generic RAG 实际在 PreCall 触发；长期记忆召回以 USER 消息追加。来源：[[AI项目/AgentScope1.0/4.AgentScope Java特性：长期记忆]]、[[AI项目/AgentScope1.0/11.AgentScope Java特性：RAG]]；更新口径与定位见 Q31/Q45 和核对页。
- 工具异常可能先变成正常错误结果，外层 retryWhen 不一定触发；默认卸载内容仍在实例 HashMap，需要 Session 才能跨重启保存。来源：[[AI项目/AgentScope1.0/2.AgentScope Java特性：流式输出、结构化输出、超时与重试、执行控制]]、[[AI项目/AgentScope1.0/5.AgentScope Java特性：智能上下文压缩]]；实现追问见 Q22/Q36。

上述差异均保留原笔记，集中在 [[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]] 列出源码证据，不将注释或示例升级为未经核实的保证。

## 与项目实践连起来

先说明框架提供的工具协议、生命周期和状态接口，再讲应用自己如何管理身份、审批、方案、跨节点确认及外部幂等。对照 [[wiki/synthesis/gogo-agent面试答题主线]] 与 [[AI项目/gogo-agent/interview/README]]；两个目录的源码快照需各自核对。

## 查询补充：ReActAgent 为什么看不到完整的上下文管理？

- **职责分工**：`ReActAgent` 负责执行循环、消息入库和输入组装。`doCall` 将新输入加入 Memory，`prepareMessages` 拼接系统提示与 `memory.getMessages()`，`reasoning` 触发 `PreReasoningEvent` 后将事件中的消息及工具 Schema 交给模型。自动压缩策略通过扩展接入，而非固化在循环中。来源：[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]；本地 v1.0.12 `ReActAgent.java:264、460、821`。
- **默认行为**：Builder 使用 `InMemoryMemory`，其 `getMessages()` 返回全部非空历史消息，不自带窗口截断或摘要。`AutoContextMemory.getMessages()` 也只读取工作视图，不自动触发压缩。来源：[[AI项目/AgentScope1.0/3.AgentScope Java特性：多轮会话&会话持久化]]、[[AI项目/AgentScope1.0/5.AgentScope Java特性：智能上下文压缩]]；本地 `ReActAgent.java:1106`、`InMemoryMemory.java:99`、`AutoContextMemory.java:157`。
- **1.0.12 接法**：引入 `agentscope-extensions-autocontext-memory`，同时配置 `.memory(new AutoContextMemory(config, chatModel))` 与 `.hook(new AutoContextHook())`。Hook 在 PreCall 注册重载工具，在每次常规推理的 PreReasoning 调用 `compressIfNeeded()`，随后重建并设置实际输入；单换 Memory 不会自动压缩。来源：[[AI项目/AgentScope1.0/6.实战：实现AgentScope的三层记忆体系]]、[[AI项目/AgentScope1.0/interview/05.自动上下文压缩原理]]。
- **持久化与召回**：Session 负责跨重启保存/恢复 Memory 状态，长期记忆负责跨会话事实召回；二者不能代替当前请求的 token 预算。预算需额外考虑系统提示、工具 Schema、检索内容及输出空间，单次压缩成功不保证完整请求已经降到安全范围。来源：[[AI项目/AgentScope1.0/3.AgentScope Java特性：多轮会话&会话持久化]]、[[AI项目/AgentScope1.0/4.AgentScope Java特性：长期记忆]]、[[AI项目/AgentScope1.0/interview/05.自动上下文压缩原理]]。
- **区分业务上下文**：用户身份、数据库连接等工具侧参数可用 `ToolExecutionContext` 注入；这与模型消费的对话上下文不是同一概念。来源：[[AI项目/AgentScope1.0/9.AgentScope Java特性：工具集成]]。

## 查询补充：如何读 Mono 与 Flux 执行链？

- 在本系列 API 中，`agent.call(...)` 返回 `Mono<Msg>` 表达最终回复，`agent.stream(...)` 返回 `Flux<Event>` 表达执行过程中的多条事件。普通同步示例用 `.block()` 等待最终结果；流式 Web 示例返回 Flux 交给框架消费。来源：[[AI项目/AgentScope1.0/2.AgentScope Java特性：流式输出、结构化输出、超时与重试、执行控制]]。
- 阅读 `reasoning` 时按数据类型追踪：推理前事件 → `flatMapMany` 接模型 chunk 流 → `doOnNext` 更新累积器 → `then(Mono.defer(...))` 在流完成后构造最终消息 → `flatMap` 接 PostReasoning Hook。该实现使用延后构造来读取累积后的最终状态。来源：[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]；本地 v1.0.12 `ReActAgent.java:460`。
- Hook 返回 `Mono<T>`，调用方用 `flatMap(hook::onEvent)` 将处理串入主链；业务代码应将需要等待的异步步骤接回返回链，而非照抄内部通知场景的独立 `subscribe()`。来源：[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]、[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]。

## 证据边界

本次为静态源码与文档核对，未运行原测试或真实模型/MCP 服务。⚠️ raw 缺失，待补充：应用级压测、真实 token/账单、故障注入、权限与个人实践报告。框架支持、应用接入、实际验证三者分开表述。

关联：[[wiki/concepts/Agent]]、[[wiki/concepts/ContextEngineering]]、[[wiki/concepts/RAG]]、[[wiki/entities/MCP]]。
