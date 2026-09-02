# MyBatis 面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——从执行流程到插件扩展，成体系地理解 MyBatis。

按「**架构与执行 → 缓存 → 动态 SQL 与绑定 → 插件与高级特性**」的主线组织，每篇均含对比表、图解、易错点、一句话总结。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🏗️ 核心架构与执行流程 | [核心架构与执行流程.md](核心架构与执行流程.md) | 三层架构、核心组件、SQL 执行旅程、**Mapper 动态代理原理**、Executor 三类型、vs Hibernate |
| 2 | 💾 缓存机制 | [缓存机制.md](缓存机制.md) | 一级缓存作用域/失效、二级缓存开启与三大坑、Spring 下一级缓存真相、生产用 Redis 替代 |
| 3 | 🔐 动态SQL与参数绑定 | [动态SQL与参数绑定.md](动态SQL与参数绑定.md) | **`#{}` vs `${}` 与 SQL 注入**、like 写法、动态标签、foreach vs BATCH、N+1 |
| 4 | 🔌 插件机制与高级特性 | [插件机制与高级特性.md](插件机制与高级特性.md) | 四大拦截点+责任链、PageHelper 原理与 ThreadLocal 泄漏、延迟加载、SqlSessionTemplate、MyBatis-Plus |

## P7 必背清单（速查）

- **工作原理一句话**：启动时解析配置，把每条 SQL 包装成 `MappedStatement` 存入 `Configuration`；运行时 `SqlSession → Executor → StatementHandler → JDBC`，缓存先于 DB 判断
- **Mapper 接口没有实现类**：JDK 动态代理 `MapperProxy`；**接口全限定名 = namespace，方法名 = id**；方法解析结果缓存在 `ConcurrentHashMap`
- **接口方法不能重载**：一个 namespace 下 id 唯一，同名方法找不到唯一 SQL
- **Executor**：SIMPLE（默认）/REUSE（复用 Statement）/BATCH（攒批 `executeBatch`，万级批量首选，不回传自增主键）
- **一级缓存**：SqlSession 级 HashMap（PerpetualCache），默认开；增删改/commit/close/不同 statement 失效；**Spring 非事务查询间不共享，同事务内共享**
- **二级缓存**：namespace 级，默认关（`cacheEnabled` 总开关 + `<cache/>` 分开关）；事务提交才写入；**多表 join 脏读 + 分布式不同步两大坑** → 生产关闭，换 Redis/Caffeine
- **`#{}` vs `${}`**：值一律 `#{}`（预编译防注入）；表名/列名/排序只能 `${}` 且白名单；like 用 `CONCAT('%',#{kw},'%')`
- **`<where>/<set>` 本质是 `<trim>`**；OGNL 判空数字 0 的坑（`!= ''` 会误杀 0）
- **批量插入**：几百行 foreach 分批（500~1000），万级 `ExecutorType.BATCH`
- **N+1 问题**：嵌套查询（`select=`）导致 1+N 次 SQL；解法=嵌套结果 join / 延迟加载 / IN 批量组装
- **插件 = 责任链 + JDK 动态代理**：四大拦截点 Executor/StatementHandler/ParameterHandler/ResultSetHandler；`pluginAll` 层层包装，`proceed()` 传递
- **PageHelper**：拦截 Executor 改写 count + LIMIT；分页参数走 ThreadLocal，**startPage 后必须紧跟查询**，否则污染线程池下一个任务
- **延迟加载**：关联对象是 CGLIB/Javassist 代理，首次访问才发 SQL；延迟≠消除
- **SqlSessionTemplate 线程安全**：本身无状态，方法级代理临时取/还 SqlSession；事务内绑定线程复用（一级缓存生效）
- **MyBatis-Plus**：通用 CRUD 以注入 MappedStatement 方式复用 MyBatis 架构；单表不写 SQL，复杂 SQL 回 XML

## 学习/复习建议

1. 先按 1→4 顺序建立体系：执行流程是纲，缓存/SQL/插件都是这条流水线上的展开。
2. 「Mapper 为什么不用写实现类」「`#{}` vs `${}`」「一级 vs 二级缓存」三大必考题要能白板画出流程。
3. 每篇「一句话总结」当作口述提纲，能复述即过关。
4. 「易错点」章节是细节陷阱：startPage 泄漏、二级缓存脏读、OGNL 数字 0，都是真实生产事故点。
5. 结合项目讲：用 PageHelper 排查过分页参数泄漏、用 BATCH 优化过批量导入，都是加分案例。

## 相关笔记

- SQL 层原理（索引、事务、锁）→ [../mysql/README.md](../mysql/README.md)
- 慢 SQL 排查与优化 → [../mysql/8.%20SQL优化与调优.md](../mysql/8.%20SQL优化与调优.md)
- 应用层缓存选型（穿透/击穿/雪崩）→ [../redis/缓存问题与实战.md](../redis/缓存问题与实战.md)
- ThreadLocal 原理（PageHelper 泄漏的底层）→ [../java/ThreadLocal源码分析.md](../java/ThreadLocal源码分析.md)
