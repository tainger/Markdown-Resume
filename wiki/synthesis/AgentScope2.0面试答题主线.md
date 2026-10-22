---
type: synthesis
status: growing
updated: 2026-10-22
---

# AgentScope 2.0 面试答题主线

> 答题骨架：版本与模块 → 调用/状态 → 事件与工具 → 长任务 → 人工确认与生产边界。教程入口：[[AI项目/AgentScope2.0/README]]；64 题入口：[[AI项目/AgentScope2.0/interview/README]]。

## 版本先行

本系列按 v2.0.4、提交 `0b6743b263ba8e39eb417e35048026f1e0196548` 静态核对，Java 17。1.0 题库按 v1.0.12，不能混用 import、状态模型、压缩策略与默认值。来源：[[AI项目/AgentScope2.0/1.AgentScope初探与1.0迁移]]、[[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]]。

## 六条答题线

| 主线 | 答题顺序 | 来源 |
|:---|:---|:---|
| 模块与调用 | Core / Harness → 订阅 → CallExecution → reasoning / acting → 终止 | [[AI项目/AgentScope2.0/1.AgentScope初探与1.0迁移]]、[[AI项目/AgentScope2.0/8.ReActAgent执行原理与会话并发]] |
| 状态与并发 | RuntimeContext → slot → AgentStateStore → 实例队列 → CAS / 外部幂等 | [[AI项目/AgentScope2.0/3.多轮会话、RuntimeContext与状态持久化]]、[[AI项目/AgentScope2.0/6.实战：三层记忆与会话恢复]] |
| 事件与工具 | AgentEvent → Middleware → Schema / 权限 → 请求视图 → 错误通道 | [[AI项目/AgentScope2.0/2.流式输出、结构化输出与执行控制]]、[[AI项目/AgentScope2.0/7.Middleware与Hook迁移]]、[[AI项目/AgentScope2.0/9.工具集成、权限治理与请求隔离]] |
| 长任务记忆 | 工作区 → 长期事实 / 原始日志 → 动态压缩预算 → 卸载回查 | [[AI项目/AgentScope2.0/4.长期记忆与工作区]]、[[AI项目/AgentScope2.0/5.智能上下文压缩与工具结果卸载]] |
| 知识与复用 | MCP 连接 → 应用层 RAG → Skill 作用域 / 晋升 | [[AI项目/AgentScope2.0/10.MCP接入与工具可见性]]、[[AI项目/AgentScope2.0/11.应用层RAG与旧接口迁移]]、[[AI项目/AgentScope2.0/12.Skill装配、作用域与自学习]] |
| 审批与协作 | ConfirmResult → AgentRun → Plan Mode → 子任务 / 多副本存储 | [[AI项目/AgentScope2.0/13.Human-in-the-Loop与AgentRun执行控制]]、[[AI项目/AgentScope2.0/14.Plan模式、子Agent与Harness部署]] |

## 先纠正八个口径

- Core 的 slot 内存缓存与 Harness 默认文件持久化不同；同会话串行是本实例队列，不能推导跨节点互斥。来源：[[AI项目/AgentScope2.0/3.多轮会话、RuntimeContext与状态持久化]]。
- Harness 在该标签默认装配压缩与大结果卸载；动态 token 保留与消息数保留要分开。来源：[[AI项目/AgentScope2.0/5.智能上下文压缩与工具结果卸载]]。
- @Tool.concurrencySafe 默认 true；声明不是线程安全证明。来源：[[AI项目/AgentScope2.0/9.工具集成、权限治理与请求隔离]]。
- ModelConfig.maxRetries 经 maxAttempts 解释，默认 3 是总尝试预算。来源：[[AI项目/AgentScope2.0/2.流式输出、结构化输出与执行控制]]。
- 旧 RAG Builder 为 deprecated 兼容入口；RAGMode 没有 STATIC。来源：[[AI项目/AgentScope2.0/11.应用层RAG与旧接口迁移]]。
- 技能晋升要求显式 RuntimeContext，不能沿用文档残留的两参数示例。来源：[[AI项目/AgentScope2.0/12.Skill装配、作用域与自学习]]。
- ConfirmResult 校验 ID，不等于完整的工具名、参数或新增规则授权。来源：[[AI项目/AgentScope2.0/13.Human-in-the-Loop与AgentRun执行控制]]。
- cancel、interrupt、权限 ASK 和外部执行挂起是不同路径；returnDirect 也有批次条件。来源：[[AI项目/AgentScope2.0/8.ReActAgent执行原理与会话并发]]、[[AI项目/AgentScope2.0/13.Human-in-the-Loop与AgentRun执行控制]]。

上述差异集中在 [[AI项目/AgentScope2.0/interview/09.版本口径与源码核对]]，保留原文与源码，不静默覆盖。

## 面试收束

先讲运行时机制，再说明应用怎样做身份、审批、运行登记、预算、外部幂等和验证。第 6 篇配置与验收清单仍标为待执行，不能说成已经跑通：[[AI项目/AgentScope2.0/6.实战：三层记忆与会话恢复]]。应用项目可对照 [[wiki/synthesis/gogo-agent面试答题主线]]，各自源码版本独立。

## 源码修复证据

[[AI项目/AgentScope2.0/review/01.会话标识碰撞与修复]] 记录 v2.0.4 内部会话键碰撞与反向拆分保存错误；修复分支 `codex/fix-session-slot-identity`、提交 `761af7f6`。5 项回归原版失败、修复后通过，103 项相关测试及格式检查通过。该证据仅覆盖本次修复，不将内存替身测试泛化为真实模型或分布式部署验证。

## 证据边界与关联

教程与题库原维护为静态核对；后续源码修复的运行证据见上节。⚠️ raw 缺失，待补充：真实模型/MCP 联调、重启恢复、故障注入、性能成本、领域评测与个人贡献，详见 [[AI项目/AgentScope2.0/interview/09.版本口径与源码核对]]。

- 版本比较：[[wiki/comparisons/AgentScope1.0与2.0迁移对照]]。
- 旧版答题线：[[wiki/synthesis/AgentScope1.0面试答题主线]]。
- 概念关联：[[wiki/concepts/Agent]]、[[wiki/concepts/ContextEngineering]]、[[wiki/concepts/RAG]]、[[wiki/entities/MCP]]。
