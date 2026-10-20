# 六、RAG 评测体系

> 覆盖 RAG Triad、检索层/生成层/端到端评测指标、RAGAS 框架、Bad Case 分析与优化。

---

## Q1: 什么是 RAG Triad？它定义了 RAG 系统的哪三个核心维度？

**难度**: ⭐⭐ 中级

**参考答案**:

RAG Triad（RAG 三元组）是一套**评估方法论**，由 Shining Pang 在 2024 年提出。它把 RAG 系统拆成三个核心要素，两两配对评估：

```
        用户问题（Query）
           /    \
          /      \
  检索资料        最终答案
  (Context)      (Answer)
```

三对关系：

| 关系 | 含义 | 评测目标 |
|:---|:---|:---|
| **Query ↔ Context** | 检索到的资料和问题相关吗？ | **Context Relevance**（检索层目标） |
| **Context ↔ Answer** | 答案是基于检索资料生成的吗？ | **Faithfulness**（生成层目标，反幻觉） |
| **Query ↔ Answer** | 答案直接回答了问题吗？ | **Answer Relevancy**（端到端目标） |

一句话：**Triad 是评测框架，下面的 Recall/Precision/Faithfulness 才是具体指标**。

> 来源：[[RAG评测/2.RAG评测之RAG Triad]]、[[RAG评测/3.RAG的核心评测指标有哪些]]

---

## Q2: 检索层有哪些核心评测指标？Context Recall 和传统 Recall 有什么区别？

**难度**: ⭐⭐ 中级

**参考答案**:

**检索层核心指标**：

| 指标 | 衡量什么 | 计算方式 | 需要 GT 吗 |
|:---|:---|:---|:---|
| **Context Recall** | 相关资料被找到了多少（查全率） | 相关文档中被检索到的比例 | ✅ 需要 |
| **Context Precision** | 检索回来的资料里有多少是真相关的（查准率） | 检索结果中相关的比例 | ✅ 需要 |
| **F1 Score** | P 和 R 的调和平均（双高才高） | F1 = 2PR / (P+R) | ✅ 需要 |
| **MRR** | 正确答案排得靠不靠前 | 1 / rank_of_first_relevant | ✅ 需要 |
| **NDCG** | 排名质量（考虑等级相关性） | DCG / IDCG | ✅ 需要 |

**Context P/R 和传统 P/R 的区别**：

| 维度 | 传统 P/R | Context P/R |
|:---|:---|:---|
| 判定方式 | **死板**数学统计：检索到的文档 ID 是否和 GT 标注的 ID 精确匹配 | **灵活**语义评估：用 LLM-as-Judge 判断检索回来的**内容**语义上是否真的有用 |
| 需要 GT | 需要（标注文档 ID 列表） | **不需要**（或只需要理想答案，不需要标注文档 ID） |
| 优势 | 可复现、计算快 | 更贴近实际效果（ID 不同但内容相同也能判对） |

RAG 评测**主流用 Context P/R + LLM-as-Judge**，因为实际场景中"正确答案出现在哪几个 chunk"很难提前标注，直接判语义更合理。

> 来源：[[RAG评测/3.RAG的核心评测指标有哪些]]

---

## Q3: 生成层有哪些核心评测指标？Faithfulness 和 Answer Relevancy 分别是什么？

**难度**: ⭐⭐ 中级

**参考答案**:

**生成层核心指标**：

| 指标 | 衡量什么 | 需要 GT 吗 | 说明 |
|:---|:---|:---|:---|
| **Faithfulness** | 答案是否严格基于检索资料（反幻觉） | ❌ 不需要 | 答案中每个 claim 都要能在检索上下文里找到支撑 |
| **Answer Relevancy** | 答案是否直接回答了用户问题 | ❌ 不需要 | 不跑题、不答非所问 |
| **Answer Correctness** | 答案是否正确（金标准对比） | ✅ 需要 | 和 GT 答案做语义相似度 + 事实准确度比较 |

**Faithfulness（忠诚度）** 是 RAG 生成层**最核心的指标**——直接衡量有没有幻觉：
```
检索上下文：[A, B, C]
生成答案："根据检索内容，X、Y、Z"
  ─ X 能在 A 里找到 → ✓ faithful
  ─ Y 能在 B 里找到 → ✓ faithful  
  ─ Z 在 A/B/C 里都找不到 → ✗ hallucination
Faithfulness = faithful_claims / total_claims
```

