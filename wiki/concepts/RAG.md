---
type: concept
status: growing
updated: 2026-09-05
---

# RAG 检索增强生成 Retrieval-Augmented Generation

> 一句话：外挂知识让模型答得准、答得新、可溯源；工程核心是「切片 → 检索 → 重排 → 生成」四级流水线的质量叠加。

## 核心笔记

- [[AI应用开发/3. RAG检索增强生成]]（主笔记）
- [[AI应用开发/6.1 讲下一次RAG查询底层发生了什么事情]]（全链路实战：6 阶段深度剖析）
- [[AI应用开发/4. rag如何与大模型集成]]（集成架构层）
- [[AI应用开发/5. LLMWIKI 如何优化传统rag]]（优化演进层）
- [[AI应用开发/6. 幻觉，幻觉审计，幻觉治理，幻觉评测]]（幻觉治理）
- [[项目/agentMate/向量化模型选型]]（Embedding 模型选型 + 阿里云百炼清单）
- [[自媒体/微信公众号/探小虎/RAG实战]]（双混合检索落地）
- [[AI应用开发/14. AI高频面试题速查]]（RAG 相关追问链）

## 知识骨架

- **RAG vs 微调**：知识频繁更新/需溯源 → RAG；风格/格式内化 → 微调；两者可叠加
- **双混合检索**：精确术语/黑话走结构化检索，自然语言走向量检索，互为兜底
- **切片**：按 Markdown 标题层级切，表格/代码块不切断，命中后回调父级章节补上下文
- **幻觉治理**：生成内容必须能指回证据来源，控制引用锚点

## 查询全链路（6 阶段）

1. **Query Processing**：查询改写 / HyDE 假设性回答 / 多查询扩展
2. **Query Embedding**：Embedding 模型选型 / 归一化 / 批处理优化
3. **Vector Retrieval**：ANN 算法（HNSW/IVF-PQ/DiskANN）/ 元数据过滤 / BM25 混合检索 + RRF 融合
4. **Reranking**：双塔 vs Cross-Encoder / 两阶段检索架构 / Reranker 选型
5. **Context Assembly**：Token 预算分配 / Chunk 去重 / 上下文压缩 / 引用标注
6. **LLM Generation**：Prompt 组装 / 生成约束（Groundedness/Citation）/ SSE 流式输出

**关键考点**：HNSW 分层图原理 / HyDE 为什么能提升召回 / Reranking 的必要性 / 端到端延迟分布（检索侧 ~250ms / 生成侧 ~2500ms）

## 关联

- [[wiki/concepts/Agent]]（RAG 是 Agent 的知识供给方式之一）
- [[wiki/concepts/ContextEngineering]]
