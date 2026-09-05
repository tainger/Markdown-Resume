---
type: concept
status: growing
updated: 2026-09-05
---

# Context Engineering 上下文工程

> 一句话：把模型可用上下文窗口当作稀缺资源做「预算管理」——放什么、不放什么、按什么顺序放。

## 核心笔记

- [[AI应用开发/2.5 上下文工程]]（基础理论）
- [[AI应用开发/openclaw/15. OpenClaw技术架构与AI Agent技术解析]]（OpenClaw ContextEngine 插件接口 + Compaction Pipeline）
- [[AI应用开发/openclaw/16. OpenClaw上下文工程设计经验]]（OpenClaw 10 条设计模式：安全净化/工具调用对保护/token 安全边距/三级预算阈值）
- [[AI应用开发/deepseek-harness/17. DeepSeek-Harness技术架构源码解析]]（dsh Surface 模型 + Request-Reconstruction Invariant）
- [[AI应用开发/deepseek-harness/18. DeepSeek-Harness上下文工程设计经验]]（dsh 10 条设计模式：日志优先/增量 BalanceCache/差量注入/KV Cache 前缀复用）
- [[自媒体/微信公众号/探小虎/ContextEngineering]]（主笔记）
- [[AI应用开发/1. 大模型工程基础]]
- [[AI应用开发/2. 提示词工程]]（指令层子集：五段模板 / CoT / prompt 即代码 / 评测集回归）

## 知识骨架

- 上下文 = 系统提示 + 工具定义 + 检索内容 + 对话历史 + 输出预算
- 与 RAG 的关系：RAG 是上下文的**供给**策略之一
- 与记忆的关系：压缩历史 = 牺牲细节换窗口，见 [[wiki/concepts/Agent]] 记忆三层
- 与提示词工程的关系：Prompt 是变量，上下文是环境——提示词只管指令性文本这一层

## 两大开源框架对比

| 维度 | OpenClaw | DeepSeek-Harness |
|:---|:---|:---|
| 架构基础 | 自研微内核网关 | Cordis 插件框架 |
| 核心哲学 | 配置优先（SOUL.md） | 日志优先（Session Log as Truth） |
| 压缩模型 | ContextEngine.compact() | Surface 投影 + 压缩事务 |
| 工具调用保护 | pendingToolCalls 队列 | 增量 BalanceCache + 切点平衡 |
| 安全性 | 压缩前净化 | Request-Reconstruction Invariant 运行时断言 |

## 十大设计模式（综合两框架）

1. 可插拔上下文接口（Strategy Pattern）
2. 安全净化前置（Defense in Depth）
3. 工具调用对原子保护（协议感知分区）
4. token 估算安全边距（悲观估算）
5. 多源预算降级链（配置优先级链）
6. 差量注入（Change Data Capture）
7. KV Cache 前缀复用（Prefix Caching Optimization）
8. 结构化摘要 + Checkpoint Framing（Structured Output）
9. 压缩事务 + 乐观并发控制（Optimistic CC）
10. Waterfall 事件语义（Chain of Responsibility）

## 关联

- [[wiki/concepts/RAG]]、[[wiki/concepts/Agent]]
