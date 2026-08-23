# TreeSet 的应用场景与源码分析

> `TreeSet` 是 Java 中**有序**的 `Set`：元素**自动按大小排序**、去重，并支持「找最接近某值的元素」等**导航（navigation）**操作。底层不是自己实现的树，而是**直接复用 `TreeMap`（红黑树）**，是「排序 + 去重 + 范围查询」场景的首选。

---

## 一、TreeSet 是什么

`TreeSet` 实现了 `NavigableSet`（继承自 `SortedSet`），三大特点：

| 特点 | 说明 |
|:---|:---|
| **有序** | 元素按**自然顺序**（`Comparable`）或**自定义 `Comparator`** 升序排列 |
| **去重** | 是 `Set`，不允许重复元素（用「比较结果为 0」判重，**不是 `equals`**！） |
| **可导航** | 支持 `first/last`、`ceiling/floor`、`higher/lower`、`headSet/tailSet/subSet` 等 |

```java
TreeSet<Integer> set = new TreeSet<>();
set.add(5); set.add(1); set.add(3); set.add(3);   // 重复的 3 被忽略
System.out.println(set);              // [1, 3, 5]  自动升序
System.out.println(set.first());      // 1   最小
System.out.println(set.last());       // 5   最大
System.out.println(set.ceiling(2));   // 3   ≥ 2 的最小元素
System.out.println(set.floor(4));     // 3   ≤ 4 的最大元素
```

---

## 二、典型应用场景

| 场景 | 为什么用 TreeSet | 关键方法 |
|:---|:---|:---|
| **需要有序去重** | 一步到位排序 + 去重，遍历即有序 | `add` / 迭代 |
| **找最接近的值** | \(O(\log n)\) 找「≥ x 的最小」「≤ x 的最大」 | `ceiling` / `floor` / `higher` / `lower` |
| **范围查询 / 区间统计** | 取某区间内的所有元素 | `subSet` / `headSet` / `tailSet` |
| **动态维护极值** | 随时取当前最大/最小，且可删除任意元素 | `first` / `last` / `pollFirst` / `pollLast` |
| **排行榜 / 时间线** | 按分数/时间戳有序，插入删除都 \(O(\log n)\) | 综合 |

> 对比 `PriorityQueue`（堆）：堆只能高效取**一个极值**、不能有序遍历、不能按值删除任意元素；`TreeSet` 能取两端极值、有序遍历、\(O(\log n)\) 删除任意元素、还能做范围/最近邻查询。见 [优先队列.md](../数据结构/优先队列.md)。

### 刷题里的用法示例

```java
// 找数据流中比 x 大的最小值（存在则返回，否则 null）
TreeSet<Integer> set = new TreeSet<>();
// ... 不断 add ...
Integer justBigger = set.higher(x);   // 严格大于 x 的最小元素，O(log n)
```

「最近的请求次数」「存在重复元素 III（值差与索引差双约束）」「日程表可预订」等题都靠 `TreeSet` 的 `ceiling/floor` + `subSet`。

---

## 三、底层结构：它就是一个 TreeMap

`TreeSet` **没有自己的树实现**，内部持有一个 `NavigableMap`（实际是 `TreeMap`），把元素当作 **key**，用一个**固定的哑元对象 `PRESENT` 当 value**。

```java
// —— JDK 源码（节选，含注释）——
public class TreeSet<E> extends AbstractSet<E>
        implements NavigableSet<E>, Cloneable, java.io.Serializable {

    private transient NavigableMap<E,Object> m;   // 委托给 TreeMap

    // 所有 value 都指向这个共享的空对象（省内存）
    private static final Object PRESENT = new Object();

    public TreeSet() {
        this(new TreeMap<E,Object>());            // 默认自然排序的 TreeMap
    }

    // 传入比较器 → 底层 TreeMap 用该比较器排序
    public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<>(comparator));
    }

    TreeSet(NavigableMap<E,Object> m) { this.m = m; }
}
```

因此：**TreeSet 的一切操作都是对 TreeMap 的 key 进行操作**。理解 TreeSet = 理解 TreeMap 的红黑树。

### add / remove / contains 的委托

```java
public boolean add(E e) {
    return m.put(e, PRESENT) == null;   // put 返回旧 value；无旧值(null)说明是新增
}
public boolean remove(Object o) {
    return m.remove(o) == PRESENT;      // 删掉返回被删的 value
}
public boolean contains(Object o) {
    return m.containsKey(o);
}
```

