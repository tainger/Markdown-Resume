---
type: log
status: stable
updated: 2026-08-30
---

# 操作日志（append-only）

> 只追加不修改。每条记录：日期 | 动作 | 摘要。

## 2026-08-30

- **[bootstrap]** 初始化 wiki 层：按 AGENTS.md 规范创建 index / concepts×12 / entities×8 / comparisons×1 / synthesis×4。全部为 seed 状态（索引 + 一句话总结 + 双链），内容深度靠日常喂养。
- **[lint]** 首轮扫描发现 **raw 层同名重复笔记**（不擅动 raw，仅在 wiki 层互链，待人类决策合并或差异化）：
  - `算法思想/前缀和与差分` ↔ `数据结构/前缀和与差分`
  - `算法思想/并查集` ↔ `数据结构/并查集`
  - `算法思想/哈希表` ↔ `数据结构/哈希表`
  - `算法思想/栈` ↔ `数据结构/栈`
  - `算法思想/队列` ↔ `数据结构/队列`
  - `算法思想/链表` ↔ `数据结构/链表`
- **[note]** `自媒体/微信公众号/探小虎/ LLM Wiki 技术调研` 文件名含前导空格，站上路由已正常生成，但链接书写时需注意。
- **[ingest/raw 决策]** 用户确认处理 6 对同名笔记（raw 层变更已获授权）：
  - `算法思想/前缀和与差分`、`算法思想/并查集` → **真重复**。独有内容（437 树上前缀和提示、华为OD 25/3 题链接）并入 `数据结构/` 对应篇目后删除原文件；`算法思想/README.md` 索引重排（32→31 篇）并留痕。
  - `算法思想/{哈希表,栈,队列,链表}` ↔ `数据结构/{同名}` → **差异化双篇**（套路篇 vs 结构篇，README 尾注已注明定位），全部保留。
  - 同步修复入链 9 处：`算法思想/{循环,数组,统计,哈希表,图论专题}`、`面试/算法面试实战指南.md`、`算法思想/README.md`。
  - wiki 侧：concepts/前缀和与差分、concepts/并查集、concepts/链表技巧、concepts/单调栈与单调队列、entities/HashMap 已同步更新。
- **[output]** 根目录 README.md 新增「LLM Wiki（Agent 知识编译层）」章节（三层架构/四条指令/纪律），目录树补入 `wiki/` 与 `AGENTS.md`。
- **[output]** 新建 [[wiki/usage]] 使用手册：7 个实战 Case（Ingest/Query/Drill/Lint/Output/Plan/Deep-dive）+ 指令速查 + 日常节奏；index、README、AGENTS.md 三处已挂入口。
- **[output]** usage.md 扩充至 12 个 Case，新增：复盘回流 Backflow（真题喂回实体页）、JD Gap 分析（三档覆盖清单）、Locator 定位器、简历背书审计、新主题播种 Seed；日常节奏表同步扩充。
- **[output]** 按「单页 ≤200 行」纪律拆分手册：新建 [[wiki/usage-advanced]] 进阶卷，新增 8 个 Case：考前速记包、答案评分器（五段式）、闪卡+遗忘曲线、踩坑本聚合（hot100 个人总结复用）、对比页工厂、全站死链巡检（补 ignoreDeadLinks 盲区）、选题日历、健康度报告；usage.md 改名「核心卷」。
- **[ingest]** 执行 Case 16 踩坑本聚合：扫 leetcode-hot100 全部 75 个含「## 个人总结」文件（有效 59 / 占位 16）→ 产出 [[wiki/synthesis/高频踩坑本]]（Top 10 高频模式 + 13 簇速查 + 待补坑清单）。已挂载：index 综合主线、P7面试复习主线 刷题保温、usage-advanced Case 16 标记已执行。**lint 发现**：16 篇个人总结为占位符待补，列表见踩坑本末节。
