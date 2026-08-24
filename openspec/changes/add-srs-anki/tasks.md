# Tasks

每个任务末尾的 `验证方式` 描述如何判断该任务真的完成（v1.10 要求）。

## 1. 依赖与配置

- [ ] 1.1 在 `package.json` 添加 `ts-fsrs` 与 `idb` 依赖，执行 `npm install`
  - **验证方式**：`npm ls ts-fsrs idb` 输出两个包的版本号且无 missing 标记
- [ ] 1.2 在 `.vitepress/config.mts` 的 `markdown.config` hook 注册 `srs-front` / `srs-back` 两个 markdown-it 自定义容器（参考 markdown-it-container 用法）
  - **验证方式**：写一个测试 md 文件含 `::: srs-front 测试 :::`，`npm run docs:dev` 后页面 DOM 中存在 `<div class="srs-front">` 元素

## 2. 卡片扫描与入库

- [ ] 2.1 实现 `.vitepress/theme/srs/db.ts`：用 `idb` 封装 IndexedDB，创建 `cards` / `reviews` / `meta` 三张表，建立 `due` 与 `filePath` 索引
  - **验证方式**：浏览器 DevTools → Application → IndexedDB 看到数据库 `srs-notes` 含三张 object store 与索引
- [ ] 2.2 实现 `.vitepress/theme/srs/CardScanner.ts`：扫描 `.vp-doc` 中的 `div.srs-front` + `div.srs-back`，按 DOM 顺序两两配对，对偶校验（孤儿 front/back 记 warning），upsert 到 `cards` 表
  - **验证方式**：在笔记里加测试卡片，刷新页面后 IndexedDB `cards` 表看到对应记录；故意只写 front 不写 back，控制台看到 warning 但不报错
- [ ] 2.3 卡片 ID 策略：锚点优先取容器标题（`.srs-front-title` 文本），无标题回退 `card-{页内序号}`；`hash(decodeURIComponent(filePath) + '#' + anchor).slice(0, 16)`，已存在的卡片只更新内容字段不重置 FSRS 状态
  - **验证方式**：改卡片内容（不改顺序）后刷新，DB 中该卡片 `due` / `stability` / `reps` 字段保持原值；给卡片加标题后重排卡片顺序，DB 中 `id` 不变且进度保留；删一个卡片再添加，新卡状态为 New
- [ ] 2.4 在 `.vitepress/theme/index.ts` 把 `CardScanner` 注入 `doc-after` slot（紧邻 `CodeFolder`）
  - **验证方式**：路由切换时 DB 中卡片正确 upsert（手动切到不含卡片的页面，已入库卡片不被删除）

## 3. FSRS 算法集成

- [ ] 3.1 实现 `.vitepress/theme/srs/fsrs.ts`：用 `ts-fsrs` 初始化 FSRS-5 实例，目标保留率 0.9，导出 `grade(card, rating)` 与 `initNewCard()` 函数
  - **验证方式**：浏览器控制台执行 `import('/.vitepress/theme/srs/fsrs.ts').then(m => m.grade({state:'New'}, 3))` 返回的对象包含 `due`、`stability`、`difficulty`、`state` 字段
- [ ] 3.2 实现 `initNewCard()`：返回 New 状态的默认卡片对象（`reps=0`, `lapses=0`, `state='New'`），供扫描入库时初始化
  - **验证方式**：新卡入库后 `state` 字段为 `'New'`，`due` 等于当前时间（立即可复习）

## 4. 复习页面 UI

- [ ] 4.1 新建 `review.md`（位于项目根，作为 `/review` 路由占位），内容为 `<ReviewPage />`
  - **验证方式**：访问 `/review` 路径不报 404
- [ ] 4.2 实现 `.vitepress/theme/srs/ReviewPage.vue`：
  - 进入页面时查询 `cards` 表中 `due <= now` 的卡片，按 due 升序排
  - 显示当前卡片正面 Markdown 渲染
  - "显示答案"按钮 → 翻面，显示背面
  - 翻面后底部 4 个评分按钮：Again / Hard / Good / Easy
  - 点击评分按钮 → 调 `grade()` → 写回 `cards` 与 `reviews` 表 → 推进队列下一张
  - 「删除」按钮：二次确认后从 `cards` 表删除当前卡片并推进队列（`reviews` 历史保留）
  - 队列空时显示"今日复习完成"
  - 所有 IndexedDB 访问放在 `onMounted` 内（SSG 构建兼容）
  - **验证方式**：在测试卡片入库后访问 `/review`，能完整跑通「翻面 → 评分 → 队列推进 → 完成」全流程
- [ ] 4.3 实现 `.vitepress/theme/srs/styles.css`：卡片容器、翻面动画、评分按钮样式，深色/浅色模式适配（用 VitePress 的 CSS 变量如 `--vp-c-brand-1`）
  - **验证方式**：浅色与深色模式下视觉对比无明显违和，按钮 hover/active 状态符合预期

## 5. 导航栏入口

- [ ] 5.1 在 `.vitepress/config.mts` 的 `themeConfig.nav` 添加 `{ text: '复习', link: '/review' }`（考虑用 emoji 前缀 `📖 复习` 节省宽度）
  - **验证方式**：站点任意页面顶部导航栏可见「复习」按钮，点击跳转到 `/review`
- [ ] 5.2 在 `ReviewPage.vue` 顶部显示「今日待复习 N 张 / 已复习 M 张」计数器，每完成一次评分实时更新
  - **验证方式**：完成一张卡评分后，"已复习"计数 +1，"待复习"计数 -1

## 6. 端到端验收

- [ ] 6.1 在某篇笔记里写 2-3 张测试卡片（如 JVM 内存结构 / GC 算法），跑通完整闭环：访问笔记页 → 卡片入库 → 进入 `/review` → 翻面评分 → 关闭浏览器重开 → 复习进度仍在
  - **验证方式**：关闭重开浏览器后访问 `/review`，未到期的卡片不出现在今日队列，已评分卡片的 `reps` 字段保留
- [ ] 6.2 在 `vercel.json` 不变的前提下执行 `npm run docs:build`，构建无报错
  - **验证方式**：构建产物 `.vitepress/dist` 目录生成，无 TypeScript 错误
- [ ] 6.3 提交 PR 前在本地 `npm run docs:preview` 预览，访问 `/review` 与原笔记页均无 console 错误
  - **验证方式**：DevTools Console 干净，无 unhandled rejection

## 后续 change 候选（不在本 change 范围）

- 卡片统计页 `/review/stats`（保留率曲线、热力图、按目录分布）
- 旧笔记批量改造脚本（按"标题→思路→代码"模式半自动生成卡片草稿）
- 云端同步（账号体系 + 后端服务）
- FSRS 参数个性化微调（基于个人复习数据拟合一组专属参数）
- 卡片图片/音频附件支持
