# RAG 如何与大模型集成

> 已有 [RAG检索增强生成.md](RAG检索增强生成.md) 聚焦**检索策略层**（混合检索/切片/重排），本篇聚焦**集成架构层**——检索到的内容怎么注入 Prompt、怎么管理 Token 预算、怎么做流式调用、怎么处理异常降级、Spring AI/LangChain4j 怎么封装。
> 一句话区分：**那篇讲「怎么搜到对的内容」，这篇讲「搜到后怎么喂给大模型」**。

---

## 一、RAG 集成的整体 Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                  RAG ↔ LLM 集成全链路 Pipeline                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户 Query                                                      │
│      │                                                           │
│      ▼                                                           │
│  [1] Query 预处理                                                │
│      ├─ 分级（P0-P4：是否需要检索、检索什么）                     │
│      ├─ 改写（同义词、缩写展开、多轮对话补全）                     │
│      └─ 检测（Prompt 注入、越权意图）                            │
│      │                                                           │
│      ▼                                                           │
│  [2] 检索（Retrieval）—— 详见 RAG检索增强生成.md                  │
│      ├─ 向量检索 + 关键词检索（双路）                            │
│      ├─ Cross-Encoder 重排                                       │
│      └─ 回调父级章节补全上下文                                    │
│      │                                                           │
│      ▼                                                           │
│  [3] Context 构建（集成核心，本篇重点）                           │
│      ├─ Chunk 格式化（XML/Markdown/标签）                        │
│      ├─ Token 预算分配（context window 切分）                    │
│      ├─ 上下文压缩（超长截断/摘要/去重）                          │
│      └─ 引用标记（为溯源做准备）                                  │
│      │                                                           │
│      ▼                                                           │
│  [4] Prompt 组装                                                 │
│      ├─ System Prompt（角色 + 规则 + 格式约束）                   │
│      ├─ Context（检索结果，带引用）                              │
│      ├─ User Query（原始问题）                                   │
│      └─ Few-shot（可选，示例）                                   │
│      │                                                           │
│      ▼                                                           │
│  [5] LLM 调用                                                    │
│      ├─ 同步 / 流式 SSE / 批量                                   │
│      ├─ temperature（RAG 场景统一 0~0.3）                        │
│      └─ 超时/重试/降级                                           │
│      │                                                           │
│      ▼                                                           │
│  [6] 输出后处理                                                  │
│      ├─ 引用校验（溯源是否有效）                                  │
│      ├─ 事实一致性校验（防幻觉）                                  │
│      ├─ PII 脱敏                                                 │
│      └─ 格式修复（Markdown/JSON 校正）                           │
│      │                                                           │
│      ▼                                                           │
│  返回用户（带引用来源）                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

本篇聚焦 **[3] Context 构建 → [4] Prompt 组装 → [5] LLM 调用 → [6] 输出后处理** 四个集成环节。

---

## 二、Context 构建：检索结果怎么喂给 LLM

### 2.1 Chunk 格式化策略

检索到的 chunk 不能直接拼接，需要**格式化**成 LLM 容易理解的结构：

| 格式 | 示例 | 优点 | 缺点 |
|:---|:---|:---|:---|
| **XML 标签** | `<doc source="文档1" section="§3.2">内容</doc>` | 结构清晰，LLM 容易识别边界，引用方便 | token 开销大 |
| **Markdown 分隔** | `### [文档1 §3.2]\n内容\n---` | 人类可读，LLM 熟悉 Markdown | 边界可能混淆 |
| **编号 + 元信息** | `[1] (文档1 §3.2, 相关度 0.92)\n内容` | 引用简单，可溯源 | 元信息占 token |
| **纯文本拼接** | `内容1\n\n内容2` | 省 token | LLM 难以区分边界，引用困难 |

**AgentMate 实战（推荐）**：XML 标签 + 编号混合

```xml
<context>
  <doc id="1" source="MSE微服务文档" section="§3.2 网关协议">
    MSE 网关支持 HTTP、gRPC、Dubbo 三种协议。Dubbo 协议需要在配置中开启 rpc-mode。
  </doc>
  <doc id="2" source="MSE微服务文档" section="§3.4 Dubbo配置">
    rpc-mode 参数可选值：dubbo、tri。默认值为 dubbo。
  </doc>
</context>
```

