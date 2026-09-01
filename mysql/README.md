# MySQL 面试题笔记

按主题整理的 MySQL 高频面试题，覆盖索引、事务与锁、锁机制深度、存储引擎与架构、SQL 优化、explain 深度、深分页、JOIN 笛卡尔积、分库分表、多租户、数据类型、MySQL 8.0 新特性、实际场景排查十三大板块。

## 目录

| 主题 | 笔记 | 核心考点 |
|:---|:---|:---|
| 🔍 索引 | [索引.md](索引.md) | B+ 树、聚簇/二级索引、覆盖索引、最左前缀、回表、索引失效 |
| 🔒 事务与锁 | [事务与锁.md](事务与锁.md) | ACID、隔离级别、MVCC、ReadView、间隙锁、死锁 |
| 🔐 锁机制深度 | [锁机制深度.md](锁机制深度.md) | 意向锁/MDL/AUTO-INC/插入意向锁、兼容矩阵、加锁规则「两原则两优化」、不同 SQL 加锁分析、RC/RR 加锁差异、死锁排查与监控命令、乐观锁 vs 悲观锁 |
| ⚙️ 存储引擎与架构 | [存储引擎与架构.md](存储引擎与架构.md) | InnoDB vs MyISAM、redo/undo/binlog、WAL、两阶段提交、Buffer Pool |
| 🚀 SQL 优化与调优 | [SQL优化与调优.md](SQL优化与调优.md) | explain、慢查询、深分页、count、join、分库分表、主从复制 |
| 🔬 explain 深度详解 | [explain详解.md](explain详解.md) | 12 字段全解读、id 执行顺序、key_len 计算公式、Extra 完整速查、多表 JOIN 驱动表选择、format=json / optimizer_trace |
| 🩺 SQL 优化实际场景 | [SQL优化的实际场景.md](SQL优化的实际场景.md) | 排查五步骨架、索引失效场景、时快时慢分诊、批量导入、CPU 100% 定位、锁超时排查、动态筛选索引设计、慢 SQL 治理 |
| 📄 深分页优化 | [深分页优化.md](深分页优化.md) | offset 慢因剖析、游标/书签法（多列排序变体）、延迟关联、业务限深、ES/分库分表场景 |
| 🔗 笛卡尔积 | [笛卡尔积.md](笛卡尔积.md) | 意外产生场景、CROSS JOIN 正经用途、JOIN 语义=笛卡尔积+过滤 vs 物理执行、JOIN 放大分页/COUNT（先收敛再放大） |
| 🗂️ 分库分表 | [分库分表.md](分库分表.md) | 拆分时机、分片键与基因法、预分槽扩容、跨片查询/分页、平滑迁移、ShardingSphere |
| 🏢 多租户设计 | [多租户设计.md](多租户设计.md) | 三种隔离方案、ThreadLocal 上下文、MyBatis 拦截器改写、分片结合、串库事故 |
| 🧮 数据类型选型 | [数据类型选型.md](数据类型选型.md) | 整数/字符串/文本/浮点/时间/JSON/ENUM/字符集/主键选型、NULL 代价、隐式类型转换、2038 问题、UUID 性能 |
| 🆕 MySQL 8.0 新特性 | [MySQL8新特性.md](MySQL8新特性.md) | 窗口函数、递归 CTE、INSTANT ADD COLUMN、原子 DDL、隐藏/降序/函数索引、Hash Join、EXPLAIN ANALYZE、查询缓存移除、数据字典事务化、升级坑 |

## 高频「必背」清单

