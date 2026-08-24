# Delta for card-authoring

## Purpose

笔记作者能在任意 Markdown 笔记中以双容器语法标记知识点卡片（正/反面），前端访问该笔记时自动扫描标记并 upsert 到浏览器 IndexedDB。卡片 ID 由「文件相对路径 + 卡片锚点（容器标题优先、序号兜底）」hash 生成，使笔记内容修改不重置 FSRS 复习进度。

## ADDED Requirements

### Requirement: 双容器标记语法

笔记 Markdown MUST 支持以 `::: srs-front` 与 `::: srs-back` 两个 fenced container 标记知识点卡片的正反面，各自以 `:::` 闭合。

#### Scenario: 标准双卡片标记

- GIVEN 一个 Markdown 文件含
  ```
  ::: srs-front
  MVCC 是怎么实现的？
  :::

  ::: srs-back
  通过 undo log + read view 实现。
  :::
  ```
- WHEN VitePress 渲染该页面
- THEN `.vp-doc` DOM 中存在一个 `<div class="srs-front">` 包含"MVCC 是怎么实现的？"
- AND 存在一个 `<div class="srs-back">` 包含"通过 undo log + read view 实现。"

#### Scenario: 带标题的卡片

- GIVEN Markdown 含 `::: srs-front mvcc`（标题为 mvcc）
- WHEN VitePress 渲染该页面
- THEN `.vp-doc` DOM 中存在 `<div class="srs-front">` 且内含 `<p class="srs-front-title">mvcc</p>`
- AND 该标题作为卡片锚点参与 ID 生成（见「卡片 ID 稳定性」）

#### Scenario: 卡片内含代码块

- GIVEN `::: srs-back` 容器内含 ` ```java ... ``` ` 代码块
- WHEN 页面渲染
- THEN 代码块在 `<div class="srs-back">` 内被正确语法高亮渲染
- AND 不与现有的 markdown-it 花括号转义 hook 冲突

#### Scenario: 容器未闭合

- GIVEN 一个 Markdown 文件含 `::: srs-front` 但缺少闭合 `:::`
- WHEN VitePress 渲染该页面
- THEN markdown-it 容器插件按其默认规则处理（要么渲染为段落、要么报 markdown 警告）
- AND 浏览器 console 不抛出未捕获异常

### Requirement: 卡片扫描入库

访问任笔记页时，前端 SHALL 扫描 `.vp-doc` 中所有 `div.srs-front` 与 `div.srs-back`，按 DOM 顺序两两配对，提取 front 容器内标题（`.srs-front-title` 文本）作为锚点，并 upsert 到 IndexedDB `cards` 表。

#### Scenario: 首次访问含卡片的笔记

- GIVEN 笔记 `/jvm/内存结构.md` 含 3 对 srs-front/srs-back，IndexedDB `cards` 表为空
- WHEN 用户访问 `/jvm/内存结构.html`
- THEN `cards` 表新增 3 条记录，每条记录的 `front` 与 `back` 字段为该容器渲染后的 HTML（来自 `.vp-doc` DOM，`v-html` 直接可渲染）
- AND `state` 字段为 `New`
- AND `reps` 与 `lapses` 字段为 0

#### Scenario: 重复访问同笔记

- GIVEN 笔记 `/jvm/内存结构.md` 已有 3 张卡片入库，其中卡片 #2 已复习过一次（`reps=1`）
- WHEN 用户再次访问该页面
- THEN `cards` 表中卡片 #2 的 `reps` 字段保持为 1（不重置）
- AND 卡片 #2 的 `front` / `back` 字段更新为当前页面内容（如果作者改写过）

#### Scenario: 改写卡片内容不改顺序

- GIVEN 卡片 `id=abc123` 已入库，`due` 为 3 天后，`stability=2.5`
- WHEN 作者改写该卡片 front 内容（不增删、不调换卡片顺序）后访问页面
- THEN `cards` 表中 `id=abc123` 的 `front` 字段更新为新内容
- AND `due` 仍为 3 天后
- AND `stability` 仍为 2.5

#### Scenario: 笔记中无卡片

- GIVEN 笔记 `/index.md` 不含任何 srs-front/srs-back 标记
- WHEN 用户访问首页
- THEN `cards` 表不被修改（不删除任何已有卡片）
- AND 控制台无错误日志

### Requirement: 卡片 ID 稳定性

卡片 ID SHALL 由「文件相对路径 + `#` + 卡片锚点」的 hash 前 16 位生成，使得笔记内容修改不改变 ID。锚点 SHALL 优先取容器标题（front 容器内 `.srs-front-title` 文本），无标题时回退为「`card-` + 页内 srs-front 序号」。文件路径在参与 hash 前 MUST 先经 `decodeURIComponent` 解码（中文路径可能被 URL 编码）。

#### Scenario: 改写卡片内容

- GIVEN 笔记 `/jvm/内存结构.md` 第 2 个 srs-front 对应卡片 `id=X`
- WHEN 作者改写该卡片 front 内容（顺序不变）
- THEN 扫描后该卡片在 `cards` 表中的 `id` 仍为 `X`

#### Scenario: 带标题卡片重排不丢进度

- GIVEN 笔记含带标题卡片 A（`::: srs-front a`）与卡片 B（`::: srs-front b`）
- WHEN 作者把卡片 B 移到卡片 A 之前
- THEN 卡片 B 的 `id` 不变（锚点为标题 `b`，与序号无关）
- AND 卡片 A 的 `id` 不变
- AND 两张卡的复习进度均保留

#### Scenario: 无标题卡片重排

- GIVEN 笔记 `/jvm/内存结构.md` 含无标题卡片 A（序号 0）与卡片 B（序号 1）
- WHEN 作者把卡片 B 移到卡片 A 之前
- THEN 卡片 B 在 `cards` 表中 `id` 改变（序号从 1 变 0）
- AND 卡片 B 的复习进度被新 ID 视为新卡（`state=New`）

  > 注：这是无标题卡片的已知 trade-off，记录在 design.md。

### Requirement: 孤儿容错

扫描时遇到无配对的孤儿 `srs-front` 或 `srs-back`，前端 SHALL 在控制台记录 warning 但不阻断扫描，也不抛出未捕获异常。

#### Scenario: 只有 front 没有 back

- GIVEN 笔记含 2 个 `srs-front` 但只有 1 个 `srs-back`
- WHEN 用户访问该页面
- THEN 多出的 `srs-front` 不入库
- AND 控制台输出 warning：`SRS: orphan srs-front at index N in <filePath>`
- AND 已配对的 1 张卡片正常入库

#### Scenario: 只有 back 没有 front

- GIVEN 笔记含 1 个 `srs-front` 与 2 个 `srs-back`
- WHEN 用户访问该页面
- THEN 多出的 `srs-back` 不入库
- AND 控制台输出 warning
- AND 已配对的 1 张卡片正常入库

### Requirement: 路由切换重新扫描

前端 SHALL 在 VitePress 路由切换时重新触发扫描，复用 `CodeFolder.vue` 的 `onMounted` + `watch(route.path)` 范式。

#### Scenario: 从笔记 A 切到笔记 B

- GIVEN 用户当前在 `/jvm/内存结构.html`（已扫描入库该页卡片）
- WHEN 用户点击侧边栏跳转到 `/mysql/索引.html`（含不同卡片）
- THEN 在 `nextTick(setTimeout(...))` 后，`/mysql/索引.html` 的卡片被 upsert 到 `cards` 表
- AND `/jvm/内存结构.html` 的卡片在 `cards` 表中保留
