# 探小虎

> 一个聚焦 **AI 开发**的技术分享公众号。

不抖机灵、不堆术语。每一篇都围绕一个主题把「为什么这样设计」讲透:从 AI Agent 的架构哲学,到 AI 编码工具的工作流,再到生产级 Agent 的工程取舍。重原理而非 API,重判断标准而非工具罗列,尽量结合真实案例给出可复用的思考框架。

## 关于这个仓库

本仓库存放公众号「**探小虎**」已发布文章的 Markdown 源文件与配图,便于检索、修订与版本管理。每篇文章独立成文件,配图统一放在 `images/` 目录下以相对路径引用。

```
探小虎/
├── README.md                                          # 本文件,文章索引
│
│  ── A. AI Agent 设计哲学系列 ──────────────────────────
├── AIAgent设计哲学.md                                   # 一·总论:从聊天到做事
├── AIAgent设计哲学：确定性归代码，不确定性归模型.md        # 二·架构选型与真实案例
├── AIAgent设计哲学:记忆的三层架构.md                      # 三·记忆分层
├── AIAgent设计哲学:工具调用与MCP.md                      # 四·工具与权限边界
├── AIAgent设计哲学:可中断可观测可回放.md                  # 五·生产级可控性
├── AIAgent设计哲学:多Agent协作.md                        # 六·子代理与技能
│
│  ── B. AI 编码工具与工作流系列 ──────────────────────────
├── OpenSpec技术分享.md                                  # 规格驱动开发
├── AI编码助手横评.md                                    # Cursor/Copilot/Claude Code
├── SpecDriven工具横评.md                               # OpenSpec/spec-kit/Kiro
├── ContextEngineering.md                               # 上下文工程化
│
│  ── C. AI 工程实践系列 ──────────────────────────────────
├── RAG实战.md                                          # 检索增强工程化
├── AI应用评测.md                                       # LLM-as-judge / 回归评测
├── AIAgent安全.md                                      # 提示注入与越权防护
├── " LLM Wiki 技术调研.md"                              # LLM + 知识库三条路线调研
│
│  ── D. 反思 / 架构选型 ──────────────────────────────────
├── 什么时候不该用Agent.md                               # 过度 Agent 化反模式
├── 万物皆可markdown.md                                  # Markdown 在 AI 时代的地位
│
└── images/                                            # 文章配图(16 张)
    ├── 01-spec-driven-workflow.jpg                     # OpenSpec 头图
    ├── 02-forgetful-ai.jpg
    ├── 03-spec-layer.jpg
    ├── 04-specs-vs-changes.jpg
    ├── 05-workflow-pipeline.jpg
    ├── agent-memory-three-layers.jpg                   # A1 记忆三层
    ├── agent-tools-mcp.jpg                             # A2 工具与 MCP
    ├── agent-observability.jpg                         # A3 可中断可观测
    ├── multi-agent-collaboration.jpg                   # A4 多 Agent 协作
    ├── coding-assistants-compare.jpg                   # B1 编码助手横评
    ├── spec-driven-tools.jpg                           # B2 Spec-Driven 横评
    ├── context-engineering.jpg                         # B3 上下文工程
    ├── rag-engineering.jpg                             # C1 RAG 实战
    ├── ai-eval.jpg                                     # C2 AI 评测
    ├── agent-security.jpg                              # C3 Agent 安全
    └── no-agent-antipattern.jpg                        # D1 不该用 Agent
```

## 选题方向

- **AI Agent 设计哲学** —— 自主循环、记忆分层、工具与权限边界、可中断可观测可回放等生产级设计原则;
- **Agent 架构选型** —— 何时用纯过程式、何时用 ReAct、何时走混合方案,以及「不确定性归谁」的判断标准;
- **AI 编码工具与工作流** —— Cursor / Copilot / Claude Code 等助手的协作范式、规格驱动开发(spec-driven)、上下文与记忆管理;
- **AI 工程实践** —— 把模型从 Demo 推向生产过程中的踩坑、复盘与沉淀:RAG、评测、安全;
- **后端架构与算法** —— 偶尔穿插,作为 Agent 落地的工程底座。

## 文章索引

### A. AI Agent 设计哲学系列

