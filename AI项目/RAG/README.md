# AI 项目 · RAG 系列

> 从"什么是 RAG"到"多模态实战"，**38 篇完整覆盖 RAG 全链路**：基础原理 → 分片策略 → 向量检索 → 优化全家桶 → 框架对比 → 多模态 + Agentic。面向 Java 技术栈（Spring AI / LangChain4J），所有文件已在头部插入 markmap 可交互脑图。

---

## 📁 目录结构

```
RAG/
├── README.md              ← 本文件（接手导航）
├── 0.开篇词.md            ← 全局脑图 + 阅读路线（最先读这篇）
├── 1-5                    ← 第一部分：基础概念与范式演进
├── 8-11.5                 ← 第二部分：文档处理与分片策略
├── 12-16                  ← 第三部分：向量模型与检索算法
├── 17-28                  ← 第四部分：ETL 总结 + RAG 优化全家桶
├── 29-32, 37-38           ← 第五部分：Modular RAG 框架与对比 + Agentic
├── 33-36                  ← 第六部分：多模态 RAG 实战
├── assets/                ← 本文引用的图片资源
├── interview/             ← 面试题集（6 大模块，66 题，含答案 + 难度标签）
└── localize_images.py     ← 工具脚本：图片本地化
```

---

## 🎯 快速上手：按目标选路线

| 目标 | 推荐路线 | 预计用时 |
|:---|:---|:---|
| **快速总览** | 0.开篇词（脑图 + 阅读路线） | 10 min |
| **面试速成** | 01 什么是 RAG → 05 范式演进 → 09 父子分片 → 27 重排序 → 25 HyDE → interview/ 全刷 | 2-3 天 |
| **落地一套 RAG** | 01 → 02 → 08/09 分片选型 → 12 Embedding → 14 向量库选型 → 19 基础增强 → 20+21+27 三件套 | 3-5 天 |
| **框架选型** | 29 Spring AI → 30 LangChain4J → 32 对比 → 37/31 实战代码 | 1 天 |
| **多模态** | 33 原理 → 35 MinIO → 36 文档处理 → 34 完整 Demo | 2 天 |
| **上线前体检** | 👉 移步兄弟项目：[AI项目/RAG评测/](../RAG评测/)（28 篇完整评测体系） | 另算 |

---

## 📖 文件清单（按章节分组）

### 第一部分：基础概念与范式演进（1-5）

| 文件 | 说明 | 难度 |
|:---|:---|:---|
| [0.开篇词](0.开篇词.md) | **全站脑图 + 阅读路线**，6 棵子树覆盖所有章节 | — |
| [1.什么是 RAG](1.什么是RAG.md) | RAG 定义 / 解决什么问题 / 使用场景 / 流程总览 | ⭐ |
| [2.深入理解——索引构建](2.深入理解RAG技术原理——索引构建.md) | 离线链路：预处理 → 分片 → 向量化 → 建索引 | ⭐⭐ |
| [3.深入理解——检索生成](3.深入理解RAG技术原理——检索生成.md) | 在线链路：用户提问 → 召回 → 增强生成 | ⭐⭐ |
| [4. LlamaIndex Demo](4.使用%20LlamaIndex%20构建一个简单的RAG系统.md) | 快速验证用 | ⭐ |
| [5.范式演进](5.RAG的范式演进.md) | Naive → Advanced → Modular + Agentic/Graph/Self-RAG/CRAG/RAG-Fusion/Long-Context/LLM Wiki 全景 | ⭐⭐⭐ |

### 第二部分：文档处理与分片策略（8-11.5, 17, 18）

