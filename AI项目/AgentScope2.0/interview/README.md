---
type: synthesis
status: growing
updated: 2026-10-22
---

# AgentScope Java 2.0 面试题库

> 参考 1.0 题库结构，按 **v2.0.4** 重写，共 **64 题**。每题包含口述答案、追问、边界、教程双链和本地源码定位。

## 复习顺序

1. Q01–Q08：Core/Harness、调用主链、CallExecution、恢复与终止。
2. Q09–Q32：事件/Middleware、工具权限、请求隔离、会话持久化。
3. Q33–Q56：压缩、MCP/RAG、Skill、计划与子 Agent。
4. Q57–Q64：审批恢复、单次执行控制、队列、生产边界。
5. 最后核对版本差异，避免混入 1.0 或其他 2.x API。

## 分主题导航

| 主题 | 题号 | 重点 |
|:---|:---|:---|
| [[AI项目/AgentScope2.0/interview/01.框架架构与ReAct执行原理]] | Q01–Q08 | 模块、惰性执行、CallExecution、returnDirect |
| [[AI项目/AgentScope2.0/interview/02.流式输出与Middleware机制]] | Q09–Q16 | 事件、洋葱链、提示词、结构化输出 |
| [[AI项目/AgentScope2.0/interview/03.工具调用、权限与执行治理]] | Q17–Q24 | Schema、默认值、请求视图、重试 |
| [[AI项目/AgentScope2.0/interview/04.会话持久化与长期记忆]] | Q25–Q32 | slot、CAS、作用域、记忆与保存 |
| [[AI项目/AgentScope2.0/interview/05.上下文压缩与工具结果卸载]] | Q33–Q40 | 动态预算、best-effort、卸载与恢复 |
| [[AI项目/AgentScope2.0/interview/06.MCP接入与应用层RAG]] | Q41–Q48 | 连接、视图、旧 RAG 与应用层检索 |
| [[AI项目/AgentScope2.0/interview/07.Skill计划与子Agent协作]] | Q49–Q56 | 作用域、晋升、计划、后台任务 |
| [[AI项目/AgentScope2.0/interview/08.HITL中断恢复与生产追问]] | Q57–Q64 | 确认协议、句柄、取消、队列、多副本 |
| [[AI项目/AgentScope2.0/interview/09.版本口径与源码核对]] | 16 组核对 | 文档/实现冲突与待补证据 |

## 先记住的边界

- 同会话串行指本实例；CAS 检测竞争不能代替外部幂等。
- Core 内存状态与 Harness 默认文件持久化不同。
- Harness 压缩/eviction 默认装配；文档残留的“默认关闭”不适用于此标签。
- @Tool.concurrencySafe 默认 true，开发者声明不构成线程安全证明。
- maxRetries 的默认传递实际按总尝试次数解释。
- ConfirmResult 的 ID 校验不等于完整的工具名/参数/新增规则授权。
- cancel、interrupt、审批挂起和外部执行挂起各有不同协议。

每一项均有对应题和版本核对页的源码依据。

## 关联与验证范围

- 教程总入口：[[AI项目/AgentScope2.0/README]]；迁移对照：[[wiki/comparisons/AgentScope1.0与2.0迁移对照]]。
- 答题骨架：[[wiki/synthesis/AgentScope2.0面试答题主线]]；旧版题库：[[AI项目/AgentScope1.0/interview/README]]。
- 本次静态核对，未执行供应商调用、应用或原测试。第 6 篇的验收清单标为待执行，不将配置示例写成实测。
- 源码链接固定本机路径与该提交行号；切换标签后须重核。完整版本和证据边界见核对页。