**Answer Relevancy（答案相关性）** 衡量的是"有没有答非所问"：
```
用户问题："R7 续航多少？"
生成答案："智界 R7 续航 640km" → ✓ relevant
生成答案："智界 R7 是华为和奇瑞联合开发的" → ✗ irrelevant
```

**Answer Correctness（答案正确性）** 是最严格的指标，但必须有 GT（金标准答案），所以**成本最高**。一般用**语义相似度 + 事实准确度加权**，给事实更高权重（防"正确的废话"——说对了结构但关键事实错了）。

> 来源：[[RAG评测/3.RAG的核心评测指标有哪些]]、[[RAG评测/4.LLM-as-Judge]]

---

## Q4: LLM-as-Judge 是什么？为什么 RAG 评测要用它？

**难度**: ⭐⭐ 中级

**参考答案**:

LLM-as-Judge = **让大模型来当裁判**，用 Prompt Engineering 让它判断一段内容是否满足某个评测标准。

```
传统评测（规则）：
  Faithfulness = 答案中的每个句子 ∈ 检索上下文？
  问题：语义等价但字面不同 → 判错

LLM-as-Judge（语义）：
  Prompt: "你是一个评测专家。请判断以下生成答案中的每个声明
          是否可以被提供的检索上下文支撑，不能就算幻觉。
          检索上下文：[A, B, C]
          生成答案：[X, Y, Z]
          输出：每个声明的 faithful / not faithful + 分数"
```

**为什么要用 LLM-as-Judge**：
1. **RAG 评测是语义问题**——字符串匹配、BM25 分数都不够用
2. **传统规则需要标注大量 GT**——人工成本极高
3. **LLM 能理解语义等价**——"电池容量 100kWh" 和 "R7 用的是 100 千瓦时电池" 能判为同一件事
4. **生产环境最主流的评测方式**——RAGAS、TruLens、DeepEval 全都是 LLM-as-Judge 实现

**风险**：LLM 评判本身也可能不稳定（温度随机、Prompt 敏感），所以要**固定温度、多次跑取平均、用强模型判弱模型**。

> 来源：[[RAG评测/4.LLM-as-Judge]]、[[RAG评测/7.RAGAS评测时，如何抑制模型幻觉导致的结果不稳定问题？]]

---

## Q5: RAGAS 框架的核心功能和实现原理？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

**RAGAS（Retrieval Augmented Generation Assessment）** 是目前最主流的 RAG 评测框架，基于 Python，核心能力：

1. **内置所有核心指标**：Context Recall、Context Precision、Faithfulness、Answer Relevancy、Answer Correctness
2. **LLM-as-Judge 实现**：用 Prompt 工程让 LLM 判分，不同指标用不同 Prompt 模板
3. **数据集格式**：接受 `{question, answer, ground_truth, contexts}` 格式的评测集
4. **支持批量评测**：CSV 批量跑，输出每个样本的详细分数 + 总体统计

**实现原理**（以 Faithfulness 为例）：
```
Step 1: 把生成答案拆成独立声明（claim）
  "根据 R7 手册，续航 640km，支持 5G 车机"
  → ["续航 640km", "支持 5G 车机"]

Step 2: 让 LLM 判断每个声明是否能被检索上下文支撑
  Prompt: "声明：R7 续航 640km
           上下文：[chunk1, chunk2, chunk3]
           这个声明能被上下文支撑吗？回答 yes/no"

Step 3: Faithfulness = yes_count / total_claims
```

**评测步骤**：
```
1. 准备评测集（CSV）
2. 跑 RAG 系统，收集 question / answer / contexts
3. RAGAS 批量计算各指标
4. 分析低分样本（Bad Case）
5. 优化 RAG 系统 → 重跑评测 → 对比
```

> 来源：[[RAG评测/5.主流RAG评测框架：RAGAS、TruLens、DeepEval、Phoenix]]、[[RAG评测/6.RAGAS框架初探&实现原理]]

---

## Q6: RAGAS 评测时，怎么抑制 LLM Judge 的幻觉/不稳定？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

LLM-as-Judge 本身也可能不稳定——温度随机、Prompt 敏感、模型幻觉。抑制手段：

