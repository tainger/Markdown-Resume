# Spring AI 与 LangChain4j：Java 生态 AI 框架

> 「Java 转 AI」的差异化必考点：面试官要验证你会不会**写代码**而不只画架构图。本文把 [RAG](RAG检索增强生成.md)、[Agent](Agent智能体框架.md)、[安全](LLM安全与工程化.md) 的思想落到 Java 框架层——版本基线：**Spring AI 1.1 GA**（2025-11，MCP 原生支持；2.x 已发布但企业采用以 1.1 为主）、**LangChain4j 1.x**（2026-08 已到 1.19）。

---

## 一、为什么 Java 工程师必须会这个

两个框架解决同一个问题：**把「调大模型」从手写 HTTP/SDK 升级为工程化框架**——

| 能力 | 手写 SDK | 框架化 |
|:---|:---|:---|
| 换模型 | 改业务代码 | **统一抽象**，改配置即可 |
| 工具调用 | 自己写循环 + 参数校验 | 注解声明 + 框架驱动循环 |
| RAG | 自己拼检索链路 | 组件化（向量库/检索器/Advisor 可插拔） |
| 记忆/流式/脱敏 | 散落各处 | **横切能力做成链**（拦截器思想） |

> 面试定位：这两个框架的设计处处能对上你已有的 Java 功底——**Advisor 链 ≈ 责任链/拦截器 ≈ MyBatis 插件**，**AiServices ≈ 动态代理 ≈ MyBatis Mapper**。答出这种映射就是「老 Java 学 AI」的最大卖点。

---

## 二、Spring AI 核心（1.1 GA）

### 2.1 ChatClient：统一模型入口（fluent API）

`ChatModel` 是**各厂商的适配层**（OpenAI/DeepSeek/Ollama…各一个实现）；`ChatClient` 是**业务代码的门面**——挂 Advisor 链、模板、结构化输出的地方。

```java
// 自动配置：starter 注入 ChatClient.Builder（绑定了默认 ChatModel）
@Bean
ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem("你是公司运维助手，只回答运维相关问题")   // 默认 system
        .build();
}

// 同步调用
String answer = chatClient.prompt()
    .user("网关 503 怎么排查")
    .call()
    .content();

// 结构化输出：entity() 自动把 JSON Schema 注入 Prompt 并反序列化（BeanOutputConverter）
record FaultReport(String cause, List<String> steps, Severity level) {}
FaultReport r = chatClient.prompt().user(userInput).call().entity(FaultReport.class);

// 流式（SSE 场景）
Flux<String> stream = chatClient.prompt().user(q).stream().content();
```

> 追问「ChatClient vs ChatModel」：Model=厂商适配（能不能换），Client=业务入口+增强链（怎么做增强）。业务代码只依赖 Client，换模型只动配置。

### 2.2 Advisor 链：AI 版拦截器（核心机制）

Advisor 就是 **AI 请求链上的责任链拦截器**：请求按 order 顺序过链、响应逆向回链，每层可改写 Prompt、观察响应、短路。

```
user prompt → [脱敏Advisor] → [MemoryAdvisor] → [RAG Advisor] → [ToolCallAdvisor] → ChatModel
   user     ← [脱敏Advisor] ← [MemoryAdvisor] ← [RAG Advisor] ← [ToolCallAdvisor] ← response
```

| 内置 Advisor | 作用 | 对应站内概念 |
|:---|:---|:---|
| `MessageChatMemoryAdvisor` | 注入对话历史（记忆） | [Agent 记忆三层](Agent智能体框架.md) |
| `QuestionAnswerAdvisor` | 检索知识库 + 拼上下文（**一行接 RAG**） | [RAG 检索增强生成](RAG检索增强生成.md) |
| `SimpleLoggerAdvisor` | 请求/响应日志 | 可观测 |
| `ToolCallAdvisor`（1.1） | **工具调用循环进链**：禁用模型内部执行，链上其他 Advisor 能观察每一轮工具调用 | [Agent 智能体框架](Agent智能体框架.md) |

自定义 Advisor（把你的**三态 Hook** 落成框架层）：

