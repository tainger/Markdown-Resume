# AgentScope Java 2.0 设计思想源码解读

> 基于 `repo/agentscope-java/`（v2.0.x）源码的通读笔记，目标是源码级理解设计思想。
> 环境：JDK 17+ · Maven `io.agentscope:agentscope-harness:2.0.1`

## 一、仓库分层：三明治架构

```
agentscope-core        → 无依赖核心：Agent 抽象、ReAct 循环、事件、状态、Toolkit
agentscope-harness     → 工程底座：Workspace/沙箱/技能/记忆/子agent/团队（依赖 core）
agentscope-extensions  → 插件：model-*（openai/dashscope/...）、storage-*（mysql/redis/...）、protocol（A2A）
agentscope-service     → 控制面：Agent 注册、观测、编排（K8s operator + Gateway）
```

设计决策：**裸 `ReActAgent` 只需 `agentscope-core`**；模型提供商按需引入扩展模块。

## 二、核心抽象：极小的接口 + 组合

### 1. Agent 是"三合一"组合接口

`agentscope-core/src/main/java/io/agentscope/core/agent/Agent.java`

由三个子接口组合：
- `CallableAgent` — 处理消息返回 `Mono<Msg>`
- `StreamableAgent` — 流式获取事件
- `ObservableAgent` — 只接收消息不回复（多 agent 协作的"观察"模式）

核心约定（类注释原话）：
- **Memory 不属于 Agent 核心接口**，由具体实现负责（如 ReActAgent）
- 一次 `call()` 恰好产生一个终态 Msg，流式变体最终也归约为一个 Msg（`Mono<Msg>` 强制）

### 2. AgentBase：生命周期模板方法

`.../agent/AgentBase.java`

```
call() → callInternal() → runLifecycle() → runLifecycleBody()
  ├── GracefulShutdownManager.registerRequest   (优雅停机跟踪，按 requestId 而非 agentId)
  ├── callSerializationKey(rc)                  (同 session 串行，不同 session 并行)
  ├── beforeAgentExecution → per-call scope     (存入 Reactor Context，并发安全)
  ├── notifyPreCall (hooks) → doCall → notifyPostCall
  └── afterAgentExecution (Mono.using 保证清理)
```

三个关键设计：
- **Per-call 状态走 Reactor Context 而非实例字段**（`CALL_SCOPE_KEY`/`RUNTIME_CONTEXT_KEY`）——"单实例并发安全"的根本
- **`serializeOnKey` 信号量门**：同 `(userId, sessionId)` 的调用 FIFO 串行；用 `Sinks.Empty` 做释放信号，任何终态（complete/error/cancel）都放行
- 每次 call 注册独立 `requestId` 到 GracefulShutdownManager，停机时精确定位在途调用

### 3. 状态与执行分离：无状态引擎 + 可持久化 State

`.../state/AgentState.java` — 纯数据类（Jackson 序列化）：会话缓冲 `context`、滚动摘要、`curIter`、权限/工具/任务/Plan 子上下文。

- `interruptControl` 标记 `transient`：**每会话中断信号不序列化**，无状态引擎按 `(userId, sessionId)` 槽位挂载，定向中断精确命中一个会话
- `.../state/AgentStateStore.java` — 持久化 SPI：内存/JSON 文件/MySQL/Redis/PG；`saveIfVersion` 乐观锁 CAS（`VersionedState`），支撑零停机滚动发布
- `RuntimeContext.agentState` 是"调用作用域状态"，中间件/工具在并发下用它而非 `agent.getAgentState()`（`resolveAgentState` 推荐）

## 三、ReActAgent：唯一的旗舰循环

`agentscope-core/src/main/java/io/agentscope/core/ReActAgent.java`（5252 行，唯一核心实现，其余能力全部"叠加"）

```
coreAgent() → executeIteration(0) → reasoning(iter)
  ├── 检查 maxIters → summarizing() 收尾
  ├── onReasoning middleware 链 → reasoningStream
  │     └── onModelCall middleware 链 → modelCallStream (Model.stream)
  │           └── 逐 chunk: processChunk → 文本/思考/工具调用分块事件
  └── runPostReasoningPipeline: HITL stop / gotoReasoning / 完成? → acting(iter)
        └── onActing middleware 链 → 执行工具 → 回到 reasoning(iter+1)
```

### HITL 是一等公民

- 工具调用进入 `ASKING` 状态后挂起（pending tool calls），状态持久化到会话中最后一条 assistant 消息上
- 下一次 `call()` 经 `doCallInner` 分支路由：`validateAndAcceptConfirmResults` 校验 `Msg.METADATA_CONFIRM_RESULTS` 中的 `ConfirmResult`，按 `RequireUserConfirmEvent` 的 replyId 精确关联恢复
- `resumeAgent()` 直接跳进 acting 阶段，跳过推理

### call() 与 streamEvents() 共享执行核心

`buildAgentStream`（L1011）：两者共用 `runLifecycle`，`onAgent` 链只套一次；`call()` 只是 `takeLast(1)` 取终态。事件流由 `AgentStartEvent`/`AgentEndEvent` 书夹；`AgentEventEmitter` 经 Reactor Context 注入，工具（如 `agent_spawn`）可把子 agent 事件注入父流（`FORWARDING_CONTEXT_KEY` 转发 + `source` 路径标记）。

## 四、Middleware：洋葱模型替代继承

`.../middleware/MiddlewareBase.java` — 5 个拦截点：

| 钩子 | 模式 | 粒度 |
|:---|:---|:---|
| `onAgent` | 洋葱（wrap） | 整个调用 |
| `onReasoning` | 洋葱 | 推理阶段 |
| `onActing` | 洋葱 | 工具执行 |
| `onModelCall` | 洋葱 | 裸模型 API 调用（最细） |
| `onSystemPrompt` | 流水线（顺序变换） | 系统提示词字符串 |

