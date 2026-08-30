---
type: entity
status: seed
updated: 2026-08-30
---

# MySQL

> 一句话：InnoDB 为核的 B+树关系库；面试四大件：索引、事务与锁、调优、分库分表。

## 核心笔记

- [[mysql/索引]]（B+树/聚簇/回表/覆盖/最左前缀）
- [[mysql/事务与锁]]（ACID/MVCC/隔离级别/锁体系）
- [[mysql/存储引擎与架构]]（InnoDB vs MyISAM、SQL 执行流程）
- [[mysql/SQL优化与调优]]（explain/慢查询）
- [[mysql/深分页优化]]
- [[mysql/笛卡尔积]]（JOIN 语义 vs 物理、JOIN 放大分页/COUNT、先收敛再放大）
- [[mysql/分库分表]]
- [[mysql/多租户设计]]
- 原理层：[[数据结构/B+树]]（见 [[wiki/comparisons/B树与B+树]]）

## 高频追问链

为什么 B+树不用 B 树/红黑树 → 聚簇 vs 二级索引 → 回表/索引下推 → MVCC 实现链 → 间隙锁解决幻读 → explain 字段逐个说 → 深分页三种方案 → 分片键怎么选 → 全局 ID（见 [[系统设计/ID生成器]]）

## 关联

- [[wiki/synthesis/分布式事务选型]]、[[wiki/synthesis/高并发库存扣减]]