```java
// 响应后置 PII 脱敏：对应 AgentMate 三态 Hook 的 post 态
public class PiiMaskAdvisor implements CallAdvisor {
    @Override
    public ChatResponse adviseCall(ChatRequest request, CallAdvisorChain chain) {
        ChatResponse resp = chain.nextCall(request);          // 先走下游拿到响应
        return PiiMasker.mask(resp);                          // 后置过滤
    }
}

// 使用：order 越小越靠外
chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new PiiMaskAdvisor(),                                 // order 默认 0
        MessageChatMemoryAdvisor.builder(chatMemory).order(100).build())
    .build();
```

> 1.1 的 **Recursive Advisors**：Advisor 可以多次调用下游子链（`chain.copy(after)`）——工具循环、输出校验重试、LLM-as-Judge 迭代都能做成链上组件。与 [双引擎评测](LLM安全与工程化.md) 的「校验不达标重试」直接同构。

### 2.3 Tool Calling：模型出意图，框架管循环

```java
class OpsTools {
    @Tool(description = "查询服务当前 CPU 使用率")
    double cpuUsage(String service) {          // 参数描述来自 @Tool 参数名 + description
        return monitor.cpu(service);
    }
}

// 挂到 ChatClient
chatClient.prompt()
    .user("看下 order-service 负载")
    .tools(new OpsTools())                     // 模型决定调谁、给什么参数
    .call()
    .content();                                // 循环由框架驱动直到模型给出最终回答
```

要点（面试追问高发区）：
- **模型只产出「调用意图 + JSON 参数」，执行永远在代码侧**（`ToolCallingManager`）——参数校验、权限、幂等都是你的地盘
- `@McpTool` vs `@Tool`：前者走 **MCP 标准协议**可被任何 MCP 客户端消费（跨生态互通），后者仅 Spring AI 内部
- **必须配工具调用次数上限**（tool call limits）防死循环烧钱
- `returnDirect=true`：工具结果直接返回用户，不再回模型（省一轮 token）
- MCP 传输：**Streamable HTTP 为推荐**（SSE transport 已废弃）

### 2.4 RAG 组件化：ETL + 向量库抽象

```java
// ① ETL 管道：读 → 切 → 入库
@Bean
VectorStore vectorStore(EmbeddingModel embedding, JdbcTemplate jdbc) {
    return new PgVectorStore.Builder(jdbc, embedding).build();   // 换 Milvus/Redis 只换这一行
}

List<Document> docs = new TextReader(file).get();                // DocumentReader
List<Document> chunks = new TokenTextSplitter().apply(docs);     // DocumentTransformer（切片）
vectorStore.write(chunks);                                       // Embedding + 落库

// ② 在线：QuestionAnswerAdvisor 一行接 RAG（检索 → 拼 Prompt）
chatClient.prompt()
    .user(question)
    .advisors(new QuestionAnswerAdvisor(vectorStore,
        SearchRequest.builder().topK(3).build()))
    .call().content();
```

对应站内体系：`EmbeddingModel`（选型见 [大模型工程基础](大模型工程基础.md)）、切片策略、混合检索在 Spring AI 里通过自定义 Advisor/`VectorStore` filter 组合落地。

### 2.5 ChatMemory 与 MCP 原生支持

- `MessageWindowChatMemory` + 存储仓库（JDBC/Redis/Cassandra）——记忆持久化开箱即用（1.x 起要求 conversation id 必传）
- **1.1 里程碑：MCP 原生支持**——注解式暴露（`@McpTool`/`@McpResource`/`@McpPrompt`）+ 客户端/服务端 Starter，你的工具一键变成 MCP Server 被别的 Agent 消费

---

## 三、LangChain4j 核心（1.x）

### 3.1 双层 API：低层 ChatModel，高层 AiServices

```java
// 低层：直接对话（1.0 起 ChatLanguageModel 更名 ChatModel，非流式）
ChatModel model = DeepSeekChatModel.builder()      // 各厂商实现
    .apiKey(System.getenv("DEEPSEEK_API_KEY"))
    .modelName("deepseek-chat")
    .build();
String ans = model.chat("你好");

// 高层：AiServices —— 声明式接口，框架生成实现
interface Assistant {
    @SystemMessage("你是公司运维助手")
    String chat(@UserMessage String question);
}

Assistant assistant = AiServices.builder(Assistant.class)
    .chatModel(model)
    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
    .tools(new OpsTools())
    .build();
assistant.chat("网关 503 怎么排查");
```

