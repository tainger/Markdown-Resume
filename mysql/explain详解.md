# MySQL explain 深度详解

> **定位**：与 [SQL优化与调优](SQL优化与调优.md) 分工——那篇是「手段层」入口（explain 4 字段 + 慢日志 + 改写手法），这篇是「**字段层**」深度：12 个字段逐个解读 + 多表 JOIN 执行顺序 + key_len 计算公式 + Extra 完整速查 + optimizer_trace 看优化器内部决策。面试官追问字段细节就看这篇。

---

## 一、explain 是什么 + 12 字段总览

`explain select ...`（或 `update / insert / delete` 前加 explain）让 MySQL 输出**执行计划**——它**不会真执行 SQL**，只把优化器选定的方案告诉你。

```sql
explain select * from user where id = 1;
-- +----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------------+
-- | id | select_type | table | partitions | type  | possible_keys | key     | key_len | ref   | rows | filtered | Extra       |
-- +----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------------+
-- |  1 | SIMPLE      | user  | NULL       | const | PRIMARY       | PRIMARY | 4       | const |    1 |   100.00 | Using index |
-- +----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------------+
```

**12 字段速查（按面试出现频率排序）**：

| 字段 | 一句话 | 高频度 |
|:---|:---|:---:|
| **type** | 访问类型（性能等级） | ⭐⭐⭐⭐⭐ |
| **key** | 实际用到的索引 | ⭐⭐⭐⭐⭐ |
| **rows** | 预估扫描行数 | ⭐⭐⭐⭐⭐ |
| **Extra** | 额外信息（看危险信号） | ⭐⭐⭐⭐⭐ |
| **key_len** | 用到索引的字节数（判断联合索引用了几列） | ⭐⭐⭐⭐ |
| **possible_keys** | 可能用到的索引 | ⭐⭐⭐ |
| **ref** | 索引比较的列或常量 | ⭐⭐⭐ |
| **filtered** | 过滤后剩余比例（百分比） | ⭐⭐⭐ |
| **id** | SQL 中 SELECT 的序号（多表 JOIN 看执行顺序） | ⭐⭐⭐ |
| **select_type** | 查询类型（SIMPLE / SUBQUERY / DERIVED 等） | ⭐⭐ |
| **table** | 当前这行输出在查哪张表 | ⭐⭐ |
| **partitions** | 分区表命中的分区 | ⭐ |

> [SQL优化与调优](SQL优化与调优.md) 讲过 type 排序、key、rows、Extra 4 个核心字段，本文重点补其余 8 个 + 4 个核心的深度细节。

---

## 二、12 字段详解

### 1. id —— SELECT 序号（多表执行顺序）

**规则**：

- `id` 相同：从上往下顺序执行（多表 JOIN 的常见情况）
- `id` 不同：**id 越大越先执行**（子查询/派生表先于外层）
- `id` 为 NULL：UNION 临时表去重阶段，最后执行

**案例 1：多表 JOIN（id 相同，从上往下）**

```sql
explain select * from orders o
  join user u on o.uid = u.id
  join product p on o.pid = p.id
  where u.age > 18;
-- +----+-------------+---------+
-- | id | select_type | table   |
-- +----+-------------+---------+
-- |  1 | SIMPLE      | u       |  ← 先执行（被驱动表的过滤条件最早落地）
-- |  1 | SIMPLE      | o       |
-- |  1 | SIMPLE      | p       |  ← 最后执行
-- +----+-------------+---------+
```

**案例 2：子查询（id 越大越先执行）**

```sql
explain select * from orders where uid in (
    select id from user where age > 18
);
-- +----+-------------+---------+
-- | id | select_type | table   |
-- +----+-------------+---------+
-- |  1 | PRIMARY     | orders  |  ← 外层查询 id=1
-- |  2 | SUBQUERY    | user    |  ← 子查询 id=2，先执行
-- +----+-------------+---------+
```

> **优化器可重写子查询为 JOIN**：MySQL 5.6+ 会自动把 `IN (SELECT ...)` 转成半连接（Semi-Join），所以你看到 id 可能都是 1。别按"子查询一定慢"的死记忆判断，看 explain。

### 2. select_type —— 查询类型

