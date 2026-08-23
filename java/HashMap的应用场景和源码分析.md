# HashMap 的应用场景与源码分析

> `HashMap` 是 Java 最常用的**键值对容器**：通过哈希把 key 映射到桶（bucket），实现平均 \(O(1)\) 的增删查。JDK 8 起底层是 **数组 + 链表 + 红黑树**——链表过长转红黑树，把最坏情况从 \(O(n)\) 压到 \(O(\log n)\)。它是「缓存、计数、去重、查找配对」的第一选择，也是面试源码考察的重灾区。

---

## 一、HashMap 是什么

| 特点 | 说明 |
|:---|:---|
| **键值映射** | 存 `key → value`，key 唯一（重复 put 覆盖旧值） |
| **无序** | 不保证遍历顺序（要顺序用 `LinkedHashMap`，要排序用 `TreeMap`） |
| **允许 null** | 允许 **1 个 null key**、多个 null value |
| **非线程安全** | 并发场景用 `ConcurrentHashMap` |
| **平均 O(1)** | 依赖好的哈希分布与受控的负载因子 |

```java
Map<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.getOrDefault("b", 0);                 // 不存在给默认值
map.merge("a", 1, Integer::sum);          // 计数神器：有则累加，无则置初值
map.computeIfAbsent("k", x -> new ArrayList<>()).add(7);  // 懒初始化 value
```

---

## 二、典型应用场景

| 场景 | 用法 | 关键方法 |
|:---|:---|:---|
| **查找配对** | 边遍历边存「已见过的值 → 下标」 | `containsKey` / `get` |
| **计数 / 频率统计** | 词频、字符计数 | `merge` / `getOrDefault` |
| **去重 / 判存在** | 记录出现过的元素 | `containsKey`（或直接用 `HashSet`） |
| **分组 / 桶** | 按 key 把元素归到 list | `computeIfAbsent` |
| **缓存 / 记忆化** | DP 记忆化、LRU（配双链表） | `get` / `put` |
| **索引 / 映射表** | id → 对象、name → 配置 | `get` |

> 刷题里几乎是「**空间换时间**」的第一反应：把暴力的 \(O(n^2)\) 打到 \(O(n)\)。哈希原理与解题套路见 [数据结构/哈希表.md](../数据结构/哈希表.md)。

---

## 三、底层结构（JDK 8）

**数组（桶）+ 链表 + 红黑树**：数组每个槽位是一个桶，冲突的元素先挂成链表，链表**长度 ≥ 8 且数组长度 ≥ 64** 时转红黑树。

```
        table 数组（每格是一个桶）
   [0] → null
   [1] → Node(k1) → Node(k5) → Node(k9)      ← 链表（冲突较少）
   [2] → null
   [3] → TreeNode(根) ...                     ← 红黑树（冲突多，链表过长树化）
   ...
   下标 = (n - 1) & hash    （n 为数组长度，2 的幂）
```

### 关键常量

```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;   // 默认容量 16
static final float DEFAULT_LOAD_FACTOR = 0.75f;       // 负载因子 0.75
static final int TREEIFY_THRESHOLD = 8;               // 链表转红黑树阈值
static final int UNTREEIFY_THRESHOLD = 6;             // 红黑树退化为链表阈值
static final int MIN_TREEIFY_CAPACITY = 64;           // 树化要求的最小数组容量
```

### Node 定义

```java
static class Node<K,V> {
    final int hash;      // 缓存的哈希值（避免重复计算）
    final K key;
    V value;
    Node<K,V> next;      // 链表指针
}
```

---

## 四、核心源码分析

### 1. hash()：扰动函数

不直接用 `key.hashCode()`，而是把高 16 位异或到低 16 位——因为定位下标用 `(n-1) & hash`，只取低位；扰动让**高位也参与**，减少碰撞。

```java
static final int hash(Object key) {
    int h;
    // key 为 null → 哈希 0（所以 null key 落在下标 0 的桶）
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

### 2. 为什么容量是 2 的幂

下标计算用 `(n - 1) & hash` 代替 `hash % n`——**当 n 是 2 的幂时二者等价**，而位运算更快。这也是初始容量会被向上取整到最近 2 的幂的原因。

### 3. put 流程（含注释）

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;               // ① 表为空 → 首次扩容（懒初始化）
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);   // ② 桶为空 → 直接放
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;                                   // ③ 桶首就是同一个 key
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(...);  // ④ 已是红黑树 → 树插入
        else {
            for (int binCount = 0; ; ++binCount) {   // ⑤ 遍历链表
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);  // 尾插（JDK8）
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);       // 链表 ≥ 8 → 尝试树化
                    break;
                }
                if (e.hash == hash && (...key 相等...)) break;  // 找到相同 key
                p = e;
            }
        }
        if (e != null) {                             // 已存在同 key → 覆盖旧值
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null) e.value = value;
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold) resize();                // ⑥ 超过阈值 → 扩容
    return null;
}
```

**要点**：
- 判 key 相同 = **`hash` 相等且（`==` 或 `equals`）**——这就是为什么自定义 key 要同时正确重写 `hashCode` 和 `equals`。
- JDK 8 链表**尾插**（JDK 7 是头插，多线程扩容会成环）。
- 树化有**双条件**：链表长 ≥ 8 **且**数组容量 ≥ 64，否则只是先 `resize` 扩容而非树化。

### 4. resize()：扩容

容量与阈值都**翻倍**（`newCap = oldCap << 1`），并把旧桶元素**重新分布**到新表。