| # | 文章 | 核心观点 |
|:---:|:---|:---|
| A0 | [从「会聊天的模型」到「会做事的系统」](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent设计哲学.md) | Agent 与聊天机器人的本质差别是「自主循环」;围绕模型的三大短板(无记忆、不能行动、会幻觉)搭脚手架,提炼五条设计原则。 |
| A0+ | [确定性归代码,不确定性归模型](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent设计哲学：确定性归代码，不确定性归模型.md) | 先判断不确定性是任务固有还是模型引入的:固有的交给模型,引入的交给代码。用两个真实案例对比纯过程式、纯 ReAct、混合方案的取舍。 |
| A1 | [记忆的三层架构,以及「什么时候记、什么时候忘」](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent设计哲学:记忆的三层架构.md) | 记忆 ≠ 把历史塞进上下文。短/长/外部三层各管一时间尺度,各用一检索策略;记忆的艺术不是记住一切,而是「在合适时机召回此刻需要的那部分」。 |
| A2 | [工具调用与 MCP,让模型长出手脚的同时给它分寸](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent设计哲学:工具调用与MCP.md) | 工具是补模型短板的脚手架;function calling vs MCP 互补不互斥;能力(工具)与授权(权限)必须分离,三道闸门兜底。 |
| A3 | [可中断、可观测、可回放——生产级 Agent 的另一半工程](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent设计哲学:可中断可观测可回放.md) | Agent 上生产的及格线:能停得下来、过程透明可追溯、崩了能从断点续跑。三者是一套「可控性」工程的不同侧面。 |
| A4 | [多 Agent 协作——主 Agent、子 Agent 与技能的分工](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent设计哲学:多Agent协作.md) | 子代理解决并行(上下文隔离 + 结论汇报),技能解决复用(标准打法沉淀);拆的收益必须大于通信代价才拆。 |

### B. AI 编码工具与工作流系列

| # | 文章 | 核心观点 |
|:---:|:---|:---|
| B0 | [OpenSpec:让 AI 写代码之前,先和你「对齐要做什么」](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/OpenSpec技术分享.md) | AI 编码的结构性缺陷是需求只活在聊天记录里。OpenSpec 在意图与代码之间加一层 Markdown 规格层,把「猜错方向」的风险前置到写代码之前。 |
| B1 | [Cursor vs Copilot vs Claude Code 横评与选型](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AI编码助手横评.md) | 三者形态不同(IDE-first / 插件-first / Agent-first),先选工作方式再选工具;看交互范式、上下文能力、Agent 能力等稳定维度而非易变功能。 |
| B2 | [Spec-Driven 工具横评:OpenSpec / spec-kit / Kiro 怎么选](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/SpecDriven工具横评.md) | 三者路线不同:OpenSpec(轻量跨工具 brownfield)/ spec-kit(重流程 greenfield)/ Kiro(IDE 一站式)。按场景选,不按「谁最好」选。 |
| B3 | [Context Engineering:给 AI 编码助手喂上下文的工程化方法](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/ContextEngineering.md) | 上下文是真天花板;CLAUDE.md/AGENTS.md 是给 AI 看的项目宪法;最小够用 + 脏了就清的闭环,比体量重要十倍。 |

### C. AI 工程实践系列

| # | 文章 | 核心观点 |
|:---:|:---|:---|
| C1 | [RAG 实战:从朴素拼接到工程化检索增强](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/RAG实战.md) | RAG 的本质是精准召回此刻需要的最小片段,不是拼接;chunking/embedding/检索/重排/评测,任一环节偷懒全链路崩。 |
| C2 | [AI 应用评测怎么做:别只靠「感觉不错」](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AI应用评测.md) | 非确定 + 长尾 + 全局波及让感觉失灵;离线 golden set / 回归评测 / 线上监控三层缺一不可;LLM-as-judge 要给具体 rubric。 |
| C3 | [AI Agent 的安全:提示注入、越权与防护](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/AIAgent安全.md) | Agent 安全边界从「输出层」挪到「执行层」;提示注入防不住但后果能兜住,靠权限分级 + 沙箱让被骗也干不成大事。 |
| C4 | [LLM Wiki 技术调研:LLM + 知识库的三条路线](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/%20LLM%20Wiki%20技术调研.md) | 知识库死循环拆成生产/检索/维护三环节,先做检索;Wiki 结构是检索质量的放大器,沉淀走抽取→挂载→冲突检测→人审流程。 |

### D. 反思 / 架构选型

| # | 文章 | 核心观点 |
|:---:|:---|:---|
| D1 | [什么时候不该用 Agent:被过度 Agent 化的场景](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/什么时候不该用Agent.md) | Agent 是「用推理替代硬编码」,本身引入不确定性;把确定任务交给 Agent = 凭空引入故障。判断标准:任务要精确,别上 Agent。 |
| D2 | [万物皆可 Markdown:人机通用协议的诞生](file:///Users/rocky/study/profile/Markdown-Resume/自媒体/微信公众号/探小虎/万物皆可markdown.md) | Markdown 已从排版语法进化为人机通用协议:模型母语、输出 UI、上下文载体、Agent 记忆格式、轻量结构化输出五位一体;散文用 Markdown,数据用 JSON,强隔离段落借 XML。 |

## 写作原则

- **先讲为什么,再讲怎么做。** 不堆 API,重在讲清设计动机;
- **给判断标准,不给银弹。** 告诉读者「什么场景该选什么」,而不是「这个最好」;
- **结合真实案例。** 能用踩过的坑、做过的功能做印证,就不空谈理论;
- **对人和机器都友好。** 文章本身就是纯 Markdown,配图清晰,可被检索与版本管理。

## 关注我们

- **微信公众号**:探小虎
- **微博**:@探小虎
- **微信**:探小虎

---

> 欢迎在公众号后台留言想看的主题,优先写呼声最高的方向。
