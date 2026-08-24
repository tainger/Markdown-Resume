# TreeMap 的应用场景与源码分析

> `TreeMap` 是 Java 中**按 key 有序**的 `Map`：底层是**红黑树**（自平衡 BST），保证 `key` 始终有序，增删查全部 \(O(\log n)\)。它同时实现了 `NavigableMap`，提供 `ceiling/floor/subMap` 等**导航与范围查询**能力——是「排序映射、区间统计、最近邻查找」的首选。[TreeSet](TreeSet的应用场景和源码分析.md) 的底层就是它。

---

## 一、TreeMap 是什么

`TreeMap` 实现了 `NavigableMap`（继承 `SortedMap` → `Map`），核心特点：

| 特点 | 说明 |
|:---|:---|
| **按 key 有序** | key 按**自然顺序**（`Comparable`）或**自定义 `Comparator`** 升序排列 |
| **红黑树支撑** | 增删查 \(O(\log n)\)，无扩容 rehash（不像 HashMap） |
| **可导航** | `firstKey/lastKey`、`ceilingKey/floorKey`、`higherKey/lowerKey`、`headMap/tailMap/subMap` |
| **key 不允许 null** | key 要参与比较，null 会 NPE（value 可以为 null） |
| **判重靠比较** | 用「比较结果为 0」判定同一个 key，**不看 `equals`/`hashCode`** |
| **非线程安全** | 并发用 `ConcurrentSkipListMap`（跳表，同样有序） |

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(5, "e"); map.put(1, "a"); map.put(3, "c");
System.out.println(map);              // {1=a, 3=c, 5=e}  自动按 key 升序
System.out.println(map.firstKey());   // 1   最小 key
System.out.println(map.ceilingKey(2)); // 3  ≥ 2 的最小 key
System.out.println(map.floorEntry(4)); // 3=c  ≤ 4 的最大 entry
System.out.println(map.subMap(1, 5)); // {1=a, 3=c}  [1,5) 区间视图
```

---

## 二、典型应用场景

| 场景 | 为什么用 TreeMap | 关键方法 |
|:---|:---|:---|
| **按 key 有序遍历** | 遍历即升序，免额外排序 | `entrySet` / `forEach` |
| **最近邻查找** | \(O(\log n)\) 找「≥ x 的最小 key」「≤ x 的最大 key」 | `ceilingKey` / `floorKey` / `higherKey` / `lowerKey` |
| **范围查询 / 区间聚合** | 取某 key 区间内的所有映射 | `subMap` / `headMap` / `tailMap` |
| **差分 / 区间覆盖** | 用 key 记录端点，value 记增量，如「日程可预订」「区间染色」 | `floorEntry` / `subMap` |
| **有序计数 / 排行** | 按分数/时间戳做有序词频，随时取两端 | `merge` + `firstEntry` / `lastEntry` |
| **TreeSet 底层** | 元素当 key、哑元当 value | 见 [TreeSet](TreeSet的应用场景和源码分析.md) |

> **对比 HashMap**：HashMap 平均 \(O(1)\) 但无序、无范围查询；一旦题目出现「排序输出」「找最接近的 key」「区间求和」，TreeMap 的导航方法就是杀手锏。「我的日程安排表」「存在重复元素 III」「离线区间查询」等题都靠它。

```java
// 「离散化区间覆盖」范式：key = 位置，value = 该位置起的增量
TreeMap<Integer, Integer> diff = new TreeMap<>();
diff.merge(start, 1, Integer::sum);   // 区间 [start, end) 进入 +1
diff.merge(end,  -1, Integer::sum);   // 离开 -1
// 按 key 升序累加即可还原每段的覆盖数
```

---

## 三、底层结构：红黑树

`TreeMap` 内部是一棵**红黑树**（一种自平衡的 [二叉搜索树](../数据结构/二叉搜索树.md)），没有数组、没有哈希、没有扩容。

```
              (13,B)                 每个节点：key + value + 颜色(红/黑)
             /      \                左子树 key 全 < 根，右子树全 > 根（BST）
         (8,R)      (17,R)           红黑五性质约束树高 = O(log n)
        /    \      /    \
    (1,B)  (11,B)(15,B) (25,B)
```

### 节点定义

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;      // 左孩子（更小的 key）
    Entry<K,V> right;     // 右孩子（更大的 key）
    Entry<K,V> parent;    // 父指针（导航/删除回溯需要）
    boolean color = BLACK; // 颜色：默认黑
}
```

### 关键字段

```java
private final Comparator<? super K> comparator; // 为 null 则用 key 的自然顺序
private transient Entry<K,V> root;              // 红黑树根
private transient int size = 0;                 // 元素个数
```

### 红黑树的五条性质（保证平衡）

