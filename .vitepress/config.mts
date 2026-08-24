import { defineConfig } from 'vitepress'
import mathjax3 from 'markdown-it-mathjax3'
import container from 'markdown-it-container'
import { generateSidebar } from './sidebar.mts'

// 注册 SRS 卡片正反面容器（::: srs-front ... ::: 与 ::: srs-back ... :::）
function srsContainer(name: string) {
  return {
    marker: ':',
    render(tokens: any[], idx: number) {
      return tokens[idx].nesting === 1
        ? `<div class="${name}">\n`
        : `</div>\n`
    },
  }
}

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
  { text: 'JVM', dir: 'jvm' },
  { text: '面试准备', dir: '面试准备' },
]

export default defineConfig({
  title: '学习笔记',
  description: '算法刷题、面试八股、数据库等个人学习笔记',
  lang: 'zh-CN',

  // 关闭死链检查：笔记里存在少量占位/纯文本链接，不因此中断构建
  ignoreDeadLinks: true,

  markdown: {
    // 支持 \(...\)、$$...$$ 等 LaTeX 数学公式渲染
    config: (md) => {
      md.use(mathjax3 as any)

      // 注册 SRS 卡片正反面容器（::: srs-front / ::: srs-back）
      md.use(container, 'srs-front', srsContainer('srs-front'))
      md.use(container, 'srs-back', srsContainer('srs-back'))

      // 笔记正文里含 {{ }} 花括号（如 Java 数组 {{1,0},{0,1}}），
      // 会被 Vue 当成模板插值导致构建报错。这里在渲染时把它转义成
      // HTML 实体，Vue 不再识别为插值，浏览器仍正常显示花括号。
      // 需同时覆盖：普通文本、行内代码、代码块。
      const escapeCurly = (s: string) =>
        s.replace(/\{\{/g, '&#123;&#123;').replace(/\}\}/g, '&#125;&#125;')

      const defaultText =
        md.renderer.rules.text ||
        ((tokens: any, idx: number) => tokens[idx].content)
      md.renderer.rules.text = (tokens, idx, options, env, self) => {
        return escapeCurly(defaultText(tokens, idx, options, env, self))
      }

      const defaultCodeInline =
        md.renderer.rules.code_inline ||
        ((tokens: any, idx: number) => tokens[idx].content)
      md.renderer.rules.code_inline = (tokens, idx, options, env, self) => {
        return escapeCurly(defaultCodeInline(tokens, idx, options, env, self))
      }

      const defaultFence = md.renderer.rules.fence!
      md.renderer.rules.fence = (tokens, idx, options, env, self) => {
        return escapeCurly(defaultFence(tokens, idx, options, env, self))
      }
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
      { text: '📖 复习', link: '/review' },
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
