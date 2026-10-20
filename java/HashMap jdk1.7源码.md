# HashMap JDK 1.7 源码分析

> JDK 1.7 的 `HashMap` 是「数组 + 链表」的纯哈希实现，没有红黑树兜底——最坏情况整条链 \(O(n)\)。它最大的历史意义是**头插法 + 并发扩容的链表成环**这个教科书级反例。学它的价值不在「怎么用它」，而在「为什么 JDK 8 要改」——理解了 1.7 的坑，8 的改动才算真正懂了。1.8 完整版见 [HashMap的应用场景和源码分析.md](HashMap的应用场景和源码分析.md)。

---

## 一、定位与对比

| 维度 | JDK 1.7 | JDK 1.8 |
|:---|:---|:---|
| 底层结构 | 数组 + 链表 | 数组 + 链表 + **红黑树** |
| 链表插入 | **头插法** | 尾插法 |
| 扩容重哈希 | 每个元素重新 `indexFor` 算下标 | `(hash & oldCap)` 拆分低位/高位两条链，免取模 |
| 并发扩容 | **链表可能成环** → get 死循环 CPU 100% | 尾插 + 保持相对顺序，不再成环（但仍非线程安全） |
| 最坏查找 | \(O(n)\) 整条链 | \(O(\log n)\) 红黑树兜底 |
| null key 位置 | 固定下标 0 | 固定下标 0 |
| 默认容量 / 负载因子 | 16 / 0.75 | 同左 |

> **面试为什么还考 1.7**：因为「JDK 7 HashMap 为什么会死循环」是并发与数据结构结合的经典题，能直接检验候选人是否真的翻过源码。

---

## 二、底层结构

```
        table 数组（每个槽位是一个 Entry 链表头）
   [0] → null
   [1] → Entry(k5) → Entry(k1)         ← 头插：新元素总是插在最前
   [2] → null
   [3] → Entry(k9)
   ...
   下标 = hash & (length - 1)     （length 为 2 的幂）
```

### Entry 定义（1.7 叫 Entry，1.8 改名为 Node）

```java
static class Entry<K,V> implements Map.Entry<K,V> {
    final K key;
    V value;
    Entry<K,V> next;      // 链表指针
    int hash;             // 缓存的哈希值（避免重算）

    Entry(int h, K k, V v, Entry<K,V> n) {
        value = v;
        next = n;         // 头插的关键：新节点的 next 指向旧链表头
        key = k;
        hash = h;
    }
}
```

### 关键常量

```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;   // 16
static final int MAXIMUM_CAPACITY = 1 << 30;
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final Entry<?,?>[] EMPTY_TABLE = {};            // 空表占位（懒初始化）
transient Entry<K,V>[] table = (Entry<K,V>[]) EMPTY_TABLE;
transient int threshold;                                // 扩容阈值 = 容量 × 0.75
```

---

## 三、核心源码分析

### 1. hash()：多次扰动

1.7 不像 1.8 只做一次「高 16 位异或」，而是更激进的多次移位异或，目的是**让高位充分影响低位**——因为下标计算只取低位。

```java
final int hash(Object k) {
    int h = hashSeed;                                  // 哈希种子（JDK7u40+ 引入防碰撞攻击）
    if (0 != h && k instanceof String) {
        return sun.misc.Hashing.stringHash32((String) k);
    }
    h ^= k.hashCode();
    // 多次扰动，把高位信息均匀扩散到低位
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}
```

### 2. indexFor：位运算定位桶

```java
static int indexFor(int h, int length) {
    return h & (length - 1);     // 等价 h % length，但要求 length 是 2 的幂
}
```

### 3. put：先查旧，再头插

```java
public V put(K key, V value) {
    if (table == EMPTY_TABLE) {
        inflateTable(threshold);                       // ① 懒初始化：第一次 put 才分配数组
    }
    if (key == null)
        return putForNullKey(value);                   // ② null key 固定走下标 0
    int hash = hash(key);
    int i = indexFor(hash, table.length);
    // ③ 遍历链表找同 key：hash 相同且 (== 或 equals) → 覆盖
    for (Entry<K,V> e = table[i]; e != null; e = e.next) {
        Object k;
        if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
            V oldValue = e.value;
            e.value = value;
            e.recordAccess(this);
            return oldValue;
        }
    }
    modCount++;
    addEntry(hash, key, value, i);                     // ④ 没找到 → 新增
    return null;
}
```

### 4. addEntry：头插法