`.../middleware/MiddlewareChain.java` 反向构建链（最后一个包核心，第一个最外层），`order()` 越大越靠外；默认实现直接委托 `next`。

**框架扩展哲学**：Harness 的 15+ 个中间件（Compaction、MemoryFlush、Transcript、SandboxLifecycle、Subagents、Teams、Skill...）全部以 middleware "插入" ReActAgent，核心循环零改动。

## 五、事件系统：可观测性即架构

`.../event/AgentEvent.java` + `AgentEventType.java`：

- **28 种类型化事件**（AGENT_START/END、TEXT/THINKING/DATA_BLOCK_*、TOOL_CALL_*、TOOL_RESULT_*、HITL 四件套、REQUEST_STOP、CUSTOM...），Jackson 多态序列化，天然跨进程传输（A2A 复用）
- `AgentEventType` 保留旧名 `@JsonAlias`（如 `RUN_STARTED`→`AGENT_START`）——向后兼容是显式设计
- 前端实时渲染（AGUI）、子 agent 事件转发、审计回放全部基于同一事件流

## 六、消息模型：sealed 密封层级

`.../message/ContentBlock.java` — Java 17 `sealed` 类：文本/思考/图片/音频/视频/工具调用/工具结果/提示块/二进制数据，**编译期穷尽匹配** + Jackson 多态；`Msg` 按 role 严格校验，多模态统一收敛。

## 七、模型抽象：SPI 插件机制

`.../model/Model.java` — 核心接口只有 `stream(...)`，能力用 default 方法逐步暴露（结构化输出支持度、上下文窗口）。

`.../model/spi/ModelProvider.java` + `META-INF/services`：每个 model 扩展模块声明 provider（openai 模块注册了 Kimi/MiniMax/GLM/DeepSeek 四个兼容 provider）；`ModelRegistry` 用 `ServiceLoader` 加载，支持 `"dashscope:qwen-plus"` 字符串解析 + API key 环境变量自动读取，**用户注册的 factory 优先于 SPI**。

## 八、权限系统：确定性决策链

`.../permission/PermissionEngine.java` — 求值顺序（Javadoc 写死）：

```
工具级 deny > ask > 工具自身 checkPermissions（bypass 免疫，如 EXPLORE 只读强制）
> allow > BYPASS 兜底 > 默认 ASK（DONT_ASK 模式转 DENY）
```

三态决策（ALLOW/ASK/DENY）驱动 HITL 暂停点；`EXPLORE`/`ACCEPT_EDITS` 模式对只读工具直接放行——安全模型是运行时的一部分。

## 九、Toolkit：注解驱动的工具门面

`.../tool/Toolkit.java` — 门面委托 5 个 manager（ToolRegistry / ToolGroupManager / ToolSchemaProvider / McpClientManager / MetaToolFactory）+ 4 个组件（schema 生成/方法调用/结果转换/执行器）。支持工具分组动态激活、MCP 客户端、`@Tool` 注解注册、`stateInjected=true` 运行时状态注入。

## 十、Harness：能力堆叠层

`agentscope-harness/.../harness/agent/HarnessAgent.java` — 继承 `ReActAgent`，builder 装配 15+ 中间件和工具：

- **Workspace**：可插拔文件系统（本地/共享存储/沙箱路由）
- **Sandbox**：本地子进程/Docker/K8s/E2B 云沙箱，快照恢复，租约管理
- **技能系统**：自进化技能仓库（SkillCurator + 金丝雀发布 + 安全扫描）
- **分层记忆**：会话缓冲 + `MEMORY.md` + 磁盘事实流水账 + 自动压缩
- **子 agent**：Markdown 声明规格，`agent_spawn`/`agent_send`，跨副本路由
- **Teams/Channel**：团队编排 + 钉钉/飞书/企微接入

## 十一、设计哲学总结

1. **反应式优先**：Reactor `Mono`/`Flux` 贯穿全栈，并发安全靠订阅上下文而非加锁
2. **状态与执行分离**：无状态 agent + 可序列化 `AgentState` + 可插拔 Store → 天然水平扩展、会话可恢复
3. **洋葱扩展**：middleware 五段拦截点取代继承树，能力按需叠加，核心循环永不被改写
4. **事件驱动可观测**：28 种类型化事件是前端渲染、HITL、子 agent 转发、跨进程传输的统一载体
5. **小接口 + 插件化**：核心零依赖，模型/存储/协议全部 SPI 模块化
6. **HITL 一等公民**：暂停-恢复状态持久化到会话，不是外挂回调

## 十二、源码阅读路径

按此顺序读，一条线打通：

1. `agent/Agent.java` → 接口组合（三合一）
2. `agent/AgentBase.java` → 生命周期模板（runLifecycle 串起全部基础设施）
3. `ReActAgent.java` 的 `coreAgent` → `reasoning` → `acting` → `summarizing`
4. `middleware/MiddlewareBase.java` + `MiddlewareChain.java` → 洋葱扩展机制
5. `event/AgentEvent.java` + `AgentEventType.java` → 事件流
6. `state/AgentState.java` + `AgentStateStore.java` → 状态与持久化
7. `agent/RuntimeContext.java` → per-call 上下文
8. `harness/agent/HarnessAgent.java` 的 builder → 看中间件如何装配成完整能力

## 子笔记计划

- [ ] ReAct 循环逐行解析（reasoning/acting/summarizing 状态机）
- [ ] Middleware 五阶段实战（自定义中间件示例）
- [ ] 状态持久化与乐观锁（AgentStateStore 各实现对比）
- [ ] 多 agent 编排与子 agent 事件转发机制
