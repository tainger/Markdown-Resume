<script setup lang="ts">
import { onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vitepress'

const route = useRoute()

// 仅在这些「题解目录」下启用代码折叠
const ENABLED_DIRS = ['/leetcode-hot100/', '/华为OD机试/']

function isEnabledPath(path: string): boolean {
  // 路由 path 可能被 URL 编码（中文目录），解码后再判断
  let decoded = path
  try {
    decoded = decodeURIComponent(path)
  } catch {
    // 解码失败就用原始 path
  }
  return ENABLED_DIRS.some((dir) => decoded.includes(dir))
}

// 把单个代码块包进可折叠容器
function wrapBlock(block: HTMLElement, index: number) {
  if (block.dataset.folded === '1') return // 已处理过，避免重复包裹
  block.dataset.folded = '1'

  const wrapper = document.createElement('div')
  wrapper.className = 'code-folder'

  const toggle = document.createElement('button')
  toggle.type = 'button'
  toggle.className = 'code-folder-toggle'
  toggle.setAttribute('aria-expanded', 'false')
  toggle.innerHTML =
    '<span class="code-folder-arrow">▶</span>' +
    '<span class="code-folder-label">点击展开代码</span>'

  // 默认收起
  block.style.display = 'none'

  toggle.addEventListener('click', () => {
    const expanded = toggle.getAttribute('aria-expanded') === 'true'
    toggle.setAttribute('aria-expanded', String(!expanded))
    block.style.display = expanded ? 'none' : ''
    toggle.querySelector('.code-folder-label')!.textContent = expanded
      ? '点击展开代码'
      : '收起代码'
  })

  // 用 wrapper 替换原 block 的位置，再把 toggle + block 塞进去
  block.parentNode?.insertBefore(wrapper, block)
  wrapper.appendChild(toggle)
  wrapper.appendChild(block)
}

// 找到「## 代码」标题之后的所有代码块并折叠
function foldCodeBlocks() {
  if (!isEnabledPath(route.path)) return

  const content = document.querySelector('.vp-doc')
  if (!content) return

  // 定位「代码」这个 h2 标题
  const headings = Array.from(content.querySelectorAll('h2'))
  const codeHeading = headings.find((h) =>
    (h.textContent || '').replace('​', '').trim().includes('代码')
  )
  if (!codeHeading) return

  // 收集该标题之后、下一个 h2 之前（含之后所有小节）的代码块
  const blocks: HTMLElement[] = []
  let node = codeHeading.nextElementSibling
  while (node) {
    if (node.matches('div[class*="language-"]')) {
      blocks.push(node as HTMLElement)
    } else {
      // 小节里的代码块可能嵌在其它元素内，兜底再查一层
      node
        .querySelectorAll?.('div[class*="language-"]')
        .forEach((el) => blocks.push(el as HTMLElement))
    }
    node = node.nextElementSibling
  }

  blocks.forEach((block, i) => wrapBlock(block, i))
}

function run() {
  // 等 DOM 渲染完成后再处理
  nextTick(() => setTimeout(foldCodeBlocks, 0))
}

onMounted(run)

// 路由切换（切换题目）时重新折叠
watch(() => route.path, run)
</script>

<template>
  <!-- 纯副作用组件，不渲染任何可见内容 -->
  <span style="display: none" />
</template>

<style>
.code-folder {
  margin: 16px 0;
}

.code-folder-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  font-weight: 500;
  color: var(--vp-c-text-2);
  background-color: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.code-folder-toggle:hover {
  color: var(--vp-c-brand-1);
  background-color: var(--vp-c-bg-alt);
  border-color: var(--vp-c-brand-1);
}

.code-folder-arrow {
  display: inline-block;
  font-size: 11px;
  transition: transform 0.2s;
}

.code-folder-toggle[aria-expanded='true'] .code-folder-arrow {
  transform: rotate(90deg);
}

/* 展开后代码块与折叠条留一点间距 */
.code-folder-toggle[aria-expanded='true'] {
  margin-bottom: 8px;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}
</style>