**AiServices 的原理 = JDK 动态代理**：接口方法调用被代理拦截 → 翻译成 Prompt（`@SystemMessage`/`@UserMessage`/`@V` 参数绑定）→ 调模型 → 解析返回（支持直接返回 POJO 结构化输出）。**和 MyBatis Mapper 完全同构**——Mapper 也是「接口 + 动态代理把方法翻译成 SQL」。这一句是 Java 老兵的必杀答。

### 3.2 Tools 与 Agentic（1.x 新模块）

```java
class OpsTools {
    @Tool("查询服务当前 CPU 使用率")
    double cpuUsage(String service) { ... }
}
AiServices.builder(Assistant.class).tools(new OpsTools()) ...   // 模型自主决定调用

// langchain4j-agentic 模块：@Agent 声明 + 编排（顺序/并行/GOAP 目标驱动）
interface Writer { @Agent("写初稿") String draft(String topic); }
interface Reviewer { @Agent("审稿") String review(@V String draft); }
// AgenticServices.sequenceBuilder(Writer, Reviewer)...  多 Agent 流水线
```

对应站内：多 Agent 编排思想见 [Agent 智能体框架](Agent智能体框架.md)（区别：LangChain4j 的编排仍是模型自主性较强，生产强管控场景你仍会选自研状态机——这就是「框架 vs 自研」的选型答案）。

### 3.3 RAG 组件化（比 Spring AI 更细的管道抽象）

```
Query → QueryTransformer（改写/扩展）
      → QueryRouter（按 query 类型路由到不同检索器）
      → ContentRetriever（向量库/关键词/多路）
      → ContentAggregator（融合 + 重排）
      → 增强后的 Prompt
```

```java
EmbeddingStore<Document> store = new MilvusEmbeddingStore.Builder()...build();
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(3)
    .build();

Assistant assistant = AiServices.builder(Assistant.class)
    .chatModel(model)
    .retrievalAugmentor(DefaultRetrievalAugmentor.builder()
        .queryTransformer(CompressingQueryTransformer.builder().chatModel(model).build()) // 多轮改写
        .contentRetriever(retriever)
        .build())
    .build();
```

- 抽象粒度：**RetrievalAugmentor 可自由组合改写/路由/聚合/重排**——把 [双混合检索架构](RAG检索增强生成.md) 用组件拼出来
- 快速起步：`langchain4j-easy-rag`（自动解析/切片/入库，demo 神器）
- 记忆两版：`MessageWindowChatMemory`（按条数）vs `TokenWindowChatMemory`（按 token，更准但要 TokenCountEstimator）

### 3.4 其他能力

- **结构化输出**：接口方法直接返回 `record/POJO/List`，框架负责 schema 注入与解析
- **Guardrails**：输入/输出护栏（敏感词、格式校验），对应你的三态 Hook 前后置
- **MCP**：客户端/服务端齐全（跟进 2026-07 规范）；可观测：Langfuse/OpenTelemetry 集成

---

## 四、选型对比（必考表）

| 维度 | Spring AI | LangChain4j |
|:---|:---|:---|
| 背书 | **Spring 官方**，与 Boot 生态/自动配置/可观测无缝 | 社区驱动（Dmytro 等核心维护），独立演进 |
| 编程模型 | `ChatClient` fluent + Advisor 链 | `AiServices` **接口代理** + 组件化 RAG 管道 |
| RAG 抽象 | VectorStore + QuestionAnswerAdvisor（简） | RetrievalAugmentor 管道（**改写/路由/聚合可深度定制**） |
| Agent 能力 | 递归 Advisor + 社区 Agents 项目 | **agentic 模块**（@Agent、顺序/并行/GOAP） |
| MCP | 1.1 原生（注解式 + Starter） | 客户端/服务端完整 |
| 模型/向量库数量 | 主流覆盖 | **极广**（60+ 模型、30+ 向量库，含国内厂商） |
| 适用 | **Spring 技术栈企业应用首选**、长期支持诉求 | 非 Spring 项目、RAG 管道深度定制、多模型异构 |

