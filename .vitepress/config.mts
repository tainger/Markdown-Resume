import { defineConfig } from 'vitepress'
import mathjax3 from 'markdown-it-mathjax3'
import { generateSidebar } from './sidebar.mts'

// 只收录这些学习笔记目录（按导航栏顺序）
export const INCLUDE_DIRS: { text: string; dir: string }[] = [
  { text: '算法思想', dir: '算法思想' },
  { text: 'LeetCode Hot100', dir: 'leetcode-hot100' },
  { text: '华为OD机试', dir: '华为OD机试' },
  { text: '数据结构', dir: '数据结构' },
  { text: '分布式', dir: '分布式' },
  { text: '系统设计', dir: '系统设计' },
  { text: '权限设计', dir: '权限设计' },
  { text: 'AI应用开发', dir: 'AI应用开发' },
  { text: 'DeepSeek Harness', dir: 'DeepSeek Harness' },
  { text: 'MySQL', dir: 'mysql' },
  { text: 'Redis', dir: 'redis' },
  { text: 'Redisson', dir: 'redisson' },
  { text: 'RocketMQ', dir: 'rocketMq' },
  { text: 'ElasticSearch', dir: 'ElasticSearch' },
  { text: 'MyBatis', dir: 'Mybatis' },
  { text: 'Dubbo', dir: 'dubbo' },
  { text: 'Spring', dir: 'Spring' },
  { text: 'Java', dir: 'java' },
  { text: 'JVM', dir: 'jvm' },
  { text: 'IO', dir: 'io' },
  { text: '计算机网络', dir: '计算机网络' },
  { text: '面试', dir: '面试' },
  { text: '面试准备', dir: '面试准备' },
  { text: '英语能力', dir: '英语能力' },
  { text: '探小虎', dir: '自媒体/微信公众号/探小虎' },
  { text: '每日记录', dir: '每日记录' },
]

// 顶部导航栏分组（下拉菜单）：目录太多平铺会溢出，按类折叠
// dirs 里的值必须能在 INCLUDE_DIRS 中找到
export const NAV_GROUPS: { text: string; dirs: string[] }[] = [
  { text: '算法', dirs: ['算法思想', 'leetcode-hot100', '华为OD机试', '数据结构'] },
  { text: '后端', dirs: ['分布式', '系统设计', '权限设计', 'mysql', 'redis', 'redisson', 'rocketMq', 'ElasticSearch', 'Mybatis', 'dubbo', 'Spring'] },
  { text: '基础', dirs: ['java', 'jvm', 'io', '计算机网络'] },
  { text: '更多', dirs: ['AI应用开发', 'DeepSeek Harness', '面试', '面试准备', '英语能力', '自媒体/微信公众号/探小虎', '每日记录'] },
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
        // Mermaid 代码块 → 交给 Mermaid Vue 组件渲染脑图/流程图
        const token = tokens[idx]
        if (token.info.trim() === 'mermaid') {
          return `<Mermaid :code='${JSON.stringify(token.content)}' />`
        }
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
    'resume (copy).md',
    'README.md',
    // LLM Wiki 层：Agent 维护的知识编译层，仅作 AI 工作记忆，不在站上发布
    'wiki/**',
    'AGENTS.md',
    'node_modules/**',
    '.venv/**',
    '**/node_modules/**',
  ],

  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      ...NAV_GROUPS.map((g) => ({
        text: g.text,
        items: g.dirs.map((dir) => ({
          text: INCLUDE_DIRS.find((d) => d.dir === dir)?.text ?? dir,
          link: firstDocLink(dir),
        })),
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
