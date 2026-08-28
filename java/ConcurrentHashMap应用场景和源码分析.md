# ConcurrentHashMap 的应用场景与源码分析

并发容器的头号面试题。JDK 7 与 JDK 8 的实现是两代完全不同的方案：**分段锁 → CAS + 桶级锁**。本篇从「为什么 HashMap 并发不安全」讲起，深挖 JDK 8 的 `putVal`/`transfer`/`size` 源码，补齐并发复合操作原子性等工程易错点。

> 概览版（一页速记）见 [JUC并发包.md](JUC并发包.md) 第九节；HashMap 单线程版见 [HashMap的应用场景和源码分析.md](HashMap的应用场景和源码分析.md)。

---

## 一、ConcurrentHashMap 是什么 / 解决什么问题

**定位**：线程安全的 HashMap，读几乎无锁、写锁粒度细到单个桶，是并发场景下的 Map 默认正解。

为什么不能用别的：

| 方案 | 锁粒度 | 并发读 | 并发写 | 问题 |
|:---|:---|:---:|:---:|:---|
| `HashMap` | 无锁 | 安全❓ | ❌ 丢数据 / JDK7 链表成环死循环 | 并发正解是下面的 CHM |
| `Hashtable` | **锁整张表**（方法级 synchronized） | 阻塞 | 串行 | 性能差；不允许 null；已过时 |
| `Collections.synchronizedMap` | 锁包装对象（一把互斥锁） | 阻塞 | 串行 | 同上，本质是装饰器加锁 |
| **`ConcurrentHashMap`** | **桶级**（JDK8） | ✅ 无锁 | 桶间并行 | 首选 |

> HashMap 并发不安全的两个经典现场：JDK 7 扩容**头插法**导致链表成环 → `get` 死循环 CPU 100%；JDK 8 改尾插不再成环，但并发 `put` 仍会**数据覆盖丢失**。

**为什么不允许 null key / null value**（必考）：并发环境下 `map.get(key) == null` 无法区分「不存在」和「值就是 null」——单线程可以用 `containsKey` 二次确认，但并发下两次调用之间可能被别的线程修改，**存在竞态，语义无法自洽**，所以干脆禁止（`Hashtable` 同理；`HashMap` 允许正是因为单线程无此歧义）。

---

## 二、版本演进：JDK 7 分段锁 → JDK 8 CAS + synchronized

```
 JDK 7：Segment 分段锁                          JDK 8：Node 数组 + CAS + synchronized
 ┌──────────────────────────────┐              ┌──────────────────────────────┐
 │ Segment[0]  Segment[1]  ...   │              │ table[0] table[1] table[2]... │
 │ (ReentrantLock)               │              │   │        │                  │
 │   └ HashEntry[]               │              │  Node     Node                │
 │      └ 链表                    │              │   │        │                  │
 └──────────────────────────────┘              │  Node     链表/红黑树           │
  锁一段（多个桶）→ 并发度=16                      └──────────────────────────────┘
                                                锁一个桶头节点 → 并发度=桶数
```

| 对比项 | JDK 7（Segment） | JDK 8（Node 数组） |
|:---|:---|:---|
| 数据结构 | `Segment[]` + `HashEntry[]` + 链表 | `Node[]` table + 链表 + **红黑树** |
| 锁实现 | `Segment` 继承 `ReentrantLock` | **CAS（空桶）+ `synchronized`（桶头节点）** |
| 锁粒度 | 一个 Segment（含多个桶） | **单个桶** |
| 并发度 | 固定 = Segment 数（默认 16） | 动态 = 桶数，冲突少时接近无锁 |
| hash 定位 | 两级哈希（先段后桶） | 一次 `spread()` 扰动 |
| size 统计 | 先试无锁求和，失败再锁全部 Segment | `baseCount` + `CounterCell[]`（LongAdder 思想） |

**为什么 JDK 8 放弃分段锁**（高频追问）：

1. **粒度粗**：一个 Segment 管多个桶，不同桶的写也会互相阻塞；JDK 8 锁到桶，冲突概率大幅下降。
2. **内存开销**：Segment 是 `ReentrantLock` 子类对象，16 个段常驻；JDK 8 空桶零成本。
3. **锁优化红利**：`synchronized` 经过锁升级优化后，低竞争下性能不输 `ReentrantLock`，且免 AQS 对象开销。