| select_type | 含义 | 例子 |
|:---|:---|:---|
| **SIMPLE** | 简单查询，无子查询 / UNION | `select * from t` |
| **PRIMARY** | 复杂查询的最外层 | `select * from t where id in (select ...)` 的外层 |
| **SUBQUERY** | 子查询中的第一个 SELECT | 上例的 `select id from ...` |
| **DERIVED** | 派生表（FROM 子句的子查询） | `select * from (select ...) x` |
| **UNION** | UNION 中的第二个及之后的 SELECT | `select ... union select ...` 的第二条 |
| **UNION RESULT** | UNION 临时去重结果 | id 为 NULL 的那一行 |
| **DEPENDENT SUBQUERY** | 子查询依赖外层（相关子查询） | `where id in (select uid from b where b.x = a.x)` |
| **MATERIALIZED** | 子查询物化（生成临时表） | MySQL 5.6+ 优化 `IN` 子查询 |

> **DEPRECATED 字段**：MySQL 8.0.16+ 部分子查询类型不再显示 SUBQUERY，改为 SIMPLE/MATERIALIZED，别死记。

### 3. table —— 当前在查哪张表

- 普通表名
- `<derivedN>`：派生表，N 是 id
- `<unionM,N>`：UNION 临时表，M、N 是参与 UNION 的 id
- `<subqueryN>`：物化子查询

### 4. partitions —— 分区命中的分区

非分区表显示 NULL。分区表查询若没命中分区键过滤，可能扫所有分区 → 性能差。

### 5. type —— 访问类型（核心，性能等级）

`SQL优化与调优.md` 已给 type 排序，这里补**每个 type 的判定条件**：

| type | 触发条件 | 案例 |
|:---|:---|:---|
| **system** | 表只有一行（系统表） | `select * from mysql.proxies_priv` |
| **const** | 主键或唯一索引**等值**查询，最多 1 行 | `where id = 1`（id 是主键） |
| **eq_ref** | JOIN 时被驱动表用主键/唯一索引关联，最多 1 行 | `join b on a.id = b.id`（b.id 是主键） |
| **ref** | 普通索引等值查询 / JOIN 走非唯一索引 | `where name = 'tom'`（name 普通索引） |
| **range** | 索引范围扫描（`>`, `<`, `between`, `in`, `>=`） | `where id > 100` |
| **index** | 扫描**整个索引树**（不回表也比 ALL 快） | `count(*)` 走二级索引 |
| **ALL** | **全表扫描** | 无索引或索引失效 |

**进阶 type**（MySQL 5.6+）：

| type | 含义 |
|:---|:---|
| **fulltext** | 走全文索引 |
| **ref_or_null** | 等值查询 + 包含 NULL 行（`where name = 'tom' or name is null`） |
| **index_merge** | 索引合并（多个单列索引 OR 起来用） |
| **unique_subquery** | IN 子查询走主键/唯一索引 |
| **index_subquery** | IN 子查询走普通索引 |

> **优化目标**：至少到 **range**，理想到 **ref / const / eq_ref**。看到 `type=ALL` 且 rows 很大，重点优化（详见 [SQL优化的实际场景](SQL优化的实际场景.md) Q2）。

### 6. possible_keys —— 可能用到的索引

**可能 keys 多但实际 key 为 NULL = 索引失效**：

- 字段做了函数/运算：`WHERE DATE(create_time)='2026-08-30'`
- 隐式类型转换：`WHERE phone=13800001111`（phone 是 varchar）
- 最左前缀断裂：联合索引 `(a,b,c)` 但 `WHERE b=1`
- 优化器估算全表扫更快（rows 太多）：用 `force index(...)` 验证

### 7. key —— 实际用到的索引

- 显示**实际选用**的索引名
- NULL = 没走索引
- `possible_keys` 多但 `key` 是另一个 → 优化器选了更优的；想强制走某个用 `force index(...)`

### 8. key_len —— 用到索引的字节数（必考）

**判断联合索引用了几列**——这是 key_len 最常见的面试考点。

#### 计算公式（ utf8mb4 字符集为基准）