**为什么用 XML**：
- LLM 对 XML 结构理解好，能明确区分「这是检索内容」vs「这是用户问题」
- `id` 字段方便引用溯源（LLM 输出 `[doc:1]` 即可标记来源）
- `source` + `section` 让引用可点击跳转

### 2.2 Token 预算管理（最容易踩的坑）

context window 是有限资源，需要**精确分配**：

```
LLM context window（以 Qwen-72B 为例，32K tokens）
├─ System Prompt：~500 tokens（角色 + 规则 + 格式约束）
├─ Few-shot 示例：~1000 tokens（如果有）
├─ User Query：~200 tokens（原始问题 + 历史对话）
├─ 预留输出空间：~4000 tokens（max_tokens）
└─ 可用于检索 Context 的预算：32000 - 500 - 1000 - 200 - 4000 = 26300 tokens
```

**预算不足时的处理策略**：

| 策略 | 原理 | 适用场景 |
|:---|:---|:---|
| **截断** | 按相关度排序，只取 Top-N 直到预算用完 | 简单粗暴，可能丢关键信息 |
| **摘要压缩** | 用小模型先对 chunk 摘要，再拼进 context | chunk 长但信息密度低 |
| **Rerank 后精选** | Cross-Encoder 重排后只取 Top-K（K=3~5） | 最常用，效果好 |
| **Map-Reduce 总结** | 先对每个 chunk 单独总结，再汇总 | 文档极长，需要全局视角 |
| **父级回调** | 优先取父章节，保证上下文完整 | 表格/代码块被切断时 |

**AgentMate 实战**：两阶段召回天然解决预算问题

```
向量粗排 Top-20 → Cross-Encoder 重排 Top-3（控制在 ~3000 tokens）
→ 回调父级章节补全（但截断到预算上限）
→ XML 格式化后注入 Prompt
```

### 2.3 上下文压缩与去重

检索结果可能有**重复内容**（不同 chunk 命中同一段），需要去重：

```java
public class ContextBuilder {
    public String buildContext(List<Document> docs, int maxTokens) {
        // 1. 按相关度排序（重排后）
        docs.sort(Comparator.comparingDouble(Document::getScore).reversed());

        // 2. 去重（SimHash 或简单的内容哈希）
        Set<String> seen = new HashSet<>();
        List<Document> unique = docs.stream()
            .filter(d -> seen.add(simHash(d.getContent())))
            .toList();

        // 3. Token 预算内拼接
        StringBuilder context = new StringBuilder("<context>\n");
        int usedTokens = 0;
        for (Document doc : unique) {
            int docTokens = tokenCount(doc.getContent());
            if (usedTokens + docTokens > maxTokens) break;
            context.append(formatAsXml(doc));
            usedTokens += docTokens;
        }
        context.append("</context>");
        return context.toString();
    }
}
```

---

## 三、Prompt 组装：把 Context + Query 喂给 LLM

### 3.1 Prompt 结构模板

```markdown
# System Prompt（角色 + 规则 + 约束）
你是 MSE 微服务产品知识助手。请严格遵守以下规则：
1. 只允许基于 <context> 中的内容回答，禁止使用你自己的知识
2. 如果 <context> 中没有明确答案，回答「知识库暂无此问题答案」
3. 回答必须标注引用来源 [doc:id]
4. 不允许编造 API、参数、错误码

# Context（检索结果）
<context>
  <doc id="1" source="..." section="...">...</doc>
  <doc id="2" source="..." section="...">...</doc>
</context>

# User Query（用户问题）
MSE 网关支持哪些协议？Dubbo 怎么配置？
```

### 3.2 三种 Prompt 注入方式

| 方式 | 说明 | 适用框架 |
|:---|:---|:---|
| **静态拼接** | 字符串模板直接拼接 `String.format(template, context, query)` | 所有框架，最基础 |
| **Chat Message 分离** | SystemMessage / HumanMessage 分开传 | Spring AI / LangChain4j |
| **RAG Advisor 自动注入** | 框架自动检索+注入，开发只写模板 | Spring AI RetrievalAugmentationAdvisor |

