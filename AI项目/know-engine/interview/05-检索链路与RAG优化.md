# 五、检索链路与RAG优化
[返回题库目录](./README.md) · 更新：2026-10-21

> 覆盖 know-engine 检索链路全貌：LangChain4J RAG 五大组件与并行执行、问题改写、查询路由、ES 混合检索、父子分段、多级缓存、BGE-ReRanker 重排，以及路由错误、结构化结果重排失败两类故障的排查修复。

---

## Q1: LangChain4J 中一次 RAG 检索的完整流程是什么？各组件职责是什么？

**难度**: ⭐ 基础

**参考答案**:

**先说结论**：RAG 检索的核心入口是 `RetrievalAugmentor`——只有给 AIService 配置了它，AI 服务才会做检索。内部按「查询转换 → 路由 → 检索 → 聚合 → 注入」五步执行。

| 组件 | 作用 | 默认实现 |
|:---|:---|:---|
| QueryTransformer | 把用户原始查询转换成一个或多个新查询，提升检索质量 | 直接返回原查询 |
| QueryRouter | 按查询语义路由到最合适的一个或多个 ContentRetriever | 发到所有检索器 |
| ContentRetriever | 接收 Query，从底层数据源检索相关内容，返回按相关性排序的 `List<Content>`；**唯一必须由用户提供的组件** | / |
| ContentAggregator | 把所有 Query × 所有检索器的结果聚合、去重、排序，只留最相关且不冗余的内容 | RRF 融合排序 |
| ContentInjector | 把检索内容注入原始用户消息，构造增强提示词 | 用模板把内容追加到用户消息末尾：`{{userMessage}}\n\nAnswer using the following information:\n{{contents}}` |

执行流程（`DefaultRetrievalAugmentor.augment()`）：

```text
从 UserMessage 提取原始 Query
1. queryTransformer.transform()      查询转换
2+3. process()                       查询路由 + 内容检索（内部并行）
4. contentAggregator.aggregate()     内容聚合
5. contentInjector.inject()          内容注入
→ 返回 AugmentationResult（增强后的 ChatMessage + contents）
```

组装方式：`DefaultRetrievalAugmentor.builder()` 依次配置 queryRouter（如 DefaultQueryRouter 传入多个检索器）、queryTransformer、contentAggregator、contentInjector 后 build。

**验证**：不看资料能默写五个组件名与各自默认实现，并说清 ContentRetriever 是唯一必须自己实现的组件。

**追问**：
- 五个组件里哪个必须自己提供？ContentRetriever，其余都有默认实现。
- 多检索器结果默认怎么合？ContentAggregator 默认用 RRF 融合排序。

