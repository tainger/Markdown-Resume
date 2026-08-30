---
type: concept
status: seed
updated: 2026-08-30
---

# Context Engineering 上下文工程

> 一句话：把模型可用上下文窗口当作稀缺资源做「预算管理」——放什么、不放什么、按什么顺序放。

## 核心笔记

- [[自媒体/微信公众号/探小虎/ContextEngineering]]（主笔记）
- [[AI应用开发/大模型工程基础]]
- [[AI应用开发/提示词工程]]（指令层子集：五段模板 / CoT / prompt 即代码 / 评测集回归）

## 知识骨架

- 上下文 = 系统提示 + 工具定义 + 检索内容 + 对话历史 + 输出预算
- 与 RAG 的关系：RAG 是上下文的**供给**策略之一
- 与记忆的关系：压缩历史 = 牺牲细节换窗口，见 [[wiki/concepts/Agent]] 记忆三层
- 与提示词工程的关系：Prompt 是变量，上下文是环境——提示词只管指令性文本这一层

## 关联

- [[wiki/concepts/RAG]]、[[wiki/concepts/Agent]]
