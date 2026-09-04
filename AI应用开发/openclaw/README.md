# OpenClaw 技术解析

> 基于 OpenClaw 开源项目源码（`repo/AI-project/openclaw`）的技术解析笔记。

## 笔记列表

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 15 | 🦞 **OpenClaw 技术架构与 AI Agent 技术解析** | [15. OpenClaw技术架构与AI Agent技术解析.md](15.%20OpenClaw技术架构与AI%20Agent技术解析.md) | 微内核网关四层架构、ReAct Agentic Loop、模型无关 Provider 抽象、SOUL.md 配置优先、Skills/ClawHub 生态、Hooks 生命周期、**ContextEngine 插件接口 + Compaction Pipeline（自适应分块/工具调用对保护/三级预算阈值）**、OpenClaw vs AgentMate 对比 |

## 源码路径

- OpenClaw 源码：[repo/AI-project/openclaw](../../repo/AI-project/openclaw)
- Context Engine 模块：`src/context-engine/`
- Compaction 模块：`src/agents/compaction-*.ts`
- Context Window Guard：`src/agents/context-window-guard.ts`
- Context Cache：`src/agents/context-cache.ts`