---

## 三、底层结构（JDK 8）

```java
// 桶数组，懒加载（构造时不分配，第一次 put 才 initTable）
transient volatile Node<K,V>[] table;

// 扩容 / 初始化的控制变量（一个字段顶四个状态，面试必讲）
private transient volatile int sizeCtl;
//   == 0        ：未初始化，走默认容量 16
//   >  0        ：未初始化时=初始容量；初始化后=扩容阈值(n * 0.75)
//   == -1       ：正在初始化
//   <  -1       ：正在扩容，低16位 = 参与扩容的线程数 + 1

static class Node<K,V> {
    final int hash;
    final K key;
    volatile V val;   // volatile：保证 get 无锁读的可见性
    volatile Node<K,V> next; // volatile：扩容迁移 / 并发遍历可见
}
```

关键常量与特殊 hash 值：

| 常量 | 值 | 含义 |
|:---|:---:|:---|
| `DEFAULT_CAPACITY` | 16 | 默认表容量 |
| `LOAD_FACTOR` | 0.75f | 负载因子 |
| `TREEIFY_THRESHOLD` | 8 | 链表 → 红黑树的节点数阈值 |
| `MIN_TREEIFY_CAPACITY` | 64 | 树化要求的最小表容量，否则**先扩容** |
| `hash == MOVED(-1)` | -1 | 该位置是 **ForwardingNode**：已迁移完，读操作「转发」到新表 |
| `hash == TREEBIN(-2)` | -2 | 该位置是树根 TreeBin（红黑树的包装） |

---

## 四、核心源码分析

### 4.1 putVal：CAS + synchronized 的完整编排

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());          // ① 扰动：高低位异或 + 抹掉符号位
    int binCount = 0;                           // 记录桶内节点数，用于树化判断
    for (Node<K,V>[] tab = table;;) {           // ② 自旋：CAS 失败/状态变化就重试
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();                  // ③ 懒初始化（CAS 保证只建一次）
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
                break;                          // ④ 空桶：CAS 插入头节点，不加锁！
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);         // ⑤ 桶已迁移完：帮着一起扩容
        else {
            V oldVal = null;
            synchronized (f) {                  // ⑥ 锁桶头节点（锁粒度 = 一个桶）
                if (tabAt(tab, i) == f) {       //    再确认头节点没变（防锁错对象）
                    if (fh >= 0) {              //    链表：尾插遍历
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash && (ek = e.key) == key ||
                                (ek != null && key.equals(ek))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent) e.val = value; // volatile 写
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<>(hash, key, value);
                                break;          //    尾部插入（HashMap JDK8 同款尾插）
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        // ⑦ 红黑树：TreeBin 内部加读写锁处理
                        oldVal = ...;
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD - 1) // ⑧ 链表够长 → 树化(或先扩容)
                    treeifyBin(tab, i);
                if (oldVal != null) return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);                     // ⑨ 元素计数 + 判断是否要扩容
    return null;
}
```

流程图：

```
 put(key, value)
   │
   ├─ tab==null → initTable()（CAS，sizeCtl=-1 防并发初始化）
   ├─ 桶为空   → CAS 插入头节点 ──────────────→ 成功 → addCount
   ├─ hash==-1 → helpTransfer（协助扩容）
   └─ 否则     → synchronized(桶头节点)
                   ├─ 链表遍历：有同 key 覆盖 / 尾插新节点
                   ├─ 是树：TreeBin 红黑树插入
                   └─ 出锁后 binCount≥7 → treeifyBin（表<64 先扩容）
```

> 细节亮点：`synchronized(f)` 后还会 `tabAt(tab, i) == f` **二次校验**——锁的对象可能已被扩容换掉（旧桶头被 ForwardingNode 替换），避免锁了过期的节点。

### 4.2 get：全程无锁

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {   // tabAt：Unsafe volatile 读
        if ((eh = e.hash) == h) { ... return e.val; }        // 头节点命中
        else if (eh < 0)                            // -1=ForwardingNode：去新表找
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {              // 链表顺序遍历
            if (e.hash == h && ...) return e.val;   // e.val / e.next 都是 volatile
        }
    }
    return null;
}
```

