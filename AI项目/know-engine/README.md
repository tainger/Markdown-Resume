# know-engine 学习资料库

> 基于 RAG 架构的企业知识管理与智能问答系统的**学习笔记与面试题库**。本文档帮助接手者快速了解目录结构和使用方式。

## 项目简介

know-engine 是一个 Java + LangChain4J 技术栈的 RAG 项目，实现从文档接入、智能切片、向量化存储到多源检索、意图路由、流式对话的全链路闭环。核心业务场景是汽车领域的智能客服（售前/售后/使用指导等 6 大意图），支持 ES 语义检索、MySQL Text2SQL、Neo4j Text2Cypher 三源查询路由。

**技术栈速览**：Java 21 + Spring Boot 3.5 · LangChain4J 1.11.0 · Elasticsearch 8.x · Neo4j 5.x + APOC · MySQL 8 + MyBatis-Plus · Redis + Redisson · MinerU（PDF 解析）· XXL-Job 2.4 · BGE-Reranker（ONNX 本地推理）· MinIO · 通义千问系列模型

## 目录结构

```
know-engine/
├── README.md                # ← 本文件（导航总览）
├── 1~61.*.md                # 学习笔记（61 篇，按课程顺序编号）
├── assets/                  # 笔记配图（已本地化的图片资源）
├── interview/               # 面试题库（9 个模块、84 道主问题）
│   ├── README.md            #   题库总索引与复习路线
│   └── 01~09-*.md           #   分模块面试题
└── repo/
    └── know-engine/         # 项目源码（独立 git 仓库，不进本库 git）
        ├── src/             #   Java 源码（ai/chat/config/document/infra/rag 六层包结构）
        ├── pom.xml
        ├── README.md        #   源码工程自带的说明
        ├── STARTUP.md       #   源码工程自带的启动指南
        └── RAG_EVALUATION.md
```

## 三个目录怎么用

| 目录 | 内容 | 适用场景 |
|:---|:---|:---|
| **笔记（当前目录 *.md）** | 61 篇课程笔记，按编号递进：1~7 项目概览与技术选型 → 8~11 MinerU 解析 → 12~21 索引构建（切分/ES/批量插入/雪花ID）→ 17~19、26 事件驱动/XXL-JOB/分布式锁 → 25~48 检索与对话链路（意图识别/重写/路由/混合检索/重排/多轮记忆）→ 49~51、54 图片召回/Text2SQL/Text2Cypher/钉钉机器人 → 52~61 工程化（版本管理/权限/缓存） | 按顺序系统学习，或按主题查某个机制的实现细节 |
| **interview/** | 从笔记提炼的面试题：难度分级（⭐/⭐⭐/⭐⭐⭐）+ 参考答案 + 验证方法 + 追问 + 来源双链 | 面试前突击、自测掌握程度；先看 [interview/README.md](./interview/README.md) 的复习路线 |
| **repo/know-engine/** | 完整可运行源码 | 对照笔记读实现、本地启动调试、二次开发。注意它是**独立 git 仓库**，有自己的提交历史 |

三者关系：**笔记讲 what/why，源码是 how，面试题用于自测**。笔记里提到的类名（如 `KnowEngineQueryTransformer`、`DatabaseChatMemoryStore`）都可以在 repo 中直接全局搜索定位。

## 快速上手路线

**想系统学习这个项目**：按笔记编号 1→61 顺序读，重点篇章：2（整体架构）、12~16（索引构建）、29~36（检索链路）、44~47（多轮对话）、52~61（工程化）。

**想跑起来看看**：进入 `repo/know-engine/`，环境要求 JDK 21+、MySQL 8、Redis 7、ES 8.x（开 KNN）、Neo4j 5.x（APOC）、MinIO、XXL-Job Admin 2.4+；改 `application.yml` 配置后 `mvn clean package -DskipTests` 启动，访问 `http://localhost:8009/chat.html`（对话页）和 `/upload.html`（文档管理页）。详细步骤见笔记 [4.项目如何启动&快速开始](./4.项目如何启动&快速开始.md) 和源码的 `STARTUP.md`。

**想准备面试**：直接看 [interview/README.md](./interview/README.md)，按「基础必答 → 原理追问 → 工程面试 → 踩坑复盘」路线刷题；同主题的通用 RAG 理论题见 [../RAG/interview/](../RAG/interview/README.md)，两边配合使用。

## 维护约定

- **笔记与面试题**：图片已本地化管理（放 `assets/`，命名如 `know-engine-XX-N.png`），引用用相对路径 `./assets/xx.png`；新增笔记请沿用「编号.标题.md」命名
- **面试题**：每题必须带来源双链（`[[AI项目/know-engine/笔记名]]`），答案忠实于笔记内容，笔记没有的标注「通用补充/设计建议」
- **源码**：`repo/know-engine/` 是独立 git 仓库，已被本库 `.gitignore` 忽略；拉取/切换分支需进入该目录单独操作，不要在本库提交源码变更
- 本目录属于学习资料库，笔记正文不随意修改（raw 层）；批注和提炼请写到 interview/ 或另行新建文件