- **为什么索引用 B+ 树**：矮胖 + 叶子有序双向链表 → 少 I/O、支持范围查询
- **聚簇索引 vs 二级索引**：叶子存整行 vs 存主键；查非索引列要回表
- **MySQL 默认隔离级别**：RR（可重复读），靠 MVCC + Next-Key Lock 基本解决幻读
- **MVCC 三件套**：隐藏字段（trx_id/roll_ptr）+ undo 版本链 + ReadView
- **RC vs RR 区别**：ReadView 生成时机（每次读 / 首次读）
- **三大日志**：redo（物理/持久性）、undo（回滚/MVCC）、binlog（复制）
- **两阶段提交**：保证 redo log 与 binlog 一致
- **InnoDB 行锁锁的是索引**：无索引退化为表锁；意向锁 IS/IX 之间全兼容；MDL 元数据锁在长事务 + DDL 时导致雪崩
- **加锁规则「两原则两优化」**：原则 1 基本单位 Next-Key Lock（左开右闭）、原则 2 访问到的对象才加锁、优化 1 唯一索引等值命中退化为 Record Lock、优化 2 非唯一索引向右遍历最后一个不满足值退化为 Gap Lock；RC 不加 Gap，RR 默认加
- **死锁排查五步**：`show engine innodb status` 看 LATEST DETECTED DEADLOCK → `innodb_trx` 找未提交事务 → `data_locks/waits` 找锁等待链 → kill 源头事务；wait-for graph 算法回滚 undo 量最小的事务
- **乐观锁 vs 悲观锁**：悲观 = `for update` 加锁；乐观 = version 字段应用层判断；秒杀扣库存用悲观，订单更新用乐观
- **数据类型铁律**：主键一律 BIGINT 留余量、金额一律 DECIMAL 或 BIGINT 存分（禁 FLOAT）、字符串能用 VARCHAR 不用 TEXT、字符集统一 utf8mb4、所有字段 NOT NULL DEFAULT
- **TIMESTAMP 2038 问题**：4 字节有符号 → 溢出时间 2038-01-19；跨期业务用 DATETIME
- **UUID 做主键的代价**：36 字节随机值 → B+ 树频繁页分裂、二级索引撑大 4 倍；分布式优先雪花 ID
- **隐式类型转换导致索引失效**：字段 varchar + 查询传 int → MySQL 把 phone 转 SIGNED 比较 → 索引失效；规则「字符串优先转数字」
- **MySQL 8.0 新特性速背**：窗口函数（row_number/rank/dense_rank/lag/lead，不折叠行）、递归 CTE（`with recursive` 查树）、INSTANT 加列（8.0.12 毫秒级）、隐藏索引（invisible→drop→秒级恢复）、Hash Join（8.0.18 无索引等值 join）、EXPLAIN ANALYZE（真实执行统计）、查询缓存移除
- **8.0 升级三大坑**：caching_sha2_password 老驱动连不上、`rank/row/groups` 成保留字、GROUP BY 不再隐式排序必须显式 order by
- **explain type 顺序**：`const > eq_ref > ref > range > index > ALL`
- **explain 12 字段速背**：id 看执行顺序、select_type 看查询类型、type 看性能、key/possible_keys 看（可能）索引、key_len 看联合索引用了几列、ref 看比较对象、rows 是估算扫描行数、filtered 看过滤比例、Extra 看危险信号
- **key_len 计算**：单列类型字节数 + 可空 1 + 变长 2；联合索引 key_len 突然变小 = 最左前缀断裂
- **Extra 危险信号**：Using filesort / Using temporary 必看；Using index 是覆盖索引是好事；Using join buffer 是被驱动表无索引
- **多表 JOIN 执行顺序**：id 同号从上往下、不同号大值先执行；优化器选小结果集表做驱动表
- **深度排查**：`explain format=json` 看成本估算、`optimizer_trace` 看索引选择决策过程
- **深分页优化**：延迟关联 / 书签记录法
- **场景排查五步**：现象定性 → show processlist → explain → 分诊解决（索引→改写→缓存→架构）→ 复盘防再犯
- **JOIN 放大**：分页/COUNT 在一对多 JOIN 之后做会被撑爆——先收敛（主表先分页/明细先聚合）再放大；漏 ON 是静默笛卡尔积
- **分库分表选型**：分片键选高频查询字段；预分槽（一致性哈希）留扩容余量；基因法/冗余表对抗多维查询
- **扩容三步**：双写 → 全量 + binlog 追平 → 校验灰度切流
- **多租户三方案**：独立库（合规）/ 独立 Schema（折中）/ 共享表 + tenant_id（主流），按客户等级分池
- **多租户保命线**：唯一索引含 tenant_id、缓存 key 带租户、ThreadLocal 用完清理

## 相关笔记

- 数据结构：[B+树.md](../数据结构/B+树.md)、[B树.md](../数据结构/B树.md)