| 文件 | 说明 | 难度 |
|:---|:---|:---|
| [8.分片代码实现](8文档分片的代码实现.md) | Spring AI / LangChain4J 的 Splitter 实现对比 | ⭐⭐ |
| [9.父子分片](9.什么是父子分片.md) | **重点**：父子块架构原理 + 优缺点 + 工程挑战 | ⭐⭐⭐ |
| [10. Markdown 标题分片器（选学）](10.（选学）自定义支持父子分片的基于Markdown标题的分片器.md) | LangChain 参考实现 | ⭐⭐ |
| [11. Word 标题分片器（选学）](11.（选学）自定义支持父子分片的基于Word标题的分片器.md) | Word 文档分片 | ⭐⭐ |
| [11.5 语义识别父子分片](11.5自定义语义识别的父子分片.md) | 占位文件，暂无内容 | — |
| [17. ETL 总结](17.索引构建流程（ETL）总结与查缺补漏.md) | Spring AI Transformer 三件套 + ETL Pipeline | ⭐⭐ |
| [18. 文档处理功能对比](18.Spring%20AI%20和%20LangChain4J中文档处理功能对比.md) | 两框架 Splitter / Transformer 对照表 | ⭐⭐ |

### 第三部分：向量模型与检索算法（12-16）

| 文件 | 说明 | 难度 |
|:---|:---|:---|
| [12.向量模型 & 向量库 & 向量存储](12.向量模型&向量数据库&向量存储.md) | DashScopeEmbeddingModel / PgVector 实现原理 | ⭐⭐ |
| [13. PostgresML Embedding（选学）](13.（选学）PostgresMlEmbeddingModel.md) | pgml 扩展 + 数据库内运行模型 | ⭐⭐⭐ |
| [14.向量库选型](14.向量是数据库如何选型.md) | PgVector / Milvus / Qdrant / Chroma / ES 对比表 | ⭐⭐⭐ |
| [15.离线向量模型](15.如何使用离线向量模型.md) | BGE + Ollama 私有化部署 | ⭐⭐ |
| [16.相似度搜索算法](16.相似度搜索的常见算法.md) | KNN → IVF → **HNSW** → LSH 原理 + 选型 | ⭐⭐⭐ |

### 第四部分：RAG 优化全家桶（19-28）

| 文件 | 说明 | 难度 |
|:---|:---|:---|
| [19.检索增强生成](19.检索增强生成.md) | QuestionAnswerAdvisor 基础用法 | ⭐⭐ |
| [20.元数据过滤](20.RAG优化技术：元数据过滤.md) | 多版本 / 多租户 / 权限隔离 / 引用来源 | ⭐⭐ |
| [21.问题改写](21.RAG优化技术：问题改写.md) | 富化 / 分解 / 多样化 / 回溯 | ⭐⭐ |
| [22.查询路由](22.RAG优化技术：查询路由.md) | 数据源路由 / 领域路由 | ⭐⭐⭐ |
| [23.查询构造](23.RAG优化技术：查询构造.md) | Text2SQL / Text2Cypher | ⭐⭐⭐ |
| [24.问题澄清](24.RAG优化技术:问题澄清.md) | 指代消解 / 歧义检测 / 信息收集 | ⭐⭐ |
| [25. HyDE](25.RAG优化技术：HyDE.md) | 让 LLM 先编造答案再检索 → 解决词汇不匹配 | ⭐⭐⭐ |
| [26. ES 关键词检索](26.使用%20ElasticSearch%20做关键词检索.md) | IK 分词器 + 混合检索 | ⭐⭐ |
| [27.重排序](27.RAG优化技术：重排序.md) | Bi vs Cross-Encoder + RRF 公式 + 两阶段架构 | ⭐⭐⭐ |
| [28. Graph RAG](28.RAG优化技术:Graph%20RAG.md) | 知识图谱 + 社区摘要 + 全局推理 | ⭐⭐⭐ |

### 第五部分：Modular RAG 框架与 Agentic（29-32, 37, 38）

| 文件 | 说明 | 难度 |
|:---|:---|:---|
| [29. Spring AI Modular](29.Spring%20AI中的Modular%20RAG支持.md) | RetrievalAugmentationAdvisor + Advisor 模式 | ⭐⭐ |
| [30. LangChain4J Modular](30.LangChain4J中的Modular%20RAG支持.md) | RagChain + 组件全对照 | ⭐⭐⭐ |
| [31. LangChain4J 简单 Demo](31.基于LangChain4J实现简单的RAG检索.md) | DefaultRetrievalAugmentor 上手 | ⭐⭐ |
| [32. 两框架 Modular 对比](32.Spring%20AI%20和%20LangChain4J的模块化RAG功能对比.md) | 查询路由 / 混合检索 / 重排序 等维度对比 | ⭐⭐⭐ |
| [37. RetrievalAugmentationAdvisor 实战](37.检索增强生成.md) | 最小定制：documentRetriever + queryAugmenter | ⭐⭐ |
| [38. Agentic RAG](38.Agentic%20RAG.md) | Generic RAG vs Agentic RAG 区别与适用场景 | ⭐⭐⭐ |

