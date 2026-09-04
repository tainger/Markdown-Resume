# Redis 面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——从 5 种数据类型的底层优化到 Cluster 哈希槽分片，成体系地理解 Redis「为什么快、怎么持久化、内存不够怎么办、怎么扛高可用、怎么当缓存/锁」。

按「**命令执行全链路（建立全局认知）→ 数据类型与底层 → 持久化 → 过期与内存淘汰 → 高可用与集群 → 缓存实战 → 大 Key 与数据倾斜治理 → 生产问题排查**」由浅入深组织，每篇均含对比表、ASCII 图解、易错点、一句话总结。回答默认以 **7.x** 为准（ziplist 被 listpack 取代、Set 支持 listpack 编码）。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🔗 一条命令执行过程（枢纽篇） | [1. 一条redis命令执行过程.md](1. 一条redis命令执行过程.md) | **7 阶段全链路**：客户端 RESP 编码 → TCP → epoll 事件循环 → IO 线程读+解析 → 主线程查命令表单线程执行 → 写回输出缓冲区 → 异步触发持久化/复制/过期；**Redis 6.0 多线程 IO** 但命令仍单线程的关键区别；一图串联所有 Redis 机制 |
| 2 | 📦 数据类型与底层结构 | [2. 数据类型与底层结构.md](2. 数据类型与底层结构.md) | **为什么快（纯内存+单线程+IO多路复用+高效结构）**、SDS vs C字符串、embstr/raw 44字节分界、quicklist、listpack/intset 紧凑编码、**ZSet 跳表+哈希表双结构原因** |
| 3 | 💾 持久化 | [3. 持久化.md](3. 持久化.md) | **bgsave 写时复制 COW**、AOF **写后日志**与 MySQL WAL 对比、appendfsync 三策略、**AOF 重写两缓冲区**、4.0 **混合持久化**（RDB打底+AOF增量） |
| 4 | ⏰ 过期与内存淘汰 | [4. 过期与内存淘汰.md](4. 过期与内存淘汰.md) | 过期字典、**惰性删除+定期删除（随机抽样+25%阈值）**、**8 种淘汰策略**（volatile-/allkeys- × lru/lfu/random/ttl）、近似 LRU 采样、**LFU 概率递增+时间衰减** |
| 5 | 🌐 高可用与集群 | [5. 高可用与集群.md](5. 高可用与集群.md) | 主从**全量复制（RDB+repl buffer）**、**增量复制（repl_backlog+offset）**、哨兵**主观/客观下线**+Raft选Leader+故障转移选新主依据、**Cluster 16384 哈希槽**（CRC16%16384）、hash tag |
| 6 | 🔥 缓存问题与实战 | [6. 缓存问题与实战.md](6. 缓存问题与实战.md) | **缓存三兄弟（穿透/击穿/雪崩）**对比与解法、Cache Aside **先更新库再删缓存**+延迟双删、binlog订阅最终一致、**分布式锁（SET NX EX+唯一value+Lua解锁）**、Redisson看门狗续期、RedLock争议 |
| 7 | 📊 大 Key 与数据倾斜 | [7. 大Key与数据倾斜.md](7. 大Key与数据倾斜.md) | **大 Key 判定**（String 看字节/容器看元素数）、四大危害（阻塞主线程/复制延迟/容量倾斜/淘汰困难）、识别（--bigkeys/SCAN 巡检/MEMORY USAGE）、治理（**分桶拆分 + UNLINK 异步删 + 压缩**）、**数据倾斜四成因**（大key/热key/hash tag/业务分布）、热 key 本地缓存兜底 + 多副本打散 + 读写分离 |
| 8 | 🚨 生产问题与排查手册 | [8. 生产问题与排查手册.md](8. 生产问题与排查手册.md) | **7 大故障域**实战排查：阻塞类（慢命令/大key/fork/Lua）、内存类（OOM/碎片率/淘汰）、连接类（maxclients/泄漏）、持久化类（AOF损坏/RDB失败/IO瓶颈）、高可用类（主从延迟/脑裂/cluster迁移）、一致性类（双写/异步丢数据/缓存三兄弟）、性能类（CPU/网络/热key）；每个统一「现象→排查命令→根因→方案」+ **排查命令速查表** |
| 9 | 🔥 热点 Key 问题与解决方案 | [9. 热点Key问题.md](9. 热点Key问题.md) | **发现定位四方法**（--hotkeys/客户端埋点/代理层/MONITOR 采样）、**选型决策树**（读多写少→本地缓存、QPS 极高→多副本、有从库→读写分离、写多读少→分片聚合）、Caffeine 本地缓存生产级 5 优化、多副本副本数决策、EasyBI 报表热点 key 实战案例 |

