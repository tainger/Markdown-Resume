import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
// 项目根目录（.vitepress 的上一级）
const ROOT = path.resolve(__dirname, '..')

type SidebarItem = {
  text: string
  link?: string
  items?: SidebarItem[]
  collapsed?: boolean
}

// 从文件/目录名提取排序键：优先按前导数字（如 "10. xxx"）做自然排序
function sortKey(name: string): [number, string] {
  const m = name.match(/^(\d+)/)
  return m ? [parseInt(m[1], 10), name] : [Number.MAX_SAFE_INTEGER, name]
}

function compareNames(a: string, b: string): number {
  const [na, sa] = sortKey(a)
  const [nb, sb] = sortKey(b)
  if (na !== nb) return na - nb
  return sa.localeCompare(sb, 'zh-Hans-CN')
}

// 去掉 .md 后缀作为菜单显示文本
function titleOf(fileName: string): string {
  return fileName.replace(/\.md$/i, '')
}

// 把绝对文件路径转成 VitePress 链接（相对项目根、以 / 开头、无扩展名）
function toLink(absFile: string): string {
  let rel = path.relative(ROOT, absFile).split(path.sep).join('/')
  rel = rel.replace(/\.md$/i, '')
  return '/' + rel
}

// 递归扫描一个目录，生成侧边栏 items
function scanDir(absDir: string): SidebarItem[] {
  if (!fs.existsSync(absDir)) return []
  const entries = fs.readdirSync(absDir, { withFileTypes: true })

  const dirs = entries
    .filter((e) => e.isDirectory() && !e.name.startsWith('.'))
    .map((e) => e.name)
    .sort(compareNames)

  const files = entries
    .filter((e) => e.isFile() && e.name.toLowerCase().endsWith('.md'))
    .map((e) => e.name)
    .sort(compareNames)

  const items: SidebarItem[] = []

  // README 置顶（若存在）
  const readmeIdx = files.findIndex((f) => f.toLowerCase() === 'readme.md')
  if (readmeIdx !== -1) {
    const readme = files.splice(readmeIdx, 1)[0]
    items.push({ text: '📖 概览', link: toLink(path.join(absDir, readme)) })
  }

  // 子目录作为可折叠分组
  for (const d of dirs) {
    const childItems = scanDir(path.join(absDir, d))
    if (childItems.length > 0) {
      items.push({ text: d, collapsed: true, items: childItems })
    }
  }

  // 普通文件
  for (const f of files) {
    items.push({ text: titleOf(f), link: toLink(path.join(absDir, f)) })
  }

  return items
}

// 找到某目录下第一篇可跳转文档（用于导航栏点击）
export function firstDoc(dir: string): string {
  const items = scanDir(path.join(ROOT, dir))
  const stack = [...items]
  while (stack.length) {
    const it = stack.shift()!
    if (it.link) return it.link
    if (it.items) stack.unshift(...it.items)
  }
  return '/'
}

// 为每个收录目录生成一份侧边栏（进入该目录下任意页面时显示对应侧边栏）
export function generateSidebar(
  includeDirs: { text: string; dir: string }[]
): Record<string, SidebarItem[]> {
  const sidebar: Record<string, SidebarItem[]> = {}
  for (const { text, dir } of includeDirs) {
    const items = scanDir(path.join(ROOT, dir))
    if (items.length === 0) continue
    sidebar['/' + dir + '/'] = [{ text, items }]
  }
  return sidebar
}
