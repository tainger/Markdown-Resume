<script setup lang="ts">
import { onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vitepress'
import { upsertCard, getCard } from './db'
import { initNewCard } from './fsrs'
import type { SRSCard } from './db'

const route = useRoute()

// /jvm/内存结构.html -> jvm/内存结构.md
function getFilePath(path: string): string {
  return path.replace(/^\//, '').replace(/\.html$/, '.md')
}

// SHA-256 前 16 字符；crypto.subtle 不可用时 fallback 到 djb2
async function hashId(s: string): Promise<string> {
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const data = new TextEncoder().encode(s)
    const buf = await crypto.subtle.digest('SHA-256', data)
    return Array.from(new Uint8Array(buf))
      .slice(0, 8)
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('')
  }
  let h = 5381
  for (let i = 0; i < s.length; i++) {
    h = ((h << 5) + h + s.charCodeAt(i)) | 0
  }
  return (h >>> 0).toString(16).padStart(8, '0').repeat(2)
}

async function scanCards() {
  // 复习页本身不扫描，避免误把 ReviewPage 的展示 DOM 当卡片入库
  if (route.path === '/review/' || route.path === '/review') return

  const content = document.querySelector('.vp-doc')
  if (!content) return

  const fronts = Array.from(
    content.querySelectorAll<HTMLElement>('div.srs-front'),
  )
  const backs = Array.from(
    content.querySelectorAll<HTMLElement>('div.srs-back'),
  )

  const filePath = getFilePath(route.path)
  const total = Math.max(fronts.length, backs.length)
  const pairs: { front: string; back: string }[] = []

  for (let i = 0; i < total; i++) {
    const front = fronts[i]
    const back = backs[i]
    if (front && back) {
      pairs.push({ front: front.innerHTML, back: back.innerHTML })
    } else if (front && !back) {
      console.warn(`SRS: orphan srs-front at index ${i} in ${filePath}`)
    } else if (!front && back) {
      console.warn(`SRS: orphan srs-back at index ${i} in ${filePath}`)
    }
  }

  // upsert 到 DB：ID 用 (filePath + '#' + 序号) 的 hash
  // 已存在的卡片只更新 front/back/filePath/tags，不重置 FSRS 状态
  for (let i = 0; i < pairs.length; i++) {
    const pair = pairs[i]
    const id = await hashId(`${filePath}#${i}`)
    const existing = await getCard(id)
    const baseCard: SRSCard = existing ?? initNewCard()
    const card: SRSCard = {
      ...baseCard,
      id,
      filePath,
      anchor: '',
      front: pair.front,
      back: pair.back,
      tags: filePath.split('/').slice(0, -1),
    }
    await upsertCard(card)
  }
}

function run() {
  nextTick(() => setTimeout(scanCards, 0))
}

onMounted(run)
watch(() => route.path, run)
</script>

<template>
  <span style="display: none" />
</template>
