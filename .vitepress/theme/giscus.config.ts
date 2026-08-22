// ============================================================
// Giscus 评论配置
// ------------------------------------------------------------
// 下面 4 个 TODO 参数需要你去 https://giscus.app 生成后填入：
//   1. 打开你的 GitHub 仓库 → Settings → General → 勾选 Discussions
//   2. 安装 giscus app：https://github.com/apps/giscus （授权给该仓库）
//   3. 打开 https://giscus.app ，填入仓库名，页面会自动算出下面 4 个值
//   4. 把算出来的值替换下面的 TODO
//
// 填好后本地 `npm run docs:dev` 就能看到评论框；push 后 Vercel 自动生效。
// ============================================================

export const giscusConfig = {
  // 形如 "tainger/Markdown-Resume"
  repo: 'tainger/Markdown-Resume',

  // giscus.app 生成的 data-repo-id，形如 "R_kgD..."
  repoId: 'R_kgDOQyDAbw',

  // Discussions 分类名，建议用 "Announcements" 或专门新建的 "Comments"
  category: 'Announcements',

  // giscus.app 生成的 data-category-id，形如 "DIC_kwD..."
  categoryId: 'DIC_kwDOQyDAb84DD6wb',

  // 评论与页面的映射方式：pathname = 按页面路径一一对应（推荐）
  mapping: 'pathname' as const,

  // 是否开启 reaction、把评论框放在上方等，按需调整
  reactionsEnabled: '1',
  emitMetadata: '0',
  inputPosition: 'top' as const,

  // 语言
  lang: 'zh-CN',
}

// 参数是否已填好（含 TODO 时视为未配置，构建/运行时不挂载评论框）
export const giscusReady =
  !giscusConfig.repoId.startsWith('TODO') &&
  !giscusConfig.categoryId.startsWith('TODO')
