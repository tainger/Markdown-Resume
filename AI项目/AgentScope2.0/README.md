---
type: synthesis
status: growing
updated: 2026-10-22
---

# AgentScope Java 2.0 学习与面试导航

> 参考 AgentScope1.0 的 14 篇教程与 64 题结构维护；按本地 **v2.0.4** 源码重新组织，突出 RuntimeContext、AgentState、Middleware、Harness 和迁移边界。

## 版本与阅读约定

- 源码标签 `v2.0.4`，提交 `0b6743b263ba8e39eb417e35048026f1e0196548`；核对日期 2026-10-22；Java 17。
- 源码目录：`AI项目/AgentScope2.0/repo/agentscope-java`；[官方发布记录](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.4)。
- 对照资料：[[AI项目/AgentScope1.0/1.AgentScope初探]] 与 [[AI项目/AgentScope1.0/interview/README]]，旧笔记保持独立版本。
- 本次基于本地文档与实现静态核对；示例没有真实调用模型/MCP，也没有执行原项目测试。
- 双链从本仓库根开始、不带 .md；源码链接用本机绝对路径与行号。

## 14 篇教程

| 编号 | 页面 | 核心问题 |
|:---|:---|:---|
| 01 | [[AI项目/AgentScope2.0/1.AgentScope初探与1.0迁移]] | 先区分 Core、Harness 与模型扩展，再开始写 Agent |
| 02 | [[AI项目/AgentScope2.0/2.流式输出、结构化输出与执行控制]] | 围绕 AgentEvent 消费事件，把循环限制与调用预算分开 |
| 03 | [[AI项目/AgentScope2.0/3.多轮会话、RuntimeContext与状态持久化]] | 将调用元数据、可恢复状态和工作区文件分开管理 |
| 04 | [[AI项目/AgentScope2.0/4.长期记忆与工作区]] | 以工作区记忆文件理解 Harness，不沿用旧长期记忆模式作为主线 |
| 05 | [[AI项目/AgentScope2.0/5.智能上下文压缩与工具结果卸载]] | 理解摘要、原文日志、结果卸载和溢出兜底的分工 |
| 06 | [[AI项目/AgentScope2.0/6.实战：三层记忆与会话恢复]] | 给出可复查的配置与验收步骤，区分源码支持和实际跑通 |
| 07 | [[AI项目/AgentScope2.0/7.Middleware与Hook迁移]] | 用五个拦截位置表达横切逻辑，显式传入调用上下文 |
| 08 | [[AI项目/AgentScope2.0/8.ReActAgent执行原理与会话并发]] | 以 CallExecution 串起状态、推理、工具和恢复分支 |
| 09 | [[AI项目/AgentScope2.0/9.工具集成、权限治理与请求隔离]] | 把 Schema、身份、并发安全和执行权限逐层说明 |
| 10 | [[AI项目/AgentScope2.0/10.MCP接入与工具可见性]] | 协议连接、注册与过滤分别核对，模型看到工具不代表调用已授权 |
| 11 | [[AI项目/AgentScope2.0/11.应用层RAG与旧接口迁移]] | 新示例先检索再组装上下文，兼容接口只作为迁移背景 |
| 12 | [[AI项目/AgentScope2.0/12.Skill装配、作用域与自学习]] | 技能是按需读取的能力包，执行仍通过工具和权限系统 |
| 13 | [[AI项目/AgentScope2.0/13.Human-in-the-Loop与AgentRun执行控制]] | 区分审批挂起、外部工具挂起、协作中断和立即取消 |
| 14 | [[AI项目/AgentScope2.0/14.Plan模式、子Agent与Harness部署]] | 把只读规划、任务清单、子任务隔离和运行存储连起来 |

## 推荐路线

- 初次上手：1 → 3 → 8 → 9，理解最小 Agent、状态与执行。
- Web/服务接入：2 → 7 → 13，掌握事件、Middleware、运行句柄和审批。
- 长任务：4 → 5 → 6 → 12 → 14，串起工作区、记忆、压缩、技能与子任务。
- 外部知识与工具：10 → 11，明确 MCP 和应用层 RAG 的接入边界。
- 面试复习：先看题库导航，再按主题练口述，最后复查版本核对页。

## 从 1.0 迁移的关键变化

| 1.0 阅读主题 | 2.0 学习主线 | 本系列入口 |
|:---|:---|:---|
| Memory / Session | RuntimeContext + AgentState + AgentStateStore | 第 3、6 篇 |
| 旧长期记忆模式 | Harness 工作区记忆与历史日志 | 第 4 篇 |
| 六级上下文压缩 | Compaction / eviction / 溢出恢复 | 第 5 篇 |
| Hook | Middleware 主线，Hook 保留兼容路径 | 第 7 篇 |
| 工具执行 | 权限引擎、请求视图、并发与错误通道 | 第 9 篇 |
| Knowledge / RAGMode | 应用层检索，旧 Builder 为 deprecated 兼容入口 | 第 11 篇 |
| SkillBox | 仓库与用户作用域、动态装配 | 第 12 篇 |
| 挂起与恢复 | ConfirmResult / 外部结果 / AgentRun 分开处理 | 第 13 篇 |
| Plan-and-Execute | Plan Mode、todo、子 Agent 与存储分工 | 第 14 篇 |

这些是按本标签整理的学习映射，并非逐项等价替换。原文/API 冲突不静默覆盖。

## 面试与源码核对

- [[AI项目/AgentScope2.0/interview/README]]：8 个主题、Q01–Q64，每题有答案、追问、边界和来源。
- [[AI项目/AgentScope2.0/interview/09.版本口径与源码核对]]：16 组差异，包括压缩默认值、工具并发默认值、RAG 枚举、重试计数、技能晋升签名与审批校验范围。
- [[wiki/synthesis/AgentScope2.0面试答题主线]]：跨主题答题顺序。
- [[wiki/comparisons/AgentScope1.0与2.0迁移对照]]：选用/迁移结论。

## 源码审查与修复

- [[AI项目/AgentScope2.0/review/01.会话标识碰撞与修复]]：会话键碰撞/保存寻址错误，独立本地提交 `761af7f6`；5 项先失败后通过，103 项相关测试通过，附可移植补丁。原教程快照保持 v2.0.4。

## 证据与维护

- 教程每篇关联对应 1.0 原文、本地 2.0 文档与源码；API、默认值冲突集中记入核对页。
- ⚠️ raw 缺失，待补充：真实模型/MCP 联调、重启恢复、多副本故障、成本与领域质量数据；不写成已完成成果。
- 后续更新先比较源码提交与本目录改动，只更新受影响教程/题库，维护 wiki 导航与操作日志。
- 本目录新建标准 `interview/`；初始化遗留的带前导空格空目录 ` interview/` 保留，未做改名或删除。
