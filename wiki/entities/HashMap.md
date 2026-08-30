---
type: entity
status: seed
updated: 2026-08-30
---

# HashMap

> 一句话：数组 + 链表 + 红黑树（1.8 树化），面试最高频的集合源码；并发场景一律换 ConcurrentHashMap。

## 核心笔记

- [[java/HashMap的应用场景和源码分析]]（主笔记）
- [[java/ConcurrentHashMap应用场景和源码分析]]（并发版）
- [[java/TreeMap源码分析]]、[[java/TreeSet的应用场景和源码分析]]（有序兄弟）
- [[数据结构/哈希表]]（结构原理）+ [[算法思想/哈希表]]（刷题套路、LRU/LFU 设计）——两篇差异化互补
- [[leetcode-hot100/0.java中集合总结(方便刷算法题)]]（刷题速查）

## 高频追问链

容量为什么是 2 的幂 → hash 扰动 → 1.7 头插死循环 → 1.8 尾插 + 树化阈值 8/64/6 → 扩容 rehash 优化 → ConcurrentHashMap 分段锁演进