| 类型 | 单字段长度 | 备注 |
|:---|:---|:---|
| `int` | 4 | |
| `bigint` | 8 | |
| `char(n)` | `n × 4` | utf8mb4 一个字符最多 4 字节 |
| `varchar(n)` | `n × 4 + 2` | 2 字节存长度前缀 |
| `tinyint` | 1 | |
| `date` | 3 | |
| `datetime` | 5 | |
| `timestamp` | 4 | |

**变长字段额外加 1~2 字节**：

- 字段允许 NULL：+ 1 字节（存 NULL 标记）
- varchar 等变长字段：+ 2 字节（存实际长度）

#### 案例：联合索引 `(a, b, c)` 用了几列？

```sql
-- 假设：a int, b varchar(20), c bigint，且都允许 NULL
-- 单列字节数：a=4+1=5（NULL 标记），b=20×4+2+1=83（变长+NULL），c=8+1=9

-- 查询 1：where a = 1
key_len = 5           → 用了 1 列（a）

-- 查询 2：where a = 1 and b = 'x'
key_len = 5 + 83 = 88 → 用了 2 列（a, b）

-- 查询 3：where a = 1 and b = 'x' and c = 1
key_len = 5 + 83 + 9 = 97 → 用了 3 列（a, b, c）

-- 查询 4：where a = 1 and c = 1   ❌ 跳过 b
key_len = 5           → 只用了 1 列（a），中间断裂
```

> **口诀**：key_len 越大 = 联合索引用得越充分；key_len 突然变小 = 某列没走索引（最左前缀断裂）。

### 9. ref —— 索引比较的列或常量

显示**用哪个列或常量**来与索引列比较：

| ref | 含义 |
|:---|:---|
| `const` | 与常量比较（`where id = 1`） |
| `db.t1.col` | 与另一表的列比较（`join on a.id = b.uid` 时被驱动表的 ref 是 `db.a.id`） |
| `func` | 与函数结果比较 |
| `NULL` | 还未确定（动态优化时） |

### 10. rows —— 预估扫描行数

- **统计信息估算值**，不是实际行数
- MySQL 8.0+ 基于 data sampling 估算，统计信息过期会失真 → `analyze table xxx;` 重新收集
- 是优化器选索引的核心依据：rows 小的索引优先（[SQL优化的实际场景](SQL优化的实际场景.md) Q3「时快时慢」根因之一）

### 11. filtered —— 过滤后剩余比例

- 5.1 引入，表示**用表条件过滤后剩余行占比（百分比）**
- 例：rows=1000，filtered=10 → 优化器估算过滤后剩 100 行
- **越接近 100 越好**——意味着 SQL 几乎没浪费扫描

> **filtered 在配合 JOIN 时尤其重要**：被驱动表实际行数 = 驱动表 filtered × 驱动表 rows ÷ 100 × 被驱动表 rows（粗算）。filtered 低 = 大量扫描浪费在过滤上。

### 12. Extra —— 额外信息（看危险信号）

**完整 Extra 速查表**：

| Extra | 含义 | 好坏 | 优化方向 |
|:---|:---|:---:|:---|
| `Using index` | ✅ 覆盖索引，不回表 | 极好 | — |
| `Using where` | 用 where 过滤 | 正常 | — |
| `Using index condition` | 索引条件下推（ICP） | 好 | 5.6+ 优化，默认开 |
| `Using MRR` | 多范围读优化 | 好 | 5.1+ 优化，默认关 |
| `Using join buffer` | 用 BNL/BKA join 缓存 | ⚠️ 慢 | 加被驱动表索引 |
| `Using filesort` | ❌ 额外排序 | 慢 | 给 order by 加索引 |
| `Using temporary` | ❌ 用临时表 | 慢 | 给 group by 加索引 |
| `Using FTE...` | 走全文索引 | 中 | 看场景 |
| `Impossible WHERE` | where 恒为假（如 `1=0`） | 0 行 | 检查 SQL |
| `No tables used` | 没有 FROM 子句 | 正常 | — |
| `Select tables optimized away` | 优化器直接用索引得出结果，无需扫表 | 极好 | — |
| `Distinct` | 优化 DISTINCT，找到一行就停 | 好 | — |
| `Using union(...)` | 索引合并 OR | 中 | 看场景 |
| `Using sort_union(...)` | 索引合并 OR，但需排序 | 中慢 | 看 [SQL优化的实际场景](SQL优化的实际场景.md) Q2 |

