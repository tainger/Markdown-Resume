<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useData } from 'vitepress'

const props = defineProps<{ code: string }>()
const container = ref<HTMLDivElement>()
const svg = ref('')
let mermaid: any = null

const { isDark } = useData()

async function render() {
  if (!mermaid) {
    mermaid = (await import('mermaid')).default
  }
  mermaid.initialize({
    startOnLoad: false,
    theme: isDark.value ? 'dark' : 'default',
    securityLevel: 'loose',
  })
  try {
    const id = 'm' + Math.random().toString(36).slice(2, 9)
    const { svg: rendered } = await mermaid.render(id, props.code)
    svg.value = rendered
  } catch (e) {
    // 渲染失败时显示原始代码，不阻断页面
    svg.value = `<pre style="color:#999;padding:12px">${props.code}</pre>`
  }
}

onMounted(render)
watch(isDark, () => nextTick(render))
</script>

<template>
  <div ref="container" class="mermaid-wrapper" v-html="svg" />
</template>

<style>
.mermaid-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0;
  overflow-x: auto;
}
.mermaid-wrapper svg {
  max-width: 100%;
  height: auto;
}
</style>
