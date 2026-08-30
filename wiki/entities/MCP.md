---
type: entity
status: growing
updated: 2026-08-30
---

# MCP（Model Context Protocol）

> 一句话：工具接入的 USB-C——把 M×N 的胶水成本压成 M+N 的标准协议；Function Calling 是模型会用手，MCP 是给手接上标准接口。

## 定位

Anthropic 2024-11 开源的工具/资源接入标准协议，Agent 工具接入的事实标准。三角色（Host/Client/Server 1:1）、六大原语（tools/resources/prompts + sampling/roots/elicitation）、JSON-RPC 2.0 报文、stdio / Streamable HTTP 传输。

## 核心笔记

- [[AI应用开发/MCP协议]]（主笔记，T14 详细篇）：架构/生命周期/六原语/传输/Java 落地/安全
- 速查骨架：[[AI应用开发/AI高频面试题速查]] T14（M×N → M+N、USB-C 类比、三态 Hook）
- 框架落地：[[AI应用开发/SpringAI与LangChain4j]]（@McpTool vs @Tool、Starter、Streamable HTTP）
- 上层框架：[[AI应用开发/Agent智能体框架]]（工具是 Agent 的手脚）
- 设计哲学：[[自媒体/微信公众号/探小虎/AIAgent设计哲学:工具调用与MCP]]

## 高频追问链

```
MCP 是什么？为什么 M+N？（接入经济学 + USB-C 类比）
→ 和 Function Calling 什么关系？（FC 是模型能力，MCP 是接入协议，上下层互补）
→ tools 和 resources 区别？（动手 vs 喂料，控制权模型/应用）
→ 传输怎么选？（stdio 本地 / Streamable HTTP 远程；SSE 已废弃，说错扣分）
→ 和 Dubbo/RPC 做工具接入有什么不同？（契约面向模型：自然语言 description + JSON Schema + 审批流）
→ 工具太多选不过来？（分组/检索式选择，别塞几十个 schema）
→ 安全怎么管？（工具描述投毒/混淆代理/越权 → 三态 Hook 套 Server 外层 + 白名单 + human-in-the-loop）
```

## 易错点（面试扣分项）

1. 说「MCP 用 SSE 传输」——已废弃，标准说法 **Streamable HTTP**
2. 把 MCP 说成「模型能力」——它是**协议**，模型能力是 Function Calling
3. Client/Server 说成多对一——**1:1 有状态连接**，Host 聚合多个 Client
4. 只知道三个服务端原语——sampling/roots/elicitation 是客户端三件套
5. tool description 当注释写——它是给模型的说明书，也是攻击面

## 项目结合（AgentMate）

三态 Hook（调用前鉴权 → 执行 → 审计落库）套在 MCP Server 实现层，Client 侧 Host 审批兜底；Server 对应 Aone 工单 / SchedulerX / Git / SLB 监控，进程级隔离崩一个不拖垮宿主。

## 关联

- [[wiki/concepts/Agent]]（工具接入是 Agent 能力的一部分）、[[wiki/concepts/ContextEngineering]]（工具 schema 占上下文预算）