**选型话术**：「两者抽象在趋同（都支持 MCP、都有记忆/工具/RAG）。**Spring 全家桶项目选 Spring AI**（官方背书、 starter 顺滑、团队零学习成本）；**RAG 管道要深度定制或多模型异构选 LangChain4j**。我的项目在 Spring 栈内，但我理解两者的抽象差异——真迁移时业务层只依赖自己的门面封装。」——答出「**自己的 ChatClient 门面**」这一层（统一抽象包裹框架），是 P7 和 P6 的分界。

---

## 五、面试追问速答

| 追问 | 速答 |
|:---|:---|
| Advisor 和 Spring AOP 什么关系？ | 同为责任链/横切思想；Advisor 专门面向「消息序列」而非方法调用，且流式响应要配 `StreamAdvisor` |
| 工具执行发生在哪？ | **代码侧**。模型只产出意图与参数；校验、鉴权、幂等、限流全是代码的地盘 |
| 怎么防止工具死循环烧钱？ | 工具调用次数上限 + 单轮 token 限额 + 总预算熔断（对齐 [成本优化](AI高频面试题速查.md) T28） |
| entity() 结构化输出可靠吗？ | 不可靠时是常态：schema 注入提高成功率 + 解析失败带错误重试 + 关键字段校验（对齐速查 T21） |
| AiServices 怎么实现的？ | JDK 动态代理翻译接口调用（与 MyBatis Mapper 同构） |
| 流式场景注意什么？ | Advisor 分同步/流式两接口；SSE 转发注意粘包半包（[LLM 安全与工程化 §五](LLM安全与工程化.md)） |
| 换模型要改业务代码吗？ | 不。业务只依赖 ChatClient/自己封装的门面；模型切换是配置 + 回归评测的事 |
| 版本坑？ | Spring AI 1.1→2.0 有 Advisor/工具自动注册变化；LangChain4j 1.0 把 `ChatLanguageModel` 更名 `ChatModel`（面试报版本基线显专业） |

---

## 易错点

| 坑 | 说明 |
|:---|:---|
| ChatClient 上挂了阻塞 Advisor | 流式链路用的是 `StreamAdvisor`，同步阻塞调用会把流卡死——两套接口别混 |
| Advisor 不设 order | 链序随机化，脱敏可能跑到记忆注入之后（漏拦 system 里的内容）——**显式指定 order** |
| 工具无次数限制 | 模型陷入循环调用，token 账单爆炸；必配 limits + 预算熔断 |
| entity() 当强保证 | JSON 输出本就非确定，解析失败要有重试与兜底 |
| 把系统提示词放进 user 消息 | 应走 `defaultSystem`/`@SystemMessage`，才能被注入类 Advisor（记忆/RAG）正确处理 |
| MCP 传输选 SSE | 已废弃，新项目直接 Streamable HTTP |
| 拿框架硬套强管控 Agent | 生产变更类链路仍需过程式状态机兜底（框架循环是模型自主的），参考 [Agent 框架篇选型](Agent智能体框架.md) |

---

## 一句话总结

**Spring AI = ChatClient 门面 + Advisor 责任链 + 组件化 RAG/MCP**，**LangChain4j = AiServices 动态代理 + 更细的检索管道编排**——两者共同把「调模型」工程化为「拦截器 + 抽象层」；Java 老兵的答法是把每个机制映射回已会的概念（Advisor≈MyBatis 插件、AiServices≈Mapper 代理、工具循环≈自己写的 Agent 状态机），最后落在「**业务只依赖自己的门面，框架可替换**」的架构观上。

## 相关笔记

- Advisor 链同构：[Mybatis 插件机制](../Mybatis/插件机制与高级特性.md)（Interceptor 责任链）
- AiServices 代理原理：[java/反射与动态代理](../java/反射与动态代理.md)
- 三态 Hook ↔ Advisor/Guardrails：[LLM 安全与工程化](LLM安全与工程化.md)
- RAG 组件与检索原理：[RAG 检索增强生成](RAG检索增强生成.md)
- 工具循环与 Agent 编排：[Agent 智能体框架](Agent智能体框架.md)
- 模型接入细节：[DeepSeek API 应用开发](../DeepSeek%20Harness/02.API应用开发.md)
