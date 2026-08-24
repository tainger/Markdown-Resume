<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getDueCards, upsertCard, appendReview } from './db'
import { grade } from './fsrs'
import type { SRSCard, Rating } from './db'

const cards = ref<SRSCard[]>([])
const currentIndex = ref(0)
const isFlipped = ref(false)
const reviewedCount = ref(0)
const startTime = ref<number | null>(null)
const endTime = ref<number | null>(null)
const errorMsg = ref<string | null>(null)
const loading = ref(true)

const currentCard = computed(() => cards.value[currentIndex.value])
const totalCount = computed(() => cards.value.length)
const remainingCount = computed(
  () => cards.value.length - currentIndex.value,
)
const isComplete = computed(
  () => !loading.value && cards.value.length > 0 && currentIndex.value >= cards.value.length,
)
const isEmpty = computed(
  () => !loading.value && cards.value.length === 0,
)
const elapsedMinutes = computed(() => {
  if (!startTime.value || !endTime.value) return 0
  return Math.max(1, Math.round((endTime.value - startTime.value) / 60000))
})

async function loadDueCards() {
  loading.value = true
  try {
    cards.value = await getDueCards()
    if (cards.value.length > 0) startTime.value = Date.now()
  } catch (e: any) {
    errorMsg.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function flip() {
  isFlipped.value = true
}

async function rate(rating: Rating) {
  // 诊断 trace 暴露到 window，方便外部读取
  const trace: any[] = []
  ;(window as any).__srs_trace = trace

  trace.push({ step: 'rate_start', rating, cardId: currentCard.value?.id, cardsLen: cards.value.length, curIdx: currentIndex.value })

  const card = currentCard.value
  if (!card) {
    trace.push({ step: 'no_card_abort' })
    return
  }
  const prev_state = card.state
  let next: SRSCard
  try {
    next = grade(card, rating)
    trace.push({ step: 'grade_ok', next_state: next.state, next_reps: next.reps, next_due_type: typeof next.due, next_due: next.due })
  } catch (e: any) {
    trace.push({ step: 'grade_threw', msg: e.message, stack: e.stack?.slice(0, 500) })
    return
  }
  try {
    await upsertCard(next)
    trace.push({ step: 'upsertCard_ok' })
  } catch (e: any) {
    trace.push({ step: 'upsertCard_threw', msg: e.message, stack: e.stack?.slice(0, 500) })
    return
  }
  try {
    await appendReview({
      cardId: card.id,
      ts: Date.now(),
      rating,
      prev_state,
      next_state: next.state,
    })
    trace.push({ step: 'appendReview_ok' })
  } catch (e: any) {
    trace.push({ step: 'appendReview_threw', msg: e.message, stack: e.stack?.slice(0, 500) })
    return
  }
  reviewedCount.value++
  isFlipped.value = false
  currentIndex.value++
  trace.push({ step: 'rate_done', new_curIdx: currentIndex.value, new_reviewed: reviewedCount.value, new_isFlipped: isFlipped.value })
  if (currentIndex.value >= cards.value.length) {
    endTime.value = Date.now()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (isComplete.value || isEmpty.value || loading.value) return
  if (!isFlipped.value) {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault()
      flip()
    }
  } else {
    const map: Record<string, Rating> = { '1': 1, '2': 2, '3': 3, '4': 4 }
    const rating = map[e.key]
    if (rating) {
      e.preventDefault()
      rate(rating)
    }
  }
}

onMounted(() => {
  loadDueCards()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="srs-review">
    <div v-if="loading" class="srs-status">加载复习队列...</div>

    <div v-else-if="errorMsg" class="srs-error">加载失败：{{ errorMsg }}</div>

    <header
      v-else-if="!isComplete && !isEmpty"
      class="srs-header"
    >
      <span class="srs-counter">待复习 <b>{{ remainingCount }}</b></span>
      <span class="srs-counter">已复习 <b>{{ reviewedCount }}</b></span>
      <span class="srs-counter">共 <b>{{ totalCount }}</b></span>
    </header>

    <div
      v-if="!isComplete && !isEmpty && currentCard"
      class="srs-card"
    >
      <div class="srs-card-face srs-card-front" :class="{ hidden: isFlipped }">
        <div class="srs-card-content" v-html="currentCard.front" />
        <button class="srs-flip-btn" @click="flip">
          显示答案 <kbd>Space</kbd>
        </button>
      </div>

      <div v-if="isFlipped" class="srs-card-face srs-card-back">
        <div class="srs-card-content" v-html="currentCard.back" />
        <div class="srs-rating-buttons">
          <button class="srs-rating again" @click="rate(1)">
            Again <kbd>1</kbd>
          </button>
          <button class="srs-rating hard" @click="rate(2)">
            Hard <kbd>2</kbd>
          </button>
          <button class="srs-rating good" @click="rate(3)">
            Good <kbd>3</kbd>
          </button>
          <button class="srs-rating easy" @click="rate(4)">
            Easy <kbd>4</kbd>
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="isComplete" class="srs-complete">
      <h2>今日复习完成 🎉</h2>
      <p>已复习 {{ reviewedCount }} 张</p>
      <p>总用时 {{ elapsedMinutes }} 分钟</p>
      <a href="/" class="srs-back-home">返回首页</a>
    </div>

    <div v-else-if="isEmpty" class="srs-empty">
      <h2>暂无待复习卡片 📭</h2>
      <p>访问含 <code>::: srs-front</code> 标记的笔记页，扫描后会自动入库。</p>
      <a href="/" class="srs-back-home">返回首页</a>
    </div>
  </div>
</template>

<style>
.srs-review {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 64px;
}

.srs-status,
.srs-error {
  padding: 16px;
  text-align: center;
  color: var(--vp-c-text-2);
}

.srs-header {
  display: flex;
  gap: 24px;
  justify-content: center;
  padding: 12px 0 24px;
  border-bottom: 1px solid var(--vp-c-divider);
  margin-bottom: 24px;
}

.srs-counter {
  font-size: 14px;
  color: var(--vp-c-text-2);
}

.srs-counter b {
  color: var(--vp-c-text-1);
  font-size: 16px;
  margin-left: 4px;
}

.srs-card {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  padding: 32px 24px;
  min-height: 240px;
  display: flex;
  flex-direction: column;
}

.srs-card-face.hidden {
  display: none;
}

.srs-card-content {
  flex: 1;
  font-size: 15px;
  line-height: 1.7;
  color: var(--vp-c-text-1);
}

.srs-card-content :deep(p:first-child) {
  margin-top: 0;
}

.srs-card-content :deep(p:last-child) {
  margin-bottom: 0;
}

.srs-flip-btn {
  margin-top: 24px;
  align-self: center;
  padding: 10px 24px;
  border: 1px solid var(--vp-c-brand-1);
  background: transparent;
  color: var(--vp-c-brand-1);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s, color 0.2s;
}

.srs-flip-btn:hover {
  background: var(--vp-c-brand-1);
  color: var(--vp-c-bg);
}

.srs-flip-btn kbd,
.srs-rating kbd {
  margin-left: 6px;
  padding: 1px 6px;
  background: var(--vp-c-bg-alt);
  border: 1px solid var(--vp-c-divider);
  border-radius: 4px;
  font-size: 11px;
  font-family: var(--vp-font-family-mono);
}

.srs-rating-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.srs-rating {
  padding: 8px 18px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: transform 0.1s, background-color 0.2s, border-color 0.2s;
}

.srs-rating:hover {
  transform: translateY(-1px);
}

.srs-rating.again {
  border-color: #e53e3e;
  color: #e53e3e;
}

.srs-rating.again:hover {
  background: #e53e3e;
  color: #fff;
}

.srs-rating.hard {
  border-color: #dd6b20;
  color: #dd6b20;
}

.srs-rating.hard:hover {
  background: #dd6b20;
  color: #fff;
}

.srs-rating.good {
  border-color: #38a169;
  color: #38a169;
}

.srs-rating.good:hover {
  background: #38a169;
  color: #fff;
}

.srs-rating.easy {
  border-color: #3182ce;
  color: #3182ce;
}

.srs-rating.easy:hover {
  background: #3182ce;
  color: #fff;
}

.srs-complete,
.srs-empty {
  text-align: center;
  padding: 48px 16px;
}

.srs-complete h2,
.srs-empty h2 {
  margin-top: 0;
}

.srs-back-home {
  display: inline-block;
  margin-top: 16px;
  padding: 8px 16px;
  border: 1px solid var(--vp-c-brand-1);
  border-radius: 6px;
  color: var(--vp-c-brand-1);
  text-decoration: none;
}

.srs-back-home:hover {
  background: var(--vp-c-brand-1);
  color: var(--vp-c-bg);
}
</style>
