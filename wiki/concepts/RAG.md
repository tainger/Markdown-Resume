---
type: concept
status: seed
updated: 2026-08-30
---

# RAG 检索增强生成 Retrieval-Augmented Generation

> 一句话：外挂知识让模型答得准、答得新、可溯源；工程核心是「切片 → 检索 → 重排 → 生成」四级流水线的质量叠加。

## 核心笔记

- [[AI应用开发/RAG检索增强生成]]（主笔记）
- [[自媒体/微信公众号/探小虎/RAG实战]]（双混合检索落地）
- [[DeepSeek Harness/03.RAG与Agent集成]]
- [[AI应用开发/AI高频面试题速查]]（RAG 相关追问链）

## 知识骨架

- **RAG vs 微调**：知识频繁更新/需溯源 → RAG；风格/格式内化 → 微调；两者可叠加
- **双混合检索**：精确术语/黑话走结构化检索，自然语言走向量检索，互为兜底
- **切片**：按 Markdown 标题层级切，表格/代码块不切断，命中后回调父级章节补上下文
- **幻觉治理**：生成内容必须能指回证据来源，控制引用锚点

## 关联

- [[wiki/concepts/Agent]]（RAG 是 Agent 的知识供给方式之一）
- [[wiki/concepts/ContextEngineering]]
