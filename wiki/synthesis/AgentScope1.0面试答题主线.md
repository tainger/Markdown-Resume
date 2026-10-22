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

## 证据边界

本次为静态源码与文档核对，未运行原测试或真实模型/MCP 服务。⚠️ raw 缺失，待补充：应用级压测、真实 token/账单、故障注入、权限与个人实践报告。框架支持、应用接入、实际验证三者分开表述。

关联：[[wiki/concepts/Agent]]、[[wiki/concepts/ContextEngineering]]、[[wiki/concepts/RAG]]、[[wiki/entities/MCP]]。