## P7 必背清单（速查）

- **Redis 为什么快**：纯内存操作 + **单线程命令执行**（无锁竞争、无上下文切换）+ **epoll IO 多路复用**（Reactor模型）+ SDS/跳表/压缩列表等高效结构；瓶颈在内存和网络不在CPU
- **一条命令执行全链路**（枢纽篇）：客户端 RESP 编码 → TCP → epoll 事件循环检测 EPOLLIN → IO 线程读+解析（6.0+多线程）→ 主线程查 redisCommand 表单线程执行 dictAdd → 写回 client->buf → epoll 检测 EPOLLOUT → IO 线程写 socket → 后置动作（AOF 写缓冲区/复制写 repl_backlog/过期字典）；**命令执行严格单线程，多线程只加速网络 IO**——[1. 一条redis命令执行过程.md](1. 一条redis命令执行过程.md)
- **6.0 多线程**：仅**网络 IO（读写socket、协议解析）**用多线程，**命令执行仍严格单线程**，避免并发问题
- **SDS 解决 C 字符串 4 个痛点**：O(1)取长度（存len）、拼接防溢出（存free）、空间预分配+惰性释放（减少realloc）、二进制安全（用len而非\0判结尾）
- **String 三编码**：int（long可表示的整数）→ embstr（≤44字节，redisObject+SDS连续分配一次malloc）→ raw（>44字节，分开分配）
- **底层编码演进**：3.2 List 统一为 **quicklist**（双向链表+每个节点内嵌listpack）；7.0 **ziplist 全面被 listpack 取代**（解决连锁更新问题）
- **ZSet 双结构缺一不可**：哈希表存 member→score 让 **zscore O(1)**，跳表按 score 有序让 **zrange/zrank O(log n)**；只用跳表查分要遍历、只用哈希表做不了范围查询
- **跳表 vs 红黑树**：跳表实现更简单，**范围查询找到起点后沿底层链表顺序走即可**（无需中序遍历），插入删除只需局部调指针（旋转代价小）
- **bgsave 写时复制**：fork子进程共享父进程物理页（只读），父进程改页时OS才复制该页；**fork瞬间复制页表会短暂阻塞**，大内存实例慎用高频bgsave
- **AOF 是写后日志**：**先执行命令、成功后才写缓冲区**（与MySQL redolog WAL相反）；好处是不记错误命令，坏处是执行完写日志前宕机丢这条
- **AOF 重写不是压缩旧文件**：是 fork 子进程**根据当前内存状态重新生成**最精简命令集；重写期间新写命令同时写「AOF缓冲区」和「AOF重写缓冲区」，重写完成后把重写缓冲区追加到新文件
- **混合持久化**：AOF重写时前半段写**RDB格式全量快照**（恢复快），后半段写**AOF格式增量命令**（少丢数据），4.0后默认开启 aof-use-rdb-preamble yes
- **过期删除不是全量扫**：过期字典 + **惰性删除**（访问时检查）+ **定期删除**（每秒10次，每次随机抽20个key，过期占比>25%就继续抽），靠定期+惰性+内存淘汰三重兜底
- **8 种淘汰速记**：`volatile-*`=只在设了TTL的key里淘汰、`allkeys-*`=所有key都可能被淘汰；后缀=算法 **lru/lfu/random/ttl**；默认 **noeviction 写报错**，最常用 **allkeys-lru**
- **近似 LRU 不是标准 LRU**：每个key的redisObject存lru时间戳，淘汰时**随机采样N个（默认5）**淘汰这批里最旧的；省掉了标准LRU的双向链表内存开销
- **LFU 4.0 引入**：计数器不是简单累加，而是**基于概率递增**（值越大越难加）+**随时间衰减**（解决早期热点永久霸榜）
- **主从复制全量流程**：从库发psync → 主库bgsave生成RDB → 同时写命令进**repl buffer复制缓冲区** → 从库加载RDB → 追执行repl buffer中的命令
- **增量复制三要素**：**repl_backlog复制积压缓冲区**（环形）、主库offset、从库offset；从库重连上报自己的offset，还在积压区内就补差，否则退化为全量
- **主从是最终一致**：命令异步传播，主库写完立即返回不等待从库确认，从库读可能有延迟
- **哨兵两阶段下线**：**主观下线SDOWN**=单个哨兵ping不通 → **客观下线ODOWN**=≥quorum个哨兵都认为主库挂了 → 才触发故障转移
- **哨兵选 Leader 用 Raft**：故障转移由一个Leader哨兵主持；选新主库依据：**slave-priority优先级 → repl offset最大（数据最全）→ runid最小**
- **哨兵部署铁律**：至少 **3个且奇数**，否则 quorum+majority 选举可能死锁；quorum通常设为 N/2+1
- **Cluster 不用一致性哈希**：用**16384个哈希槽**，slot=CRC16(key)%16384；每个主节点负责一段槽；增删节点只需迁移槽+数据，比一致性哈希更可控
- **哈希槽为什么是 16384**：心跳包要带槽位图，16384bit=2KB大小适中；集群节点一般≤1000，够用
- **Cluster 多 key 限制**：mset/事务/Lua涉及多key时，不同槽会失败；用 **hash tag**：`{user1}:name` 强制按{}内部分算槽，保证落同一节点
- **缓存三兄弟区别**：**穿透**=查DB也不存在的数据→布隆过滤器/缓存空值；**击穿**=单个热点key过期→互斥锁重建/逻辑过期；**雪崩**=大量key同时过期或Redis宕→过期加随机值+集群高可用
- **双写一致性结论**：无法强一致，只能**最终一致**；常规「**先更新DB，再删缓存** + 缓存过期兜底」；高要求上**Canal订阅binlog异步删缓存**；延迟双删是补充手段不是银弹
- **分布式锁五要素**：`SET lock_key unique_value NX EX 30` → NX保证互斥、EX过期防死锁、**唯一value防误删别人的锁**、**Lua脚本保证判断+删除原子**、Redisson**看门狗每10秒续期到30秒**防提前过期
- **RedLock 有争议**：向多个独立Redis节点加锁，多数成功才算拿到；时钟漂移、节点崩溃恢复可能导致双持；强一致场景优先考虑 ZooKeeper/etcd
- **大 Key 判定标准**：String value > 10KB 或 > 1MB 必查；Hash/List/Set/ZSet 元素数 > 5000；口诀「**String 看字节，容器看元素数**」
- **大 Key 四大危害**：阻塞主线程（DEL 同步释放/HGETALL O(N)）、复制延迟（大 value 传输）、容量倾斜（单 slot 被撑满）、淘汰与持久化困难
- **大 Key 治理三板斧**：① **拆分**（Hash 分桶、List 按时间段、String 大 JSON 拆多 key）② **UNLINK 异步删**（4.0+ 后台线程释放，不用 DEL）③ **压缩**（Protobuf/MessagePack + ziplist 阈值）
- **大 Key 识别**：`redis-cli --bigkeys` 快速概览（只给 TOP1 会漏）→ `SCAN + TYPE + HLEN/LLEN` 全量巡检脚本 → `MEMORY USAGE key` 精确字节
- **数据倾斜四成因**：① 大 key 占满单槽 ② 热点 key 集中访问 ③ hash tag 滥用（tag 分布不均）④ 业务 key 天然不均（如按省份分）
- **数据倾斜治理优先级**：先拆大 Key（治本）→ 再治热 key 兜底（本地 Caffeine 缓存短 TTL + 多副本随机后缀 + 读写分离）→ 最后调槽分布（运维）
- **热 Key 本地缓存兜底**：Caffeine maximumSize + expireAfterWrite 1~5s 短 TTL 保证一致性，多实例天然分散 QPS；强一致场景（库存/余额）不能用读写分离
- **热点 Key 治理全流程**（独立篇）：发现四方法（--hotkeys 需 LFU / 客户端埋点 / 代理层统计 / MONITOR 采样）→ 选型决策树（读多写少→本地缓存、QPS 极高→多副本打散、有从库→读写分离、写多读少→分片聚合）→ 生产级 5 优化（热点提升+短 TTL+命中率监控+双删+淘汰监听）——[9. 热点Key问题.md](9. 热点Key问题.md)
- **热 key vs 缓存击穿**：热 key 是**持续**高 QPS（空间倾斜），击穿是热 key **过期瞬间**大量回源（时间冲击）；本地缓存兜底天然缓解两者
- **Redis 生产排查闭环**：现象 → 故障域（阻塞/内存/连接/持久化/高可用/一致性/性能）→ 命令 → 根因 → 止血+根治；阻塞首查 SLOWLOG+CLIENT LIST，内存首查 INFO memory+MEMORY USAGE，连接首查 INFO clients
- **慢命令四大元凶**：KEYS * / HGETALL 全量 / DEL 大key / Lua 阻塞；生产禁用 KEYS+FLUSHALL，大命令改 SCAN 系列，删大key 用 UNLINK
- **fork 阻塞根因与治理**：fork 复制**页表**（只读共享非复制数据），大内存实例页表大 fork 慢；治理：实例<10GB、关 THP、vm.overcommit_memory=1、低峰调度 bgsave
- **内存碎片率高**：mem_fragmentation_ratio>1.5，jemalloc 碎片；治理：activedefrag yes 自动整理 + DEBUG MEMORY PURGE 临时 + 低峰重启
- **连接数打满根因**：maxclients reached 根因是连接泄漏/慢命令堆积，调大 maxclients 只治标；排查 CLIENT LIST idle 字段修连接池
- **主从同步延迟大**：主从 offset 差是从库单线程回放跟不上/大key同步/网络；延迟敏感请求走主库 + 拆大key + 半同步复制
- **异步复制会丢数据**：主库写完立即返回不等待从库ACK；半同步复制 WAIT numreplicas timeout 缓解，金融强一致用 ZooKeeper/etcd

