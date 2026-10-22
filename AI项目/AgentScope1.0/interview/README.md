---
type: synthesis
status: growing
updated: 2026-10-22
---

# AgentScope Java 1.0 面试题库

> 以本地 **v1.0.12** 源码与本目录 14 篇文档整理，共 64 题。每题含口述答案、追问/边界和原文与源码定位；不混用 2.x API。

## 复习顺序

1. 先练 Q01–Q06：框架定位、一次 call、ReAct 循环和恢复分支。
2. 深挖 Q08、Q14–Q16、Q18、Q22、Q27–Q31：复制边界、Hook、工具执行和状态持久化。
3. 再看 Q33–Q60：自动压缩、MCP/RAG、Skill、计划、协作与 HITL。
4. 最后看 Q61–Q64 和 [[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]]，练中断、关机与生产边界。

## 分主题导航

| 主题 | 题号 | 主要内容 |
|:---|:---|:---|
| [[AI项目/AgentScope1.0/interview/01.框架架构与ReAct执行原理]] | Q01–Q08 | 组件、call、reasoning/acting、pending、终止、复制范围 |
| [[AI项目/AgentScope1.0/interview/02.流式输出与Hook机制]] | Q09–Q16 | 流式事件、累积/增量、结构化输出、Hook 时序与状态 |
| [[AI项目/AgentScope1.0/interview/03.工具调用与执行治理]] | Q17–Q24 | Schema、身份注入、工具组、并行、超时与实际重试路径 |
| [[AI项目/AgentScope1.0/interview/04.会话持久化与长期记忆]] | Q25–Q32 | Memory/Session、恢复范围、保存时机、长期记忆三模式 |
| [[AI项目/AgentScope1.0/interview/05.自动上下文压缩原理]] | Q33–Q40 | 触发、六级策略、卸载重载、lastKeep、估算与评测 |
| [[AI项目/AgentScope1.0/interview/06.MCP接入与RAG机制]] | Q41–Q48 | 传输与过滤、Knowledge、RAG 模式、多库融合与评测 |
| [[AI项目/AgentScope1.0/interview/07.Skill计划与多Agent协作]] | Q49–Q56 | 技能加载、执行边界、计划、子 Agent 与 Pipeline |
| [[AI项目/AgentScope1.0/interview/08.HITL中断恢复与生产追问]] | Q57–Q64 | 前后确认、挂起回填、补洞、中断、关机与故障窗口 |
| [[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]] | 14 组核对 | 文档/注释与实现差异、版本基线与待补证据 |

## 主调用链速记

```text
订阅 call
  → 获取执行权 / tracing
  → PreCall（可加载状态、检索知识/长期记忆）
  → doCall：无 pending 正常推理 / 空输入续跑 / 校验 ToolResult
  → PreReasoning（自动压缩、技能/计划提示等）
  → model.stream → chunk 累积 → PostReasoning → 写 Memory
      ├─ stop / 无工具调用：返回
      └─ PreActing → Toolkit 执行 → PostActing → 写工具消息
           ├─ 挂起 / stop：返回对应 GenerateReason
           └─ 下一轮 reasoning
  → 正常结果 PostCall → 释放执行权
中断：错误处理 → handleInterrupt（不重新经过正常 PostCall）
```

**依据**：[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]、[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/AgentScope1.0/13.AgentScope Java进阶：Human-in-the-Loop]]；[AgentBase.java](</Users/rocky/study/profile/Markdown-Resume/AI项目/AgentScope1.0/repo/agentscope-java/agentscope-core/src/main/java/io/agentscope/core/agent/AgentBase.java:172>)、[ReActAgent.java](</Users/rocky/study/profile/Markdown-Resume/AI项目/AgentScope1.0/repo/agentscope-java/agentscope-core/src/main/java/io/agentscope/core/ReActAgent.java:264>)。图中加载状态是应用可接入的动作，不是每次 call 自动完成所有 Session 读写。

## 最容易答错的八点

| 错误理解 | v1.0.12 正确口径 | 题号 |
|:---|:---|:---|
| Pending 恢复 Hook 默认开 | Builder 默认 false，可显式开启或注册 | Q60 |
| Toolkit 复制 = 所有业务对象深拷贝 | 工具实例/注册对象仍可能复用引用 | Q08 |
| 所有 Hook 都顺序完成 | 部分 chunk/error 通知允许并发 | Q14 |
| 工具异常配置 retryOn 后必重试 | 异常可能先转正常错误结果，外层看不到 | Q22 |
| 保存 Memory = 保存完整 Agent | 计划、组状态与卸载原文等要另核对 | Q27/Q37 |
| 默认卸载到外部数据库 | offloadContext 默认是实例 HashMap | Q36 |
| Generic/静态长期记忆每轮推理检索 | 此标签实际在 PreCall 触发 | Q31/Q45 |
| 有 Session 就保证宕机精确续跑 | 保存窗口、原子性与外部幂等仍需设计 | Q62 |

每项都有对应题和核对页的源码证据；原文不静默覆盖。

## 版本与校验范围

- 源码按用户要求已切到标签 `v1.0.12`，提交 `b9080f27a8384403284fcc225f5e074373cb05e9`，处于 detached HEAD。
- 核对日期 2026-10-22；pom 声明 revision=1.0.12、Java 17。[pom.xml](</Users/rocky/study/profile/Markdown-Resume/AI项目/AgentScope1.0/repo/agentscope-java/pom.xml:30>)
- 源码链接定位本机路径与行号，后续切换标签后须重新核对；没有将其他版本的行号冒充此标签。
- 原文用仓库根双链；题库来自定向文档/源码阅读，未全库重扫，原 14 篇文档未改动。
- 本次只做静态核对、文档结构与引用检查，未运行原测试或真实模型/MCP 服务；没有新增可运行 API 示例。
- ⚠️ raw 缺失，待补充：具体项目的准确率、延迟、容量、成本、故障报告与个人贡献。建议不写成既有成果。

## 与应用项目连起来复习

- 框架复习主线：[[wiki/synthesis/AgentScope1.0面试答题主线]]。
- 应用实践对照：[[AI项目/gogo-agent/interview/README]]；框架机制和应用自己的 Hook/业务状态不要混讲。
- 基础概念：[[wiki/concepts/Agent]]、[[wiki/concepts/ContextEngineering]]、[[wiki/concepts/RAG]]、[[wiki/entities/MCP]]。
