# Delta for spaced-repetition

## Purpose

为已入库的卡片提供 FSRS-5 间隔重复算法调度：根据用户评分（Again/Hard/Good/Easy）更新卡片的 `due`、`stability`、`difficulty`、`state` 等字段，目标保留率 0.9。算法实现复用 `ts-fsrs` 库，不自实现 SM-2。

## ADDED Requirements

### Requirement: FSRS-5 算法集成

系统 SHALL 使用 `ts-fsrs` 库初始化 FSRS-5 算法实例，目标保留率（`request_retention`）默认 0.9，`enable_fuzz` 默认 false。

#### Scenario: 新卡首次评分

- GIVEN 一张 `state=New` 的卡片
- WHEN 用户评分 `Good`（Rating=3）
- THEN 返回的卡片状态包含 `due`（晚于当前时间）、`stability`（>0）、`difficulty`（在合理区间）、`state=Learning`、`reps=1`、`lapses=0`

#### Scenario: 学习阶段卡再次评 Again

- GIVEN 一张 `state=Learning` 的卡片，`reps=1`
- WHEN 用户评分 `Again`（Rating=1）
- THEN 返回的卡片 `state=Relearning`，`lapses` +1
- AND `due` 在当前时间附近（10 分钟级别）

### Requirement: 评分接口

`fsrs.ts` SHALL 导出 `grade(card, rating)` 函数，输入一张卡片与一个 Rating 枚举值，输出更新后的卡片对象。原卡片对象不被 mutate。

#### Scenario: 评分不修改原对象

- GIVEN 卡片对象 `cardA`，`due` 为时间戳 T1
- WHEN 调用 `grade(cardA, Rating.Good)`，返回新对象 `cardB`
- THEN `cardA.due` 仍为 T1（未被修改）
- AND `cardB.due` 不同于 T1

#### Scenario: 四档评分都产生不同 due

- GIVEN 同一张 `state=Review` 卡片，stability=10 天
- WHEN 分别调用 `grade(card, Again)` / `Hard` / `Good` / `Easy`
- THEN 四次返回的 `due` 时间戳按 Again < Hard < Good < Easy 排序
- AND Again 的 `state` 为 `Relearning`，其他为 `Review`

### Requirement: 新卡初始化

`fsrs.ts` SHALL 导出 `initNewCard()` 函数，返回一张 `state=New` 的默认卡片对象，`due` 为当前时间（立即可复习），`reps=0`、`lapses=0`、`stability` 与 `difficulty` 为 FSRS 默认初值。卡片的 `anchor` 字段 SHALL 为卡片锚点文本（容器标题；无标题时为 `card-{序号}`），由扫描器写入，用于 ID 生成与复习页展示。

#### Scenario: 扫描入库时初始化

- GIVEN CardScanner 扫描到一对新的 srs-front/srs-back，IndexedDB 中无此 id 的卡片
- WHEN 执行 upsert
- THEN 新卡片以 `initNewCard()` 返回值为基础，补充 `id`、`front`、`back`、`filePath`、`anchor` 字段后入库
- AND 该卡片在 `/review` 立即出现在 due 队列中

### Requirement: 评分历史记录

每次评分 SHALL 在 `reviews` 表追加一条记录，字段包含 `cardId`、`ts`、`rating`、`prev_state`、`next_state`，用于后续统计与参数微调。

#### Scenario: 评分后 reviews 表新增记录

- GIVEN IndexedDB `reviews` 表为空，`cards` 表有一张卡片 `id=X`
- WHEN 用户在 `/review` 对该卡片评分 `Good`
- THEN `reviews` 表新增一条记录：`{ cardId: 'X', ts: <now>, rating: 3, prev_state: 'New', next_state: 'Learning' }`

#### Scenario: 多次评分形成时间序列

- GIVEN 卡片 `id=X` 已被评分 3 次
- WHEN 查询 `reviews` 表中 `cardId='X'` 的所有记录
- THEN 返回 3 条记录，按 `ts` 升序排列
- AND 第 1 条的 `prev_state` 为 `New`，第 2 条的 `prev_state` 为第 1 条的 `next_state`

### Requirement: 状态字段完整性

评分后卡片的 `due`、`stability`、`difficulty`、`state`、`reps`、`lapses`、`lastReview` 字段 SHALL 全部被更新。

#### Scenario: 字段同步更新

- GIVEN 卡片 `id=X`，当前 `reps=2`、`lapses=0`、`lastReview=null`
- WHEN 用户评分 `Again`
- THEN `cards` 表中该卡片 `reps=3`、`lapses=1`、`lastReview` 为当前时间戳
- AND `due`、`stability`、`difficulty`、`state` 同步更新为 FSRS 计算结果
