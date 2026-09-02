# ElasticSearch 面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——从倒排索引到集群治理，成体系地理解 ES「为什么快、怎么写、怎么查、怎么扛高可用」。

按「**概念与架构 → 写入与近实时 → 搜索与评分 → 深分页与调优 → 高可用与治理**」的主线组织，每篇均含对比表、图解、易错点、一句话总结。回答默认以 **7.x/8.x** 为准（`_type` 已移除、选主已自动化）。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🏛️ 核心概念与整体架构 | [核心概念与整体架构.md](核心概念与整体架构.md) | **倒排索引三层结构（FST/Roaring Bitmap）**、与 MySQL 对比互补、概念层级（Cluster/Shard/Replica）、**分片路由公式**、节点角色、**选主与脑裂** |
| 2 | ✍️ 写入流程与近实时原理 | [写入流程与近实时原理.md](写入流程与近实时原理.md) | 单文档写入全链路、**refresh/flush/translog 三件套**、为什么是「近实时」、**Segment 不可变性**、更新删除原理、乐观锁 `if_seq_no` |
| 3 | 🔍 搜索流程与相关性评分 | [搜索流程与相关性评分.md](搜索流程与相关性评分.md) | **Query Then Fetch 两阶段**、query vs filter context、**BM25 评分**、分词器三件套与 IK、match vs term、聚合与 doc_values |
| 4 | 📄 深分页与性能调优 | [深分页与性能调优.md](深分页与性能调优.md) | **from+size 深分页陷阱**、scroll vs search_after、写入调优（bulk/refresh）、查询调优、**JVM 与断路器** |
| 5 | 🛡️ 高可用与集群治理 | [高可用与集群治理.md](高可用与集群治理.md) | 副本容灾、**脑裂防范演进**、分片分配与延迟恢复、扩容与 reindex、**冷热分离与 ILM**、生产实战案例 |

## P7 必背清单（速查）

- **倒排索引三层**：term index（**FST，常驻内存**，前缀定位）→ term dictionary（磁盘，分 block）→ posting list（**Roaring Bitmap 压缩 + skip list** 加速交集）
- **ES 为什么快**：倒排 + 列存 doc_values + 文件系统缓存 + 顺序写 segment，读多写少场景天然友好
- **与 MySQL 互补**：MySQL=OLTP 强事务/点查更新，ES=全文检索/多维聚合/日志分析；ES 不支持事务、join 弱、更新代价高
- **分片路由**：`shard = hash(routing) % number_of_primary_shards`；**主分片数创建后不可改**（只能 reindex/split），副本可动态调
- **写入链路**：协调节点按 routing 定位主分片 → 主分片写成功 → 并行同步 in-sync 副本 → 达到 `wait_for_active_shards`（默认 quorum）即响应客户端
- **近实时 NRT**：写入先进内存 buffer，**refresh（默认 1s）生成新 segment 即可搜**，不是写入即可见
- **refresh / flush / translog**：refresh=可搜索（内存→segment，不落盘）；flush=Lucene commit **fsync 落盘**+清空 translog；translog（7.x 默认 request 级 fsync）保证宕机不丢已确认数据
- **Segment 不可变**：并发读无锁、可压缩缓存友好；代价是删除只做标记（.live docs）、更新=删旧+写新、空间膨胀靠后台 merge 回收
- **乐观锁**：`if_seq_no` + `if_primary_term`（6.7+ 替代外部 version）
- **搜索两阶段**：Query Phase 各分片返回「top N 的 docId + 排序值」→ 协调节点归并 → Fetch Phase 再回源拉 `_source`；`dfs_query_then_fetch` 可消除分片词频偏差但更慢
- **query vs filter context**：query 算相关性分；filter **不算分且结果可被 bitset 缓存**，能用 filter 就用 filter
- **BM25**：解决 TF-IDF 词频无饱和问题；`k1` 控饱和速度、`b` 控文档长度归一
- **深分页三方案**：from+size（≤10000，协调点要归并 `分片数×(from+size)` 条）；scroll（快照、不实时、占上下文，适合离线导出）；**search_after+PIT（实时推荐，游标按唯一排序值续查）**
- **写入调优**：bulk 5~15MB、大导入期 `refresh_interval=-1` + `replicas=0` 导完恢复、`_source`/`index:false` 按需瘦身
- **JVM 铁律**：堆 ≤32G（压缩指针）、**≤物理内存 50%，另一半留给 Lucene 文件系统缓存**、聚合靠 doc_values 别开 fielddata、断路器兜底 OOM
- **脑裂演进**：6.x 手工 `minimum_master_nodes`；**7.x 起自动 voting configuration 仲裁，master-eligible 保持 ≥3 奇数**即可
- **节点掉线**：默认 1 分钟后即触发全量再平衡，生产配 `index.unassigned.node_left.delayed_timeout: 5m` 防 IO 风暴
- **冷热分离**：hot（SSD 新数据）/warm（大磁盘旧数据）+ ILM 按 rollover→delete 自动流转

## 学习/复习建议

1. 按 1→5 顺序建立体系：**「一条写入链 + 一次搜索两阶段」是纲**，其余都是这条链上的展开与调优。
2. 必须能白板画的三张图：倒排索引三层结构、写入流程时序图、Query Then Fetch 两阶段。
3. 三大追问题要答到原理级：「为什么是近实时而不是实时」「深分页为什么默认 10000」「脑裂怎么防」。
4. 「易错点」多为真实生产事故点（refresh 误解为落盘、fielddata 打爆堆、scroll 上下文泄漏），结合第 5 篇案例记忆。
5. 结合项目讲：日志平台冷热分层、search_after 替换 from+size、大导入期关闭副本，都是 P7 加分案例。

## 相关笔记

- 分布式基础（CAP、一致性）→ [../分布式/README.md](../分布式/README.md)
- 缓存与检索互补（Redis 数据类型与实战）→ [../redis/README.md](../redis/README.md)
- 消息中间件削峰写入（RocketMQ 存储机制）→ [../rocketMq/存储机制与刷盘.md](../rocketMq/存储机制与刷盘.md)
- MySQL 索引与分库分表（数据迁移到 ES 的源端）→ [../mysql/4.%20索引.md](../mysql/4.%20索引.md)、[../mysql/12.%20分库分表.md](../mysql/12.%20分库分表.md)
- 系统设计（Feed 流、日志平台类设计）→ [../系统设计/README.md](../系统设计/README.md)