#### 方式 1：静态拼接（最底层）

```java
public String buildPrompt(String query, String context) {
    return """
        你是产品知识助手，基于以下参考资料回答问题。
        如果参考资料中没有答案，直接说不知道。

        参考资料：
        %s

        用户问题：%s
        """.formatted(context, query);
}
```

#### 方式 2：Chat Message 分离（Spring AI）

```java
List<Message> messages = List.of(
    new SystemMessage("你是产品知识助手，基于参考资料回答，不知道就说不知道。"),
    new SystemMessage("参考资料：" + context),
    new UserMessage(query)
);
ChatResponse response = chatClient.prompt(messages).call().content();
```

#### 方式 3：RAG Advisor 自动注入（Spring AI 高级）

```java
// 框架自动完成 检索 → 注入 context → 调用 LLM
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        RetrievalAugmentationAdvisor.builder()
            .documentRetriever(vectorStore.asRetriever())  // 自动检索
            .build()
    )
    .build();

// 开发只需要写 Prompt 模板，框架自动注入 context
String answer = chatClient.prompt()
    .user(u -> u.text("""
        基于以下参考资料回答用户问题。
        参考资料：{question_answer_context}

        用户问题：{query}
        """).param("query", userQuery))
    .call()
    .content();
```

> **注意**：Spring AI 的 `{question_answer_context}` 是 Advisor 占位符，框架自动用检索结果替换。

---

## 四、LLM 调用模式

### 4.1 同步 vs 流式 vs 批量

| 模式 | API | 适用场景 | 优缺点 |
|:---|:---|:---|:---|
| **同步调用** | `chatClient.prompt().call().content()` | 后台批处理、简单问答 | 简单，但用户等全部生成完才看到 |
| **流式 SSE** | `chatClient.prompt().stream().content()` | 前端对话（AgentMate 主用） | 用户逐字看到，体验好，但后端复杂 |
| **批量调用** | 并发 CompletableFuture | 回归测试、批量总结 | 快，但需要限流 |

### 4.2 流式 SSE 集成（AgentMate 实战）

```java
// AgentMate 用 Spring AI + SSE 实现流式输出
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestParam String query) {
    SseEmitter emitter = new SseEmitter(60_000L);

    // 异步流式调用
    CompletableFuture.runAsync(() -> {
        try {
            chatClient.prompt()
                .system("基于参考资料回答，标注引用来源。参考资料：{context}")
                .user(query)
                .advisors(retrievalAdvisor)
                .stream()
                .content()
                .subscribe(
                    chunk -> emitter.send(SseEmitter.event().data(chunk)),
                    error -> {
                        log.error("Stream error", error);
                        emitter.completeWithError(error);
                    },
                    () -> emitter.complete()
                );
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });

    return emitter;
}
```

**前端 SSE 接收**：

```javascript
const eventSource = new EventSource(`/chat/stream?query=${query}`);
eventSource.onmessage = (event) => {
    document.getElementById('answer').innerHTML += event.data;  // 逐字追加
};
```

### 4.3 调用参数配置

```java
ChatOptions options = ChatOptions.builder()
    .model("qwen-72b-instruct")
    .temperature(0.1)                    // RAG 场景低随机性
    .maxTokens(4096)                     // 输出上限
    .topP(0.9)                           // 核采样
    .frequencyPenalty(0.0)               // 不惩罚重复
    .presencePenalty(0.0)
    .stop(List.of("</context>"))         // 停止词
    .build();
```

**RAG 场景关键参数**：
- `temperature=0~0.3`：降低幻觉，事实问答要确定性
- `stop=["</context>"]`：防止 LLM 输出中出现 context 结束标记
- `maxTokens`：与预算管理配合，不能超过预留输出空间

---

## 五、错误处理与降级策略

### 5.1 常见异常场景与降级