**最常见的危险信号 = `Using filesort` + `Using temporary`**，详见 [SQL优化与调优](SQL优化与调优.md) 第二节。

---

## 三、多表 JOIN 执行顺序（id 顺序规则）

### 驱动表 vs 被驱动表

```
JOIN 执行模型：
  遍历驱动表的每一行 → 拿这一行的关联列去被驱动表里找匹配 → 拼结果

  驱动表 rows ×（被驱动表每次扫描行数）= 总扫描行数
  ↑ 这是 Nested Loop Join 的核心代价模型
```

**NLJ 三种变体**（优化器自动选）：

| 变体 | 触发条件 | 性能 |
|:---|:---|:---:|
| **Index Nested Loop（INL）** | 被驱动表关联列有索引 | ✅ 快 |
| **Block Nested Loop（BNL）** | 被驱动表关联列无索引 | ❌ 慢（需用 join buffer 缓存） |
| **Batched Key Access（BKA）** | INL + MRR 批量查索引 | ✅ 较快 |

> 看 `Extra=Using join buffer (Block Nested Loop)` → 被驱动表无索引，加索引治本。

### 驱动表选择

优化器选**小结果集的表做驱动表**：

- 一行 join 一行 vs 一行 join 一万行 → 前者快 1 万倍
- MySQL 8.0.18+ 也支持 Hash Join（替代 BNL，更快）

---

## 四、key_len 计算公式（必考）

### 公式总结

```
单列 key_len = 类型字节数 + 是否可空（1 字节） + 是否变长（2 字节）

联合索引 key_len = 各列 key_len 之和
```

### 完整类型对照表（utf8mb4 字符集）

| 类型 | 长度（非空定长） | 可空 +1 | 变长 +2 | 备注 |
|:---|:---:|:---:|:---:|:---|
| `tinyint` | 1 | 2 | — | |
| `smallint` | 2 | 3 | — | |
| `int` | 4 | 5 | — | |
| `bigint` | 8 | 9 | — | |
| `char(n)` | `n×4` | `n×4+1` | — | utf8mb4 |
| `varchar(n)` | `n×4+2` | `n×4+3` | +2（已含） | utf8mb4，2 是长度前缀 |
| `date` | 3 | 4 | — | |
| `timestamp` | 4 | 5 | — | |
| `datetime` | 5 | 6 | — | |
| `decimal(p,s)` | varies | varies | — | 整数部分+小数部分每 9 位 4 字节 |

### 实战案例

```sql
-- 建表
create table t (
  a int not null,
  b varchar(20) not null,
  c bigint not null,
  d varchar(30) null,
  index idx_abc (a, b, c, d)
) engine=innodb default charset=utf8mb4;

-- 单列长度：a=4，b=20×4+2=82，c=8，d=30×4+2+1=123

-- 测试
explain select * from t where a = 1;
-- key_len = 4  → 用了 a

explain select * from t where a = 1 and b = 'x';
-- key_len = 4 + 82 = 86 → 用了 a, b

explain select * from t where a = 1 and c = 1;   -- 跳过 b
-- key_len = 4  → 只用了 a（最左前缀断裂）

explain select * from t where a > 1;
-- key_len = 4，type=range → 范围查询后边的 b/c/d 都用不上
```

> **范围查询是 key_len 杀手**：`>`、`<`、`between`、`like 'x%'` 之后的列都用不上联合索引。

---

## 五、format=json 与 optimizer_trace（看优化器内部决策）

### 1. `explain format=json` —— 详细成本估算

```sql
explain format=json select * from user where age > 18;
-- 输出 JSON，含 cost_info（查询成本）、used_columns（用到的列）、attached_condition（附加条件）等
```

关键字段：

| JSON 字段 | 含义 |
|:---|:---|
| `cost_info.query_cost` | 优化器估算的总成本 |
| `cost_info.read_cost` | 读成本（I/O） |
| `cost_info.eval_cost` | 评估成本（CPU） |
| `used_columns` | 实际用到的列 |
| `attached_condition` | 经过优化器改写后的 where |

### 2. `optimizer_trace` —— 看优化器选索引的决策过程

