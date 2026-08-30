---
type: entity
status: seed
updated: 2026-08-30
---

# Redis

> 一句话：内存 KV 存储，单线程命令执行 + 多路复用；面试四大件：数据结构、持久化、高可用、缓存问题。

## 核心笔记

- [[redis/数据类型与底层结构]]（SDS/ziplist/skiplist…）
- [[redis/持久化]]（RDB vs AOF vs 混合）
- [[redis/过期与内存淘汰]]
- [[redis/高可用与集群]]（主从/哨兵/Cluster）
- [[redis/缓存问题与实战]]（穿透/击穿/雪崩/污染 + 分布式锁）

## 高频追问链

为什么快 → zset 跳表为什么不用红黑树（见 [[wiki/comparisons/B树与B+树]]）→ RDB/AOF 选型 → 主从复制原理 → 哨兵选主 → Cluster 槽位 → 缓存三兄弟 → 缓存一致性（先更库再删缓存 + 延迟双删）

## 关联

- [[wiki/synthesis/缓存体系与一致性]]、[[wiki/synthesis/高并发库存扣减]]
- [[分布式/分布式锁]]