| 异常 | 根因 | 降级策略 |
|:---|:---|:---|
| **检索为空** | 知识库没有相关内容 | 返回「知识库暂无此问题答案」，不调用 LLM |
| **检索质量差** | 召回结果相关度低 | 不注入 context，LLM 用自身知识回答（标注「未找到相关文档」） |
| **Context 超长** | 检索结果太多 | 截断到预算上限 + 提示用户「结果较多，仅展示最相关部分」 |
| **LLM 超时** | 模型响应慢 | 重试 2 次（指数退避），仍失败返回降级话术 |
| **LLM 输出超长** | 超出 maxTokens | 自动续写（多轮）或截断 + 提示 |
| **引用校验失败** | LLM 引用了不存在的 doc id | 移除无效引用，保留有效部分 |
| **事实校验失败** | LLM 输出与 context 矛盾 | 重写（带上矛盾片段重新问）或拒答 |

### 5.2 降级流程图

```
用户 Query
    │
    ▼
检索
    │
    ├─ 检索为空 → ✅ 直接返回「暂无此问题答案」（不浪费 LLM 调用）
    │
    ├─ 检索相关度 <阈值 → ⚠️ 降级：不注入 context，LLM 自身回答 + 标注「未找到相关文档，以下基于通用知识」
    │
    └─ 检索正常 → 构建 Context → 调用 LLM
                        │
                        ├─ LLM 超时 → 重试 2 次（指数退避：1s, 2s）
                        │           └─ 仍超时 → 返回「服务繁忙，请稍后重试」
                        │
                        ├─ LLM 输出超长 → 自动续写 1 次
                        │           └─ 仍超长 → 截断 + 提示「回答过长已截断」
                        │
                        └─ LLM 输出正常 → 后处理
                                │
                                ├─ 引用校验失败 → 移除无效引用
                                └─ 事实校验失败 → 重写 / 拒答
```

### 5.3 重试与超时配置

```java
// Spring AI Retry Advisor
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new RetryAdvisor(RetryTemplate.builder()
            .maxAttempts(3)
            .backoff(Backoff.exponential(Duration.ofSeconds(1), 2.0))
            .retryOn(TransientAiException.class)  // 网络抖动/限流重试
            .build())
    )
    .build();
```

---

## 六、框架集成对比：Spring AI vs LangChain4j

| 维度 | Spring AI | LangChain4j |
|:---|:---|:---|
| **RAG 组件** | `RetrievalAugmentationAdvisor` + `DocumentRetriever` | `ContentRetriever` + `EmbeddingStore` |
| **Prompt 模板** | `PromptTemplate` + `{param}` 占位符 | `AiServices` + 接口定义 + `@SystemMessage` |
| **Advisor 链** | `Advisor` 接口（责任链模式） | `AiService` 注解 + `ContentRetriever` |
| **向量存储** | 抽象 `VectorStore` 接口（Redis/PG/Milvus） | 抽象 `EmbeddingStore` 接口 |
| **RAG 集成代码量** | 少（Advisor 自动注入） | 少（注解式 AiServices） |
| **Spring Boot 集成** | 原生，自动配置 | 需手动配置但兼容 |
| **AgentMate 选型** | ✅ Spring AI（团队 Spring 背景） | 备选 |

### 6.1 Spring AI RAG 集成完整示例

```java
// 1. 向量存储配置
@Bean
VectorStore vectorStore(EmbeddingModel embeddingModel, RedisConnectionFactory redisFactory) {
    return RedisVectorStore.builder(redisFactory, embeddingModel)
        .indexName("mse_docs")
        .build();
}

// 2. 检索器
@Bean
DocumentRetriever documentRetriever(VectorStore vectorStore) {
    return VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .similarityThreshold(0.7)
        .topK(5)
        .build();
}

// 3. RAG Advisor
@Bean
RetrievalAugmentationAdvisor ragAdvisor(DocumentRetriever retriever) {
    return RetrievalAugmentationAdvisor.builder()
        .documentRetriever(retriever)
        .build();
}

// 4. ChatClient 集成
@Bean
ChatClient chatClient(ChatModel chatModel, RetrievalAugmentationAdvisor ragAdvisor) {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(ragAdvisor)
        .build();
}

// 5. 使用
@Component
public class RagService {
    private final ChatClient chatClient;

    public String ask(String query) {
        return chatClient.prompt()
            .system("基于参考资料回答，标注引用来源 [doc:id]。参考资料：{question_answer_context}")
            .user(query)
            .call()
            .content();
    }
}
```

