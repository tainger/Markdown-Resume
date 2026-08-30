# AGENTS.md — LLM Wiki Schema 规范

> 本文件是 LLM Agent（Trae / Claude Code / Codex 等）在本库中维护 wiki 层的唯一规范。
> 模式来源：Karpathy 的 LLM Wiki —— 知识被**编译一次、持续积累**，而不是每次查询从零推导。

## 1. 分层与所有权（最重要的纪律）

| 层 | 位置 | 所有者 | Agent 权限 |
|:---|:---|:---|:---|
| **raw 层** | 除 `wiki/` 外的所有笔记目录 | 人类 | **只读**。禁止修改、移动、重命名任何现有笔记 |
| **wiki 层** | `wiki/` | LLM | 读写。人类可随时推翻 Agent 的修改，以人类为准 |
| **output 层** | `自媒体/` 等成品目录 | 人类定稿 | 只可**新建**草稿文件，禁止修改已定稿文章 |

违反所有权 = 破坏整个体系。当任务模糊时，默认写进 `wiki/`，不碰 raw 层。

## 2. wiki 目录结构

```
wiki/
├── index.md        # 全局索引（每次 ingest 后保持最新）
├── log.md          # 操作日志（append-only，记录每次 ingest/lint/output）
├── concepts/       # 概念页：算法思想、技术机制、方法论
├── entities/       # 实体页：具体组件/工具/系统（HashMap、Redis…）
├── comparisons/    # 对比页：两个以上对象的横向比较
└── synthesis/      # 综合页：跨目录串联的面试主线/方案主线
```

## 3. 页面 frontmatter 规范

每个 wiki 页面必须带：

```yaml
---
type: concept      # concept | entity | comparison | synthesis
status: seed       # seed(骨架：索引+一句话总结) | growing(喂养中) | stable(已稳定)
updated: 2026-08-30
---
```

页面职责：
- **concept**：一个思想的「一句话总结 + 覆盖例题/笔记 + 易错点」，是复习入口
- **entity**：一个组件的「定位 + 核心笔记导航 + 高频追问链」
- **comparison**：只放对比表和选型结论，细节链回 raw
- **synthesis**：按面试答题顺序串联多个目录的笔记，标注「答题骨架」

单页超过 200 行必须拆分。

## 4. 双链语法

- 统一用 `[[目录/文件名]]`，路径从仓库根开始，**不带 .md 后缀**
- 示例：`[[redis/持久化]]`、`[[算法思想/双指针与滑动窗口]]`
- wiki 层当前是私有层（不发布到 VitePress），双链供 Agent 与编辑器使用；
  未来若发布，由 markdown-it hook 把 `[[..]]` 转成站内路由

## 5. 四个工作流

| 工作流 | 触发指令 | Agent 动作 |
|:---|:---|:---|
| **Ingest** | `消化 <路径>` | 读 raw 笔记 → 创建/更新相关 concept/entity 页 → 维护双链 → 更新 index.md → log 追加记录 |
| **Query** | 向 Agent 提问 | 优先基于 wiki 已编译知识回答，引用来源页；有价值的结论落回 wiki（对应页面 +1 条） |
| **Lint** | `lint wiki` | 检查：死链、矛盾条目、孤立页（无任何入链/出链）、raw 已删除导致过期 → 输出「更新/合并/标过期」三动作 → log 记录 |
| **Output** | `从 wiki 出草稿` | 从 synthesis/comparison 提炼文章草稿，**新建文件**放到目标目录，不动旧文 |

### Ingest 的增量原则

- 优先用 `git diff` 找变更文件，只消化新增/变更部分，**禁止全库重扫**
- 新笔记挂载时先查现有 concept 页，命中就追加，不命中再新建页面
- 同一主题出现两份 raw 笔记时，不擅自合并 raw，只在 wiki 层互相链接并在 log 标注

## 6. 内容纪律

1. **不虚构**：每个论断必须能指回 raw 笔记路径（双链）。raw 里没有的知识，标注「⚠️ raw 缺失，待补充」
2. **冲突处理**：发现新旧条目矛盾 → 输出「更新/合并/标过期」三动作之一，并写入 log，不静默覆盖
3. **溯源**：concept 页的每道例题、entity 页的每条结论都带双链
4. **一页一主题**：页面超范围就拆新页并双链
5. **中文书写**，术语首次出现给英文（如 滑动窗口 Sliding Window）