1. 每个节点非红即黑；
2. 根节点是黑色；
3. 红节点的孩子必须是黑色（**不能连续两个红**）；
4. 从任一节点到其所有叶子（NIL）的路径含**相同数目的黑节点**；
5. 叶子（NIL 空节点）视为黑色。

> 这五条共同约束「最长路径 ≤ 2×最短路径」，使树高恒为 \(O(\log n)\)——比普通 BST「有序插入退化成链」稳，比 AVL 树旋转更少（增删更快，查询略逊）。红黑树性质详见 [TreeSet](TreeSet的应用场景和源码分析.md) 与 [二叉搜索树.md](../数据结构/二叉搜索树.md)。

---

## 四、核心源码分析

### 1. put：定位 + 插入 + 平衡修复

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {                       // ① 空树 → 新 key 当根
        compare(key, key);                 //   触发类型/ null 检查
        root = new Entry<>(key, value, null);
        size = 1; modCount++;
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {                     // ② 有自定义比较器
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)      t = t.left;   //   比当前小 → 往左
            else if (cmp > 0) t = t.right;  //   比当前大 → 往右
            else return t.setValue(value);  //   cmp==0 → 同一个 key，覆盖旧值并返回
        } while (t != null);
    } else {                               // ③ 无比较器 → 用 Comparable 自然顺序
        if (key == null) throw new NullPointerException();
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)      t = t.left;
            else if (cmp > 0) t = t.right;
            else return t.setValue(value);
        } while (t != null);
    }
    Entry<K,V> e = new Entry<>(key, value, parent); // ④ 按 BST 规则挂到叶子
    if (cmp < 0) parent.left = e;
    else         parent.right = e;
    fixAfterInsertion(e);                  // ⑤ 关键：变色 + 旋转恢复红黑性质
    size++; modCount++;
    return null;
}
```

**要点**：
- **判重靠 `compare(...) == 0`**，完全不调用 `equals`/`hashCode`——这是与 HashMap 的本质区别。
- 新节点先按 BST 挂到叶子（**默认染红**，因为红节点不改变黑高，破坏的性质最少），再由 `fixAfterInsertion` 修复。

### 2. fixAfterInsertion：插入后平衡（变色 + 旋转）

新节点染红后可能出现「连续两个红」（违反性质 3），根据**叔叔节点颜色**分三种情况处理：

```java
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;                                  // 新节点先染红
    while (x != null && x != root && x.parent.color == RED) {
        if (parentOf(x) == leftOf(parentOf(parentOf(x)))) { // 父是祖父的左孩子
            Entry<K,V> y = rightOf(parentOf(parentOf(x)));  // y = 叔叔
            if (colorOf(y) == RED) {                // 情况1：叔叔红 → 只变色，问题上移
                setColor(parentOf(x), BLACK);
                setColor(y, BLACK);
                setColor(parentOf(parentOf(x)), RED);
                x = parentOf(parentOf(x));          // 祖父变红，继续向上检查
            } else {
                if (x == rightOf(parentOf(x))) {    // 情况2：叔叔黑 + 当前是右孩子（LR）
                    x = parentOf(x);
                    rotateLeft(x);                  // 先左旋转成情况3
                }
                setColor(parentOf(x), BLACK);       // 情况3：叔叔黑 + 左孩子（LL）
                setColor(parentOf(parentOf(x)), RED);
                rotateRight(parentOf(parentOf(x))); // 右旋 + 变色，结束
            }
        } else { /* 对称：父是祖父的右孩子，左右镜像 */ }
    }
    root.color = BLACK;                             // 性质2：根恒为黑
}
```

| 情况 | 叔叔颜色 | 处理 | 效果 |
|:---:|:---:|:---|:---|
| 1 | **红** | 父、叔变黑，祖父变红 | 冲突上移到祖父，循环继续 |
| 2 | **黑**（LR/RL） | 先旋转成情况 3 | 转为情况 3 |
| 3 | **黑**（LL/RR） | 父变黑、祖父变红 + 旋转 | 修复完成，退出 |

> 记忆口诀：**叔红只变色（问题上浮），叔黑靠旋转（一两次旋转封顶）**。插入最多 2 次旋转，删除最多 3 次旋转——都是 \(O(1)\) 次，整体仍 \(O(\log n)\)。

### 3. deleteEntry：删除（找后继 + 平衡修复）

删除是红黑树最复杂的操作，核心思路：

```java
private void deleteEntry(Entry<K,V> p) {
    modCount++; size--;
    // ① 若有两个孩子：找中序后继 s（右子树最左），把 s 的 key/value 拷到 p，
    //    转为删除只有 ≤1 个孩子的后继节点 s——问题降级
    if (p.left != null && p.right != null) {
        Entry<K,V> s = successor(p);
        p.key = s.key; p.value = s.value;
        p = s;
    }
    // ② 此时 p 至多一个孩子，用孩子 replacement 顶替
    Entry<K,V> replacement = (p.left != null ? p.left : p.right);
    if (replacement != null) {
        // 用孩子接管 p 的位置
        replacement.parent = p.parent;
        if (p.parent == null)          root = replacement;
        else if (p == p.parent.left)   p.parent.left = replacement;
        else                           p.parent.right = replacement;
        p.left = p.right = p.parent = null;
        if (p.color == BLACK)
            fixAfterDeletion(replacement); // ③ 删的是黑节点 → 黑高失衡，需修复
    } else if (p.parent == null) {
        root = null;                       // 树里只剩根
    } else {
        if (p.color == BLACK)
            fixAfterDeletion(p);           // 先修复再摘除
        // 摘除 p
        if (p.parent != null) {
            if (p == p.parent.left)  p.parent.left = null;
            else if (p == p.parent.right) p.parent.right = null;
            p.parent = null;
        }
    }
}
```

**要点**：
- 删「有两个孩子」的节点时，先用**中序后继**（右子树最左，key 紧邻的下一个）替换，把问题降级为删除至多一个孩子的节点。
- **删红节点不影响黑高**，无需修复；**删黑节点**会让某条路径少一个黑节点（违反性质 4），由 `fixAfterDeletion` 通过「借兄弟节点的黑」变色 + 旋转修复。

### 4. 旋转：所有平衡操作的基石

```java
// 左旋：把 p 的右孩子 r 提上来当父，p 变成 r 的左孩子
private void rotateLeft(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> r = p.right;
        p.right = r.left;                  // r 的左子树挂到 p 的右边
        if (r.left != null) r.left.parent = p;
        r.parent = p.parent;               // r 顶替 p 的位置
        if (p.parent == null)        root = r;
        else if (p.parent.left == p) p.parent.left = r;
        else                         p.parent.right = r;
        r.left = p;                        // p 成为 r 的左孩子
        p.parent = r;
    }
}
// rotateRight 与之完全镜像
```

> 旋转只改动**局部指针**，且**不破坏 BST 中序有序性**——这是它能安全用于平衡的前提。

### 5. 导航方法：getCeilingEntry 等

`ceilingKey(k)` = 「≥ k 的最小 key」，本质是带回溯的 BST 查找，\(O(\log n)\)：

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

`floorEntry`（≤ key 的最大）、`higherEntry`（> key 的最小）、`lowerEntry`（< key 的最大）同理，只是比较方向不同。`subMap/headMap/tailMap` 返回的是**视图**（不复制数据），对视图增删会反映到原 map。

---

## 五、复杂度分析

| 操作 | 时间复杂度 | 说明 |
|:---|:---:|:---|
| `put` / `get` / `remove` / `containsKey` | \(O(\log n)\) | 红黑树查找 + 平衡 |
| `firstKey` / `lastKey` | \(O(\log n)\) | 一路走到最左 / 最右 |
| `ceilingKey` / `floorKey` / `higherKey` / `lowerKey` | \(O(\log n)\) | 带回溯的 BST 查找 |
| `pollFirstEntry` / `pollLastEntry` | \(O(\log n)\) | 取并删极值 |
| 遍历（有序） | \(O(n)\) | 中序遍历 |
| `subMap` / `headMap` / `tailMap` | \(O(\log n)\) 建视图 | 返回**视图**，遍历才计元素数 |
| 空间 | \(O(n)\) | 每节点多存 parent 指针与颜色 |

> 相比 HashMap 平均 \(O(1)\)，TreeMap 慢一个 log，但换来**有序性 + 导航 + 范围查询**，且**没有扩容 rehash 的抖动**（key 数量大且需有序时更平稳）。

---

## 六、三种 Map 对比

| 维度 | `HashMap` | `LinkedHashMap` | **`TreeMap`** |
|:---|:---|:---|:---|
| 底层 | 数组 + 链表 + 红黑树 | HashMap + 双向链表 | **红黑树** |
| 顺序 | 无序 | 插入序 / 访问序（可做 LRU） | **按 key 排序** |
| 增删查 | 平均 \(O(1)\) | 平均 \(O(1)\) | \(O(\log n)\) |
| null key | 允许 1 个 | 允许 1 个 | **不允许** |
| 判等依据 | `hashCode` + `equals` | `hashCode` + `equals` | **`compareTo` / `Comparator`** |
| 独有能力 | 最快 | 保留访问/插入序 | **导航、范围查询、取两端极值** |

> **选型口诀**：默认最快用 `HashMap`；要保留插入/访问顺序（或做 LRU）用 `LinkedHashMap`；**要 key 有序 / 范围 / 最近邻查询用 `TreeMap`**。HashMap 源码见 [HashMap 的应用场景与源码分析](HashMap的应用场景和源码分析.md)。

---

## 七、易错点

### 1. key 判重用「比较结果」而非 `equals`

若 `compareTo`/`Comparator` 返回 0，TreeMap 就认为是**同一个 key**（即使 `equals` 为 false），后 put 会**覆盖**前者。务必让比较逻辑与 `equals` 语义一致，否则数据「诡异丢失」。

### 2. key 不能为 null

put 时 key 要参与比较，`null` 会抛 `NullPointerException`（空树放第一个 null 也会，因 `put` 里的 `compare(key,key)` 检查）。**value 可以为 null**。

### 3. key 必须可比较

存自定义对象当 key，要么让类 `implements Comparable`，要么构造时传 `Comparator`，否则 `put` 抛 `ClassCastException`。

### 4. 比较器不要用 `a - b`

整型相减可能**溢出**（如 `Integer.MIN_VALUE`），必须用 `Integer.compare(a, b)`；比较器还要满足**自反、对称、传递**，否则红黑树结构错乱。

### 5. `subMap` / `headMap` / `tailMap` 返回视图

对返回的子 map 增删会**反映到原 map**，且越界 put 会抛 `IllegalArgumentException`；注意区间**左闭右开**（`subMap(from, to)` 含 from 不含 to，重载版可指定闭合）。

### 6. 判存在用 `containsKey` 而非 `get() != null`

value 本身可能为 null，`get` 返回 null 无法区分「不存在」和「值为 null」。

### 7. 迭代中修改抛异常

遍历时直接 `put`/`remove` 触发 `ConcurrentModificationException`，用迭代器的 `remove` 或先收集后处理。并发场景用 `ConcurrentSkipListMap`。

---

## 八、面试重点

| 优先级 | 考点 | 一句话答法 |
|:---:|:---|:---|
| ⭐⭐⭐ | **底层结构** | 红黑树（自平衡 BST），无哈希无扩容；增删查恒 \(O(\log n)\) |
| ⭐⭐⭐ | **红黑树为何 \(O(\log n)\)** | 五条性质约束「最长路径 ≤ 2×最短路径」，树高稳定，避免 BST 退化成链 |
| ⭐⭐⭐ | **put 流程** | 按比较结果做 BST 查找定位 → 相等则覆盖 → 否则挂叶子染红 → `fixAfterInsertion` 变色/旋转修复 |
| ⭐⭐ | **插入平衡** | 看叔叔颜色：叔红只变色（问题上移）、叔黑靠旋转（最多 2 次），根恒黑 |
| ⭐⭐ | **删除平衡** | 两孩子先用中序后继替换降级；删黑节点破坏黑高，`fixAfterDeletion` 修复（最多 3 次旋转） |
| ⭐⭐ | **判重机制** | 靠 `compareTo`/`Comparator` 返回 0，**不看 equals/hashCode**——与 HashMap 的本质区别 |
| ⭐⭐ | **导航方法** | `ceiling/floor/higher/lower` 是带回溯的 BST 查找，\(O(\log n)\)，最近邻/范围查询杀手锏 |
| ⭐ | **HashMap vs TreeMap 选型** | 无序最快 HashMap，有序/范围 TreeMap；并发有序用 ConcurrentSkipListMap |
| ⭐ | **红黑树 vs AVL** | 红黑树旋转更少、增删更快（近似平衡）；AVL 更严格、查询略优。JDK 集合选红黑树 |

---

## 九、一句话总结

`TreeMap` = **红黑树支撑的按 key 有序的 Map**：所有操作都是红黑树上的 BST 查找 + 变色/旋转平衡，增删查与导航全部 \(O(\log n)\)——它靠 `compareTo`/`Comparator`（而非 `equals`）判重排序，插入靠「看叔叔颜色变色或旋转」、删除靠「后继替换 + 修复黑高」维持平衡；独有的 `ceilingKey/floorKey/subMap` 让它成为「排序映射 + 范围查询 + 最近邻」的首选，也是 [TreeSet](TreeSet的应用场景和源码分析.md) 的底层。不需要顺序就用 [HashMap](HashMap的应用场景和源码分析.md)，只需插入序就用 LinkedHashMap，并发有序用 ConcurrentSkipListMap。

---

## 相关链接

- 红黑树 = 自平衡 BST，性质与旋转见 [数据结构/二叉搜索树.md](../数据结构/二叉搜索树.md)
- 底层同为 TreeMap 的有序去重集合 [TreeSet](TreeSet的应用场景和源码分析.md)
- 平均 \(O(1)\) 的无序映射 [HashMap 的应用场景与源码分析](HashMap的应用场景和源码分析.md)
- 与「只取单极值」的堆对比见 [数据结构/优先队列.md](../数据结构/优先队列.md)
