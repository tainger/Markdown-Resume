# Java 后端高频面经速查

> 本文是面试**前一晚速背版**，不是详细讲解：只列**面试场上最常问的 30+ 道题**、每题的**关键词骨架**（答对这些就能拿 70% 分）、面试官的**常见追问方向**。每道题的完整解析，点击链接跳转站内八股文。

---

## 一、Java 基础 & 集合（几乎每家一面必问）

### ⭐ T1. HashMap 底层原理？JDK1.7 vs 1.8 区别？为什么引入红黑树？

**回答骨架（按顺序说，加分）**：
1. **整体结构**：数组 + 链表 + 红黑树；key 无序，允许 null 键/值各一个
2. **核心参数**：默认初始容量 `16`，负载因子 `0.75`，扩容 `2×`（保证 rehash 只在原位置 or 原位置+oldCap 二选一）
3. **1.7 → 1.8 的三大变化**：
   - 链表插入：头插法 → **尾插法**（解决并发扩容时的环形链表死循环）
   - 结构优化：当链表长度 ≥ **8** 且数组长度 ≥ **64** 时转红黑树；长度回落 ≤ **6** 时退化为链表
   - rehash 算法：直接 `& (n-1)` 计算桶位（等价于 `% n` 但更快）
4. **为什么要树化 + 为什么阈值是 8**：
   - 树化原因：纯链表哈希冲突严重时查询退化为 $O(n)$，红黑树保证 $O(\log n)$
   - 阈值 8：基于**泊松分布**统计，哈希均匀时链表长度达到 8 的概率 ≈ $0.000006\%$，概率极低才转（树节点内存是普通节点的 2 倍，维护成本高）
5. **线程安全问题**：非线程安全 → 替代方案 `ConcurrentHashMap`（推荐）/ `Collections.synchronizedMap`

**常见追问链**：
- 桶位是怎么算的？hash(key) 做了什么扰动？为什么不直接用 hashCode？
- 自定义对象当 key，要注意什么？（`hashCode` + `equals` 必须同时重写，HashMap 先比 hash 再比 equals）
- 扩容 rehash 过程？1.8 迁移时为什么不用重新算 hash？（高低位判断：最高位=0 原位置，=1 原位置+oldCap）
- 两个对象 hashCode 相同 equals 不同，和 equals 相同 hashCode 不同，分别会发生什么？

**详情页**：[Java / HashMap 的应用场景和源码分析](../java/HashMap的应用场景和源码分析.md)

---

### ⭐ T2. ConcurrentHashMap 如何保证线程安全？1.7 vs 1.8？

**回答骨架**：
1. **1.7 分段锁**：数组切 16 段 `Segment`（继承 `ReentrantLock`），每段独立加锁；优点是分段并发，缺点是锁粒度粗 + 分段数固定
2. **1.8 CAS + synchronized 桶级锁**：取消分段锁，锁粒度降到单个桶
   - 桶位为空 → **CAS** 直接插入，无锁
   - 桶位已有节点 → **synchronized** 锁住桶头（对象头 Mark Word，性能接近优化后的 biased lock）
   - **多线程协助扩容**：`transfer` 方法分段迁移（每个线程认领一段桶位），`transferIndex` 游标自减分配
3. **计数怎么线程安全**：用 **LongAdder 思想**（`baseCount` + `CounterCell[]` 分段计数，高并发下各线程写不同 CounterCell，汇总时累加），比 AtomicInteger 吞吐高
4. **size() 只是近似值**：并发场景不精确（因为写 size 的同时别的线程可能在增删）

**常见追问链**：
- `sizeCtl` 几个值分别代表什么？（-1 正在扩容 / -N N-1 个线程在扩容协助 / 正数 下一次扩容阈值）
- 1.8 中对 treeNode 的锁也是 synchronized 吗？
- 为什么 JDK1.8 用 synchronized 放弃 ReentrantLock？（synchronized 经过锁升级优化，无竞争/偏向锁场景比 ReentrantLock 更轻；而且锁粒度降到单个桶后，Lock 的可中断/超时等特性用不上）
- putVal 全流程能讲一下吗？（初始化→空桶CAS→冲突锁桶头→链表尾插/树插→检查树化阈值→检查扩容）

**详情页**：[Java / ConcurrentHashMap](../java/ConcurrentHashMap应用场景和源码分析.md)

---

### ⭐ T3. ArrayList vs LinkedList？扩容机制？

| 维度 | ArrayList | LinkedList |
|:---|:---|:---|
| 底层 | `Object[]` 数组 | 双向链表 `Node<E> {prev, item, next}` |
| 随机访问 | $O(1)$ ✅ | $O(n)$ ❌ |
| 头/尾增删 | 尾 $O(1)$ 均摊；头 $O(n)$（搬移） | 头尾 $O(1)$ ✅ |
| 中间增删 | $O(n)$（搬移） | $O(n)$（先定位到位置，但定位完改指针 O(1)） |
| 扩容 | 默认初容 10，扩容 **1.5 倍**（`old + old>>1`），`Arrays.copyOf` 拷贝 | 无需扩容 |
| 缓存局部性 | 连续内存，CPU cache 命中好 | 节点分散内存，遍历性能常常不如 ArrayList |
| 场景 | 查询多、增删少（绝大多数业务场景） | 频繁头尾操作 / 当队列或栈用 |

**常见追问**：
- ArrayList 已知数据量时应该做什么？（`new ArrayList<>(capacity)` 指定初容，避免多次扩容拷贝）
- 为什么工程中 90% 情况都用 ArrayList？（现代 CPU 缓存行友好 + 内存紧凑 + 随机访问快，中间增删 LinkedList 实际也不快）

---

## 二、并发 & 锁（大厂一二面重灾区）

### ⭐ T4. `synchronized` vs `ReentrantLock`？锁升级过程？

**对比表（先给结论，再讲原理）**：

| 维度 | `synchronized`（关键字） | `ReentrantLock`（API，JUC 下） |
|:---|:---|:---|
| 实现 | JVM 层（对象头 Mark Word + Monitor） | AQS 队列 + CAS（Java 层实现） |
| 释放锁 | **自动**（出代码块 / 抛异常） | **手动 `finally { unlock(); }`，否则死锁** |
| 可中断 | 不可以 | `lockInterruptibly()` 可以 |
| 超时获取 | 不支持 | `tryLock(time, unit)` 支持 |
| 公平性 | 非公平，不可选 | 可选：`new ReentrantLock(true)` |
| 条件变量 | 1 个（`wait/notify/notifyAll`） | 多个 `Condition`，分组唤醒 |
| 读锁状态 | 不可查 | `isLocked() / getHoldCount() / hasQueuedThreads()` |
| 性能 | JDK6+ 锁升级优化后，绝大多数场景不差 | 大量竞争下略优，但 API 复杂、易漏 unlock |