### 第六部分：多模态 RAG 实战（33-36）

| 文件 | 说明 | 难度 |
|:---|:---|:---|
| [33.多模态 RAG 原理](33.多模态RAG.md) | 图片/表格/公式怎么救回来 + PdfMultimodalProcessor | ⭐⭐⭐ |
| [34.多模态问答系统实战](34.实战：多模态RAG智能问答系统.md) | 完整工程链路 | ⭐⭐⭐ |
| [35. MinIO 接入](35.MinIO接入.md) | 占位文件，暂无内容 | — |
| [36.多模态文档处理串联](36.多模态文档处理.md) | image2Text() 真实多模态模型 + 坐标保序 | ⭐⭐⭐ |

---

## 🧰 配套资源

| 资源 | 位置 | 说明 |
|:---|:---|:---|
| **面试题集** | [interview/](interview/README.md) | 6 大模块 66 题，含难度标签 + 参考答案 + 源文档引用 |
| **RAG 评测系列** | [AI项目/RAG评测/](../RAG评测/0.%E5%BC%80%E7%AF%87%E8%AF%8D.md) | 28 篇完整评测体系（指标 / RAGAS / 数据集 / Bad Case 逐个击破） |
| **脑图插件** | VitePress `vitepress-plugin-legend` | 全站每个文件头部都有 markmap 可交互脑图 |
| **图片本地化脚本** | `localize_images.py` | 把远程图片下载到本地 assets/ 并改写链接 |

---

## 🧱 技术栈速查

| 层 | 选型 | 备注 |
|:---|:---|:---|
| RAG 框架 | Spring AI / LangChain4J | 全文以 Java 为主 |
| Embedding | 云端：DashScope text-embedding-v4<br>本地：BGE + Ollama | 中文场景优先中文 Embedding |
| 向量库 | PgVector（MVP）→ Milvus（生产）→ ES（混合检索） | 选型见 [14 号](14.向量是数据库如何选型.md) |
| 向量检索 | HNSW（综合最优）/ IVF_PQ（内存吃紧） | 原理见 [16 号](16.相似度搜索的常见算法.md) |
| 关键词检索 | Elasticsearch 8.x + IK 分词器 | 与向量库混合检索 |
| 多模态模型 | qwen-vl-plus（阿里云百炼） | 图片转描述 |
| 对象存储 | MinIO | 原始文档长期存档 |
| 关系库 | MySQL / PostgreSQL | 父子分片场景存父块 |

---

## 🔗 相关笔记（跨目录）

- [AI应用开发/3. RAG检索增强生成](../AI应用开发/3.%20RAG检索增强生成.md) — RAG 本体总览
- [AI应用开发/6.1 讲下一次 RAG 查询底层发生了什么事情](../AI应用开发/6.1%20讲下一次RAG查询底层发生了什么事情.md) — 查询链路拆解
- [AI应用开发/13. LLM 安全与工程化](../AI应用开发/13.%20LLM%E5%AE%89%E5%85%A8%E4%B8%8E%E5%B7%A5%E7%A8%8B%E5%8C%96.md) — LLM-as-Judge 双引擎回归
- [RAG评测/0.开篇词](../RAG评测/0.%E5%BC%80%E7%AF%87%E8%AF%8D.md) — 上线前怎么体检

---

## 📝 备注

- 文件头部的 markmap 脑图由脚本自动生成 + 部分手工补全，38 个文件中覆盖 **34 个**（2 个为空文件、2 个重复）
- 所有 RAG 系列文档已部署到 VitePress 站点，`interview/` 目录和脑图同样自动渲染
- 评测问题 / Bad Case 修复等上线质量问题 → 移步兄弟目录 `AI项目/RAG评测/`