```java
void addEntry(int hash, K key, V value, int bucketIndex) {
    // ① 先判扩容：size 超阈值 且 目标桶非空（注意这个"且"，1.7 的扩容条件比 1.8 苛刻）
    if ((size >= threshold) && (null != table[bucketIndex])) {
        resize(2 * table.length);
        hash = (null != key) ? hash(key) : 0;
        bucketIndex = indexFor(hash, table.length);
    }
    createEntry(hash, key, value, bucketIndex);
}

void createEntry(int hash, K key, V value, int bucketIndex) {
    Entry<K,V> e = table[bucketIndex];
    table[bucketIndex] = new Entry<>(hash, key, value, e);   // ② 头插：新节点的 next = 旧链表头
    size++;
}
```

> **头插法示意**：原链表 `A → B`，插入 `C` 后变 `C → A → B`。`C.next = A`，`table[i] = C`。

### 5. resize：扩容 + 重哈希（成环的案发现场）

```java
void resize(int newCapacity) {
    Entry[] oldTable = table;
    int oldCapacity = oldTable.length;
    if (oldCapacity == MAXIMUM_CAPACITY) {
        threshold = Integer.MAX_VALUE;
        return;
    }
    Entry[] newTable = new Entry[newCapacity];
    transfer(newTable, initHashSeedAsNeeded(newCapacity));   // 关键：把旧表所有元素搬到新表
    table = newTable;
    threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
}

void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e : table) {              // ① 外层：遍历旧表每个桶
        while (null != e) {                   // ② 内层：遍历该桶的链表
            Entry<K,V> next = e.next;         // ⚠️ 先暂存 next，因为马上要被改
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            e.next = newTable[i];             // ⚠️ 头插到新表：新节点的 next = 新表当前桶头
            newTable[i] = e;                  // ⚠️ 新表桶头 = 新节点
            e = next;                         // ③ 推进到旧链表的下一个
        }
    }
}
```

**这段 transfer 是 1.7 最经典的并发陷阱**，下面单开一节讲。

---

## 四、并发扩容的死循环原理（面试必考）

### 场景还原

- 初始：桶 `i` 上链表为 `A → B → null`
- 线程 T1、T2 同时触发 `resize`，都进入 `transfer`
- 假设扩容后 A、B 仍分到新表的同一个桶 `j`（下标相同）

### T1 正常搬完，新表得到 `B → A → null`（因为头插，顺序反转）

```
旧表桶 i:    A → B → null
              ↓ 头插到新表
新表桶 j:    B → A → null
```

### T2 在 T1 之前已经读到 `e = A`、`next = B`，然后被挂起；T1 跑完后 T2 恢复执行

T2 的视角（拿的是**旧引用** A 和 B）：

1. 第一轮：`e = A`，`next = B`。`A.next = newTable[j]`（此时 newTable[j] 已是 T1 搬完的 B），`newTable[j] = A`。新表变 `A → B → A → ...` ⚠️ 已经成环了
2. 第二轮：`e = next = B`，`next = B.next`（**B.next 已经被 T1 改成 A**），`B.next = newTable[j]`（=A），`newTable[j] = B`。环固定：`A ↔ B`
3. 第三轮：`e = A`，但 `A.next = B`，`B.next = A`，死循环

```
最终状态（线程视角混乱后）：
  A.next = B
  B.next = A      ← 环！
```

### 后果

任何后续对这个桶的 `get` / `put`，只要找不到 key，就会**在环上死循环**——CPU 直接 100%，且无法通过异常恢复（不是死锁，是活锁）。

### 为什么 JDK 8 不会成环

1.8 的 `resize` 改成**尾插**并用 `(e.hash & oldCap)` 把一条链拆成两条（低位、高位），**保持原有相对顺序**，且不需要暂存 `next`——遍历只读取，不修改旧链表的 next 指针，从根本上消除了"两个线程并发改 next"的窗口。

---

## 五、复杂度分析

| 操作 | 平均 | 最坏（哈希全冲突） |
|:---|:---:|:---:|
| `get` / `put` / `remove` | \(O(1)\) | \(O(n)\)（无红黑树兜底） |
| 遍历 | \(O(n + 桶数)\) | 同 |
| 空间 | \(O(n)\) | — |

> 1.7 没有树化，遇到恶意构造的哈希碰撞攻击（如 2011 年 Java 哈希 DoS），整条链退化到 \(O(n)\)，服务端会被打挂。这也是 JDK 8 引入红黑树 + 哈希种子的原因之一。

---

## 六、1.7 vs 1.8 核心改动一览

