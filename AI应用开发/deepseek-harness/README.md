# DeepSeek-Harness 技术解析

> 基于 DeepSeek-Harness 开源项目源码（`repo/AI-project/deepseek-harness`）的技术解析笔记。

## 项目简介

DeepSeek Harness（`dsh`）是 DeepSeek AI 开发的开源 Agent Harness，基于 Cordis 插件架构。核心设计哲学：
- **Everything is a plugin**：包括 Agent Loop、LLM 适配器、会话日志都可从配置替换
- **Model-visible ⟺ Logged**：任何到达模型请求的内容必须能从 session log 重建

## 笔记列表

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 17 | 🐋 **DeepSeek-Harness 技术架构源码解析** | [17. DeepSeek-Harness技术架构源码解析.md](17.%20DeepSeek-Harness技术架构源码解析.md) | Cordis 插件树架构、Profile/Bundle 组合机制、**Model-visible ⟺ Logged 核心原则**、Turn/Step 模型、**能力缝三角色模式**、Surface 模型压缩、工具调用对增量 BalanceCache、压缩事务生命周期、KV Cache 优化、**Request-Reconstruction Invariant 运行时断言**、RuntimeContextProjection 动态投影、与 OpenClaw 对比 |
| 18 | 📐 **DeepSeek-Harness 上下文工程设计经验** | [18. DeepSeek-Harness上下文工程设计经验.md](18.%20DeepSeek-Harness上下文工程设计经验.md) | 从源码提炼的 10 条可复用设计模式：日志优先（Event Sourcing）、运行时不变量断言、Surface 投影模型（MVCC）、增量平衡缓存（代际失效）、差量注入（CDC）、KV Cache 前缀复用、结构化摘要 + Checkpoint Framing、压缩事务 + 乐观并发、分层配置 + 加载校验、Waterfall 事件语义（责任链） |

## 源码路径

- DeepSeek-Harness 源码：[repo/AI-project/deepseek-harness](../../repo/AI-project/deepseek-harness)
- 架构文档：`docs/architecture.md`
- Agent Loop：`packages/core/agent-loop/src/agent.ts`
- Request Invariant：`packages/core/agent-loop/src/invariant.ts`
- Runtime Context：`packages/core/agent-loop/src/runtime-context.ts`
- Compaction 区域选择：`packages/compaction/compaction-basic/src/region.ts`
- Tool Pairing：`packages/compaction/compaction/src/tool-pairing.ts`
- Compaction 类型：`packages/compaction/compaction/src/types.ts`