> 来源：[29.LangChain4J中的RAG执行流程](../29.LangChain4J中的RAG执行流程.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/chat/service/ChatApplicationService.java`

---

## Q2: 多个 Query × 多个检索器为什么要并行？有哪些并行方案，怎么选？

**难度**: ⭐⭐ 中级

**参考答案**:

**先说结论**：每个 Query × 每个 Retriever 的调用都是独立 IO（查 ES / SQL / Neo4j），串行会把延迟叠加，并行能把总耗时压到「最慢的那一个」。

五种并行方案对比：

| 方案 | 结论 |
|:---|:---|
| CompletableFuture | ✅ 推荐：JDK 原生无依赖，可组合（thenCombine / allOf），可自定义线程池；异常处理稍繁琐 |
| 虚拟线程 + StructuredTaskScope（JDK 21+） | 代码最直观、像同步写法，父子关系明确、自动取消；但属预览特性，需 `--enable-preview`，生产有风险 |
| Parallel Stream | ❌ 不推荐：默认 ForkJoinPool 线程数 = CPU 核数，为 CPU 密集设计；检索是 IO 密集，阻塞 IO 会占满公共线程池，且无法控制并发数 |
| ExecutorService + Future | 成熟稳定、可精确控制线程数；但代码冗长，`future.get()` 逐个等待，总耗时不如 allOf 高效 |
| Reactor / RxJava | 背压控制强、适合流式 SSE；但学习曲线陡、引入额外依赖，对简单并行来说过重 |

项目建议两条路径：保守方案 `CompletableFuture` + 自定义线程池（成熟稳定、无预览特性风险）；前沿方案 `StructuredTaskScope`（代码最直观，但需开启预览）。

**关键点**：LangChain4j 内部的 `DefaultRetrievalAugmentor.process()` 已经用 CompletableFuture 做了并行检索——只要把多个 Retriever 配进去，框架会自动并行，通常不需要自己手写并行逻辑，除非自定义了 RetrievalAugmentor。

（设计建议）为每个并行检索任务补充超时控制与失败降级，避免单个慢检索器拖垮整体请求；这一点笔记未展开，属通用补充。

**验证**：能说出 parallelStream 不适合 RAG 检索的原因（IO 密集 vs CPU 密集设计、无法控并发），并指出框架已内置并行。

**追问**：
- 框架默认就并行吗？是，DefaultRetrievalAugmentor.process() 已用 CompletableFuture，配多个 Retriever 即可。
- 并行后总耗时取决于什么？最慢的那一路检索，所以应对慢路径做监控与超时（设计建议）。

> 来源：[29.LangChain4J中的RAG执行流程](../29.LangChain4J中的RAG执行流程.md)。

---

## Q3: 项目里如何做问题重写？用了哪些策略？为什么要返回两个查询？

**难度**: ⭐⭐ 中级

**参考答案**:

**先说结论**：自定义 `KnowEngineQueryTransformer` 实现 QueryTransformer，用**一次 LLM 调用**组合多种改写策略、统一输出一个改写结果；`transform()` 当前返回单个改写后查询（`singletonList(compressedQuery)`，源码中保留了返回双查询 `List.of(compressedQuery, query)` 的注释代码）。

理论铺垫：改写策略分两类——1 对 1（回溯提示、富化）和 1 对多（分解、多样化）。实际不会每个方案各调一次 LLM（太慢），一般组合处理；先做 1 对 1，效果不好再上 1 对多。富化主要解决多轮对话问题（单独处理）。

落地 Prompt 组合了 5 种策略，逐一使用后给出统一结果；不需要改写则直接输出原问题：

| 策略 | 做法 |
|:---|:---|
| 简洁改写 | 删除无意义语气词、修饰词、日期编号；疑问句转陈述句，更适合搜索引擎检索 |
| 抽象概念改写 | 把具体细节问题转成更基础、更简洁、更抽象的表述（"刹车吱吱响很吵" → "车辆刹车异响故障排查"），体现回溯提示思想 |
| 错别字改写 | 纠正错别字和打成拼音的汽车术语（大小写不一样不算错别字） |
| 车型信息提取 | 车型标准化："特斯拉毛豆3" → "Tesla Model 3"，"比亚迪汉"保持不变 |
| 上下文补全 | 结合历史对话和最新提问，识别相关细节、术语和上下文，把问题重组为独立完整的查询 |

处理流程：① 发送进度事件通知前端"正在优化您的问题" → ② 从 Query metadata 提取历史对话记忆并格式化为 Prompt 上下文 → ③ LLM 按 Prompt 模板改写用户查询 → ④ 构造改写查询（保留原 Query 的 metadata，供后续路由/检索使用）→ ⑤ 用虚拟线程（`Thread.ofVirtual()`）异步回写改写结果到 chat_message 表 → ⑥ 返回改写后的单个查询。重点是改写、保存、传递三件事。

**为什么曾设计返回两个查询**：改写后的查询用于语义检索，原始查询用于精确匹配——两路都保留，交给后续路由和聚合处理。该双查询方案在源码中以注释形式保留（`// return List.of(compressedQuery, query);`），当前版本只返回改写后的查询。

**验证**：能说出"一次 LLM 调用组合多策略"而非多次调用，并能解释双查询设计（注释保留的方案）各自的用途与异步回写对 bad case 排查的价值。

**追问**：
- 为什么改写结果要回写数据库？把改写内容落到 chat_message，便于后期 bad case 排查。
- 改写一定更好吗？不保证，源码中保留了返回双查询的注释代码，可用原始查询兜底精确匹配（当前未启用）。

> 源码校核：笔记中的"Prompt 组合 4 种策略、`transform()` 返回 `List.of(改写后查询, 原始查询)` 两个 Query"在源码中实际为 5 种策略（多"上下文补全"）、返回单个改写查询 `singletonList(compressedQuery)`，双查询代码被注释保留；流程中还包含从 Query metadata 提取历史对话记忆一步。

> 来源：[30.如何针对用户问题做重写](../30.如何针对用户问题做重写.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineQueryTransformer.java`

---

## Q4: 查询路由是怎么实现的？支持路由到哪几种数据源？

**难度**: ⭐⭐ 中级

**参考答案**:

**先说结论**：`KnowEngineQueryRouter` 实现 QueryRouter，用 LLM 按预定义 Prompt 分析问题语义，输出 JSON 决策，再按 strategy 过滤返回对应的 ContentRetriever 集合。

三种数据源路由策略：

| strategy | 适用场景 | 对应检索器 |
|:---|:---|:---|
| relational_db | 结构化数据查询：车辆/保险/订单信息，查发动机号、下次保养时间、里程；含实体属性、时间范围、数值比较、SUM/COUNT/AVG 聚合 | SqlDatabaseContentRetriever（langchain4j-experimental-sql 模块，结合 DataSource 与 LLM 把自然语言转 SQL 执行并返回结果） |
| graph_db | 实体关系/路径/层级："纯电车型都有哪些？"、"型号 A 和型号 B 有什么关系"、最短路径、影响链 | Neo4jText2CypherRetriever |
| knowledge_base | 语义相似、模糊匹配、非结构化文本：售前售后、技术支持，"发动机异响怎么处理" | AbstractElasticsearchEmbeddingStore |

决策流程：LLM 分析语义 → Prompt 模板判断 → 返回对应 ContentRetriever 集合。要求严格按 JSON 输出：`{intent, strategy, reasoning, confidence}`，strategy 三选一。

代码行为：`JSON.parseObject(JsonUtil.fixJson(response))` 解析后 switch——relational_db / graph_db / knowledge_base 三个分支按 `instanceof` 过滤检索器（会先解包 ProgressAwareContentRetriever 取 delegate 再判断）；default 返回全部检索器；JSON 解析异常或其他异常时同样返回全部检索器（降级为全量检索，避免直接无结果）。

实测效果："我的车怎么加玻璃水" → ES 两个检索器；"我的车险什么时候到期" → SqlDatabaseContentRetriever；"问界L9混动版和纯电版哪些配置相同" → Neo4jText2CypherRetriever。

（设计建议）Prompt 要求模型输出 confidence（0–1、保留两位小数），但当前代码未用它参与分流；后续可作为低置信度时多源兜底检索的依据。

**验证**：能默写三个 strategy 字符串及各自对应的检索器类名，并说出路由失败时返回全部检索器的降级行为。

**追问**：
- 路由错了或失败了怎么办？路由解析失败返回全部检索器（全量检索兜底）；SQL/图检索器内部还有知识库兜底降级（见 Q12）。
- SqlDatabaseContentRetriever 的原理？结合 DataSource 和 LLM，把自然语言问题转成 SQL 查询、执行并返回结果。

> 源码校核：笔记中的"JSON 解析异常或其他异常时返回空列表（fixme 降级）"在源码中实际为返回全部 contentRetrievers（降级为全量检索，避免直接无结果）。

> 来源：[31.如何实现查询路由--检索器路由](../31.如何实现查询路由--检索器路由.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineQueryRouter.java`

---

## Q5: 为什么必须做混合检索？只用向量或只用 BM25 各有什么问题？

**难度**: ⭐⭐ 中级

**参考答案**:

**先说结论**：单独用向量检索或 BM25 都有明显短板，两者互补——向量懂语义但抓不住精确词，BM25 抓得住字面但不懂语义。

只用向量检索的问题：依赖语义相似度，对专有名词、精确短语、编号等缺乏精确匹配能力；极短查询语义上下文有限，召回内容过于宽泛。典型 case：「超级桌面」是汽车领域专有名词，但 3D 桌面、手机桌面、车机桌面、桌面 等相似词的文档得分都很高，导致检索效果差。

只用 BM25 的问题：关键词匹配是死板的字面匹配，无法理解同义词、语义表达变化和复杂自然语言意图。典型 case：用户问"手机能否启动汽车"，实际问的是汽车启停、无钥匙进入、智能座舱功能，按分词后的关键词检索不到想要的内容，就算检索到得分也不高。

为什么选 ES 做混合检索：项目用 ES 存知识，而 ES 同时支持向量检索和关键词检索，只需依赖 `langchain4j-elasticsearch` 一个扩展包。它提供两个类：ElasticsearchEmbeddingStore（EmbeddingStore 接口实现，用 ES 存储和检索嵌入向量）和 ElasticsearchContentRetriever（ContentRetriever 接口实现，基于向量相似度搜索检索文档）。

**验证**：能各举一个失败 case（超级桌面 / 手机启动汽车），并说出 langchain4j-elasticsearch 提供的两个核心类及分工。

**追问**：
- 哪类查询必须靠 BM25 补位？专有名词、产品型号、精确短语这类需要字面精确匹配的查询。
- 两路结果在哪里融合？在 LangChain4J 的 ContentAggregator 层融合（项目自实现方案，见 Q6）。

> 来源：[32.混合检索如何实现](../32.混合检索如何实现.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineElasticsearchContentRetriever.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/chat/service/ChatApplicationService.java`

---

## Q6: ES 开箱的混合检索为什么走不通？最终如何实现混合检索？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

**先说结论**：`ElasticsearchConfigurationHybrid` 依赖 ES 内置 RRF 重排序，开源版 License 不支持，直接报 license 异常；解法是用 `ElasticsearchConfigurationKnn` + `ElasticsearchConfigurationFullText` 构造两个检索器并行检索，把融合放到应用层 ContentAggregator。

报错现场：`[security_exception] current license is non-compliant for [Reciprocal Rank Fusion (RRF)]`。原因：ElasticsearchContentRetriever 在 Hybrid 配置下默认使用 ES 内置的 RRF 做重排序，该能力只有企业版支持，开源版直接报错——`ElasticsearchContentRetriever + ElasticsearchConfigurationHybrid` 这条路走不通，必须自己实现混合检索。

自定义实现三步：

```java
// 1. 全文（BM25）检索器
fullTextRetriever = ElasticsearchContentRetriever.builder()
    .restClient(restClient)
    .configuration(ElasticsearchConfigurationFullText.builder().build())
    .maxResults(MAX_RESULT).indexName(INDEX_NAME).minScore(MIN_SCORE)
    .build();

// 2. 向量（KNN）检索器
embeddingRetriever = ElasticsearchContentRetriever.builder()
    .restClient(restClient)
    .embeddingModel(openAiEmbeddingModel)
    .configuration(ElasticsearchConfigurationKnn.builder().build())
    .maxResults(MAX_RESULT).indexName(INDEX_NAME).minScore(MIN_SCORE)
    .build();

// 3. 两个检索器交给路由器，融合交给 ContentAggregator（RRF）
DefaultRetrievalAugmentor.builder()
    .queryRouter(new KnowEngineQueryRouter(List.of(embeddingRetriever, fullTextRetriever), chatModel))
    .contentInjector(contentInjector)
    .queryTransformer(queryTransformer)
    .contentAggregator(contentAggregator)
    .build();
```

即基于 KNN 做相似度检索、基于全文做关键词检索，两路结果由 LangChain4J 的 ContentAggregator（RRF）聚合，而不是依赖 ES 内部融合。

实际工程还要对向量检索器做四项定制：① 精确/近似 KNN 的选择；② 检索过滤；③ 父子分段/兄弟分段的替换与合并；④ 针对混合检索到的结果做重排序。

**验证**：能写出 RRF license 报错关键字，说明 ElasticsearchConfigurationFullText 与 ElasticsearchConfigurationKnn 的分工，并指出融合发生在应用层而非 ES 内部。

**追问**：
- 为什么不直接用 ES 内置混合检索？开源版不支持内置 RRF，报 license 异常；项目改为应用层聚合绕开。
- minScore 在这里的作用？过滤低分结果（项目配置 0.5），减少噪声进入聚合环节。

> 来源：[32.混合检索如何实现](../32.混合检索如何实现.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/controller/RagModuleController.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/chat/service/ChatApplicationService.java`

---

## Q7: ES 的近似 KNN 和精确 KNN 怎么选？HNSW 关键参数如何理解？

**难度**: ⭐⭐ 中级

**参考答案**:

**先说结论**：近似 kNN（HNSW）低延迟、高扩展，适合大规模数据的生产环境；精确 kNN（script_score 暴力遍历）适合小数据集。LangChain4J 里分别对应 ElasticsearchConfigurationKnn 和 ElasticsearchConfigurationScript。

ES 支持的两种 kNN 搜索方法：

| 维度 | 近似 kNN | 精确 kNN |
|:---|:---|:---|
| 实现方式 | `knn` 查询 DSL，底层默认 HNSW 图算法 | `script_score` + 向量函数（如 cosineSimilarity），暴力搜索遍历所有文档 |
| 性能 | 低延迟、高扩展，适合大规模 | 性能差，不适用于大数据 |
| 适用 | 生产环境推荐 | 数据量小、需 100% 精确结果、或配合强过滤条件缩小范围 |

ES 自动创建的索引中向量字段定义：

```json
"vector": { "type": "dense_vector", "dims": 1536, "index": true,
            "similarity": "cosine",
            "index_options": { "type": "int8_hnsw", "m": 16, "ef_construction": 100 } }
```

- `index: true`：开启高性能 kNN 搜索的"开关"，告诉 ES 为向量建立 HNSW 导航图以支持毫秒级近似搜索（ES 8.0+ 默认开启）；`similarity: cosine` 表示用余弦相似度。
- `int8_hnsw`：int8 是存储格式（每个数字转换为 8 位整数，只占 1 字节），hnsw 是索引结构。相比普通 float32 hnsw：内存约降到 25%、搜索更快（数据更紧凑、CPU 缓存命中率更高）、精度 ~99%+（几乎无感知损失），是 8.x 默认推荐，解决内存成本痛点。
- `m`：HNSW 图中每个节点的邻居数量，默认 16；越大召回率相应提高，但对性能影响较大并增加内存，严格要求可设 64 或更大。
- `ef_construction`：插入新向量建索引时候选池的大小，默认 100；越大图结构更合理、未来搜索更容易找到真正最近邻（召回率更高），但索引构建时间显著变长，严格要求可设 512 或更大。

选型落地：更看重精确度 → ElasticsearchConfigurationScript（效率差一些）；更看重速度、可适当牺牲精度 → ElasticsearchConfigurationKnn。

**验证**：能说出 int8_hnsw 的内存收益（约 25%）与 m / ef_construction 增大对召回、性能、构建时间的方向性影响。

**追问**：
- index:true 是什么开关？告诉 ES 为向量建 HNSW 导航图，以便毫秒级速度进行近似搜索。
- m 越大一定越好吗？不是，召回提高的同时内存与性能代价增大，需按需设置。

> 来源：[33.自定义基于ElasticSearch的向量检索器](../33.自定义基于ElasticSearch的向量检索器.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/chat/service/ChatApplicationService.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/rag/config/ElasticSearchConfiguration.java`

---

## Q8: 召回率差时按什么顺序优化？为什么要自定义 ES 向量检索器？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

**先说结论**：优化顺序遵循「先易后难、先外后内、性能优先」：默认用近似 KNN（HNSW）检索，召回率差先优化提示词、分段、查询重写、多路召回/混合检索，再调 m 和 ef_construction 两个参数，最后才考虑精确 KNN。自定义检索器是因为除了检索本身，还要完善父子分块的检索处理。

工程实践顺序（笔记原文总结）：

- **先易后难**：从最简单的提示词开始
- **先外后内**：先优化检索层之外的环节（提示词、分段、查询重写、多路召回与混合检索）
- **性能优先**：影响性能的"重操作"放最后——先调 m、ef_construction 两个索引参数，最后才用精确 KNN（暴力遍历、影响性能）

自定义 `KnowEngineElasticsearchContentRetriever`（继承 AbstractElasticsearchEmbeddingStore、实现 ContentRetriever）的定制点：

1. 按配置支持三种模式：FullText 直接执行全文搜索返回；Hybrid 走向量+全文混合搜索；默认 KNN 向量检索（查询文本先 embed，构造 EmbeddingSearchRequest 带 maxResults / minScore / filter）。
2. 检索后处理：结果去重排序 → **兄弟分段扩展**（读 metadata 的 brotherChunkId，按它过滤检索同一父分段下的其他兄弟分段补全上下文；brotherDocMap 缓存避免同 ID 重复查 ES）→ **父分段替换**（读 parentChunkId，从 KnowledgeSegmentService 取父分段完整文本，构造新 Content 替换子分段；Redis 查不到则记 warn 并移除该子分段）。
3. mapResultsToContentList 按 minScore 过滤，并为每个 Content 携带 SCORE、EMBEDDING_ID 元数据。

**验证**：能复述三条优化原则的先后顺序，并指出自定义检索器至少两个后处理定制点（兄弟分段扩展、父分段替换）。

**追问**：
- 为什么最后才考虑精确 KNN？它是暴力遍历、影响性能，属于"性能优先"原则中放到最后的重操作。
- 兄弟分段扩展解决什么问题？命中子分片时把同一父分段下的兄弟分片一并补进上下文，获得更完整上下文。

> 来源：[33.自定义基于ElasticSearch的向量检索器](../33.自定义基于ElasticSearch的向量检索器.md)、[34.父子分段的切分和检索逻辑](../34.父子分段的切分和检索逻辑.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineElasticsearchContentRetriever.java`

---

## Q9: 父子分段是怎么切分的？检索和展示各用什么？

**难度**: ⭐⭐ 中级

**参考答案**:

**先说结论**：为解决传统固定长度切分把完整语义单元（一段说明、一个步骤）切成两半的问题，采用「标题分段 + 父子分段」：检索时用小分片定位，返回展示时用父分段呈现完整内容。

```text
传统方式：           本设计：
├─ 分段1(前500字)     ├─ 分段1-完整内容(父分段)
├─ 分段2(后500字)     │   ├─ 分段1a(子分片，用于检索)
                     │   └─ 分段1b(子分片，用于检索)
                     └─ 分段2-完整内容
```

切分逻辑（`MarkdownHeaderParentTextSplitter`，基于 Markdown 标题层级分段并建立父子关系）三步：

1. **标题检测与元数据维护**：用 headerStack 维护当前标题层级；遇到新标题时把之前累积的内容保存为一个分段；每个分段生成唯一 chunkId。
2. **聚合相同元数据的行**：相同标题路径（相同元数据）的行合并为一个分块；标题与其下第一段内容合并。
3. **chunkSize 二次切割**：超出 chunkSize 的分段——保留完整分片并标记 `skipEmbedding=1`（不生成向量、不用于向量检索）；再切成多个子分片，每个子分片携带 parentChunkId 建立父子关系。

检索逻辑（`KnowEngineElasticsearchContentRetriever`）：检索到 chunk 之后，检查结果是否携带 parentChunkId → 有则读取父分段的完整文本 → 用父分段内容替换子分段，获得更完整的语义。

两个阶段的分工：检索阶段小分片做向量匹配，精度高、能定位到具体知识点；展示阶段用父分段替换子分片，内容完整、用户看到完整上下文。

**验证**：能说出 skipEmbedding=1 的含义（父分段不参与向量检索）与 parentChunkId 的作用（检索后回溯父分段替换展示）。

**追问**：
- 父分段会参与向量检索吗？不会，标记 skipEmbedding=1，不生成向量。
- 为什么检索用小分片而不用父分段？小分片向量匹配精度高、能定位到具体知识点，父分段只负责展示完整上下文。

> 源码校核：笔记中的"KnowEngineElasticsearchHnswContentRetriever / KnowEngineElasticsearchKnnContentRetriever"在源码中实际为统一的 `KnowEngineElasticsearchContentRetriever`（按 configuration 区分 KNN / FullText / Hybrid 模式）。

> 来源：[34.父子分段的切分和检索逻辑](../34.父子分段的切分和检索逻辑.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/splitter/MarkdownHeaderParentTextSplitter.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineElasticsearchContentRetriever.java`

---

## Q10: chunk 检索的多级缓存是怎么设计的？为什么不引入 Caffeine？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

**先说结论**：两级缓存——检索器内的方法级 Map（本地缓存）+ Redis，解决「一次检索中多个子分段命中同一个父分段」导致的重复查询。

背景：父子分块下，一般一整个段落的内容相互关联，语义检索时多个子分段同时被召回、且对应同一个父分段的概率较高；不加缓存就会对同一个分段反复查数据库。

第一级（本地 Map，单次请求内）：检索器中维护 `parentDocMap`（HashMap），方法执行结束即释放。父分段已处理过就直接跳过（同时从结果中移除重复子分段），避免同一次检索内重复加父分段；兄弟分段同理用 brotherDocMap 缓存。

第二级（Redis，跨请求）：`KnowledgeSegmentService.getTextByChunkId()`——先 Redis `get(chunkId)`，命中直接返回；未命中按 chunk_id 查数据库（`getOne`），查到则回写 Redis 再返回；数据库也没有时**缓存空值 `""`，避免缓存击穿**（同一个不存在的 chunkId 反复穿透到数据库）。

为什么不用 Guava Cache / Caffeine：要解决的是"同一次检索内多个子分段对应同一个父分段"的问题，跨请求共享的本地缓存帮助不一定大；真要用也可以，只要设计好淘汰策略，还能在多次不同请求间共享缓存数据。

数据一致性边界：源码中子分段文本变更时会同步更新父分段文本，并主动删除父分段的 Redis 缓存（`syncParentSegmentText` → `stringRedisTemplate.delete(parentChunkId)`）；同时 Redis 中命中的文本缓存带 30 秒 TTL，进一步缩短脏数据窗口。

**验证**：能画出"Map → Redis → DB"的查找顺序，说出缓存空值防击穿、方法级 Map 用完即释放这两个细节。

**追问**：
- 为什么要缓存空值？避免不存在的 chunkId 每次都穿透到数据库重复查询。
- chunk 更新后会脏读吗？不会，子分段变更会同步更新父分段并主动清除父分段的 Redis 缓存，且缓存本身有 30 秒 TTL。

> 源码校核：笔记中的"chunk 变了会生成新的 chunkId、旧 key 自然失效"在源码中实际为子分段变更时同步更新父分段文本并主动删除 Redis 缓存（`syncParentSegmentText`），且命中缓存设 30 秒 TTL。

> 来源：[35.多级缓存实现chunk的快速检索](../35.多级缓存实现chunk的快速检索.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineElasticsearchContentRetriever.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/document/service/impl/KnowledgeSegmentServiceImpl.java`

---

## Q11: 如何用 BGE-ReRanker 在本地做重排序？默认聚合器有什么不足？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

**先说结论**：DefaultContentAggregator 只做 RRF 融合、会把全部检索结果都给 LLM（如向量 5 条 + 全文 5 条 = 10 条，模型根本不需要这么多）；ReRankingContentAggregator 先 RRF 融合再用 ScoringModel 精排过滤。项目用 ONNX Runtime 在 JVM 内本地跑 bge-reranker-v2-m3。

两个聚合器的分工：

- **DefaultContentAggregator**：只做融合（Fusion）。RRF 不关心检索器返回的原始相似度分数，只关注文档在各自结果列表中的排名位置——排名靠前权重更高，同一文档跨列表权重相加得综合排名。关键问题：只融合不过滤，所有结果全给 LLM。
- **ReRankingContentAggregator**：先像默认实现一样用 RRF 融合，再调用配置的 ScoringModel（如 Jina、Cohere、BGE 等）对每个内容片段与查询的相关性精确评分，按新分数重排并过滤低于阈值的内容。官方已集成部分模型；更适合中文的 BGE-Reranker、Qwen3-Reranker 需要自己定制开发。

模型选型（bge-reranker 三个版本）：

| 版本 | 定位 |
|:---|:---|
| bge-reranker-v2-m3（项目选择） | 全能首选：性价比最高，体积小、速度快，原生支持超 100 种语言的跨语言检索 |
| bge-reranker-large | 中文专精：精度略高，但模型更大、推理更慢，适合纯中文 + GPU 资源充足 |
| bge-reranker-base | 轻量入门：资源占用最小，性能弱的机器甚至 CPU 可流畅运行，适合延迟敏感或资源受限场景 |

从 ModelScope 下载 `model_quantized.onnx`（量化版：体积小、推理快、精度损失极小）和 `tokenizer.json`（分词器核心文件，含词汇表和分词算法，把句子切分成模型能看懂的数字 ID），放到 classpath 目录。

OnnxScoringModel 关键参数：

- **modelPath**：ONNX 模型文件路径，推荐量化版 model_quantized.onnx
- **tokenizerPath**：HuggingFace 格式 tokenizer.json，必须与模型配套
- **modelMaxLength**：单个文本最大 token 数，需与模型一致（查 tokenizer_config.json 的 model_max_length）；v2-m3 为 8192，large / base 为 512
- **normalize**：`true` 把原始 logits 通过 Sigmoid 归一化到 (0,1)，便于设置统一 minScore 阈值；`false` 直接用原始分数（可能是 8.66、-11.24 这类任意实数），需按实际分数分布设定过滤阈值
- **sessionOptions**：默认 CPU 运行；可调线程数与优化级别，有 NVIDIA GPU 可 `addCUDA(0)` 启用 CUDA 加速

工程实现：`BgeScoringModel` 用 volatile + synchronized 双重检查锁做单例；classpath 资源在 JAR 包内时复制到临时文件再加载；依赖 `langchain4j-onnx-scoring` + `onnxruntime`（排除其默认传递版本，显式引入兼容版本）。

**常见问题**：若问题改写环节返回多个 Query（如源码注释中保留的双查询方案 `List.of(compressedQuery, query)`；当前 `KnowEngineQueryTransformer` 只返回单个改写查询），ReRankingContentAggregator 不知道用哪个做重排，抛 "re-ranking ambiguous" 异常。解法：builder 加 `querySelector` 指定参考查询——`keySet().iterator().next()` 取第一个（改写后的查询）；要按原始查询重排则取列表最后一个。通常用改写后的查询重排效果更好：更精准、噪声更少。生产链路（ChatApplicationService）已配置 querySelector 取第一个 Query，且 scoringModel 不可用时降级为 DefaultContentAggregator。

**验证**：能说出两个聚合器的差异、normalize 两种取值对阈值设定的影响，以及多查询场景为什么要 querySelector。

**追问**：
- 为什么选 v2-m3？体积小速度快、原生支持 100+ 语言，性价比最高。
- 多查询时用哪个问题重排？querySelector 指定；通常用改写后的查询，更精准、噪声更少。

> 源码校核：笔记中的"aggregate 入参含两个 Query（`List.of(compressedQuery, query)`）"在源码当前版本中实际为单个查询（双查询代码已注释），querySelector 配置仍保留；另笔记称模型文件从 ModelScope 下载，源码注释标注的下载源为 HuggingFace（onnx-community/bge-reranker-v2-m3-ONNX）。

> 来源：[36.使用BGE-ReRanker针对检索结果做重排序](../36.使用BGE-ReRanker针对检索结果做重排序.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/reranker/BgeScoringModel.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineReRankingContentAggregator.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/chat/service/ChatApplicationService.java`

---

## Q12: SQL/图这类结构化检索在链路里有哪些坑？如何做兜底与重排适配？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

**先说结论**：两类坑——① 模型幻觉导致路由错误，需要在结构化检索器内做知识库兜底降级；② 结构化结果不能进 RRF 融合和 BGE 重排，需要 skipRerank 标记 + 混合聚合器分离透传。

**坑 1：路由错误导致问答失败**。模型存在幻觉，可能把该路由到知识库的问题路由到了关系型数据库或图数据库。兜底策略：在 `KnowEngineSqlDatabaseContentRetriever` 和 `KnowEngineNeo4jContentRetriever` 中都做降级——检索报错 catch 异常后返回 `fallbackRetriever.retrieve(query)`（走知识库再检索一次）；结果为空（null / isEmpty / 结构上空结果判定 isSqlResultEmpty）同样降级知识库。目的：尽量减少拒答的情况。

**坑 2：非向量检索导致重排序失败/效果异常**。混合场景下，ES 向量/全文检索返回带相似度分数的文本片段，SQL/Cypher 检索器返回精确的表格数据；后者进入 `KnowEngineReRankingContentAggregator` 做 RRF 融合 + BGE 重排时会出三个问题：

1. **评分模型不适用**：BGE scoring model 是为文本相似度设计的，对 SQL/Cypher 返回的结构化表格数据评分不准确
2. **RRF 融合不合理**：结构化查询结果是精确匹配，不需要与向量检索结果做倒数秩融合
3. **去重报错**：代码基于 chunk 的 embedding_id 做去重，结构化查询结果没有这个字段，会报错

解法：设计基于元数据标记的混合聚合架构，让结构化结果跳过重排/融合、直接透传给 LLM：

1. 定义常量 `SKIP_RERANK = "skipRerank"`
2. SQL/Cypher 检索器对**成功结果**打标记（`ContentUtil.markAsSkipRerank`）；异常或空结果降级到 fallbackRetriever 的结果不带标记（要参与正常重排）
3. `KnowEngineHybridContentAggregator` 三步：第一步遍历所有结果按 skipRerank 标记分离（结构化直接收集，非结构化重新组装 queryToContents）；第二步只对非结构化结果调用原聚合器做 RRF + BGE 重排序；第三步合并返回——**结构化在前，非结构化在后**
4. 组装链：`ProgressAwareContentAggregator( KnowEngineHybridContentAggregator( KnowEngineReRankingContentAggregator(scoringModel, minScore 0.6, maxResults 5, querySelector 取第一个 Query) ) )`

**验证**：能说出三个重排失败原因，描述 skipRerank 标记从检索器到混合聚合器的完整流向，以及兜底触发的两个条件（异常、空结果）。

**追问**：
- 兜底降级的目标是什么？尽量减少拒答——SQL/图检索失败或为空时再走知识库检索一次。
- 结构化结果在最终上下文里排什么位置？合并时结构化在前、非结构化在后。

> 来源：[57.解决路由错误导致问答失败问题](../57.解决路由错误导致问答失败问题.md)、[58.解决针对非向量检索导致重排序失败问题](../58.解决针对非向量检索导致重排序失败问题.md)。
> 源码：`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineSqlDatabaseContentRetriever.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/KnowEngineHybridContentAggregator.java`、`src/main/java/cn/hollis/llm/mentor/know/engine/rag/modules/ContentUtil.java`
