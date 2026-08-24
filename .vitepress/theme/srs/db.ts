import { openDB, type DBSchema, type IDBPDatabase } from 'idb'

export type CardState = 'New' | 'Learning' | 'Review' | 'Relearning'
export type Rating = 1 | 2 | 3 | 4

export interface SRSCard {
  id: string
  filePath: string
  anchor: string
  front: string
  back: string
  tags: string[]
  due: number
  stability: number
  difficulty: number
  reps: number
  lapses: number
  state: CardState
  lastReview: number | null
}

export interface ReviewRecord {
  cardId: string
  ts: number
  rating: Rating
  prev_state: CardState
  next_state: CardState
}

interface SRSDB extends DBSchema {
  cards: {
    key: string
    value: SRSCard
    indexes: { due: number; filePath: string }
  }
  reviews: {
    key: [string, number]
    value: ReviewRecord
    indexes: { 'by-card': string }
  }
  meta: {
    key: string
    value: { key: string; value: unknown }
  }
}

const DB_NAME = 'srs-notes'
const DB_VERSION = 1

let _dbPromise: Promise<IDBPDatabase<SRSDB> | null> | null = null

function getDB(): Promise<IDBPDatabase<SRSDB> | null> {
  if (_dbPromise) return _dbPromise
  if (typeof indexedDB === 'undefined') {
    _dbPromise = Promise.resolve(null)
    return _dbPromise
  }
  _dbPromise = openDB<SRSDB>(DB_NAME, DB_VERSION, {
    upgrade(db) {
      const cards = db.createObjectStore('cards', { keyPath: 'id' })
      cards.createIndex('due', 'due')
      cards.createIndex('filePath', 'filePath')

      const reviews = db.createObjectStore('reviews', {
        keyPath: ['cardId', 'ts'],
      })
      reviews.createIndex('by-card', 'cardId')

      db.createObjectStore('meta', { keyPath: 'key' })
    },
  }).catch((err) => {
    console.warn('SRS: IndexedDB unavailable, all DB operations will be no-ops. Reason:', err)
    return null
  })
  return _dbPromise
}

// ---------- Cards ----------
export async function upsertCard(card: SRSCard): Promise<void> {
  const db = await getDB()
  if (!db) return
  await db.put('cards', card)
}

export async function getCard(id: string): Promise<SRSCard | undefined> {
  const db = await getDB()
  if (!db) return undefined
  return db.get('cards', id)
}

export async function getDueCards(now: number = Date.now()): Promise<SRSCard[]> {
  const db = await getDB()
  if (!db) return []
  const cards = await db.getAllFromIndex('cards', 'due', IDBKeyRange.upperBound(now))
  return cards.sort((a, b) => a.due - b.due)
}

export async function getAllCards(): Promise<SRSCard[]> {
  const db = await getDB()
  if (!db) return []
  return db.getAll('cards')
}

// ---------- Reviews ----------
export async function appendReview(r: ReviewRecord): Promise<void> {
  const db = await getDB()
  if (!db) return
  await db.add('reviews', r)
}

export async function getReviewsByCard(cardId: string): Promise<ReviewRecord[]> {
  const db = await getDB()
  if (!db) return []
  return db.getAllFromIndex('reviews', 'by-card', cardId)
}

// ---------- Meta ----------
export async function getMeta<T>(key: string): Promise<T | undefined> {
  const db = await getDB()
  if (!db) return undefined
  const rec = await db.get('meta', key)
  return rec?.value as T | undefined
}

export async function setMeta<T>(key: string, value: T): Promise<void> {
  const db = await getDB()
  if (!db) return
  await db.put('meta', { key, value })
}