**锁升级过程（JDK6+ 引入，方向不可逆）**：
```
无锁 ──(单线程反复进入)──► 偏向锁 ──(第二个线程来竞争)──► 轻量级锁(自旋CAS)
                                                              │
                                                      (自旋10次失败/多线程持续竞争)
                                                              ▼
                                                          重量级锁(Monitor)
```
- **偏向锁**：Mark Word 存线程 ID，下次同线程进入只需 CAS 换 ID，几乎零成本；**JDK15 后默认关闭**（多核下撤销成本高）
- **轻量级锁**：线程栈帧创建 Lock Record，CAS 把对象头指向它；**自适应自旋**（10 次，根据历史成功率调整），避免用户态/内核态切换
- **重量级锁**：Monitor 的 `_EntryList` + `_WaitSet`，阻塞由操作系统调度，开销最大

**常见追问**：
- 什么是锁消除、锁粗化？（锁消除：逃逸分析发现锁对象无共享则消除；锁粗化：连续加解锁合并成一次大范围）
- 为什么锁升级不可逆？（降级的开销远大于收益，状态维护复杂）
- ReentrantLock 公平模式为什么吞吐量低？（每次加锁前要检查 `hasQueuedPredecessors()`，多一次判断 + 防止"插队"会多一次唤醒开销）

**详情页**：[Java / JUC 并发包](../java/JUC并发包.md)

---

### ⭐ T5. volatile 的作用？为什么只能保证可见性和有序性？

**回答骨架**：
1. **两大作用**：
   - **可见性**：写 volatile 变量时，JVM 插入 `StoreStore + StoreLoad` 内存屏障 → 强制把工作内存写回主内存；读时插入 `LoadLoad + LoadStore` 屏障 → 强制从主内存读
   - **有序性（禁止指令重排序）**：volatile 前后的指令不能跨过它重排（如 DCL 单例中，防止 `new Singleton()` 的「分配→初始化→引用赋值」三步被重排成「分配→引用赋值→初始化」导致其他线程拿到半初始化对象）
2. **为什么不能保证原子性**：典型例子 `volatile int count; count++`，count++ 是**读-改-写**三步操作（`load i` → `inc` → `store i`），每一步之间其他线程可以插入，volatile 只保证每一步的可见性，不保证三步的原子性

**常见追问**：
- DCL（双重检查锁）单例中 volatile 的必要性？（答上面的半初始化对象问题）
- happens-before 规则里和 volatile 相关的是哪条？（volatile 写 happens-before 后续对它的读）
- 内存屏障有几种？CPU 层面怎么实现？（lfence/sfence/mfence x86 指令）

**详情页**：[JVM / JMM 内存模型](../jvm/JMM内存模型.md)

---

### ⭐ T6. 线程池 7 大参数？工作流程？为什么阿里规范不建议用 Executors？

**七大参数（按重要度排序）**：