| 改动点 | 1.7 | 1.8 | 为什么改 |
|:---|:---|:---|:---|
| 节点类型 | `Entry` | `Node`（链表）/ `TreeNode`（红黑树） | 支持树化 |
| 链表插入 | 头插 | 尾插 | 消除扩容成环 |
| 哈希扰动 | 多次移位异或 | `(h = key.hashCode()) ^ (h >>> 16)` 一次 | 简化 + 配合红黑树兜底 |
| 树化阈值 | 无 | 链表 ≥ 8 且容量 ≥ 64 树化，≤ 6 退化 | 防哈希 DoS |
| 扩容重哈希 | 逐个重新 `indexFor` | `(hash & oldCap)` 拆低位/高位 | 效率 + 保序 |
| 扩容触发条件 | `size >= threshold && 目标桶非空` | `++size > threshold` | 1.7 那个"且"很反直觉，1.8 修正 |
| null key | `putForNullKey` 走下标 0 | 走 `hash = 0` 统一下标 0 | 代码统一 |
| 并发 | 头插成环死循环 | 尾插不再成环，但仍非线程安全 | 部分修复 |

---

## 七、易错点

### 1. 「JDK 8 之后 HashMap 并发就安全了」是错的

1.8 只解决了**成环死循环**这一个问题。并发 `put` 仍会**互相覆盖丢数据**，`get` 仍可能看到中间态。并发场景依然只能用 `ConcurrentHashMap`。

### 2. 1.7 的扩容条件 `size >= threshold && 目标桶非空`

这是个坑：即使 `size` 超阈值，只要新元素落到一个**空桶**，就**不扩容**。结果可能是某些桶链已经很长、但表没扩。1.8 改成 `++size > threshold` 直接扩，逻辑更直白。

### 3. 头插法本身没问题，问题在「头插 + 扩容 + 并发」三者叠加

单线程下 1.7 的头插完全正确。把它当黑锅扣在头插法头上不公平——真正的问题是 `transfer` 修改了 `next` 指针，且并发下没有同步。

### 4. 别把 1.7 的 Entry 和 1.8 的 Node 当同一个类讲

面试官追问「Node 和 Entry 区别」时，直接答「1.7 叫 Entry，1.8 改名 Node 并抽出 Map.Entry 接口」即可，不要硬编差异。

### 5. 1.7 的 `hashSeed` 是后来加的

JDK 7u40 才引入哈希种子防 DoS。早期 1.7 完全没这个保护，这也是它被打挂的历史背景。

---

## 八、面试重点

| 优先级 | 考点 | 一句话答法 |
|:---:|:---|:---|
| ⭐⭐⭐ | **1.7 并发扩容为什么会死循环** | 头插法 + `transfer` 改 next 指针 + 无同步，两线程并发迁移时 next 被互相覆盖成环，get 在环上死循环 CPU 100% |
| ⭐⭐⭐ | **1.7 和 1.8 的核心区别** | 结构（无树 vs 有树）、插入（头 vs 尾）、扩容（逐个重算 vs `(hash&oldCap)` 拆链）、并发（成环 vs 丢数据） |
| ⭐⭐ | **为什么用头插法** | 简单 + 理论上「最近插入的最可能被访问」（局部性），但实际收益小，1.8 为了消除成环问题放弃 |
| ⭐⭐ | **1.7 的哈希扰动** | 多次移位异或让高位影响低位，减少碰撞（但没有红黑树兜底，仍可能被打挂） |
| ⭐ | **1.7 的扩容触发条件** | `size >= threshold && 目标桶非空`，比 1.8 的 `size > threshold` 苛刻 |

---

## 九、一句话总结

JDK 1.7 `HashMap` 用「**数组 + 链表 + 头插法**」实现平均 \(O(1)\) 增删查，但 `transfer` 在扩容时**修改链表 next 指针**，并发下两线程互相覆盖会形成**链表环**，导致 `get` 死循环 CPU 100%；JDK 8 通过**尾插法 + `(hash & oldCap)` 保序拆链 + 红黑树兜底**根治了成环问题，同时把最坏查找从 \(O(n)\) 压到 \(O(\log n)\)。学 1.7 的价值不在用它，而在理解 8 为什么要这样改。

---

## 相关链接

- JDK 8 完整版见 [HashMap的应用场景和源码分析.md](HashMap的应用场景和源码分析.md)
- 并发版本见 [ConcurrentHashMap应用场景和源码分析.md](ConcurrentHashMap应用场景和源码分析.md)
- 哈希原理与刷题套路见 [数据结构/哈希表.md](../数据结构/哈希表.md)