### 6.2 LangChain4j RAG 集成对比

```java
// LangChain4j 写法
interface Assistant {
    @SystemMessage("基于参考资料回答，标注引用来源。参考资料：{{context}}")
    String ask(@UserMessage String query);
}

Assistant assistant = AiServices.builder(Assistant.class)
    .contentRetriever(contentRetriever)  // 自动检索注入
    .chatModel(chatModel)
    .build();

String answer = assistant.ask("MSE 网关支持哪些协议？");
```

> 两种框架思路一致：**定义接口/模板 + 框架自动检索注入 context + 调用 LLM**，Spring AI 更适合 Spring Boot 生态，LangChain4j 更轻量。

---

## 七、AgentMate 实战集成细节

### 7.1 集成架构

```
AgentMate RAG 集成（基于 Spring AI）
│
├─ ChatClient 配置
│   ├─ RetrievalAugmentationAdvisor（自动检索+注入）
│   ├─ RetryAdvisor（超时重试）
│   └─ LoggingAdvisor（全链路日志）
│
├─ Prompt 模板
│   ├─ System：角色 + 幻觉防线规则 + 引用格式
│   ├─ Context：XML 格式检索结果（带 doc id）
│   └─ User：原始 query（含多轮历史）
│
├─ 调用模式
│   ├─ 对话场景：流式 SSE（逐字输出）
│   ├─ 回归测试：批量同步（CompletableFuture 并发）
│   └─ 告警排查：工具调用 + RAG 组合
│
└─ 后处理
    ├─ 引用校验（doc id 是否存在）
    ├─ 事实一致性（LLM-as-Judge）
    └─ 点赞点踩收集 badcase
```

### 7.2 多轮对话中的 Context 管理

多轮对话不能只检索当前 query，还要考虑**历史对话上下文**：

```java
public String buildQueryWithHistory(List<Message> history, String currentQuery) {
    // 1. 历史对话压缩：取最近 3 轮，太长则摘要
    String compressedHistory = compressHistory(history, maxHistoryTokens);

    // 2. 多轮补全：代词消解（"它" → 具体产品名）
    String resolvedQuery = resolvePronouns(currentQuery, history);

    // 3. 检索时用补全后的 query
    return resolvedQuery;
}
```

**坑**：直接把全部历史对话塞进 prompt 会快速消耗 token 预算，且早期对话的约束会被稀释。

### 7.3 引用溯源实现

LLM 输出 `[doc:1]` 后，前端需要渲染成可点击链接：

```java
// 后处理：把 [doc:1] 替换成可点击的引用
public String renderCitations(String llmOutput, List<Document> docs) {
    String result = llmOutput;
    for (Document doc : docs) {
        String citation = "[doc:" + doc.getId() + "]";
        String link = String.format(
            "<a href='%s' class='citation' data-section='%s'>[%s §%s]</a>",
            doc.getSourceUrl(), doc.getSection(), doc.getSource(), doc.getSection()
        );
        result = result.replace(citation, link);
    }
    return result;
}
```

---

## 八、面试高频问答

### Q1：RAG 怎么和大模型集成？

四步：
1. **构建 Context**：检索结果 XML 格式化 + Token 预算分配 + 去重压缩
2. **组装 Prompt**：System（规则）+ Context（带引用）+ User（query）
3. **调用 LLM**：同步/流式 SSE，temperature=0~0.3，超时重试
4. **后处理**：引用校验 + 事实一致性校验 + PII 脱敏

### Q2：检索结果怎么注入到 Prompt？

三种方式：
- **静态拼接**：字符串模板 `String.format`，最基础
- **Chat Message 分离**：SystemMessage + UserMessage 分开传，Spring AI/LangChain4j 标准
- **Advisor 自动注入**：框架自动检索+替换占位符 `{question_answer_context}`，最省事

### Q3：Context 太长超出 token 限制怎么办？

