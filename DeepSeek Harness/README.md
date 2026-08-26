# DeepSeek Harness

> 应用开发视角的 DeepSeek 学习路径 + 面试资料。「Harness」= 驾驭 DeepSeek 做工程落地：**选模型 → 调 API → 接 RAG/Agent → 懂必要原理 → 部署控成本**。模型原理只讲「应用开发者够用」的深度，数学推导见原论文。

---

## 学习路径（按 P 级推进）

| 级别 | 主题 | 文件 | 目标 |
|:---:|:---|:---|:---|
| P0 | 模型家族与选型 | [01.模型家族与选型](01.模型家族与选型.md) | 知道 `deepseek-chat` / `deepseek-reasoner` 分别对应 V3 / R1，什么场景用哪个 |
| P0 | API 应用开发 | [02.API应用开发](02.API应用开发.md) | Chat / JSON / Function Call / FIM / Context Cache 全套调用 |
| P0 | RAG 与 Agent 集成 | [03.RAG与Agent集成](03.RAG与Agent集成.md) | 把 DeepSeek 接进 LangChain/LlamaIndex，做工具调用与规划 |
| P1 | 核心原理速览 | [04.核心原理速览](04.核心原理速览.md) | MoE / MLA / MTP / R1 RL，面试够用即可 |
| P1 | 部署与成本 | [05.部署与成本](05.部署与成本.md) | 官方 API vs 自部署，定价，并发与限流 |
| P0 | 面试题 | [06.面试题](06.面试题.md) | 应用开发岗高频 Q&A |

---

## 必背速查

- `deepseek-chat` = **DeepSeek-V3**（含 V3.1 混合思考）；`deepseek-reasoner` = **DeepSeek-R1**。
- API **OpenAI 兼容**：`base_url=https://api.deepseek.com`，换 key 和 base_url 即可复用 OpenAI SDK。
- V3 = **671B MoE / 37B 激活** + **MLA**（KV 压缩）+ **MTP**；R1 = **GRPO 纯 RL** 推理模型。
- **Context Cache** 自动生效，命中价 ≈ 1/4 miss 价 → 长前缀 / 固定 system prompt 能省一大笔。
- R1 的思考过程在 `reasoning_content` 字段（不在 `content`）；`max_tokens` 算思考 token。
- 应用开发 90% 走官方 API；要私有化 / 离线 / 数据合规才上 vLLM/SGLang 自部署。

---

## 版本说明

> 本文截至 2026-08 整理。DeepSeek 迭代快（V2 → V3 → V3.1 → R1 → …），**模型版本号、API 模型名、定价、上下文长度会变**，工程落地前以官方 [platform.deepseek.com](https://platform.deepseek.com) 文档为准。

---

## 相关链接

- 官方 API 文档：<https://api-docs.deepseek.com>
- API 平台（拿 key、看用量）：<https://platform.deepseek.com>
- HuggingFace 权重：<https://huggingface.co/deepseek-ai>
- 论文：DeepSeek-V3 技术报告、DeepSeek-R1 技术报告
- 本仓相关：`AI应用开发/`、`AI应用开发岗/`、`Spring/`（Spring AI 集成 OpenAI 兼容客户端）
