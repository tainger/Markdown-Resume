---
type: synthesis
status: growing
updated: 2026-10-22
---

# gogo-agent 面试题库

> 64 道问答，以项目文档和本地源码交叉核对；每题包含可口述答案、追问/边界和来源。所有“改进建议”均与当前实现分开。

## 使用顺序

1. 先读 Q01、Q03，能在两分钟内讲清业务与一次请求。
2. 精读 Q14、Q19、Q22、Q27、Q30、Q34、Q42、Q45、Q50、Q55，准备源码追问。
3. 再按模块查漏，最后读 [[AI项目/gogo-agent/interview/09.源码核对与待补证据]]，避免混用旧实现。
4. 模拟面试时先答每题“口述答案”，被追问再展开；涉及个人工作只描述实际承担部分。

## 分主题导航

| 模块 | 题号 | 核心问题 |
|:---|:---|:---|
| [[AI项目/gogo-agent/interview/01.项目架构与技术选型]] | Q01–Q08 | 项目介绍、多 Agent、请求链路、委派、多例、ReAct、模型、取舍 |
| [[AI项目/gogo-agent/interview/02.意图识别与会话路由]] | Q09–Q16 | 三层识别、多意图、Top-2、问题改写、Master、续聊、评测 |
| [[AI项目/gogo-agent/interview/03.工具设计与预订一致性]] | Q17–Q24 | Tool/Skill/MCP、身份注入、缓存、下单幂等、取消、事务 |
| [[AI项目/gogo-agent/interview/04.规划算法与审核闭环]] | Q25–Q32 | 计划执行、确定性算分、代表方案、共享结果、审核、修复 |
| [[AI项目/gogo-agent/interview/05.上下文工程与RAG]] | Q33–Q40 | 记忆分层、上下文传播、压缩、折叠、缓存、偏好、政策 RAG |
| [[AI项目/gogo-agent/interview/06.人机交互与集群恢复]] | Q41–Q48 | 挂起、跨节点恢复、订阅标记、中断、SSE、结构化事件 |
| [[AI项目/gogo-agent/interview/07.安全护栏与故障治理]] | Q49–Q56 | 凭据、加密差异、CLI 隔离、Shell、脱敏、并行重试、熔断、注入 |
| [[AI项目/gogo-agent/interview/08.场景追问与评测复盘]] | Q57–Q64 | 慢请求、成本、质量、下单漏记、方案覆盖、并发确认、生产化、贡献 |
| [[AI项目/gogo-agent/interview/09.源码核对与待补证据]] | 13 组核对 | 旧方案、实现缺口、静态发现与尚需补齐的实测证据 |

## 一次请求的答题骨架

```text
登录身份 → ChatController → 保存用户消息 / 预中断旧执行 / 建 SSE
  ├─ 普通请求：原问题 L0/L1/L2
  │    ├─ 命中：跳过改写 → Master
  │    └─ 未命中：近期聊天改写 → L1/L2/L3 → Master
  ├─ 续聊信号：活跃 Agent / 对应 Pipeline 分支
  └─ respond：toolUseId → 本地复用或共享状态重建 → 续跑
Master → 管理 / 规划 / Info / Booking
规划 → 搜索捕获 → Java 计算 → 多维审核 → 有限修复 → 展示并等确认
预订 → 审批与确认要求 → 真实下单 → Hook 落库 / 支付事件
```

**注意**：图中审批与确认包含提示词要求，不能当作已完整实现的服务端强制状态机；具体见 Q32。实现依据：[[AI项目/gogo-agent/17.意图识别与问题改写的流水线编排]]、[[AI项目/gogo-agent/65.多维行程审核架构的方案设计]]、[[AI项目/gogo-agent/69.行程预订智能体的功能介绍及开发]]、[[AI项目/gogo-agent/115.Human-in-the-Loop在集群模式下的支持]]；[ChatController.java](</Users/rocky/study/profile/Markdown-Resume/AI项目/gogo-agent/repo/gogo-agent/src/main/java/com/gogo/travel/controller/ChatController.java:1>)、[AgentPipelineService.java](</Users/rocky/study/profile/Markdown-Resume/AI项目/gogo-agent/repo/gogo-agent/src/main/java/com/gogo/travel/agent/service/AgentPipelineService.java:1>)。

## 必须区分的六对概念

| 容易混淆 | 正确区分 | 题号 |
|:---|:---|:---|
| 普通意图直跳 / continuation | 前者停用，后者仍有直接续跑 | Q14/Q15 |
| 聊天展示历史 / Agent 运行 Memory | 前者给用户与改写，后者含工具执行状态 | Q33 |
| 偏好降分 / 硬排除 | 0 分仍可能当选，不等于从候选池剔除 | Q31 |
| 提示词限制 / 代码强制 | 最多两轮是提示词，客观 fail 硬否决由代码做 | Q30/Q31 |
| 本地记录去重 / 外部下单幂等 | 外部单号唯一不阻止生成两个不同订单 | Q22 |
| 可跨节点重建 / 并发一次消费 | 有恢复路径，不代表 pending 已原子消费 | Q48 |

上述判断分别可回到对应题的原笔记与源码；发现的新旧冲突集中在核对页，不静默覆盖 raw。

## 资料与证据约定

- 笔记用仓库根双链；源码链接直接定位本机文件及行号，基于本地快照 `ae3f19dc`，后续改动可能使行号漂移。
- 核对日期：2026-10-22；仅定向读取此项目文档/源码及相关 wiki，未全库重扫。
- 源码 pom 声明 Java 21、AgentScope 1.0.12；它们是项目快照信息，不是最新版本推荐。[pom.xml](</Users/rocky/study/profile/Markdown-Resume/AI项目/gogo-agent/repo/gogo-agent/pom.xml:1>)
- 当前 `repo/` 被主笔记库忽略；分享题库时需同时提供源码快照或可访问地址。
- 没有运行项目、调用真实供应商或执行原测试；本次校验范围为新文档结构、双链与源码定位。
- 不把目标延迟当测量值，不编造 QPS、准确率、节省比例、用户规模或个人贡献。缺失证据统一标“⚠️ raw 缺失，待补充”。

## 继续复习

- 原理入口：[[wiki/concepts/Agent]]、[[wiki/concepts/ContextEngineering]]、[[wiki/concepts/RAG]]、[[wiki/entities/MCP]]。
- 项目复习主线：[[wiki/synthesis/gogo-agent面试答题主线]]。
