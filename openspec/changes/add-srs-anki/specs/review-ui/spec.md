# Delta for review-ui

## Purpose

提供独立的复习路由 `/review` 与导航栏常驻入口，展示当日到期（`due <= now`）的卡片队列，支持「显示答案 → 翻面 → 四档评分 → 队列推进」的标准 Anki 复习交互，复习进度持久化在浏览器 IndexedDB，跨刷新保留。

## ADDED Requirements

### Requirement: 复习路由可达

系统 SHALL 在 `/review` 路径暴露复习主界面，通过项目根的 `review.md` 占位路由 + Vue 组件注入的方式实现，不破坏现有 VitePress 路由。

#### Scenario: 访问复习页

- GIVEN 站点已构建并预览（`npm run docs:preview`）
- WHEN 用户在浏览器访问 `/review`
- THEN 返回 200 状态，页面渲染 `ReviewPage` 组件
- AND 不报 404 或 Vue runtime 错误

### Requirement: 到期卡片队列

进入 `/review` 时，前端 SHALL 查询 IndexedDB `cards` 表中所有 `due <= Date.now()` 的卡片，按 `due` 升序排列作为复习队列。

#### Scenario: 首次进入有新卡

- GIVEN IndexedDB 中有 5 张 `state=New` 卡片（`due=now`）和 3 张 `state=Review` 卡片（`due` 为 2 天后）
- WHEN 用户访问 `/review`
- THEN 队列长度为 5（新卡），不包含 2 天后到期的卡片

#### Scenario: 部分卡片到期

- GIVEN IndexedDB 中有 10 张卡片，其中 3 张 `due` 已过
- WHEN 用户访问 `/review`
- THEN 队列长度为 3，按 `due` 升序排列

#### Scenario: 无到期卡片

- GIVEN IndexedDB 中所有卡片 `due` 都在未来
- WHEN 用户访问 `/review`
- THEN 页面显示"今日复习完成 🎉"或"暂无待复习卡片"提示
- AND 不显示翻面交互按钮

### Requirement: 翻面交互

卡片正面 SHALL 默认显示，底部有"显示答案"按钮；点击后显示背面，按钮切换为四个评分按钮。

#### Scenario: 显示正面

- GIVEN 队列首张卡片为 `id=X`，`front="MVCC 是怎么实现的？"`
- WHEN 页面渲染
- THEN 卡片正面显示"MVCC 是怎么实现的？"（Markdown 渲染）
- AND 底部仅显示"显示答案"按钮，无评分按钮

#### Scenario: 翻面

- GIVEN 当前显示正面
- WHEN 用户点击"显示答案"
- THEN 背面内容渲染显示
- AND "显示答案"按钮消失
- AND 底部出现 4 个评分按钮：Again / Hard / Good / Easy

### Requirement: 四档评分

翻面后用户 SHALL 能点击四个评分按钮之一，点击后调用 `grade()` 更新该卡片状态，并推进队列下一张。

#### Scenario: 评 Good 推进队列

- GIVEN 队列长度 5，当前显示第 1 张卡片背面
- WHEN 用户点击"Good"
- THEN 调用 `grade(currentCard, Rating.Good)`
- AND `cards` 与 `reviews` 表写入新状态
- AND 队列长度变为 4
- AND 显示第 2 张卡片正面

#### Scenario: 评 Again 不出队

- GIVEN 当前队列首张卡片
- WHEN 用户点击"Again"
- THEN 该卡片 `state` 变为 `Relearning`，`due` 在当前时间附近（10 分钟级别）
- AND 该卡片从当前队列移除（不出现在本会话剩余队列中）
- AND 队列下一张成为当前

  > 注：Again 的卡片 due 很近，会在用户下次访问 /review 时再次出现。本会话不重复显示避免体验疲劳。

#### Scenario: 键盘快捷键

- GIVEN 当前显示卡片背面
- WHEN 用户按 `1` / `2` / `3` / `4` 键
- THEN 分别触发 Again / Hard / Good / Easy 评分
- AND 空格键触发"显示答案"（当显示正面时）

### Requirement: 卡片删除

复习页 SHALL 为当前卡片提供「删除」操作：点击后二次确认，确认后从 `cards` 表删除该卡片并推进队列；`reviews` 表历史记录保留。删除 SHALL 只影响显式操作的卡片，不触发任何批量清理。

#### Scenario: 删除当前卡片

- GIVEN 队列首张卡片 `id=X` 显示背面
- WHEN 用户点击「删除」并确认
- THEN `cards` 表中 `id=X` 的记录被删除
- AND 队列推进到下一张卡片
- AND `reviews` 表中 `id=X` 的历史记录保留

#### Scenario: 删除后重新访问笔记

- GIVEN 卡片 `id=X` 已被用户删除，但笔记页仍含该卡片标记
- WHEN 用户再次访问该笔记页
- THEN 扫描器将 `id=X` 作为新卡重新入库（`state=New`，复习进度从零开始）

### Requirement: 队列完成提示

当队列为空时，页面 SHALL 显示"今日复习完成"提示与今日统计（已复习 N 张、总用时 M 分钟）。

#### Scenario: 完成今日队列

- GIVEN 队列最后一张卡片被评分
- WHEN 队列长度变为 0
- THEN 页面切换为"今日复习完成 🎉"视图
- AND 显示"已复习 N 张"
- AND 显示"总用时 M 分钟"
- AND 提供"返回笔记"链接回首页

### Requirement: 导航栏常驻入口

顶部导航栏 SHALL 常驻一个"复习"按钮指向 `/review`，全站任何页面可达。

#### Scenario: 任意页面访问复习

- GIVEN 用户当前在 `/leetcode-hot100/两数之和.html`
- WHEN 用户点击顶部导航栏"复习"按钮
- THEN 跳转到 `/review`
- AND 复习界面正常渲染

#### Scenario: 复习入口在导航栏可见

- GIVEN 站点已构建
- WHEN 在 DevTools 中查询 `header` 内的 nav 链接
- THEN 至少有一个链接的 `href` 末尾为 `/review` 或 `/review/`
- AND 链接文本含"复习"或"📖"

### Requirement: 计数器实时更新

复习页顶部 SHALL 显示「今日待复习 N 张 / 已复习 M 张」计数器，每次评分后实时更新，无需手动刷新。

#### Scenario: 评分后计数更新

- GIVEN 复习页显示「待复习 5 / 已复习 0」
- WHEN 用户对当前卡片评分 `Good`
- THEN 计数器更新为「待复习 4 / 已复习 1」
- AND 不需要重新加载页面

### Requirement: 跨刷新持久化

复习进度 SHALL 持久化在 IndexedDB，关闭浏览器重新打开后未到期卡片不出现在今日队列，已评分卡片的 `reps` 字段保留。

#### Scenario: 关闭重开保留进度

- GIVEN 用户已对卡片 `id=X` 评分 Good，`due` 为 1 天后
- WHEN 用户关闭浏览器，重新打开访问 `/review`
- THEN 卡片 `id=X` 不在今日队列中（未到期）
- AND IndexedDB 中卡片 `id=X` 的 `reps` 字段保留评分后的值

#### Scenario: 跨设备不同步（已知 trade-off）

- GIVEN 用户在浏览器 A 上对若干卡片评分
- WHEN 用户在浏览器 B 上访问 `/review`
- THEN 浏览器 B 的 IndexedDB 中无这些卡片或评分记录
- AND 队列为空（除非该浏览器也访问过含卡片的笔记页）

  > 注：跨设备同步不在本 change 范围，记录在 design.md 的 Non-goals 中。