| 手段 | 具体做法 | 原理 |
|:---|:---|:---|
| **固定温度** | Judge 模型 temperature = 0 | 温度 0 时输出完全确定，消除随机性 |
| **多次采样取平均** | temperature = 0.2，每个样本跑 3-5 次取平均 | 平衡稳定性和覆盖性 |
| **强模型判弱模型** | 用 GPT-4o / qwen-max 判 GPT-3.5 / qwen-turbo | 大模型幻觉更少、判断更准 |
| **Prompt 结构化输出** | 要求 Judge 只输出 JSON `{"score": 0.7, "reason": "..."}` | 减少自由发挥空间，输出可解析 |
| **Few-Shot 示例** | 给 Judge 3-5 个标注好的示例 | 对齐评判标准 |
| **置信度阈值** | Judge 输出 "uncertain" 时标记为需人工复核 | 低置信度的判断直接丢弃 |
| **独立复核样本** | 随机抽 5% 样本人工复核 | 量化 Judge 本身的准确率 |

**RAGAS 内置的稳定化**：`evaluate_with_llm` 默认 temperature=0，支持 `raise_exceptions=True` 让异常直接暴露。

> 来源：[[RAG评测/7.RAGAS评测时，如何抑制模型幻觉导致的结果不稳定问题？]]

---

## Q7: 怎么准备一个好的 RAG 评测数据集？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

### 好评测集的标准

| 标准 | 说明 |
|:---|:---|
| **代表性** | 覆盖真实用户会问的所有类型问题（简单事实、复杂多跳、模糊表达、跨文档推理） |
| **难度分层** | 不能全是简单题，要有梯度（易/中/难各占一定比例） |
| **足够规模** | 50-100 个题目才能稳定评估，200+ 更可靠 |
| **标注一致** | 同一个问题的 GT 答案和相关 chunk 标注要标准 |

### 评测集的两种准备方式

| 方式 | 优点 | 缺点 |
|:---|:---|:---|
| **人工标注**（专家出题） | 质量最高、最贴近真实场景 | 成本极高、周期长 |
| **LLM 自动生成**（基于文档反向生成） | 快、成本低、规模大 | 可能生成太简单或不自然的问题 |

### LLM 自动生成流程

```
原始文档 → 分片
    │
    ▼
对每个 chunk，让 LLM 生成：
  ① 一个用户可能会问的问题
  ② 一个理想答案（ground_truth）
    │
    ▼
人工审核/筛选（去掉太简单/不自然的）
    │
    ▼
最终评测集 CSV：{question, ground_truth, contexts}
```

### 评测集质量保障

- **去重**：相似度 > 0.8 的问题保留一个
- **多样性检查**：确保覆盖不同类型的问题（可用 Embedding 聚类看分布）
- **Bad Case 专项集**：单独收集历史线上的 Bad Case，作为"必测集"
- **定期更新**：每次 RAG 系统大改后，把新发现的 Bad Case 加入评测集

> 来源：[[RAG评测/8.RAG的评测数据集该如何准备？]]、[[RAG评测/9.怎样的评测集算是好的评测集？]]

---

## Q8: RAG 评测出来分数低怎么办？常见 Bad Case 有哪些？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

### Bad Case 分类与修复方向

| Bad Case | 表现 | 根因 | 修复方向 |
|:---|:---|:---|:---|
| **召回率低** | Context Recall 低，GT 文档没被检索到 | Embedding 模型不够好 / chunk 粒度不对 / 纯向量搜不到关键词 | 换 Embedding 模型 / 调整 chunkSize / 加 BM25 混合检索 |
| **召回不准** | 召回分高但答案不对 | 检索到了 GT 但**噪声太多**，把正确答案淹没了 | 加元数据过滤 / 调低 Top-K / 重排序精筛 |
| **答案幻觉** | Faithfulness 低，答案有编造 | 检索结果不够相关 / Prompt 没约束不编造 | 换更强的检索 / Prompt 加"基于提供资料回答，不确定就说不知道" |
| **答非所问** | Answer Relevancy 低 | 查询没改写好 / 检索方向偏了 | 加强问题改写 / 加查询路由 |
| **引用乱序** | 参考来源顺序和答案不对应 | 重排序后的 chunk 顺序没传递给 Prompt | 确保 chunk 排序从高到低 |
| **跨文档断裂** | 答案需要 A、B 两部分，结果只召回了 A | 单路检索漏召回 / 多跳问题没分解 | 加问题分解（子问题）/ 父子分片 |

### 一个修复实例

**Bad Case**：用户问 "2023 年 R7 续航多少"，召回了 2024 版手册（因为向量相似度高）

**根因**：没有做版本过滤

**修复**：加元数据过滤 → `WHERE version = 2023`

> 来源：[[RAG评测/15.know-engine评测问题与优化（bad case）]]、[[RAG评测/19-27 系列优化文档]]

---

## Q9: 评测时发现 Bad Case 怎么定位是检索问题还是生成问题？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

### 定位流程（从底往上，逐层隔离）