**为什么 get 不加锁也安全？** 三重保证：

1. `Node.val` 和 `Node.next` 都是 **volatile**——写线程的修改对读线程立即可见；
2. `table` 数组元素用 `tabAt`（`Unsafe.getObjectVolatile`）读——绕过数组本身非 volatile 的问题；
3. 极端场景（读到的节点正在迁移）由 `ForwardingNode.find` **转发到新表**查询，不丢读。

### 4.3 transfer：多线程协助扩容（P7 深挖）

扩容不是一把大锁干完，而是**拆成多个迁移任务包，谁碰到谁帮忙**：

```
 旧表 n=32，stride=16（每个线程一次领 16 个桶）
 线程A 领 [16,31]   线程B 领 [0,15]      ← 边界靠 CAS 抢占 transferIndex
      │                  │
      ▼                  ▼
 逐桶迁移：高低位拆分((fh & n)==0 ? 低 : 高)，lastRun 复用尾部
 迁移完的桶放 ForwardingNode(hash=-1) 占位
 全部迁完 → table 指向新表，sizeCtl 恢复为阈值
```

要点：

- **触发**：`addCount` 发现元素数超过 `sizeCtl`（阈值）→ 发起扩容。
- **协助**：其他线程 `put` 时碰到 `hash == MOVED`，不是等待，而是 `helpTransfer` **领一段桶一起搬**——这就是「并发扩容」，把一次大停顿摊薄到多个写请求。
- **迁移算法**：与 HashMap JDK 8 相同的**高低位链表拆分**（`e.hash & n` 决定留在低位还是挪到高位），且用 `lastRun` 节点复用整段尾链减少克隆。
- **读兼容**：迁移中的桶被读到 → `ForwardingNode.find` 去新表查。

### 4.4 size：baseCount + CounterCell[]（LongAdder 思想）

```java
private transient volatile long baseCount;      // 无冲突时的计数器
private transient volatile CounterCell[] counterCells; // 冲突时分散计数

final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    if ((as = counterCells) != null ||
        !U.compareAndSetLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        // CAS baseCount 失败（有竞争）→ 找一个 cell 分散累加
        CounterCellValue...
    }
    if (check >= 0) { ...检查是否需要扩容... }
}
```

- 计数和热点 `counter++` 一样有 CAS 竞争 → 借鉴 **LongAdder**：竞争低走 `baseCount`，竞争高拆到 `counterCells` 数组按线程散列累加，读时**求和**。
- 因此 **`size()` 是弱一致的估算值**，遍历期间可能不精确；JDK 8 还提供 `mappingCount()`（返回 long，防超大集合 int 溢出）替代它。

---

## 五、一致性与迭代器：弱一致

| 行为 | ConcurrentHashMap | HashMap |
|:---|:---|:---|
| 迭代时并发修改 | **不抛 CME**，弱一致（不保证看到创建迭代器后的修改） | fail-fast 抛 `ConcurrentModificationException` |
| size()/isEmpty() | 弱一致估算 | 精确（单线程） |
| get | 无锁，读到的是某一刻的可见值 | 单线程语义 |

> 适合「统计/缓存」这类允许瞬时偏差的场景；需要强一致快照就自己加锁或拷贝。

---

## 六、典型应用场景

| 场景 | 用法 | 关键点 |
|:---|:---|:---|
| **并发缓存** | `map.computeIfAbsent(key, k -> load(k))` | 原子「查+建」，同 key 只加载一次；**别在 lambda 里再写本 map 同 key**（会死循环/异常） |
| **并发计数** | `map.merge(key, 1L, Long::sum)` | 替代 `get+put` 两步，天然原子 |
| **注册表/路由表** | 写少读多：启动注册，运行期高频读 | get 无锁，吞吐极高 |
| **本地一级缓存** | CHM + 过期淘汰策略 | 需要 LRU/TTL 上 Caffeine；CHM 本身无淘汰 |

> 记忆点：**单步操作线程安全 ≠ 复合操作线程安全**。`if (!map.containsKey(k)) map.put(k, v)` 在并发下照样会互相覆盖，必须用 `putIfAbsent` / `computeIfAbsent` / `compute` / `merge` 这些**原子复合方法**。

---

