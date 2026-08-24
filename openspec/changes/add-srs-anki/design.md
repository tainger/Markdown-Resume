# Design: Add SRS (Anki-style) Spaced Repetition to VitePress Notes

## Technical Approach

### 1. 卡片标记：markdown-it 自定义容器

VitePress 基于 markdown-it，原生支持 `::: container-name` 语法（fenced container）。在 `config.mts` 的 `markdown.config` hook 里追加两个自定义容器注册：

```ts
// 伪代码示意，完整实现在 CardScanner 注册阶段
md.use(container, 'srs-front', { /* marker */ })
md.use(container, 'srs-back',  { /* marker */ })
```

渲染输出为 `<div class="srs-front">...</div>` 与 `<div class="srs-back">...</div>`，与现有 `CodeFolder.vue` 扫描的 `div[class*="language-"]` 同模式，可直接被 `CardScanner.ts` 定位。

**为何不用 HTML 注释 `<!-- anki:front -->`**：Markdown 源文件查看时不可见，复制粘贴易丢标记，违反"原笔记可读"原则。

**为何不用单容器 + frontmatter 分隔**：双容器正反两面独立可见、易复制，写错位风险通过扫描阶段做对偶校验兜住。

### 2. 卡片扫描：复用 CodeFolder 范式

`CardScanner.ts` 完全照抄 [CodeFolder.vue](file:///Users/rocky/study/profile/Markdown-Resume/.vitepress/theme/CodeFolder.vue) 的副作用套路：

```ts
// 伪代码
onMounted(run)
watch(() => route.path, run)

function run() {
  nextTick(() => setTimeout(scanCards, 0))
}

function scanCards() {
  const content = document.querySelector('.vp-doc')
  if (!content) return
  const fronts = Array.from(content.querySelectorAll('div.srs-front'))
  const backs  = Array.from(content.querySelectorAll('div.srs-back'))
  // 按 DOM 顺序两两配对（front[i] + back[i]）
  // 锚点取 front 容器内 .srs-front-title 文本（无标题回退 card-{序号}）
  // upsert 到 IndexedDB
}
```

**关键约束**：扫描只做 upsert 不做 delete——避免用户临时切到不含卡片的页面导致删除已入库卡片。

**幂等保护**：与 CodeFolder 的 `dataset.folded` 同思路，扫描完成的 `.vp-doc` 打 `dataset.scanned` 标记，避免路由反复切换触发冗余 upsert。

### 3. 卡片 ID 策略：标题锚点优先，序号兜底

```ts
// 锚点：容器标题（::: srs-front mvcc → <p class="srs-front-title">mvcc</p>）优先，无标题回退序号
const anchor = title || `card-${indexInPage}`
const cardId = sha256(`${decodedFilePath}#${anchor}`)?.slice(0, 16)
```

- `decodedFilePath` 来自 `decodeURIComponent(useRoute().path)`（如 `/jvm/内存结构.html` → `jvm/内存结构.md`）。中文路径可能被 URL 编码，必须先解码再 hash，否则同一页面经不同编码入口（链接 vs 直接输入）会产生不同 id（CodeFolder.vue 已为此做过解码）
- `title` 是容器标题：markdown-it 默认容器渲染器会把 `::: srs-front mvcc` 渲染为 `<div class="srs-front"><p class="srs-front-title">mvcc</p>...</div>`，零新增语法
- 无标题卡片用 `card-{indexInPage}`（本页所有 `srs-front` 中的序号，0-based）作为锚点

**为何不用内容 hash**：笔记内容改了，卡片应该保留复习进度（同一知识点换种说法仍是同一张卡）；只有删除整张卡才会丢失进度，符合预期。

**为何标题优先**：序号锚点在卡片重排、前插、删除时必然漂移；显式标题使锚点与位置解耦，带标题的卡片重排不丢进度。**代价**：带标题卡片要求作者写一个短锚点名（增量新增策略下成本极低，且标题可读）；未命名卡片仍接受「重排丢进度」的 trade-off。

### 4. 持久化：IndexedDB + idb

不用 localStorage（5MB 上限，几百张卡片 + 复习历史会爆）。三张表：

```ts
// db.ts
const DB_NAME = 'srs-notes'
const DB_VERSION = 1

const db = openDB(DB_NAME, DB_VERSION, {
  upgrade(db) {
    const cards = db.createObjectStore('cards', { keyPath: 'id' })
    cards.createIndex('due', 'due')           // 按到期日查询
    cards.createIndex('filePath', 'filePath') // 按源文件查询

    const reviews = db.createObjectStore('reviews', { keyPath: ['cardId', 'ts'] })
    // 每次评分追加一条记录

    db.createObjectStore('meta', { keyPath: 'key' })
    // 存 fsrs params、target retention、schema version
  },
})
```

### 5. FSRS-5 集成

```ts
// fsrs.ts
import { fsrs, generatorParameters, Rating, State } from 'ts-fsrs'

const params = generatorParameters({ enable_fuzz: false, request_retention: 0.9 })
const f = fsrs(params)

export function grade(card: SRSCard, rating: Rating): SRSCard {
  const now = new Date()
  const result = f.repeat(cardToFSRS(card), now)
  const next = result[rating].card  // ts-fsrs 5.x：repeat 返回 Record<Rating, RecordLogItem>，取 .card
  return { ...card, ...cardFromFSRS(next), lastReview: now.getTime() }
}
```

**字段映射注意**：ts-fsrs 的 Card 字段为 snake_case（`scheduled_days`、`last_review`、`elapsed_days`，`due` 为 `Date`，`state` 为枚举），与 DB 的 camelCase / 时间戳格式不同。`cardToFSRS` / `cardFromFSRS` 两个转换函数负责双向映射，字段名不匹配是集成期最常见的坑。

### 6. 复习路由：review.md 占位 + 组件内嵌（已决策）

**决策**：项目根新建 `review.md`，内容为：

```md
# 复习

<ReviewPage />
```

VitePress 原生支持在 Markdown 中直接使用 Vue 组件，占位页在 SSG 阶段仅渲染占位文本，浏览器端交互全部发生在 `onMounted`。

**为何不用 Layout slot 注入**：slot 方案需要 `route.path === '/review/'` 精确匹配，尾斜杠、`.html` 后缀、URL 编码等变体都会导致匹配失败，脆弱；md 内嵌方案零匹配逻辑、官方支持、占位内容可读。

**SSR 约束**：`ReviewPage` 的 IndexedDB 访问必须全部放在 `onMounted` 内（SSG 构建时组件会被服务端渲染一次，浏览器 API 不可用）。

**附带影响**：`review.md` 会被本地搜索索引收录（内容仅为占位文本），噪音可忽略；如介意可在 `themeConfig.search.options` 的 `ignore` 中排除。

## Architecture Decisions

### Decision: ts-fsrs over self-implemented SM-2

**为什么**：FSRS-5 是 Anki 团队 2023+ 的默认算法，社区有大量实测对比数据，保留率预测准确度显著高于 SM-2。自实现 SM-2 看似简单，但调度效果差，且复用 `ts-fsrs`（活跃维护、TS 原生）省 200 行代码与调试成本。

**代价**：增加 ~10KB 依赖。在静态站点语境下可接受。

### Decision: IndexedDB over localStorage

**为什么**：卡片 + 复习历史会持续增长，localStorage 5MB 上限会在数百张卡片时触顶。IndexedDB 容量 GB 级、异步 API 不阻塞主线程、支持索引查询。

**代价**：API 比 localStorage 复杂。用 `idb` 库封装为 Promise 风格，API 接近 localStorage 的简洁度。

### Decision: 双容器标记语法

**为什么**：
- VitePress/markdown-it 原生支持 fenced container，零额外解析
- Markdown 源文件查看时正反两面独立可见，复制粘贴不丢
- 错位风险（front[i] 与 back[i] 编号不一致）通过扫描阶段对偶校验兜住：发现孤儿 front/back 时记录 warning 但不阻断

**拒绝的备选**：
- 单容器 + frontmatter 分隔：紧凑但单卡片整体性强，写起来稍约束
- HTML 注释：源文件查看时不可见，违反"原笔记可读"

### Decision: 卡片 ID 用 (filePath + 锚点) 而非内容 hash

**为什么**：复习进度应该跟随"这一位置的卡片"而非"这段文字"。改写卡片内容不应重置进度（同一知识点换种说法仍应继承历史评分）。

**代价**：用户重排卡片顺序会丢进度。可接受——重排笔记本身不常见。

### Decision: 扫描只 upsert 不 delete

**为什么**：避免用户切到不含卡片的页面时误删已入库卡片。代价：DB 中可能积累孤儿记录（笔记删除后卡片仍在 DB）。

**缓解**：未来加一个"清理孤儿"工具命令，或在 stats 页提示孤儿数量。

### Decision: 复习页提供卡片删除入口

**为什么**：upsert 不 delete 会永久保留废弃/孤儿卡片；个人使用场景下，「删掉这张没用的卡」是自然需求，不能等未来工具。在复习页为当前卡片提供「删除」按钮（二次确认），从 `cards` 表删除该卡片并推进队列，`reviews` 历史保留用于统计。

**代价**：误删会丢失该卡复习进度，用二次确认缓解；删除只影响显式操作的卡片，与扫描策略互不干扰（再次访问笔记页会将其作为新卡重新入库）。

### Decision: 复习入口放导航栏常驻

**为什么**：复习是高频动作，全站可达最省事。代价：导航栏已有 10 个目录按钮，再加一个会略挤，考虑用 emoji 图标 📯 "复习" 减小宽度。

**拒绝的备选**：
- 首页快捷入口：必须先回首页，多一次跳转
- 两者都加：组件开发量略大，等核心流程跑通后再加首页入口

## Data Flow

```
笔记 Markdown                            浏览器 IndexedDB
  │                                          ▲
  │ markdown-it                              │
  ▼                                          │
.vp-doc DOM                                   │
  │                                          │
  │ CardScanner.ts (onMounted + watch route) │
  └──────────────────────────────────────────┘
                                              │ upsert
                                              │
                              ┌───────────────┴────────────────┐
                              │ cards: id, front, back, due,    │
                              │   stability, difficulty, state   │
                              │ reviews: cardId, ts, rating       │
                              │ meta: fsrs_params, retention     │
                              └────────────────┬─────────────────┘
                                               │ query due
                                               ▼
                              ┌──────────────────────────────────┐
                              │ ReviewPage.vue                   │
                              │ 1. 拉 due < now 的卡片            │
                              │ 2. 显示正面 → 用户回想            │
                              │ 3. 翻面 → 4 个评分按钮             │
                              │ 4. grade() → 写回 cards 与 reviews │
                              │ 5. 推进队列下一张                 │
                              └──────────────────────────────────┘
```

## File Changes

- `.vitepress/config.mts`（修改）— 注册 `srs-front` / `srs-back` 自定义容器
- `.vitepress/theme/index.ts`（修改）— 注册 `/review` 路由 + 导航栏按钮
- `.vitepress/theme/srs/CardScanner.ts`（新建）
- `.vitepress/theme/srs/db.ts`（新建）
- `.vitepress/theme/srs/fsrs.ts`（新建）
- `.vitepress/theme/srs/ReviewPage.vue`（新建）
- `.vitepress/theme/srs/styles.css`（新建）
- `review.md`（新建，作为 `/review` 路由占位）
- `package.json`（修改）— 添加 `ts-fsrs` 与 `idb` 依赖