## 学习/复习建议

1. 按 1→7 顺序建立体系：**「为什么快」是纲**，数据结构优化→持久化→淘汰→高可用→缓存实战→大Key/倾斜治理→生产排查实战都是这条纲上的展开。
2. 必须能白板画的三张图：SDS结构与quicklist结构图、bgsave写时复制示意图、哨兵故障转移全流程、Cluster哈希槽路由。
3. 三大对比追问要答到原理级：「RDB vs AOF vs 混合持久化选型」「LRU vs LFU 场景差异」「主从/哨兵/Cluster三级递进边界」。
4. 「易错点」是 P7 面试反套路区：AOF写后日志与MySQL WAL区别、定期删除是随机抽样而非全扫、默认策略是noeviction、哈希槽是16384不是65536、哨兵必须≥3奇数。
5. 结合项目讲：缓存穿透上布隆过滤器、分布式锁看门狗续期、双写用binlog订阅最终一致，都是加分案例。
6. 速背节奏：P7必背清单过3遍 → 每篇的「一句话总结」背熟 → 对照「易错点」自查盲区。
7. ⭐ **Java 端落地看隔壁 redisson/**：Redis 服务端机制看透了不代表会用，Redisson（可重入锁/看门狗源码/本地缓存/延迟队列/限流/SpringCache+事务坑）是真正面试第二战场。

## 相关笔记

- 跳表数据结构详解 → [../数据结构/跳表.md](../数据结构/跳表.md)
- MySQL（缓存背后的源数据库，索引+事务）→ [../mysql/README.md](../mysql/README.md)
- RocketMQ（削峰填谷与缓存配合）→ [../rocketMq/README.md](../rocketMq/README.md)
- 分布式事务与一致性协议 → [../分布式/README.md](../分布式/README.md)
- Redisson 框架（Java 端分布式锁全家桶 & Spring 集成）→ [../redisson/README.md](../redisson/README.md)
- 系统设计（短链/秒杀/ID生成等Redis高频场景）→ [../系统设计/README.md](../系统设计/README.md)