四策略：
1. **截断**：按相关度排序取 Top-N 直到预算用完（最常用）
2. **摘要压缩**：小模型先摘要再注入
3. **Map-Reduce**：先逐个 chunk 总结再汇总
4. **父级回调**：优先取父章节保证上下文完整

核心是**精确预算管理**：context window = system + few-shot + query + context + output，每部分都要算 token。

### Q4：流式 SSE 和同步调用怎么选？

- **前端对话**：流式 SSE，用户逐字看到，体验好（AgentMate 主用）
- **后台批处理**：同步调用，简单
- **回归测试**：批量同步 + CompletableFuture 并发，快

### Q5：检索为空怎么办？

直接返回「知识库暂无此问题答案」，**不调用 LLM**——既省成本又防幻觉。这是 P0-P4 分级检索中 P3/P4 级别的处理逻辑。

### Q6：Spring AI 怎么做 RAG 集成？

用 `RetrievalAugmentationAdvisor` + `VectorStore`：
1. 配置 `VectorStore`（Redis/PG/Milvus）
2. 配置 `DocumentRetriever`（topK + similarityThreshold）
3. 创建 `RetrievalAugmentationAdvisor`
4. 注册到 `ChatClient.defaultAdvisors()`
5. Prompt 模板中用 `{question_answer_context}` 占位符，框架自动注入

### Q7：RAG 集成中最难的是什么？

**Token 预算管理 + 引用溯源**：
- 预算管理：context window 有限，检索结果不能全塞，要在「信息完整」和「不超预算」间平衡
- 引用溯源：LLM 可能编造 doc id，需要后处理校验，无效引用要移除或拒答

### Q8：多轮对话中 RAG 怎么处理？

不能只检索当前 query：
1. 历史对话压缩（取最近 3 轮 + 摘要）
2. 代词消解（「它」「这个」→ 具体产品名）
3. 用补全后的 query 检索
4. prompt 中同时注入历史 + context + 当前 query

---

## 九、易错点

1. **❌ 检索结果直接拼接进 Prompt** → ✅ 需要 XML/Markdown 格式化，LLM 才能区分边界
2. **❌ 不做 Token 预算管理** → ✅ 精确计算 context window 分配，超预算要截断/压缩
3. **❌ RAG 场景 temperature 随意设** → ✅ 统一 0~0.3，降低幻觉
4. **❌ 检索为空也调用 LLM** → ✅ 直接返回「暂无答案」，省成本防幻觉
5. **❌ 不做引用校验** → ✅ LLM 可能编造引用，必须后处理校验 doc id 是否存在
6. **❌ 多轮对话只检索当前 query** → ✅ 需要历史压缩 + 代词消解 + 补全后检索
7. **❌ 框架 Advisor 不懂原理** → ✅ Advisor 本质是责任链，自动注入 context 只是其中一环，底层原理要懂

---

## 十、相关笔记

- 检索策略层（混合检索/切片/重排）→ [RAG检索增强生成.md](RAG检索增强生成.md)
- Spring AI 框架细节（Advisor 链/ChatClient）→ [SpringAI与LangChain4j.md](SpringAI与LangChain4j.md)
- 幻觉治理（RAG 集成的核心目标）→ [幻觉，幻觉审计，幻觉治理，幻觉评测.md](幻觉，幻觉审计，幻觉治理，幻觉评测.md)
- Prompt 工程（System Prompt 设计）→ [提示词工程.md](提示词工程.md)
- LLM 安全（pre/do/post Hook 后处理）→ [LLM安全与工程化.md](LLM安全与工程化.md)
- SSE 流式输出（AgentMate 实战）→ [LLM安全与工程化.md](LLM安全与工程化.md) SSE 章节

---

## 十一、一句话总结

> RAG 与 LLM 集成的本质是**「检索 → 格式化 → 预算管理 → Prompt 注入 → 调用 → 后处理」**六步 Pipeline：检索结果用 XML 格式化 + Token 预算精确分配 + System/Context/User 三段式 Prompt + 流式 SSE 低 temperature 调用 + 引用校验和事实一致性后处理，Spring AI 的 `RetrievalAugmentationAdvisor` 把检索和注入自动化，但底层每一步的工程细节必须懂。