```java
// JDK 8 巧妙点：扩容后元素要么留在「原下标」，要么移到「原下标 + oldCap」
// 靠 (e.hash & oldCap) 判断：为 0 留原位，非 0 移动，无需重新取模
if ((e.hash & oldCap) == 0) { /* 挂到 loHead 低位链 */ }
else                        { /* 挂到 hiHead 高位链，新下标 = j + oldCap */ }
```

> JDK 8 扩容用 `(e.hash & oldCap)` 把一条链**拆成低位、高位两条**，避免逐个重新 `%` 取模，且**保持相对顺序**（配合尾插，不再有 JDK 7 头插的成环问题）。

### 5. 链表 ↔ 红黑树

- **树化**：链表长度达 8 且容量 ≥ 64 → 转红黑树，查找从 \(O(n)\) 变 \(O(\log n)\)。
- **退化**：扩容拆分后树节点 ≤ 6 → 退回链表（阈值 8 和 6 之间留缓冲，避免临界点反复转换）。

---

## 五、复杂度分析

| 操作 | 平均 | 最坏（大量冲突） |
|:---|:---:|:---:|
| `get` / `put` / `remove` | \(O(1)\) | 链表 \(O(n)\)；**树化后 \(O(\log n)\)** |
| 遍历 | \(O(n + 桶数)\) | 同 |
| 空间 | \(O(n)\) | — |

> 平均 \(O(1)\) 前提：**哈希分布均匀** + **负载因子受控**（默认 0.75，元素超 `容量×0.75` 就扩容）。0.75 是「空间利用率」与「冲突概率」的经验折中。

---

## 六、三种 Map 对比

| 维度 | `HashMap` | `LinkedHashMap` | `TreeMap` |
|:---|:---|:---|:---|
| 底层 | 数组+链表+红黑树 | HashMap + 双向链表 | 红黑树 |
| 顺序 | 无序 | **插入序 / 访问序**（可做 LRU） | **按 key 排序** |
| 增删查 | 平均 \(O(1)\) | 平均 \(O(1)\) | \(O(\log n)\) |
| null key | 允许 1 个 | 允许 1 个 | **不允许** |
| 判等依据 | `hashCode`+`equals` | `hashCode`+`equals` | `compareTo`/`Comparator` |

> `TreeMap` 是 [TreeSet](TreeSet的应用场景和源码分析.md) 的底层；要有序/范围查询用它，普通场景 HashMap 最快。

---

## 七、易错点

### 1. 自定义 key 必须同时重写 `hashCode` 和 `equals`

且两者语义一致（`equals` 相等则 `hashCode` 必相等），否则 put 进去 get 不出来。推荐用不可变对象当 key。

### 2. key 的 `hashCode`/`equals` 依赖的字段别可变

put 之后改了参与哈希的字段，元素会「找不到」（落在原桶，但新哈希算到别的桶）。

### 3. 判存在用 `containsKey` 而非 `get() != null`

value 本身可能是 null，`get` 返回 null 无法区分「不存在」和「值为 null」。

### 4. 遍历时增删抛 `ConcurrentModificationException`

用迭代器的 `remove`，或 JDK 8 的 `removeIf` / `entrySet().iterator()`。

### 5. 并发下别用 HashMap

多线程 put 可能丢数据；**JDK 7 头插扩容还会成环导致 `get` 死循环 CPU 100%**（JDK 8 尾插已修复成环，但仍非线程安全）。并发用 `ConcurrentHashMap`。

### 6. 初始容量优化

已知大致元素数时，`new HashMap<>(expectedSize / 0.75 + 1)` 预设容量，减少扩容 rehash。

---

## 八、面试重点

| 优先级 | 考点 | 一句话答法 |
|:---:|:---|:---|
| ⭐⭐⭐ | **底层结构** | 数组 + 链表 + 红黑树；链表长 ≥ 8 且容量 ≥ 64 转红黑树，退化阈值 6 |
| ⭐⭐⭐ | **put 流程** | 算 hash → 定位桶 → 空则放/否则链表或树查找 → 相同 key 覆盖 → 超阈值扩容 |
| ⭐⭐⭐ | **扩容 resize** | 容量翻倍，`(hash & oldCap)` 拆低位/高位两条链，免重新取模、保序 |
| ⭐⭐ | **hash 扰动 + 2 的幂** | 高 16 位异或低位减碰撞；容量 2 的幂使 `(n-1)&hash` 等价取模且更快 |
| ⭐⭐ | **JDK7 vs 8** | 7 头插（扩容成环、死循环），8 尾插 + 红黑树，更安全更快 |
| ⭐⭐ | **负载因子 0.75** | 空间与冲突的折中；超 `容量×0.75` 触发扩容 |
| ⭐ | **为什么线程不安全 / 并发方案** | 丢数据、成环；并发用 ConcurrentHashMap（分段/CAS+synchronized） |

---

## 九、一句话总结

`HashMap` 用**扰动哈希 + `(n-1)&hash` 定位桶**实现平均 \(O(1)\)，底层是**数组 + 链表 + 红黑树**——冲突先挂链表、过长（≥8 且容量≥64）转红黑树把最坏压到 \(O(\log n)\)；扩容时容量翻倍并用 `(hash & oldCap)` 巧妙拆链免取模。用好它记住「自定义 key 重写 hashCode/equals、判存在用 containsKey、并发换 ConcurrentHashMap」；要顺序用 LinkedHashMap，要排序用 TreeMap。

---

## 相关链接

- 哈希原理与刷题套路见 [数据结构/哈希表.md](../数据结构/哈希表.md)
- 红黑树 = 自平衡 BST，见 [数据结构/二叉搜索树.md](../数据结构/二叉搜索树.md)
- 有序去重的 [TreeSet](TreeSet的应用场景和源码分析.md) 底层即 TreeMap