## 七、面试重点（Q&A 速答）

1. **为什么线程安全？JDK8 用什么锁？**——CAS（空桶插入）+ `synchronized`（锁桶头节点），读靠 volatile 无锁。
2. **JDK8 为什么放弃分段锁？**——粒度粗、Segment 内存开销、synchronized 锁升级后低竞争够快；桶级锁并发度=桶数。
3. **put 的加锁过程？**——空桶 CAS → MOVED 协助扩容 → 锁桶头节点二次确认再链表/树操作。
4. **get 要加锁吗？**——不用：val/next volatile + tabAt volatile 读 + ForwardingNode 转发新表。
5. **size 准确吗？怎么实现的？**——弱一致；baseCount CAS + CounterCell 分散计数（LongAdder 思想），读时求和。
6. **扩容和其他线程什么关系？**——其他写线程碰到 ForwardingNode 会 helpTransfer 分段协助迁移；读线程被转发到新表。
7. **链表转红黑树条件？**——链表 ≥ 8 且表容量 ≥ 64（否则先扩容）；退化阈值 6（JDK8 同 HashMap）。
8. **为什么禁止 null？**——并发下 get()==null 有歧义且无法用 containsKey 补救（竞态）。
9. **和 Hashtable 的区别？**——锁粒度（桶级 vs 全表）、null 支持（不允许 vs 不允许但 CHM 无迭代器并发修改异常）、性能；Hashtable 已淘汰。
10. **`computeIfAbsent` 的坑？**——mapping 函数里再修改同一 key 会 `IllegalStateException`/死锁；长耗时加载会阻塞同桶其他写。
11. **迭代时能增删吗？**——能，弱一致不抛异常；遍历到的元素看创建迭代器时的可见状态。

---

## 八、易错点 ⚠️

| 易错点 | 澄清 |
|:---|:---|
| **「用了 CHM 复合操作就安全」** | ❌ `get`+`put` 两步仍有竞态；用 `putIfAbsent`/`computeIfAbsent`/`merge` |
| **允许 null key/value** | 都不允许，`NullPointerException`；并发下 null 语义歧义 |
| **`computeIfAbsent` 里再 put 同 key** | 递归更新同一 key → `IllegalStateException`（可能死锁），加载逻辑里避免回写本 map 同 key |
| **size() 当精确值用** | 它是 LongAdder 式估算；精确快照要外部分段加锁 |
| **遍历时强一致** | 弱一致：不抛 CME，但不保证看到迭代器创建后的修改 |
| **锁的是整个 Map** | 锁的是**单个桶头节点**；不同桶的写并行，别再外面包 synchronized 全锁 |
| **树化只要 8 个节点** | 还要求 `table.length ≥ 64`，小表先扩容不树化 |
| **扩容是 STW 式一次完成** | 分段迁移 + 多线程协助，摊薄到多次写操作 |
| **构造时传容量就是最终容量** | 构造参数是「预估元素数」，内部换算成 2 的幂表大小（类似 HashMap 的 tableSizeFor 思路） |

---

## 九、一句话总结

ConcurrentHashMap JDK 8 = **volatile 无锁读 + CAS 空桶插入 + synchronized 锁桶头写 + ForwardingNode 协助扩容 + LongAdder 式分散计数**：把锁从「整表(JDK7 分段)」细化到「单桶」，把「扩容大停顿」摊给所有写线程，把「计数热点」拆到 cell 数组——三个思路分别对应面试三连：**锁粒度、并发扩容、size 原理**。

---

## 相关链接

| 主题 | 笔记 |
|:---|:---|
| HashMap 单线程源码（对照：扰动/扩容/树化同源） | [HashMap的应用场景和源码分析.md](HashMap的应用场景和源码分析.md) |
| JUC 全景速记（含 CHM 概览节） | [JUC并发包.md](JUC并发包.md) |
| 线程隔离（ThreadLocalMap 源码同款线性探测） | [ThreadLocal源码分析.md](ThreadLocal源码分析.md) |
| 并发新特性（虚拟线程/并发结构演进） | [jdk21增加了哪些新内容？.md](jdk21增加了哪些新内容？.md) |
| 线程池任务队列（BlockingQueue 家族） | [JUC并发包.md](JUC并发包.md) |
