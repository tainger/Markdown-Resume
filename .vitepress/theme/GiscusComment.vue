<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { useData, useRoute } from 'vitepress'
import { giscusConfig, giscusReady } from './giscus.config'

// 跟随 VitePress 的暗色/亮色模式
const { isDark, frontmatter } = useData()
const route = useRoute()

const container = ref<HTMLElement>()

function giscusTheme() {
  return isDark.value ? 'dark' : 'light'
}

// 向已加载的 giscus iframe 发送主题切换消息
function sendTheme() {
  const iframe = document.querySelector<HTMLIFrameElement>(
    'iframe.giscus-frame'
  )
  if (!iframe) return
  iframe.contentWindow?.postMessage(
    { giscus: { setConfig: { theme: giscusTheme() } } },
    'https://giscus.app'
  )
}

// 加载 / 重新加载 giscus
function loadGiscus() {
  if (!giscusReady || !container.value) return
  // 清空旧实例（路由切换时）
  container.value.innerHTML = ''

  const script = document.createElement('script')
  script.src = 'https://giscus.app/client.js'
  script.async = true
  script.crossOrigin = 'anonymous'
  script.setAttribute('data-repo', giscusConfig.repo)
  script.setAttribute('data-repo-id', giscusConfig.repoId)
  script.setAttribute('data-category', giscusConfig.category)
  script.setAttribute('data-category-id', giscusConfig.categoryId)
  script.setAttribute('data-mapping', giscusConfig.mapping)
  script.setAttribute('data-strict', '0')
  script.setAttribute('data-reactions-enabled', giscusConfig.reactionsEnabled)
  script.setAttribute('data-emit-metadata', giscusConfig.emitMetadata)
  script.setAttribute('data-input-position', giscusConfig.inputPosition)
  script.setAttribute('data-theme', giscusTheme())
  script.setAttribute('data-lang', giscusConfig.lang)
  container.value.appendChild(script)
}

onMounted(() => {
  loadGiscus()
})

// 主题切换 → 通知 iframe
watch(isDark, () => sendTheme())

// 路由切换 → 换页重载评论
watch(
  () => route.path,
  () => nextTick(() => loadGiscus())
)
</script>

<template>
  <!-- frontmatter 设 comment: false 的页面（如首页）不显示评论 -->
  <div
    v-if="giscusReady && frontmatter.comment !== false"
    class="giscus-wrapper"
  >
    <h2 class="giscus-title">💬 评论</h2>
    <div ref="container" class="giscus" />
  </div>
</template>

<style scoped>
.giscus-wrapper {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--vp-c-divider);
}
.giscus-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  border: none;
  padding: 0;
}
</style>
