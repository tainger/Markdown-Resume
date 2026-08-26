# ThreadLocal 源码分析

> `ThreadLocal` 是面试**高频但易答错**的题：它**不是**「线程安全的共享容器」，而是「每个线程一份独立副本」；map **不在 `ThreadLocal` 里，而在 `Thread` 里**——`ThreadLocal` 只是 key；`Entry` 的 key 是弱引用、value 是强引用，线程池下不 `remove` 就会内存泄漏 + 数据串号。本文从源码层面讲清 `set/get/remove`、`ThreadLocalMap` 的线性探测与被动清理、泄漏原理、`InheritableThreadLocal` 在线程池失效的根因。总结级速览见 [JUC 并发包 · 十五、ThreadLocal](JUC并发包.md#十五threadlocal)。

---

## 一、ThreadLocal 是什么 / 不是什么（先破三大误区）

| 常见误解 | 正确理解 |
|:---|:---|
| ❌ 「ThreadLocal 是线程安全的共享 Map」 | ✅ 它是**线程隔离**的：每个线程各存一份自己的值，互不可见 |
| ❌ 「值存在 ThreadLocal 对象里」 | ✅ 值存在 `Thread.threadLocals` 里，`ThreadLocal` 只是访问用的 **key** |
| ❌ 「ThreadLocal 用来解决并发竞争」 | ✅ 它**避开**竞争（各存各的），`synchronized` 才是「共享+互斥」 |

```java
// 用法：每个线程调 get() 拿到的是自己 set() 进去的值
ThreadLocal<User> userHolder = new ThreadLocal<>();
userHolder.set(currentUser);   // 线程 A 存 A 的 user
userHolder.get();               // 线程 B 拿到的是 B 自己存的，不是 A 的
```

> 💡 vs `synchronized`：`synchronized` = 「**一个柜台，排队进**」；`ThreadLocal` = 「**每人一个专属柜台，不用排**」。一个共享+互斥，一个隔离+无锁。

---

## 二、底层结构：Thread → ThreadLocalMap → Entry

```
Thread 对象（每个线程一个）
  └── ThreadLocalMap threadLocals          ← 字段在 Thread 上，不在 ThreadLocal 上！
        └── Entry[] table                  ← 开放寻址数组（不是链表数组）
              └── Entry extends WeakReference<ThreadLocal<?>>
                    │  key   = ThreadLocal 实例（弱引用）
                    └─ value = 实际值（强引用）
```

**关键源码**：

```java
// Thread.java —— map 持有者在 Thread 上
ThreadLocal.ThreadLocalMap threadLocals = null;
ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;   // 给 InheritableThreadLocal 用

// ThreadLocal.ThreadLocalMap —— 内部类
static class ThreadLocalMap {
    static class Entry extends WeakReference<ThreadLocal<?>> {
        Object value;                       // ⚠️ value 是强引用
        Entry(ThreadLocal<?> k, Object v) {
            super(k);                       // ⚠️ key 用 WeakReference 包，弱引用
            value = v;
        }
    }
    private static final int INITIAL_CAPACITY = 16;
    private Entry[] table;
    private int threshold;                   // 默认 2/3 负载因子
}
```

> **为什么 key 弱、value 强**：key（ThreadLocal 实例）弱引用 → 外部没强引用时 ThreadLocal 可被 GC，避免 ThreadLocal 对象本身泄漏；但 value 是业务值，强引用是设计取舍（见 [五、泄漏原理](#五内存泄漏原理p7-高频深挖)）。

---

## 三、核心源码分析

### 3.1 set：拿到当前线程的 map，把「自己(key) → value」放进去

```java
// ThreadLocal.java
public void set(T value) {
    Thread t = Thread.currentThread();          // ① 当前线程
    ThreadLocalMap map = getMap(t);              // ② 拿 t.threadLocals
    if (map != null)
        map.set(this, value);                    // ③ 已有 map → 放进去（this 作 key）
    else
        createMap(t, value);                     // ④ 没 map → 首次创建
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;                       // map 在 Thread 上
}

void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap(this, firstValue);
}
```

### 3.2 get：找不到时用 `initialValue()` 初始化

```java
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) return (T) e.value;       // 命中
    }
    return setInitialValue();                     // 没命中 → 调 initialValue() 初始化并写入
}

private T setInitialValue() {
    T value = initialValue();                     // 默认返回 null，子类可重写（withInitial 也能给初值）
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) map.set(this, value);
    else createMap(t, value);
    return value;
}
```

> 💡 `withInitial(Supplier)` = 把「`initialValue` 返回初值」包成构造器调用，写一次初值，多次 get 免判空。

### 3.3 remove：手动摘掉 Entry（防泄漏的关键）

```java
public void remove() {
    ThreadLocalMap m = getMap(Thread.currentThread());
    if (m != null) m.remove(this);                // 内部会把 Entry 的 value 置 null、table[i] 置 null
}
```

### 3.4 哈希魔数 `0x61c88664`（黄金分割比）

```java
private final int threadLocalHashCode = nextHashCode();
private static final AtomicInteger nextHashCode = new AtomicInteger();
private static final int HASH_INCREMENT = 0x61c88664;       // 斐波那契哈希黄金分割

private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);           // 每个 ThreadLocal 递增一个步长
}
```

`0x61c88664` 是 `(2^32 − 1) / φ` 附近的魔数，让连续递增的 hash 落位**均匀分散**到 `& (len-1)` 的桶里——`ThreadLocalMap` 用开放寻址（线性探测），均匀分布能把冲突降到最低。这是 ThreadLocalMap **不用链表也能低冲突**的根本原因。

### 3.5 线性探测（开放寻址，不是链表）

```java
private static int nextIndex(int i, int len) { return ((i + 1 < len) ? i + 1 : 0); }   // 往后转一圈
private static int prevIndex(int i, int len) { return ((i - 1 >= 0) ? i - 1 : len - 1); }
```

冲突时不挂链表，而是**顺延到下一个空槽**。配合黄金分割哈希 + 被动清理，冲突率低、实现简单、内存紧凑（无指针开销）。

`ThreadLocalMap.set` 线性探测主循环（简化）：

```java
private void set(ThreadLocal<?> key, Object value) {
    Entry[] tab = table;  int len = tab.length;
    int i = key.threadLocalHashCode & (len - 1);          // 定位桶

    for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();
        if (k == key) { e.value = value; return; }        // ① 同一个 key → 覆盖
        if (k == null) {                                  // ② key 被回收（弱引用失效）
            replaceStaleEntry(key, value, i); return;      //    替换这个过期槽（顺带清理）
        }
        // ③ 否则：槽被占了 → 线性探测下一个
    }
    tab[i] = new Entry(key, value);
    int sz = ++size;
    if (!cleanSomeSlots(i, sz) && sz >= threshold) rehash();  // ④ 清理 + 判扩容
}
```

---

## 四、ThreadLocalMap 的被动清理机制

ThreadLocalMap **没有后台线程清理**，只在 `get/set/remove` 触碰时「顺手」清掉 key=null 的过期 Entry。三个清理方法：

| 方法 | 触发点 | 作用 |
|:---|:---|:---|
| `expungeStaleEntry(i)` | `get` 未命中、`set` 替换过期槽时 | 清 `i` 槽，并往后扫直到空槽：遇 key=null 清掉，遇 key!=null 重新哈希前移填补空缺 |
| `cleanSomeSlots(i, n)` | `set` 插入后 | 对数级扫描（`n >>> 2` 步）探测并清过期 Entry |
| `replaceStaleEntry` | `set` 命中过期槽时 | 用新 entry 替换过期槽，并前扫后扫清一批 |

**关键推论**：因为只有「触碰」才清理，**如果一个 ThreadLocal 设了值之后再也不碰它、线程又长期活着**（线程池），那个 value 就永远不会被回收 → 泄漏。这是 [五](#五内存泄漏原理p7-高频深挖) 的根因。

---

## 五、内存泄漏原理（P7 高频深挖）

### 5.1 泄漏链路

```
Thread（线程池里长期存活）
  └── threadLocals (强) → Entry[] table
        └── Entry
              ├── key  = WeakReference<ThreadLocal>   ← ThreadLocal 外部无强引用时被 GC，key 变 null
              └── value = 强引用业务对象               ← key 没了，value 还强引用着 → 漏！
```

1. `ThreadLocal tl = new ThreadLocal()` 局部变量出栈后，外部无强引用 → ThreadLocal 对象被 GC → Entry.key 变 `null`。
2. 但 Entry.value 仍强引用业务对象 → 只要 **Thread 不结束**，value 永不回收。
3. **线程池线程复用且长生命周期** → value 不断累积 → 内存泄漏。

### 5.2 为什么不把 value 也设成弱引用？

> 看似对称，实则没用：value 弱引用意味着「ThreadLocal 还活着、但 value 被 GC 随时回收」——`tl.get()` 返回 null，ThreadLocal 形同虚设。泄漏的根因不是「key 弱 value 强」，而是「**线程不死 + 不再触碰**」。解法不是改引用强度，而是**用完 `remove`**。

### 5.3 线程池下不 remove 的另一个坑：数据串号

线程池线程被复用：任务 A `set(userA)` 后没 `remove`，任务 B 复用该线程 `get()` 拿到 `userA` → 业务串号、越权。

### 5.4 正确用法

```java
ThreadLocal<User> userHolder = new ThreadLocal<>();
try {
    userHolder.set(currentUser);
    // 业务逻辑，链路里随时 userHolder.get()
} finally {
    userHolder.remove();   // ⭐ 唯一正解：finally 必 remove，防泄漏 + 防串号
}
```

> 易错点 #4 详述：哪怕 value 是弱引用也救不了，**`remove` 是唯一解**。

---

## 六、InheritableThreadLocal & TransmittableThreadLocal（线程池传值）

### 6.1 InheritableThreadLocal：只在「构造子线程」时复制

```java
// Thread 构造函数里（简化）
if (inheritThreadLocals && parent.inheritableThreadLocals != null) {
    this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
}
// createInheritedMap 对每个 entry 调 childValue(parentValue) → 子线程拿到父线程当时的值副本
```

`InheritableThreadLocal` 重写了 `getMap` 指向 `t.inheritableThreadLocals`，子线程构造时**复制父线程**的 inheritable 表。

### 6.2 为什么在线程池下失效

```
ThreadPoolExecutor 预创建 corePool 数个线程：
  线程池线程 X 的构造发生在「池创建时」→ 复制的是「池创建线程」的 inheritableThreadLocals
  任务提交时：复用现成的 X → 不会重新复制「提交任务线程」的值
  → 提交任务线程 set 的 InheritableThreadLocal，X 拿不到 ❌
```

**根因**：`InheritableThreadLocal` 的复制时机是 `new Thread()`，而线程池线程是**预创建**的、提交任务时**不再 `new`** → 不会复制提交者的值。

### 6.3 TransmittableThreadLocal（阿里 TTL）：线程池下也能传

TTL 的解法 = **捕获-重放**：提交任务时捕获当前线程的 TTL 值，`run` 前回放到工作线程，`run` 后还原。

```java
// 用 TtlExecutors 包装线程池，或用 TtlRunnable.get 包装任务
ExecutorService pool = TtlExecutors.getTtlExecutorService(originalPool);
pool.submit(() -> {
    // 这里能正确拿到提交任务的 TTL 值 ✅
});
```

| 方案 | 普通线程 | 线程池（预创建复用） | 跨线程池 |
|:---|:---:|:---:|:---:|
| `ThreadLocal` | ❌ | ❌ | ❌ |
| `InheritableThreadLocal` | ✅（构造时复制） | ❌（提交时不再构造） | ❌ |
| `TransmittableThreadLocal` | ✅ | ✅（捕获-重放） | ✅ |

> 链路追踪 traceId、MDC 日志上下文、用户上下文跨线程池传递 → **必须用 TTL**，`InheritableThreadLocal` 在线程池下失效。

---

## 七、典型应用场景

| 场景 | 用法 | 为什么用它 |
|:---|:---|:---|
| **链路追踪 / MDC 日志** | 存 traceId / requestId | 跨方法无参透传；线程池下用 TTL |
| **用户上下文** | 存 currentUser | Controller 拦截后任意层 get，免层层透传 |
| **SimpleDateFormat 线程安全** | 每线程一个 df | SimpleDateFormat 非线程安全，synchronized 慢 |
| **DB 连接 / Session 管理** | 每线程一个 Connection | 事务绑定当前线程，避免跨线程共用连接 |
| **线程隔离缓存** | 每线程一份本地缓存 | 无锁、无竞争 |

---

## 八、面试重点（Q&A 速答）

| 优先级 | 题目 | 一句话答法 |
|:---:|:---|:---|
| ⭐⭐⭐ | **ThreadLocal 是什么？解决什么问题？** | 线程隔离的变量副本，每个线程一份自己的值，**避开竞争**而非共享互斥 |
| ⭐⭐⭐ | **底层结构？map 在哪？** | map 在 `Thread.threadLocals`，**不在 ThreadLocal**；ThreadLocal 只是 key |
| ⭐⭐⭐ | **key 和 value 的引用强度？为什么这么设计？** | key 弱引用（ThreadLocal 可被 GC）、value 强引用；key 弱是为了 ThreadLocal 对象本身不泄漏 |
| ⭐⭐⭐ | **内存泄漏怎么产生的？** | key 被 GC 后 key=null 但 value 仍强引用 + 线程不死（线程池）→ value 永不回收 |
| ⭐⭐⭐ | **怎么解决泄漏/串号？** | `try-finally` 里 `remove()`；value 改弱引用也没用（见 [5.2](#52-为什么不把-value-也设成弱引用)） |
| ⭐⭐ | **清理机制是什么？** | `ThreadLocalMap` 无后台线程，`get/set/remove` 时顺手清 key=null 的 Entry（`expungeStaleEntry` 等）→ 不碰就漏 |
| ⭐⭐ | **哈希魔数 0x61c88664 是什么？** | 黄金分割比，让连续 ThreadLocal 的 hash 均匀落桶，配合线性探测降冲突 |
| ⭐⭐ | **冲突用链表还是开放寻址？** | 开放寻址（线性探测），不挂链表；表小、无指针开销、被动清理方便 |
| ⭐⭐ | **InheritableThreadLocal vs TTL？** | Inheritable 只在 `new Thread` 时复制 → 线程池预创建线程拿不到提交者值；TTL 用捕获-重放，线程池下可用 |
| ⭐⭐ | **InheritableThreadLocal 在线程池为什么失效？** | 复制时机是线程构造时，线程池线程预创建、提交任务时不再构造 → 不会复制提交者值 |
| ⭐ | **和 synchronized 区别？** | synchronized = 共享+互斥（一柜台排队）；ThreadLocal = 隔离（每人一柜台无锁） |

---

## 九、易错点 ⚠️

| # | 易错点 | 正确理解 |
|:---:|:---|:---|
| 1 | 「ThreadLocal 自己持有 map」 | ❌ map 在 `Thread.threadLocals`，ThreadLocal 只是 key。所以「同一个 ThreadLocal 对象被多线程 get/set，不会有并发问题」——各访问各的 map |
| 2 | 「value 也能被 GC 自动回收」 | ❌ value 是强引用，ThreadLocal 被回收后 value 仍被 Entry 强引用，只有 `remove` 或 Thread 死亡才回收 |
| 3 | 「把 value 改成弱引用就解决泄漏」 | ❌ 没用，会让 `get()` 返回 null（见 [5.2](#52-为什么不把-value-也设成弱引用)）。正解是 `finally remove` |
| 4 | 「线程池下 set 完不用 remove 也行」 | ❌ 线程复用 → 下个任务 `get` 拿到上个任务的值 → **数据串号/越权** + value 泄漏 |
| 5 | 「InheritableThreadLocal 线程池也能传」 | ❌ 只在 `new Thread()` 复制；线程池预创建 → 失效。要 TTL |
| 6 | 「ThreadLocal 能替代加锁」 | ❌ 只适合「每线程各存各的」场景；共享可变状态还是要 `synchronized`/`ReentrantLock` |
| 7 | 「`initialValue` 和 `withInitial` 一样」 | 都给初值，但 `withInitial(Supplier)` 用 lambda 写一次免重写子类；`initialValue` 是 `protected` 方法需子类继承 |

---

## 十、一句话总结

`ThreadLocal` 是**线程隔离**变量——map 持有者在 `Thread`，`ThreadLocal` 只作 key；`Entry` 用**弱 key + 强 value** + **黄金分割哈希 + 线性探测**实现低冲突开放寻址；**无后台清理**，依赖 `get/set/remove` 被动清，线程池下不 `remove` 必泄漏+串号；`InheritableThreadLocal` 仅在 `new Thread` 时复制、线程池失效，跨池传值要用 **TTL**。工程铁律：**`try-finally remove`**。

---

## 相关链接

- 总结级速览见 [JUC 并发包 · 十五、ThreadLocal](JUC并发包.md)
- 线程池七参数与拒绝策略见 [JUC 并发包](JUC并发包.md)
- AQS / CAS / volatile 见 [JUC 并发包](JUC并发包.md)
