import {
  fsrs,
  generatorParameters,
  State,
  createEmptyCard,
  type Card as FSRSCard,
} from 'ts-fsrs'
import type { SRSCard, CardState, Rating } from './db'

const params = generatorParameters({
  enable_fuzz: false,
  request_retention: 0.9,
})
const f = fsrs(params)

const STATE_TO_NUM: Record<CardState, State> = {
  New: State.New,
  Learning: State.Learning,
  Review: State.Review,
  Relearning: State.Relearning,
}

const NUM_TO_STATE: Record<number, CardState> = {
  [State.New]: 'New',
  [State.Learning]: 'Learning',
  [State.Review]: 'Review',
  [State.Relearning]: 'Relearning',
}

function toFSRS(card: SRSCard, now: Date): FSRSCard {
  const lastReview = card.lastReview ? new Date(card.lastReview) : undefined
  const elapsedDays = lastReview
    ? Math.max(0, Math.floor((now.getTime() - lastReview.getTime()) / 86400000))
    : 0
  // learning_steps 暂未持久化到 SRSCard，先用 0；Review 状态下不影响计算
  return {
    due: new Date(card.due),
    stability: card.stability,
    difficulty: card.difficulty,
    elapsed_days: elapsedDays,
    scheduled_days: 0,
    learning_steps: 0,
    reps: card.reps,
    lapses: card.lapses,
    state: STATE_TO_NUM[card.state],
    last_review: lastReview,
  } as FSRSCard
}

function fromFSRS(card: SRSCard, next: FSRSCard): SRSCard {
  return {
    ...card,
    due: next.due.getTime(),
    stability: next.stability,
    difficulty: next.difficulty,
    reps: next.reps,
    lapses: next.lapses,
    state: NUM_TO_STATE[next.state],
    lastReview: next.last_review ? next.last_review.getTime() : null,
  }
}

export function grade(card: SRSCard, rating: Rating): SRSCard {
  const now = new Date()
  const fsrsCard = toFSRS(card, now)
  const result = f.repeat(fsrsCard, now)
  // ts-fsrs 5.x repeat() 返回 RecordLog = { [Rating]: { card, log } }
  // 取 .card 才是真正的 FSRS Card；用 Record<number, ...> 断言绕开 IPreview 的 Grade 索引签名
  const item = (result as unknown as Record<number, { card: FSRSCard; log: unknown }>)[rating]
  return fromFSRS(card, item.card)
}

export function initNewCard(now: number = Date.now()): SRSCard {
  const empty = createEmptyCard(new Date(now))
  return {
    id: '',
    filePath: '',
    anchor: '',
    front: '',
    back: '',
    tags: [],
    due: empty.due.getTime(),
    stability: empty.stability,
    difficulty: empty.difficulty,
    reps: empty.reps,
    lapses: empty.lapses,
    state: NUM_TO_STATE[empty.state],
    lastReview: empty.last_review ? empty.last_review.getTime() : null,
  }
}
