---
type: concept
status: growing
updated: 2026-09-05
---

# Agent 智能体

> 一句话：LLM + 工具 + 记忆 + 规划的循环；确定性归代码，不确定性归模型。

## 核心笔记

- [[AI应用开发/7. Agent智能体框架]]（主笔记）
- [[AI应用开发/openclaw/15. OpenClaw技术架构与AI Agent技术解析]]（OpenClaw 微内核网关 + ReAct Loop + Skills/ClawHub）
- [[AI应用开发/deepseek-harness/17. DeepSeek-Harness技术架构源码解析]]（dsh Cordis 插件架构 + Everything is a plugin）
- 设计哲学系列：
  - [[自媒体/微信公众号/探小虎/AIAgent设计哲学]]（总纲）
  - [[自媒体/微信公众号/探小虎/AIAgent设计哲学：确定性归代码，不确定性归模型]]
  - [[自媒体/微信公众号/探小虎/AIAgent设计哲学:工具调用与MCP]]
  - [[自媒体/微信公众号/探小虎/AIAgent设计哲学:记忆的三层架构]]
  - [[自媒体/微信公众号/探小虎/AIAgent设计哲学:多Agent协作]]
  - [[自媒体/微信公众号/探小虎/AIAgent设计哲学:可中断可观测可回放]]
- 安全与边界：[[自媒体/微信公众号/探小虎/AIAgent安全]]、[[AI应用开发/9. 沙箱机制]]、[[自媒体/微信公众号/探小虎/什么时候不该用Agent]]

## 知识骨架

- **工具调用**：Function Calling / MCP 协议（详见 [[wiki/entities/MCP]]）；工具是 Agent 的手脚
- **记忆三层**：工作记忆（上下文）/ 情景记忆（会话）/ 语义记忆（知识库，可外挂 RAG）
- **可观测**：中断、回放、审计是生产级 Agent 的底线
- **评测**：见 [[自媒体/微信公众号/探小虎/AI应用评测]]

## 两大开源 Agent Harness 对比

| 维度 | OpenClaw | DeepSeek-Harness |
|:---|:---|:---|
| 运行形态 | 本地后台服务（常驻） | 可组合插件树（CLI/Web/ACP） |
| 架构基础 | 自研微内核网关 | Cordis 插件框架 |
| 可扩展性 | ContextEngine 可插拔 | Everything 可插拔（含 Agent Loop） |
| 设计哲学 | 配置优先（SOUL.md） | 日志优先（Session Log as Truth） |
| Agent Loop | 标准 ReAct 模式 | Turn/Step 模型 + waterfall 事件 |
| 成熟度 | 145k stars 生产可用 | 预发布（foundation over blast radius） |

## 关联

- [[wiki/concepts/RAG]]、[[wiki/concepts/ContextEngineering]]
- 工具接入协议：[[wiki/entities/MCP]]
- 本库 wiki 层本身就是「Agent 长期记忆」的个人级实践（见根目录 AGENTS.md）
