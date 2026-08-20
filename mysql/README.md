# MySQL 面试题笔记

按主题整理的 MySQL 高频面试题，覆盖索引、事务与锁、存储引擎与架构、SQL 优化四大板块。

## 目录

| 主题 | 笔记 | 核心考点 |
|:---|:---|:---|
| 🔍 索引 | [索引.md](索引.md) | B+ 树、聚簇/二级索引、覆盖索引、最左前缀、回表、索引失效 |
| 🔒 事务与锁 | [事务与锁.md](事务与锁.md) | ACID、隔离级别、MVCC、ReadView、间隙锁、死锁 |
| ⚙️ 存储引擎与架构 | [存储引擎与架构.md](存储引擎与架构.md) | InnoDB vs MyISAM、redo/undo/binlog、WAL、两阶段提交、Buffer Pool |
| 🚀 SQL 优化与调优 | [SQL优化与调优.md](SQL优化与调优.md) | explain、慢查询、深分页、count、join、分库分表、主从复制 |

## 高频「必背」清单

- **为什么索引用 B+ 树**：矮胖 + 叶子有序双向链表 → 少 I/O、支持范围查询
- **聚簇索引 vs 二级索引**：叶子存整行 vs 存主键；查非索引列要回表
- **MySQL 默认隔离级别**：RR（可重复读），靠 MVCC + Next-Key Lock 基本解决幻读
- **MVCC 三件套**：隐藏字段（trx_id/roll_ptr）+ undo 版本链 + ReadView
- **RC vs RR 区别**：ReadView 生成时机（每次读 / 首次读）
- **三大日志**：redo（物理/持久性）、undo（回滚/MVCC）、binlog（复制）
- **两阶段提交**：保证 redo log 与 binlog 一致
- **explain type 顺序**：`const > eq_ref > ref > range > index > ALL`
- **深分页优化**：延迟关联 / 书签记录法

## 相关笔记

- 数据结构：[B+树.md](../数据结构/B+树.md)、[B树.md](../数据结构/B树.md)