| 参数 | 含义 | 面试考点 |
|:---|:---|:---|
| `corePoolSize` | 核心线程数（长期存活，即使空闲也不回收） | 怎么配？CPU 密集型：N±1；IO 密集型：2N ~ N/(1-阻塞系数） |
| `maximumPoolSize` | 最大线程数（含核心 + 非核心） | 与 core 相等 = 固定大小；core=1 max=∞ = CachedThreadPool（OOM 风险） |
| `keepAliveTime + unit` | 非核心线程空闲多久回收 | core 想回收：`allowCoreThreadTimeOut(true)` |
| `workQueue` | 任务等待队列 | 有界 `ArrayBlockingQueue` / 无界 `LinkedBlockingQueue`（OOM 风险）/ 同步移交 `SynchronousQueue` |
| `threadFactory` | 创建线程的工厂 | 自定义线程名前缀，方便排查"这线程是谁创建的" |
| `RejectedExecutionHandler` | 队列满 + 线程数达 max 时的拒绝策略 | AbortPolicy（抛异常）/ CallerRunsPolicy（调用者同步执行）/ DiscardPolicy（丢任务）/ DiscardOldestPolicy（丢队头最老再试） |

**工作流程（必考）**：
```
新任务进来
  │
  ├─► 当前线程数 < corePoolSize？ ──是──► 新建核心线程执行任务
  │                                    │
  │                                    否
  │                                    ▼
  │                              workQueue 未满？ ──是──► 入队等待
  │                                    │
  │                                    否
  │                                    ▼
  │                              当前线程数 < maxPoolSize？ ──是──► 新建非核心线程执行
  │                                                                    │
  │                                                                    否
  │                                                                    ▼
  │                                                              执行拒绝策略
```

**为什么阿里巴巴开发手册禁止用 `Executors` 工厂方法？**
- `newFixedThreadPool` / `newSingleThreadExecutor` → 队列是**无界 `LinkedBlockingQueue`**（容量 `Integer.MAX_VALUE`），积压任务可能 OOM
- `newCachedThreadPool` → maxPoolSize 是 `Integer.MAX_VALUE`，短时间大请求会创建爆炸数量线程，可能 OOM
- `newScheduledThreadPool` → DelayedWorkQueue 也是无界，同上
- **正确姿势**：自己 new `ThreadPoolExecutor`，明确指定队列类型、大小、线程名、拒绝策略

**常见追问**：
- 线上任务堆积了怎么排查？（jstack 看线程栈、top -Hp 找 CPU 高的线程、看队列 size / activeCount / completedTaskCount、看日志链路）
- CPU 密集型为什么核数 ±1？（N 核全跑满时 ±1 为了容错超线程/页面调度等占用，经验值，不一定严格）

---

### ⭐ T7. ThreadLocal 原理？为什么会内存泄漏？

**回答骨架**：
1. **数据结构**：每个 `Thread` 对象有一个成员变量 `threadLocals: ThreadLocalMap`；`ThreadLocalMap` 底层是 `Entry[]`，每个 `Entry extends WeakReference<ThreadLocal<?>>`（**key 是弱引用**，指向 ThreadLocal 对象本身），**value 是强引用**，指向用户存的值
2. **set 流程**：计算 `threadLocalHashCode & (len-1)` → 定位桶位 → 线性探测法解决冲突（不是拉链法）
3. **为什么内存泄漏**：
   - 场景：ThreadLocal 外部强引用没了（比如方法结束栈帧弹出），**key = 弱引用** → GC 时 key 被回收 → Entry 变成 `null → value` 的僵尸 Entry
   - 但 **value 是强引用**，Thread 不死（比如线程池里的线程长期存在），Thread → ThreadLocalMap → Entry[] → Entry → value 这条引用链一直有效 → value 永远不会被回收 → 内存泄漏
4. **怎么防泄漏**：**用完必须 `threadLocal.remove()`！** （最佳实践：`try { tl.set(x); ... } finally { tl.remove(); }`，即使线程复用也清干净）
5. **为什么 key 设计成弱引用？** 如果 key 是强引用，即使 ThreadLocal 外部引用没了，key 也不会被回收，泄漏更严重；弱引用是一种"兜底"，至少 key 能被回收（但 value 仍泄漏，所以仍需 remove）

**常见追问**：
- `InheritableThreadLocal` 原理？（子线程创建时把父线程的 `inheritableThreadLocals` 复制过去）
- 父子线程传递还有什么方案？（TTL：TransmittableThreadLocal，配合线程池使用，解决线程复用场景下的传递）
- 用了线程池 + ThreadLocal 不 remove 会怎样？（线程复用时，下一次任务拿到上一次任务遗留的值，**业务脏数据** + 内存泄漏双杀）

**详情页**：[Java / ThreadLocal 源码分析](../java/ThreadLocal源码分析.md)

---

## 三、JVM（项目跑在服务器上就会问）

### ⭐ T8. JVM 内存分哪几块？

```
     JVM 运行时数据区
    ┌───────────────────────────────┐
    │       线程私有               │  随线程创建而生，线程结束而死
    │   ① 虚拟机栈（Java 方法栈帧）│   - 局部变量表、操作数栈、方法出口
    │   ② 本地方法栈（Native 方法）│
    │   ③ 程序计数器（下一条指令） │  唯一不会 OOM 的区
    └───────────────────────────────┘
    ┌───────────────────────────────┐
    │       线程共享               │  随 JVM 启动而生
    │   ④ 堆（Heap）              │   70%~80% 对象都在这；GC 主战场
    │   ⑤ 元空间（Metaspace）     │   JDK8 代替"方法区"；存在本地内存
    │       （类元信息、常量、JIT）│
    └───────────────────────────────┘
```

**常见追问**：
- 栈上分配 / 标量替换 / 逃逸分析是什么？（小对象如果不逃逸出方法，直接在栈上分配，方法结束自动释放，减轻 GC 压力）
- 元空间和永久代（PermGen）的区别？（PermGen 在 JVM 堆内，默认上限固定，容易 OOM；Metaspace 用本地内存，默认只受物理内存限制）

---

### ⭐ T9. 对象怎么判定已死？GC Roots 有哪些？

**判定方法**：不是"引用计数法"（循环引用问题），而是 **可达性分析算法**：从 GC Roots 出发，沿着引用链向下搜，不可达的对象判定为"可回收"。

**GC Roots 四大类（速背）**：
1. **虚拟机栈（栈帧中局部变量表）** 中引用的对象（方法里的局部变量）
2. **本地方法栈 JNI** 中引用的对象
3. **方法区 / 元空间**：类静态属性引用的对象（`static` 变量）、常量引用的对象（字符串常量池）
4. **被同步锁（`synchronized`）持有的对象**

**不可达后立刻回收吗？** ——不，还有一次"缓刑"：对象覆盖了 `finalize()` 方法且没被调用过 → 放入 `F-Queue`，由低优先级 Finalizer 线程执行它 → 如果在 finalize 中把自己重新挂到引用链上（比如 `this = 某个静态变量`）→ 自救成功不回收；但 finalize 只能被调用一次，下次再不可达就直接回收。

**⚠️ 面试上千万别推荐用 finalize，必说它不确定、有坑、已废弃**。

---

### ⭐ T10. CMS vs G1 区别？什么场景选 G1？

| 维度 | CMS（Concurrent Mark Sweep） | G1（Garbage-First） |
|:---|:---|:---|
| 定位 | 老年代并发低延迟收集器（需配合年轻代 Serial/ParNew） | **全代收集器**（年轻+老年代统一管理，Java 9 起默认） |
| 算法 | 老年代：**标记-清除**（并发） | 整体：标记-整理；Region 内部：复制算法（无碎片） |
| 内存分区 | 物理上分年轻代 / 老年代 | 物理上不划分，切分 **~2048 个 Region**，每个 Region 逻辑上属于 Eden/Survivor/Old/Humongous 之一 |
| 暂停 | 初始标记 + 重新标记 两次 STW；并发标记/清除与应用线程并发 | 初始标记 + 最终标记 + 筛选回收 三次 STW；**Mixed GC 可控停顿**：可指定 `MaxGCPauseMillis`（默认 200ms） |
| 碎片 | 标记-清除有内存碎片 → 并发模式失败后回退 Serial Old 单线程 Full GC | Region 复制算法，**无碎片问题** |
| 大对象处理 | 直接进老年代，可能引发提前 Full GC | Humongous Region 专门放 > Region 50% 的对象，可被 young GC 回收 |
| 适用场景 | 老年代对象存活久、追求低停顿、堆 ≤ 8G（老年代场景） | **大堆 8G+**、需要可控停顿时间、现代主流 JDK 直接默认选 G1 |

**常见追问**：
- 增量更新（CMS）vs 原始快照 SATB（G1）？（解决并发标记时引用变动导致的漏标问题：增量更新记录"新引用"，SATB 记录"引用消失时原引用的快照"，减少重新标记时间）
- 什么时候 ZGC / Shenandoah？（JDK12+，亚毫秒级 STW，TB 级大堆，对延迟极致要求的场景，2026 面试加分项）

**详情页**：[JVM / 垃圾回收器](../jvm/垃圾收集器.md)

---

### T11. 线上 OOM / CPU 100% 排查流程？（必背命令链）

**场景 A：CPU 100%**（回答按顺序，体现你真动手排过）
```bash
# ① top 找 CPU 最高的 Java 进程 PID
top                         # 假设 PID=12345
# ② 看该进程下哪个线程 CPU 高
top -Hp 12345               # 假设找到线程 TID=9876
# ③ 十进制 TID 转十六进制
printf '%x\n' 9876          # 输出 2694
# ④ jstack 看这个线程在跑什么
jstack 12345 | grep -A 50 2694    # 看栈顶，基本一眼定位死循环/频繁 GC 等
```

**场景 B：OOM / 内存泄漏**
```bash
# ① 加启动参数让 OOM 时自动 dump（最推荐）
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof

# ② 临时手动 dump（生产慎用，可能 STW）
jmap -dump:format=b,file=/tmp/dump.hprof 12345

# ③ 离线分析：MAT / JVisualVM / JProfiler
#    - 看哪个对象实例最多、谁引用着它、GC Roots 链 → 找到泄漏源头
#    - 典型：HashMap 没清 / ThreadLocal 没 remove / 大对象缓存没淘汰

# ④ 辅助命令：看堆概况
jmap -heap 12345            # 各区使用情况
jstat -gcutil 12345 1000    # 每秒看一次 YGC/FGC 次数和耗时
```

**加分工具**：**Arthas（阿尔萨斯）** —— 阿里开源，线上无需重启：`dashboard` 看总览，`thread -n 3` 找 CPU Top3 线程，`watch` 看方法入参返回值，`heapdump` 一键导出，比 jstack/jmap 顺手 10 倍。

---

## 四、MySQL（出场率最高的模块）

### ⭐ T12. MySQL 索引为什么用 B+ 树？不用 B 树 / 哈希 / 红黑树？

**回答骨架（先讲 B+ 树再对比其他）**：
1. **B+ 树是什么**：多叉平衡树；**非叶节点只存索引键 + 指针，不存数据**，单个节点（默认 16KB）能装几千个键 → 树高只有 3~4 层 → 磁盘 IO 次数极少
2. **B+ 树对比其他结构的优势**：

| 对比对象 | 劣势 / 为什么不用 | B+ 树优势 |
|:---|:---|:---|
| 二叉搜索树 / 红黑树 | 2 叉，树高 lgN，树高太高 → 磁盘 IO 多 | 多叉（1000+叉），树高 3~4 层 |
| B 树（B-树） | 非叶节点也存 data，每个节点能装的键少 → 树更高 + 范围查询要中序回溯 | 非叶节点只存键 → 更矮；**叶子节点有双向链表串起来**，范围查询直接顺链表扫 |
| 哈希索引 | 只支持等值查询（= / IN），**不支持范围、排序、最左前缀** | 支持以上所有，B+ 树有序天然适合 ORDER BY / GROUP BY |
| 跳表 | Redis ZSet 用它，内存结构；磁盘上没优势，索引页缓存命中率低 | 每个节点存于磁盘页，操作系统 PageCache 友好 |

3. **聚簇索引 vs 非聚簇索引（二级索引）**：
   - 聚簇索引（InnoDB 主键默认）：B+ 树叶子节点 = 完整行数据 → 一张表只有一个聚簇索引
   - 二级索引：叶子节点 = `主键值` → 查询到主键后需再回表查聚簇索引（**回表**）；如果查询字段都在索引树上（覆盖索引）就不用回表

**常见追问**：
- 什么是覆盖索引 / 联合索引最左前缀原则？（答 T13）
- 二级索引 + ORDER BY 的时候怎么利用索引避免文件排序？（WHERE 走最左前缀，ORDER BY 跟上索引列的顺序且方向一致）

---

### ⭐ T13. 联合索引最左前缀 + 索引失效场景？

**联合索引 (a, b, c) 生效规则速查表**：

| WHERE 条件 | 走不走索引？哪部分？ | 说明 |
|:---|:---|:---|
| `WHERE a = 1` | ✅ a 部分 | 满足最左前缀 |
| `WHERE a = 1 AND b = 2` | ✅ a+b 部分 | |
| `WHERE a = 1 AND b = 2 AND c = 3` | ✅ 全列 | |
| `WHERE b = 2 AND c = 3` | ❌ 全表扫 | 跳过了 a，最左前缀不满足 |
| `WHERE a = 1 AND c = 3` | ✅ **只有 a 部分**，c 会在过滤后再判断（索引下推：5.6+ 会在索引层直接判 c 减少回表） | |
| `WHERE a > 1 AND b = 2` | ✅ **只有 a 部分**，范围列后面的列失效（b 不生效） | 关键！范围列后全失效 |
| `WHERE a = 1 ORDER BY b` | ✅ 索引上 b 有序，不用 filesort | |
| `WHERE a = 1 ORDER BY c` | ✅ a 走索引，但 ORDER BY c 要 filesort（跳过了 b） | |

**常见索引失效陷阱（面试问"什么情况索引失效"时按这个背）**：
1. ❌ **联合索引跳过最左列**（上面 b/c 单独用）
2. ❌ **联合索引中范围查询列之后的列失效**（`a>? AND b=2` 中 b 失效）
3. ❌ **对索引列用函数 / 表达式运算**（`WHERE YEAR(create_time) = 2026`，改写成范围：`create_time BETWEEN '2026-01-01' AND '2026-12-31'`）
4. ❌ **隐式类型转换**（列是 VARCHAR，传了 INT，MySQL 会对列做 CAST，等于函数）
5. ❌ **`%` 开头的 LIKE 模糊匹配**（`%xxx` 不行，`xxx%` 还可以走索引）
6. ❌ **`!= / <> / NOT IN / IS NOT NULL`**（多数情况优化器觉得回表不如全表扫；`IN / IS NULL` 则可以走）
7. ❌ **`OR` 两边列其中一边没索引**（`WHERE a = 1 OR unindexed_col = 2` → 全表；`OR` 两边都有索引才可能走 index merge）
8. ❌ **数据太少 / 区分度太低**（性别只有男/女两值，优化器认为走索引回表不如全表扫快；但性别 + 高区分度列组成联合索引仍有用）

---

### ⭐ T14. 事务 ACID？MVCC 怎么实现可重复读 + 防止幻读？

**ACID 四大特性**：原子性(undo log) · 一致性(最终目标) · 隔离性(锁+MVCC) · 持久性(redo log)

**四种隔离级别 + 三大问题**（必会填表格）：

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | MySQL 默认 |
|:---|:---:|:---:|:---:|:---:|
| 读未提交 Read Uncommitted | ✅ 有 | ✅ 有 | ✅ 有 | ❌ |
| 读已提交 Read Committed (RC) | ❌ 解决 | ✅ 有 | ✅ 有 | Oracle/PostgreSQL 默认 |
| **可重复读 Repeatable Read (RR)** | ❌ 解决 | ❌ 解决 | **基本解决**（间隙锁+Next-Key Lock） | **✅ InnoDB 默认** |
| 串行化 Serializable | ❌ 解决 | ❌ 解决 | ❌ 解决（全表锁） | ❌ 性能极差 |

**MVCC（多版本并发控制）怎么实现可重复读？**
1. **三大件**：每行隐藏列（`DB_TRX_ID 事务ID` + `DB_ROLL_PTR 回滚指针`）+ **undo log 版本链** + **ReadView 读视图**
2. **版本链**：每次 UPDATE，旧版本不删除，通过 undo log 指针串成链表（最新版本在链头，越老越在链尾）
3. **ReadView 生成时机**：
   - **RC**：**每次 SELECT** 都生成新的 ReadView → 能看到其他事务刚提交的修改 → 两次 SELECT 同一行可能值不同 → 不可重复读
   - **RR**：**事务内第一个 SELECT** 时生成，**后续复用同一个** → 其他事务后续提交的修改对它不可见 → 两次 SELECT 同一行值永远一致 → 可重复读 ✅
4. **可见性判断**：沿着版本链向前找，找到第一个「`trx_id` 在 ReadView 里可见」的版本返回

**RR 怎么防幻读？**（面试必考！）
- **快照读（普通 SELECT）**：靠 MVCC + 固定 ReadView，第一次 SELECT 看到什么后面永远看到什么 → 快照层面无幻读
- **当前读（SELECT ... FOR UPDATE / UPDATE / DELETE / INSERT）**：靠 **Next-Key Lock = 间隙锁(Gap) + 记录锁(Record)**，锁住条件范围内的**间隙 + 记录**，不允许其他事务在这个间隙 INSERT 新行 → 物理层面防幻读

**常见追问**：
- undo / redo / binlog 三者区别？（undo=回滚+MVCC版本链、redo=崩溃恢复保证持久性、binlog=逻辑归档用于主从复制/恢复）
- 什么场景会发生死锁？怎么排查？（互斥+占有并等待+不可抢占+循环等待；`SHOW ENGINE INNODB STATUS` 看 LATEST DETECTED DEADLOCK）

**详情页**：[MySQL / 事务与锁](../mysql/5.%20事务与锁.md)、[MySQL / 索引](../mysql/4.%20索引.md)

---

## 五、Redis（中大厂二面高频）

### ⭐ T15. 缓存穿透 / 击穿 / 雪崩？一张表搞懂 + 解决方案

| 问题 | 现象 | 原因 | 解决手段 |
|:---|:---|:---|:---|
| **穿透** | 请求根本不存在的 key（如 id=-1）→ 缓存 miss → DB 查询空 → 下一次还是 miss → DB 压力大 | 恶意攻击 / 参数校验不严 | ① 空值缓存（TTL 短，5~10 分钟，避免 DB 真插入时缓存脏）② **布隆过滤器**（在缓存前加一层，hash 判断 key 大概率存在才放行，有误判但不漏判）③ 入口参数校验 |
| **击穿** | **一个**热点 key 过期瞬间 → 同时 10 万请求打到 DB | 热点数据 + 刚好过期 | ① **互斥锁 / 分布式锁**（`SETNX`，只有拿到锁的线程去查 DB + 回写，其余自旋等待）② **热点 key 永不过期**（值里带逻辑过期时间，后台线程异步刷新）③ **本地缓存兜底**（Caffeine 本地缓存先顶一下 Redis miss 的请求） |
| **雪崩** | **大量** key **同时过期** OR Redis 实例挂了 → DB 被打垮 | 同时到期 / Redis 宕机 | ① TTL 加**随机抖动**（`expire = 基础TTL + random(0~300s)`，打散过期时间）② **多级缓存**（本地 Caffeine + Redis + DB）③ Redis **高可用集群**（Sentinel / Cluster 主从 + 自动故障转移）④ **限流 + 降级**（Hystrix / Sentinel 熔断，返回默认值/兜底数据） |

**互斥锁击穿兜底伪代码**：
```java
public String getData(String key) {
    String val = redis.get(key);
    if (val != null) return val;                 // 命中直接返回
    String lockKey = "lock:" + key;
    String holder = UUID.randomUUID().toString();
    try {
        // 只有一个线程拿到锁去查 DB
        if (redis.set(lockKey, holder, "NX", "EX", 10)) {
            val = db.select(key);
            if (val != null) {
                redis.set(key, val, "EX", 300 + ThreadLocalRandom.current().nextInt(60)); // 加抖动
            } else {
                redis.set(key, "", "EX", 5);   // 空值缓存防穿透
            }
            return val;
        } else {
            Thread.sleep(50);                   // 自旋等待（别死循环，设置最大次数）
            return getData(key);
        }
    } finally {
        // ★ 释放锁必须判断是不是自己加的（防止误删别人的锁）：Lua 脚本保证原子性
        if (holder.equals(redis.get(lockKey))) redis.del(lockKey);
    }
}
```

**详情页**：[Redis / 缓存问题与实战](../redis/缓存问题与实战.md)、[分布式 / 分布式锁](../分布式/分布式锁.md)

---

### ⭐ T16. Redis 分布式锁怎么实现？Redisson 看门狗？

**手写一个「正确」的分布式锁满足五点缺一不可**：
1. **互斥**：`SET key value NX`（不存在才插入，原子性）
2. **防死锁**：**必须同时设置过期时间**（`SET key value NX EX seconds`，SET NX 和 EX 是同一原子命令，不能先 set 再 expire）
3. **只能自己解锁**：value 存唯一标识（UUID/线程ID），解锁时判断 value 相等才删
4. **解锁原子性**：上面判断相等 + delete 两步用 **Lua 脚本**包成原子（否则判断完到 delete 之间锁刚好过期+别人抢了锁，你把别人的锁删了）
5. **业务超时续约**：业务执行时间 > 过期时间 → 锁过期 → 并发安全失守 → Redisson 看门狗兜底

**Redisson 看门狗机制（面试加分项）**：
- 加锁时如果没显式指定 leaseTime → 默认 30 秒过期 + 启动「看门狗」守护线程
- 每隔 **lockTime/3 = 10 秒** 续期：如果线程还持有锁（判断 map 里的 entry），就把过期时间重置回 30 秒
- 这样即使业务执行 2 分钟，锁也不会中途失效；线程挂了看门狗也跟着挂，不会无限续期

**和 ZooKeeper 分布式锁选型对比**：

| 维度 | Redis 分布式锁 | ZooKeeper 分布式锁 |
|:---|:---|:---|
| 实现 | NX + 过期时间 + Lua | 临时有序节点 + Watcher |
| 可靠性 | 需要看门狗 + 主从切换可能丢锁（RedLock 解决但争议大） | ZK 一致性强（ZAB 协议），不会丢锁 |
| 性能 | 极高，QPS 可达 10w+ | 中等，几千 QPS 量级 |
| 场景 | **绝大多数业务场景推荐 Redisson**（够用 + 快） | 对一致性要求严苛、宁可慢点也不能错的金融/账务场景 |

---

## 六、MQ & 分布式事务

### ⭐ T17. MQ 如何保证消息不丢失？不重复消费？有序性？

**三大可靠性问题，从「三段式」分别回答**：生产端 → Broker 存储 → 消费端。

| 可靠性问题 | 生产端（Producer） | Broker（MQ 本身） | 消费端（Consumer） |
|:---|:---|:---|:---|
| **不丢失** | **confirm 机制**：发消息后 Broker 确认 ACK，未 ACK 重发 | ① 持久化 `durable=true` ② 同步刷盘 `flushDiskType=SYNC_FLUSH` ③ 主从同步完成后才 ACK | **手动 ACK**（consumer 处理完业务逻辑再 `ack()`，而不是收到就 ack）；处理失败 `nack()` 重入队 |
| **不重复（幂等）** | 生产端 confirm 重试可能重复发（网络抖动 ACK 丢了）→ 消息带唯一业务 ID | Broker 侧一般不做，交给上下游 | **消费端幂等**：① 数据库唯一键（去重表）② Redis `SETNX` 或 BloomFilter 判重 ③ 乐观锁版本号 ④ 状态机判断（比如订单只能从「待支付」→「已支付」，重复消息过来状态不对就丢弃） |
| **有序性** | 同一业务 key 的消息 **按顺序发送到同一队列**（RocketMQ 按 hashKey 选 MessageQueue） | **单队列单线程消费**（多分区会乱序，所以需要业务上 hash 到同一分区） | **同一个有序分区只开 1 个消费线程**（并发消费就破坏顺序了）；局部有序而非全局有序（全局有序性能太差） |

**常见追问**：
- RocketMQ vs Kafka 选型？（RocketMQ：金融级可靠、事务消息、重试/死信、延时等级，适合业务；Kafka：吞吐量之王、拉模型、流式处理生态好，适合日志/大数据）
- RocketMQ 事务消息原理？（半消息 + commit/rollback + 回查 → 保证本地事务和消息发送要么都成功要么都失败 → 分布式事务最终一致性方案之一）

**详情页**：[RocketMQ / 可靠性与高可用](../rocketMq/可靠性与高可用.md)

---

### T18. 分布式事务方案对比？（送分题，按选型说）

| 方案 | 原理 | 一致性 | 侵入性 | 适用场景 |
|:---|:---|:---|:---|:---|
| **2PC / XA** | 两阶段提交：Prepare → Commit；TM 协调 + RM 参与者 | 强一致（CP） | 高（数据库需支持 XA 协议） | 单请求同步场景；小事务；跨库少；牺牲可用性 |
| **TCC** | Try-Confirm-Cancel：业务代码层面实现"预留→确认/回滚"三个接口 | 最终一致（AP） | **最高**（每个服务写 3 个接口） | 金融、强业务约束、有明确"预留资源"概念（如冻结余额） |
| **本地消息表 + MQ** | 本地事务同时写"业务数据 + 待发送消息表"→ 后台任务定时轮询消息表发 MQ → 消费端幂等消费 | 最终一致（AP） | 中 | **90% 分布式事务场景推荐此方案**，简单可靠，业务侵入中等 |
| **RocketMQ 事务消息** | 半消息 + 本地事务执行 + commit/rollback + 15 次回查 | 最终一致（AP） | 低（RocketMQ 封装好） | 已用 RocketMQ 的项目首选，省去消息表轮询 |
| **Seata（AT 模式）** | 自动生成 undo_log（前后镜像），TM 协调，回滚时自动生成反向 SQL | 最终一致（AP） | **极低**（加注解 @GlobalTransactional 即可） | 非金融、大多数业务场景首选，无侵入 |
| **最大努力通知** | 定时主动通知（3 次 / 5 次 / 10 次间隔递增）+ 接收方主动查询 | 最终一致（AP） | 低 | 支付结果通知、回调类场景，对一致性容忍度高 |

**选型经验**：强一致 → 2PC；金融强约束 → TCC；日常业务 → 本地消息表 / RocketMQ 事务消息 / Seata AT；低价值、丢几条无所谓 → 最大努力通知。

**详情页**：[分布式 / 分布式事务](../分布式/分布式事务.md)

---

## 七、计算机网络

### ⭐ T19. TCP 三次握手 / 四次挥手？为什么三次 / 四次？TIME_WAIT？

**三次握手（建立连接）**：
```
客户端                              服务端
  SYN=1, seq=x, ISN=x            ──►
  (SYN_SENT)                        (LISTEN收到SYN后变SYN_RCVD)
◄──  SYN=1, ACK=1, seq=y, ack=x+1
  (ESTABLISHED)
  ACK=1, seq=x+1, ack=y+1        ──►
                                    (ESTABLISHED)
```
**为什么三次，不是两次？**
1. **双方都要确认收发能力**：3 次握手 = 客户端确认「我发得出去+收得到」，服务端确认「我发得出去+收得到」；2 次握手的话服务端无法确认客户端收得到
2. **防止旧连接请求到达新连接**：客户端之前一个延迟的 SYN 包到了服务端，2 次握手服务端直接 ESTAB 就开连接，浪费资源；3 次握手客户端 ACK 时可以判出这是旧的 seq，回 RST 拒绝

**四次挥手（断开连接）**：
```
主动方                              被动方
  FIN=1, seq=u                    ──►
  (FIN_WAIT_1)                       (CLOSE_WAIT)
◄──  ACK=1, ack=u+1
  (FIN_WAIT_2)
                                ← 被动方自己的数据还没发完，等它传完
◄──  FIN=1, seq=w, ACK=1, ack=u+1
  (TIME_WAIT)                       (LAST_ACK)
  ACK=1, seq=u+1, ack=w+1        ──►
  (等待 2MSL 后 CLOSED)              (收到ACK后立刻CLOSED)
```
**为什么四次？** —— 因为**被动方收到 FIN 时，可能还有没传完的数据**，所以 ACK 和 FIN 分成两次发（如果被动方也没数据了，ACK 和 FIN 可以合并，变成三次挥手）。

**TIME_WAIT 为什么是 2MSL，谁有？**
- MSL = Maximum Segment Lifetime（报文段最大存活时间，RFC 建议 2 分钟，Linux 实际默认 60 秒）
- **2MSL 原因**：保证主动方最后一个 ACK 能被对方收到（如果 ACK 丢了，被动方会重发 FIN，TIME_WAIT 期间重发的 FIN 能被收到并重传 ACK + 重置 2MSL）；保证「本次连接所有老报文段都在网络中消亡」，下次新连接 seq 值不会和这次混淆
- **谁有**：**主动发起关闭**的一方才有 TIME_WAIT

**TIME_WAIT 过多怎么处理？**：`net.ipv4.tcp_tw_reuse`（允许用 TIME_WAIT socket 做新的 outbound 连接）、短连接改长连接、服务端不要主动关连接、`tcp_max_tw_buckets` 调大

---

### ⭐ T20. HTTP 1.0 / 1.1 / 2 / 3 演进对比？

| 维度 | HTTP/1.0 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|:---|:---|:---|:---|:---|
| 连接模型 | **短连接**，每次请求 TCP 建连+关 | **长连接**（默认 `Connection: keep-alive`）+ 管线化但有 HOL 阻塞 | **多路复用**（一个 TCP 连接并行 N 个请求，二进制分帧）+ 首部压缩 + 服务器推送 | **QUIC + UDP**（无 TCP 握手 + 0RTT / 1RTT）；多路复用无 HOL 阻塞（流独立）；连接迁移（ID 绑定，不绑 4 元组，切 WiFi 不中断） |
| 队头阻塞 | 无（每次一个请求） | 有：管线化要求请求按序返回，前一个堵后面全部堵 | 应用层无，但 **TCP 层仍有 HOL**（TCP 丢包会导致所有流等待重传） | 彻底解决 HOL：UDP 无重传阻塞，流独立 |
| 握手耗时 | TCP 3 次 + TLS 2RTT = 3RTT | 同左 + 长连接复用后 1RTT | 同左 | **0RTT / 1RTT**（QUIC 合并 TLS+传输握手） |
| 首部 | 文本，重复冗余大 | 同左 | **HPACK 静态表 + 霍夫曼 + 增量更新** | QPACK（HPACK 变体，支持乱序解码） |

**常见追问**：
- HTTPS 握手流程？TLS 1.3 优化了什么？（1.3 Hello 合并参数，支持 0-RTT，移除 RSA/DH 等不安全算法）
- HTTP 状态码 301 / 302 / 307 / 308 区别？（301 永久可改方法，302 临时可改方法；307/308 = 302/301 的"不允许改请求方法"版本）
- 500 / 502 / 503 / 504？（500 服务端代码异常；502 Bad Gateway 后端挂了；503 服务不可用 / 限流；504 Gateway Timeout 后端超时没响应）

**详情页**：[计算机网络 / TCP](../计算机网络/TCP.md)、[计算机网络 / HTTP](../计算机网络/HTTP.md)、[计算机网络 / HTTPS](../计算机网络/HTTPS.md)

---

## 八、Spring & 微服务

### T21. Spring 如何解决循环依赖？三级缓存？

**前提**：只解决**单例** Bean 的**构造器外循环依赖**（setter/字段注入）；构造器注入 / 多例 Bean / @Lazy 都不在自动解决范围内。

**三级缓存**：
```java
// DefaultSingletonBeanRegistry 中：
Map<String, Object> singletonObjects;         // 一级缓存：已完全初始化好的 Bean 成品
Map<String, Object> earlySingletonObjects;    // 二级缓存：早期引用（原对象，可能还没填属性），防止重复创建代理
Map<String, ObjectFactory<?>> singletonFactories; // 三级缓存：ObjectFactory 工厂（lambda），里面可以返回原始对象或 AOP 代理对象
```

**解决流程（A 依赖 B，B 依赖 A）**：
```
1. createBean(A)：实例化 A（调用构造函数，此时 A 只是"空壳"没注入属性）
2. ★ 把它的 ObjectFactory 放到三级缓存 singletonFactories：singletonFactories.put("a", () -> getEarlyBeanReference(beanName, bean))
   （如果需要 AOP，getEarlyBeanReference 会在这里生成代理对象；不需要则直接返回原始对象）
3. populateBean(A) 填属性 → 发现依赖 B → getBean(B) → 进入 createBean(B) 流程
4. createBean(B)：实例化 B → 放三级缓存 → populateBean(B) 填属性 → 发现依赖 A → getBean(A)
5. getBean(A) 进三级缓存，调用 singletonFactories.get("a").getObject() 拿到早期引用（原始或代理）
   → 移到二级缓存 earlySingletonObjects.put("a", 对象)；返回给 B 注入
6. B 初始化完成 → 移入一级缓存 → 返回给 A 注入
7. A 初始化完成 → 移入一级缓存
```

**为什么三级？不用二级行不行？**
- **如果没有 AOP，二级也够用**；但如果 Bean 有 AOP 代理，工厂层（三级）可以**懒生成代理**——没循环依赖时代理在 initializeBean 后的后置处理器里生成；只有循环依赖真的发生时才提前生成代理，避免重复创建。二级做不到这个"懒判断"。

**常见追问**：
- 构造器注入的循环依赖 Spring 为什么不解决？（因为构造 A 时需要 B，构造 B 又需要 A，Spring 还没把任何一个放缓存就卡住了，无解。解决方案：@Lazy 延迟注入 / 改成 setter 注入）
- @Lazy 原理？（注入代理对象，真正调用方法时才去容器拿 Bean）

**详情页**：[Spring / IOC 与 Bean 生命周期](../Spring/IOC与Bean生命周期.md)

---

### T22. @SpringBootApplication 注解做了什么？（自动配置原理）

**等价于 3 个注解合体**：
```java
@SpringBootConfiguration   // = @Configuration，标记这是一个配置类
@EnableAutoConfiguration   // ★ 自动配置的核心：开启自动配置
@ComponentScan(...)        // 扫描主类所在包及其子包下的 @Component/@Service 等
```

**@EnableAutoConfiguration 核心流程**：
1. 通过 `@Import(AutoConfigurationImportSelector.class)` 导入一个 ImportSelector
2. ImportSelector 的 `selectImports()` 读取 `spring-boot-autoconfigure.jar/META-INF/spring.factories`（Spring 2.7+ 改为 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件）里列的几百个 XxxAutoConfiguration 类名
3. **条件装配**（`@ConditionalOnMissingBean / @ConditionalOnClass / @ConditionalOnProperty` 等）筛选出生效的自动配置类
   - 例如引入了 `spring-boot-starter-data-redis`，类路径有 `RedisTemplate.class` → `RedisAutoConfiguration` 生效，自动帮你往容器注册 RedisTemplate Bean
   - 如果你自己写了 RedisTemplate Bean（`@ConditionalOnMissingBean`），就用你自定义的，不覆盖

---

## 九、系统设计 & 手撕代码（二三面重点）

### T23. 设计一个秒杀系统？（速答骨架，详细见系统设计目录）

**秒杀 = 高并发 + 流量削峰 + 防超卖**，标准七步法速查：

1. **需求**：1 个商品，100 件库存，100 万人同时抢；99.99% 可用性，接口 < 100ms
2. **容量**：QPS 峰值 10 万；带宽 100 万 × 5KB = 5GB/s，必须 CDN 扛静态资源
3. **API**：`POST /seckill/{goodsId}`（幂等 + 限流 + 鉴权）
4. **存储**：商品信息读多写少放 Redis；库存扣减走 Redis Lua 或 DB；订单表分库分表
5. **架构图**：
   ```
   用户 → CDN(静态页) → Nginx 限流 → 网关 → 业务服务 → Redis(库存扣减)
                                                │                 │
                                                ▼                 ▼
                                           MQ 异步下单 ───────► DB 写订单
   ```
6. **核心深挖**：
   - **限流**：前端动效按钮置灰 + 验证码；Nginx 按 IP 限流；服务层 Sentinel；Redis 令牌桶
   - **防超卖**：Redis Lua 脚本「判断库存 > 0 再减」原子操作；DB 乐观锁 `UPDATE stock SET num=num-1 WHERE goodsId=? AND num>0`
   - **防重**：用户 ID + goodsId 去重（Redis SET / 唯一索引）
   - **异步削峰**：抢成功直接返回「已下单排队中」，MQ 异步落库 + 异步回调
7. **容错**：Redis 集群；MQ 死信队列；库存兜底 DB 校验；监控报警 + 压测

### T24. 手撕 LRU Cache（代码题，字节等常考）

思路：HashMap + 双向链表。模板代码见 [算法思想 / 哈希表 - LRU 章节](../算法思想/哈希表.md)。

### T25. 手撕 TopK（N 个数里找最大的 K 个）

| 解法 | 时间 | 空间 | 适用 |
|:---|:---|:---|:---|
| 最小堆（最常用） | $O(N \log K)$ | $O(K)$ | 数据流 / 海量 N / 内存放不下 |
| 快排 partition | $O(N)$ 平均，最坏 $O(N^2)$ | $O(1)$ | 数据可全加载到内存 |
| 排序全取 | $O(N \log N)$ | $O(1)$ | K 接近 N 时直接排序更快 |

**堆写法**（Java 面试优先写 PriorityQueue 版本，3 行搞定）：
```java
public int[] topK(int[] nums, int k) {
    // 维护一个大小为 k 的最小堆，堆满后只保留比堆顶大的元素，最终堆里就是最大的 K 个
    PriorityQueue<Integer> heap = new PriorityQueue<>(k); // 默认小顶堆
    for (int x : nums) {
        if (heap.size() < k) heap.add(x);
        else if (x > heap.peek()) { heap.poll(); heap.add(x); }
    }
    return heap.stream().mapToInt(i->i).toArray();
}
```

---

## 十、设计模式 / 业务题（加分项）

### T26. 常用设计模式 + 项目里的实战例子？

| 模式 | 一句话 | 面试常考例子 |
|:---|:---|:---|
| 单例 | 全局唯一实例 | DCL volatile 版本；饿汉；枚举实现（最佳，防反射+序列化问题） |
| 工厂（简单/方法/抽象） | 把创建对象和使用分离 | Spring BeanFactory（最简单工厂抽象）；MyBatis SqlSessionFactory |
| 代理（静态 + JDK 动态 + CGLIB） | 控制对象访问 / 加前置后置逻辑 | Spring AOP；RPC 框架客户端桩；MyBatis Mapper 动态代理 |
| 策略 | 把算法族封装成可互换策略类 | Comparator 接口；支付方式选择；促销活动折扣算法 |
| 模板方法 | 定义算法骨架，子类覆写步骤 | AbstractList#get / addAll；RestTemplate；Mybatis BaseExecutor |
| 观察者 | 一对多订阅通知 | Spring ApplicationListener；消息队列 pub-sub；RxJava |
| 责任链 | 多处理器串成链依次处理 | Servlet Filter；Spring Interceptor；Mybatis Plugin 插件链 |
| 状态 | 状态切换抽成类，避免大 switch | 订单状态流转；Finite State Machine；[算法思想 / 状态机](../算法思想/状态机.md) |

### T27. 支付成功后，库存扣减 & 订单状态修改 怎么保证一致性？

**标准答案（面试场上说这个不会错）**：
- **方案 A：RocketMQ 事务消息**（最推荐，已落地 RocketMQ 的场景）
  - 半消息到 MQ → 执行本地事务（更新订单状态为已支付）→ commit → 消费端消费扣减库存
  - 消费端幂等：用 out_trade_no 做去重
- **方案 B：本地消息表 + MQ**（通用，不绑 RocketMQ）
  - 订单状态修改 + 插入待发送消息 = 同一本地事务
  - 定时任务捞未发送消息 → 发 MQ → 消费扣库存 → 更新消息状态为已发

### T28. 1000 万订单数据导出，避免 OOM？

- **流式查询**：MyBatis Cursor / JDBC `fetchSize = Integer.MIN_VALUE`（MySQL 驱动特有）/ `resultType=forward_only` → 一次从服务器拉一批，内存里只有当前一批
- **分段分页 + 游标书签**：按 `id > lastId LIMIT N`（深分页别用 OFFSET）
- **缓冲写入**：导出用 BufferedWriter / EasyExcel（SXSSFWorkbook 内存只留滑动窗口），边查边写磁盘
- **异步 + 分段文件**：任务跑在 Worker 里，生成多个文件上传 OSS，通知用户下载；不要同步接口直接返回文件流

---

## 十一、AI 工程化面试（2026 新增高频方向）

如果岗位/简历涉及 AI / 大模型，准备以下 5 道：

| # | 高频题 | 关键词骨架 |
|:---|:---|:---|
| A1 | RAG 检索增强生成的完整流程？ | 文档切分 → Embedding → 向量库（HNSW / IVF）→ 查询 Embedding → TopK 检索 → Prompt 拼装 → LLM 生成 → 返回 |
| A2 | LLM 幻觉怎么缓解？ | RAG 知识注入 + 思维链(CoT) + 输出校验（Self-Consistency 自洽多采样 + 知识库反查）+ 结构化输出约束（JSON Schema） |
| A3 | Agent 中 Tool/MCP 调用的流程？ | 用户 Query → LLM 判断是否调用工具 → 生成工具名+参数 → 执行工具 → 工具结果回灌上下文 → LLM 二次生成最终答案 |
| A4 | 高并发 LLM 推理怎么优化？ | 连续批处理（Continuous Batching）+ 前缀 KV Cache 复用 + 量化（GPTQ/AWQ 4bit/8bit）+ 模型并行（张量/流水）+ 推理服务 vLLM / TensorRT-LLM |
| A5 | Embedding 召回和传统倒排召回结合？ | **Hybrid Search**：向量召回（语义相似度）+ BM25 / 倒排关键词召回 + RRF（Reciprocal Rank Fusion）融合两者排名，再给 LLM 综合 |

---

## ⚠️ 易错点（现场答题时别踩）

1. **答不出不要停 20 秒沉默**：直接说「这块我不太确定，但我了解相关的 XXX 是这样的……」，把话题往你会的方向引
2. **每道题讲 1.5 分钟就停**，等面试官追问；不要一口气讲 5 分钟没人打断可能是面试官在忍耐
3. **"原理题"最后一定要补一句"工程上我们是怎么用/踩过什么坑"**，比纯背原理分高 30%
4. **五段式答题模型**：定义 → 原理 → 优缺点 → 选型 → 落地/坑，任何八股都可以按这个结构答
5. **算法题**：无论多熟，都要先问「输入范围、边界条件、复杂度要求」，直接开写被视为不严谨

---

## 一句话总结

**速背这些题的关键词骨架只能保你一面不挂；二面三面能不能过，取决于你能不能对每一道题讲出「原理 + 选型 trade-off + 项目里怎么用 + 踩过的坑」四段式深度回答。**
