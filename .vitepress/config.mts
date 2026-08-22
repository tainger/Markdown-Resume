import { defineConfig } from 'vitepress'
import mathjax3 from 'markdown-it-mathjax3'
import { generateSidebar } from './sidebar.mts'

// 只收录这些学习笔记目录（按导航栏顺序）
export const INCLUDE_DIRS: { text: string; dir: string }[] = [
  { text: '算法思想', dir: '算法思想' },
  { text: 'LeetCode Hot100', dir: 'leetcode-hot100' },
  { text: '华为OD机试', dir: '华为OD机试' },
  { text: '数据结构', dir: '数据结构' },
  { text: 'MySQL', dir: 'mysql' },
  { text: 'Redis', dir: 'redis' },
  { text: 'RocketMQ', dir: 'rocketMq' },
  { text: 'Java', dir: 'java' },
  { text: '面试准备', dir: '面试准备' },
]

export default defineConfig({
  title: '学习笔记',
  description: '算法刷题、面试八股、数据库等个人学习笔记',
  lang: 'zh-CN',

  // 关闭死链检查：笔记里存在少量占位/纯文本链接，不因此中断构建
  ignoreDeadLinks: true,

  // 笔记正文里含大量 {..}、{{..}}（如 Java 数组初始化、集合表示），
  // 默认会被 Vue 当成模板插值导致构建报错。改用不冲突的分隔符规避。
  vue: {
    template: {
      compilerOptions: {
        delimiters: ['{{{{', '}}}}'],
      },
    },
  },

  markdown: {
    // 支持 \(...\)、$$...$$ 等 LaTeX 数学公式渲染
    config: (md) => {
      md.use(mathjax3 as any)
    },
  },

  // 不作为页面渲染的目录/文件：第三方仓库、隐私内容、环境目录等
  srcExclude: [
    'repo/**',
    'lover/**',
    'agentscope-java/**',
    'jd/**',
    'assets/**',
    '公司赔偿/**',
    'resume.md',
    'README.md',
    'node_modules/**',
    '.venv/**',
    '**/node_modules/**',
  ],

  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      ...INCLUDE_DIRS.map((d) => ({
        text: d.text,
        link: firstDocLink(d.dir),
      })),
    ],

    sidebar: generateSidebar(INCLUDE_DIRS),

    outline: { level: [2, 3], label: '本页大纲' },

    docFooter: { prev: '上一篇', next: '下一篇' },

    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索笔记', buttonAriaLabel: '搜索笔记' },
          modal: {
            noResultsText: '没有找到结果',
            resetButtonTitle: '清除查询',
            footer: {
              selectText: '选择',
              navigateText: '切换',
              closeText: '关闭',
            },
          },
        },
      },
    },

    darkModeSwitchLabel: '主题',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '菜单',
    returnToTopLabel: '返回顶部',
    lastUpdatedText: '最后更新于',
  },

  lastUpdated: true,
})

// 导航栏点击某个板块时，跳到该目录下第一篇文档
import { firstDoc } from './sidebar.mts'
function firstDocLink(dir: string): string {
  return firstDoc(dir)
}
