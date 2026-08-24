# Proposal: Add SRS (Anki-style) Spaced Repetition to VitePress Notes

## Intent

把现有 VitePress 学习笔记站点升级为「可主动回忆」的复习工具：在笔记 Markdown 里以双容器语法标记知识点卡片，前端扫描入库，用 FSRS-5 间隔重复算法驱动每日复习队列，复习数据持久化在浏览器 IndexedDB 中。

**核心诉求**：用户自己复习用，不切工具——边读笔记边把知识点沉淀为可复习卡片，按"今日到期 → 翻面 → 四档评分 → 算法调度"的标准 Anki 流程闭环。

## Scope

### In scope

- **卡片标记语法**：在 Markdown 中以 `::: srs-front` / `::: srs-back` 双容器标记知识点正反面，注册到 VitePress markdown-it 自定义容器列表；支持 `::: srs-front 锚点名` 为卡片命名，重排时保留复习进度。
- **卡片扫描入库**：访问任意笔记页时，前端扫描 `.vp-doc` 中标记的卡片，upsert 到 IndexedDB；卡片 id 用「文件相对路径 + 卡片锚点（容器标题优先、序号兜底）」hash，保证笔记内容修改不重置复习进度。
- **间隔重复算法**：使用 `ts-fsrs` 库（FSRS-5）实现评分 → 状态更新 → 到期日计算，目标保留率默认 0.9。
- **复习界面**：独立路由 `/review/`，展示当日 due 卡片，翻面交互 + Again/Hard/Good/Easy 四档评分按钮。
- **复习入口**：导航栏常驻「复习」按钮，全站任何页面可达。
- **存储**：IndexedDB 三表（cards / reviews / meta），用 `idb` 库封装。
- **增量新增策略**：仅从今天起新写的笔记加卡片标记，旧笔记不动。

### Out of scope

- 跨设备同步（账号体系、云端存储）——后续 change。
- 旧笔记批量改造（脚本半自动提取）——后续 change。
- 卡片图片、音频附件——后续 change。
- FSRS 参数个性化微调（基于个人复习数据拟合一组专属参数）——后续 change。
- 移动端原生 App。

## Approach

### 整体架构

复用现有 `CodeFolder.vue` 范式（`onMounted` + `watch(route.path)` + `nextTick(setTimeout(...))` 对 `.vp-doc` 做 DOM 后处理 + 通过 `doc-after` slot 注入），新增三条独立能力：

```
┌─────────────────────────────────────────────────────────────┐
│  笔记 Markdown 源                                            │
│  ::: srs-front ... :::  ::: srs-back ... :::                │
└──────────────────────┬──────────────────────────────────────┘
                       │ markdown-it 自定义容器渲染
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  .vp-doc DOM                                                 │
│  <div class="srs-front">...</div> <div class="srs-back">... │
└──────────────────────┬──────────────────────────────────────┘
                       │ CardScanner.ts 扫描
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  IndexedDB                                                   │
│  cards(id, front, back, due, stability, difficulty, ...)     │
│  reviews(cardId, ts, rating, prev_state, next_state)         │
│  meta(version, fsrs_params, target_retention)                │
└──────────────────────┬──────────────────────────────────────┘
                       │ /review/ 路由查询 due
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  ReviewPage.vue                                              │
│  正面 → 翻面 → [Again] [Hard] [Good] [Easy]                  │
│  调 ts-fsrs.grade(card, rating) → 新状态 → 写回              │
└─────────────────────────────────────────────────────────────┘
```

### 工程结构（贴合现有约定）

```
.vitepress/
├── config.mts              # 加：注册 srs-front / srs-back 自定义容器
├── theme/
│   ├── index.ts            # 加：注册 /review 路由 + 导航栏按钮
│   ├── CodeFolder.vue      # 已有，参考范式
│   ├── GiscusComment.vue   # 已有
│   └── srs/
│       ├── ReviewPage.vue  # /review 主界面
│       ├── StatsPage.vue   # /review/stats（后续 change）
│       ├── CardScanner.ts  # 扫描 .vp-doc 提取卡片 → upsert DB
│       ├── db.ts           # idb 封装
│       ├── fsrs.ts          # ts-fsrs 薄封装
│       └── styles.css      # 卡片与按钮样式
```

### 依赖增量

```
devDependencies:
  + ts-fsrs        (~10KB)
  + idb            (~5KB)
```

### 算法选择依据

| 算法 | 来源 | 准确度 | 复杂度 | 选择 |
| --- | --- | --- | --- | --- |
| SM-2 | Anki 早期（1985） | 保留率预测误差大 | 30 行 | ❌ |
| FSRS-5 | Anki 5+ 默认（2023） | 误差 <5% | 200 行，但 `ts-fsrs` 现成 | ✅ |

参考个人项目 CyberLoRA 的选型哲学："用相似度数据做选型而非拍脑袋"，这里同理——用社区实测的算法对比结论做选型，不重复造轮子。

## Non-goals / Trade-offs

- **不跨设备同步**：纯前端 IndexedDB，单机复习。代价：换设备要重新建立复习进度。后续 change 可加云端同步。
- **不批量改造旧笔记**：增量新增。代价：初期卡片库规模小，复习队列短期内不饱和。可接受——优先保证流程跑通。
- **不实现参数微调**：用 FSRS 默认参数。代价：算法精度略低于"基于个人数据微调过的参数"。后续可加。
- **不依赖后端**：纯静态站点，部署在 Vercel。代价：复习数据不能跨设备，但部署零成本不变。