```sql
-- 1) 打开 trace
set optimizer_trace = 'enabled=on', end_markers_in_json=on;
set optimizer_trace_max_mem_size = 1000000;

-- 2) 执行查询（不会真执行，被 trace 截获）
select * from user where age > 18;

-- 3) 看 trace
select * from information_schema.optimizer_trace;
-- 输出 JSON，记录优化器分析过的所有执行计划 + 成本对比 + 最终选定的方案
```

**适用场景**：

- SQL 一直没走你期望的索引，想知道优化器为啥选另一个 → 看 trace 里的 `rows_estimation`、`considered_execution_plans`
- 多表 JOIN 时优化器选的驱动表跟你预期不同 → 看 `reconsidering_access_types_for_join`
- 索引新增后没生效 → 看 trace 里的代价对比

> **生产慎用**：optimizer_trace 占用内存（默认 16KB），跑完立刻 `set optimizer_trace='enabled=off';` 关掉，避免长连接一直占用。

---

## 六、易错点

| 易错点 | 说明 |
|:---|:---|
| **以为 explain 真的执行 SQL** | 不执行，只看优化器选的方案；要看真实行数用 `select count(*)` 配合 |
| **rows 是估算值不是真实行数** | 估算基于统计信息，过期会失真；先 `analyze table xxx;` 再看 |
| **filtered 默认估算不准** | 5.7 之前估算粗（默认 100），8.0+ 条件下推后才准 |
| **possible_keys 为 NULL 不一定慢** | 没有候选索引当然为 NULL，但表小或 type=ALL + rows 小也快 |
| **key_len 大不等于性能好** | 联合索引用得全才好；如果是单列索引 key_len 大只是字段类型大 |
| **Extra=Using where 不一定慢** | 看是不是配合 `Using index`（覆盖索引）—— 用索引过滤 + 不回表是好事 |
| **type=index 不等于全表扫** | type=index 扫索引树（比扫表快），type=ALL 才是全表扫；`count(*)` 走二级索引就是 index |
| **5.6+ 子查询不一定是 SUBQUERY** | 优化器会把 `IN (SELECT)` 改写为 Semi-Join，id 全是 1，别按老规则判断 |
| **忘记 analyze 导致计划漂移** | 数据量增长/数据分布变化后统计信息过期，rows 估算失真 → 计划换索引变慢（详见 [SQL优化的实际场景](SQL优化的实际场景.md) Q3） |

---

## 七、一句话总结

`explain` 12 字段速背：**id 看执行顺序**（同号从上往下、不同号大值先执行）、**select_type 看查询类型**（SIMPLE/PRIMARY/SUBQUERY/DERIVED/UNION）、**type 看性能等级**（system/const/eq_ref/ref/range/index/ALL，目标至少到 range）、**key 看**实际**索引**、**possible_keys 看**可能**索引**（多但 key=NULL 是失效信号）、**key_len 算联合索引用了几列**（变长+2、可空+1）、**ref 看比较对象**（const/列）、**rows 是估算扫描行数**（统计信息过期 analyze 一下）、**filtered 看过滤比例**（越接近 100 越好）、**Extra 看危险信号**（Using filesort / Using temporary 必看；Using index 覆盖索引是好事）。多表 JOIN 关注 `Using join buffer`（被驱动表无索引）+ 驱动表选小结果集。深度排查用 `format=json` 看成本估算 + `optimizer_trace` 看索引选择决策过程。

---

## 八、相关笔记

| 主题 | 笔记 |
|:---|:---|
| explain 入门 4 字段 + 慢日志 + SQL 改写常见手法 | [SQL优化与调优.md](SQL优化与调优.md) |
| SQL 优化场景题（排查五步骨架、索引失效、时快时慢分诊、慢 SQL 治理） | [SQL优化的实际场景.md](SQL优化的实际场景.md) |
| 索引失效原理（B+ 树最左前缀、函数运算、隐式转换） | [索引.md](索引.md) |
| 深分页优化（key_len + 范围扫描在深分页里的代价） | [深分页优化.md](深分页优化.md) |
| JOIN 放大与笛卡尔积（漏 ON 的静默笛卡尔积） | [笛卡尔积.md](笛卡尔积.md) |
| MySQL 8.0 Hash Join / 优化器新特性 | [存储引擎与架构.md](存储引擎与架构.md) |