---

## 四、核心源码分析：TreeMap 的红黑树

`TreeMap` 是**红黑树**（一种自平衡的 [二叉搜索树](../数据结构/二叉搜索树.md)），保证增删查都 \(O(\log n)\)。

### 1. 节点定义

```java
static final class Entry<K,V> {
    K key; V value;
    Entry<K,V> left, right, parent;
    boolean color = BLACK;              // 每个节点带颜色：红 / 黑
}
```

### 2. 红黑树的五条性质（保证平衡的关键）

1. 每个节点非红即黑；
2. 根节点是黑色；
3. 红色节点的孩子必须是黑色（**不能有连续两个红**）；
4. 从任一节点到其所有叶子（NIL）的路径含**相同数目的黑节点**；
5. 叶子（NIL 空节点）视为黑色。

> 这五条共同约束「最长路径 ≤ 2×最短路径」，从而树高恒为 \(O(\log n)\)——比普通 BST「有序插入退化成链」稳，比 AVL 树旋转更少（插入删除更快）。

### 3. put（插入 + 用比较器定位 + 平衡修复）

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {                       // 空树，直接当根
        compare(key, key);                 // 触发 null 检查 / 类型检查
        root = new Entry<>(key, value, null);
        size = 1;
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {                     // 有自定义比较器
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)      t = t.left;   // 比当前小 → 左
            else if (cmp > 0) t = t.right;  // 比当前大 → 右
            else return t.setValue(value);  // cmp==0 → 视为同一个 key，覆盖
        } while (t != null);
    } else {                               // 用自然顺序 Comparable
        // ... 同上，改用 ((Comparable)key).compareTo(t.key) ...
    }
    Entry<K,V> e = new Entry<>(key, value, parent);   // 挂到叶子
    if (cmp < 0) parent.left = e; else parent.right = e;
    fixAfterInsertion(e);                  // 关键：红黑树插入后重新平衡（变色 + 旋转）
    size++;
    return null;
}
```

**要点**：
- **判重靠 `compare(...) == 0`**，完全不调用 `equals` / `hashCode`（与 `HashSet` 本质不同）。
- 新节点先按 BST 规则挂到叶子，再由 `fixAfterInsertion` 通过**变色 + 左旋/右旋**恢复红黑性质。

### 4. 导航方法：ceiling / floor 的原理

`ceiling(key)` = 「≥ key 的最小元素」，本质是一次带记录的 BST 查找：

```java
final Entry<K,V> getCeilingEntry(K key) {
    Entry<K,V> p = root;
    while (p != null) {
        int cmp = compare(key, p.key);
        if (cmp < 0) {                    // key < p：p 是候选，往左找更小的候选
            if (p.left != null) p = p.left;
            else return p;
        } else if (cmp > 0) {             // key > p：往右
            if (p.right != null) p = p.right;
            else {                        // 右边到头 → 回溯找第一个「右拐点」祖先
                Entry<K,V> parent = p.parent, ch = p;
                while (parent != null && ch == parent.right) {
                    ch = parent; parent = parent.parent;
                }
                return parent;
            }
        } else return p;                  // 相等，直接命中
    }
    return null;
}
```

`floor`（≤ key 的最大）、`higher`（> key 的最小）、`lower`（< key 的最大）同理，只是比较方向不同——都是 \(O(\log n)\)。

---

## 五、复杂度分析

| 操作 | 时间复杂度 | 说明 |
|:---|:---:|:---|
| `add` / `remove` / `contains` | \(O(\log n)\) | 红黑树查找 + 平衡 |
| `first` / `last` | \(O(\log n)\) | 一路走到最左 / 最右 |
| `ceiling` / `floor` / `higher` / `lower` | \(O(\log n)\) | 带回溯的 BST 查找 |
| `pollFirst` / `pollLast` | \(O(\log n)\) | 取并删极值 |
| 迭代（有序遍历） | \(O(n)\) | 中序遍历 |
| `subSet` / `headSet` / `tailSet` | \(O(\log n)\) 建视图 | 返回的是**视图**，遍历才计元素数 |

> 空间 \(O(n)\)。相比 `HashSet` 的平均 \(O(1)\)，`TreeSet` 慢一个 log，但**换来了有序性与导航能力**。

---

## 六、三种 Set 对比

| 维度 | `HashSet` | `LinkedHashSet` | **`TreeSet`** |
|:---|:---|:---|:---|
| 底层 | HashMap | HashMap + 双向链表 | **TreeMap（红黑树）** |
| 顺序 | 无序 | **插入顺序** | **排序（自然/比较器）** |
| 增删查 | 平均 \(O(1)\) | 平均 \(O(1)\) | \(O(\log n)\) |
| 判重依据 | `hashCode` + `equals` | `hashCode` + `equals` | **`compareTo` / `Comparator`** |
| 允许 null | 允许 1 个 | 允许 1 个 | **不允许**（比较会 NPE） |
| 独有能力 | — | 保留插入序 | **导航、范围查询、取两端极值** |

> **选型口诀**：只去重用 `HashSet`；要保留插入顺序用 `LinkedHashSet`；**要排序 / 范围 / 最近邻查询用 `TreeSet`**。

---

## 七、易错点

### 1. 判重用「比较结果」而非 `equals`

若 `compareTo` 返回 0，`TreeSet` 就认为是**同一个元素**（即使 `equals` 为 false），会被去重。**务必让 `compareTo` 与 `equals` 语义一致**，否则元素「诡异丢失」。

### 2. 不能存 null

`TreeSet` 加入元素要比较大小，`null` 参与比较抛 `NullPointerException`（空树加第一个 null 也会，因 `put` 里的 `compare(key,key)` 检查）。

### 3. 元素必须可比较

存自定义对象要么让类 `implements Comparable`，要么构造时传 `Comparator`，否则 `add` 抛 `ClassCastException`。

### 4. 比较器不要用 `a - b`

整型相减可能**溢出**（如 `Integer.MIN_VALUE`），用 `Integer.compare(a, b)`。

### 5. `subSet` 返回的是视图

对返回的子集合增删会**反映到原集合**，且越界插入会抛 `IllegalArgumentException`。

### 6. 迭代中修改抛异常

遍历时直接 `add`/`remove` 触发 `ConcurrentModificationException`，用迭代器的 `remove` 或先收集后处理。

---

## 八、面试重点

| 优先级 | 考点 | 一句话答法 |
|:---:|:---|:---|
| ⭐⭐⭐ | **底层结构** | TreeSet 内部就是 TreeMap，元素当 key、共享哑元 `PRESENT` 当 value；TreeMap 是**红黑树** |
| ⭐⭐⭐ | **红黑树为何 \(O(\log n)\)** | 五条性质约束「最长路径 ≤ 2×最短路径」，树高稳定 \(O(\log n)\)，避免 BST 退化成链 |
| ⭐⭐ | **判重机制** | 靠 `compareTo`/`Comparator` 返回 0 判重，**不看 equals/hashCode**——与 HashSet 的本质区别 |
| ⭐⭐ | **导航方法** | `ceiling/floor/higher/lower` 是带回溯的 BST 查找，\(O(\log n)\)，是「最近邻 / 范围查询」的杀手锏 |
| ⭐ | **三种 Set 选型** | 无序去重 HashSet、插入序 LinkedHashSet、排序/范围 TreeSet |
| ⭐ | **红黑树 vs AVL** | 红黑树旋转更少、增删更快（近似平衡）；AVL 更严格平衡、查询略优。JDK 集合选红黑树 |

---

## 九、一句话总结

`TreeSet` = **红黑树支撑的有序去重集合**：本质是把元素当 key 塞进 `TreeMap`，一切操作委托给红黑树，增删查与导航全部 \(O(\log n)\)——它靠 `compareTo`/`Comparator`（而非 `equals`）判重排序，独有的 `ceiling/floor/subSet` 让它成为「排序 + 去重 + 最近邻 / 范围查询」的首选；不需要顺序就用 HashSet，只需插入序就用 LinkedHashSet。

---

## 相关链接

- 红黑树 = 自平衡 BST，原理见 [数据结构/二叉搜索树.md](../数据结构/二叉搜索树.md)
- 与「只取单极值」的堆对比见 [数据结构/优先队列.md](../数据结构/优先队列.md)
- 哈希去重原理见 [数据结构/哈希表.md](../数据结构/哈希表.md)