```
Step 1：先看检索层指标
  Context Recall 低 → 问题在检索（Embedding / 分片 / 混合检索）
  Context Recall 高 → 继续
  
Step 2：再看 Context Precision
  Context Precision 低 → 问题在检索质量（噪声太多 → 元数据过滤 / 重排序）
  Context Precision 高 → 继续

Step 3：手动检查检索到的 chunks
  里面有正确答案吗？
    ├─ 没有 → 问题在检索（召回漏了）
    └─ 有，但答案还是错的 → 继续

Step 4：看 Faithfulness
  Faithfulness 低 → 问题在生成（LLM 忽略了正确 chunk，自己编造）
  Faithfulness 高 → 继续

Step 5：看 Answer Relevancy
  Answer Relevancy 低 → Prompt 指令没约束好（跑题）
  
Step 6：看 Answer Correctness
  低 → 可能是检索 chunk 太泛，答案正确但不够精确（需要更细粒度）
```

### 关键思维

**先确保 "检索到正确资料"，再确保 "LLM 不编造"**——如果检索层就有问题，调 Prompt 是白搭。所以 **Context Recall 是最高优先级的指标**，必须先拉上去。

---

## Q10: 怎么加速 RAGAS 评测？慢怎么办？

**难度**: ⭐⭐ 中级

**参考答案**:

RAGAS 评测本身需要 N 次 LLM 调用（每个样本算 Faithfulness 要拆 claim + 逐个判，算 Context P/R 要 LLM-as-Judge），天然慢。加速手段：

| 手段 | 具体做法 |
|:---|:---|
| **换小模型当 Judge** | 不用 GPT-4o，用 qwen-turbo / gpt-4o-mini 判，速度快 5-10 倍，精度损失不大 |
| **并发评测** | RAGAS 支持 `async_mode = True`，异步并发跑 |
| **减少样本量** | 先跑 30-50 个样本快速看趋势，确认再跑全量 |
| **缓存结果** | LLM-as-Judge 的判断结果可以缓存——同一个 context + answer 组合不需要重复判 |
| **简化指标** | 不跑全部指标，先只跑 Faithfulness + Context Recall（这两个最关键） |
| **本地部署 Judge 模型** | 用 bge-reranker + LLM 本地推理，不用每次调云端 API |
| **降低温度 + 固定输出** | temperature=0 时输出完全确定，不会因随机多跑几次 |

**实战**：qwen-turbo + async_mode=True + 30 并发 → 500 个样本约 10-20 分钟搞定。

> 来源：[[RAG评测/14.RAGAS评测时，如何加速评测速度.md]]

---

## Q11: RAG 上线后怎么做持续评测？怎么发现线上 Bad Case？

**难度**: ⭐⭐⭐ 高级

**参考答案**:

### 线上持续评测体系

```
在线流量（真实用户提问）
    │
    ├─→ 采集层：记录 {timestamp, question, answer, contexts, user_feedback}
    │
    ├─→ 监控层：实时计算 Context Recall / Faithfulness / Answer Relevancy
    │          ↓ 低于阈值时触发告警
    │
    ├─→ 抽样层：按比例抽样，定时跑 RAGAS 全指标评估
    │
    └─→ Bad Case 池：低质量样本自动进入 Bad Case 待处理队列
```

### Bad Case 发现与闭环

```
Step 1：线上采样（每日从真实流量中抽 1% 样本）
    │
    ▼
Step 2：自动标注（用 RAGAS Judge 批量评估，打低分的自动进 Bad Case 池）
    │
    ▼
Step 3：人工审核（工程师每周过一遍 Bad Case 池）
    │
    ▼
Step 4：根因定位（检索层？生成层？分片？Embedding？）
    │
    ▼
Step 5：修复（调 chunkSize / 换 Embedding / 加元数据过滤 / 改 Prompt）
    │
    ▼
Step 6：回归测试（把 Bad Case 加进评测集，确保下次评测得分不下降）
    │
    ▼
Step 7：上线新模型/新策略 → 监控线上指标变化
```

### 预发回放机制

上线新模型/新策略**前**，用 Bad Case 池 + 黄金评测集**离线回放**一遍，确认：
- 关键指标（Context Recall / Faithfulness）不下降
- 之前的 Bad Case 是否真的被修好了
- 有没有引入新的 Bad Case

**这是"防回归"的核心机制**——比等到线上出问题再修快得多。

> 来源：[[RAG评测/28.线上幻觉Case的预发回放机制]]、[[RAG评测/27.RAG评测总结]]
