# volatile 关键字面试题

> 本文是 volatile 的**面试问答与用法层**：三大特性、原子性反证、场景选型与易错点。内存屏障四类型、MESI、happens-before 的底层推导见 [../jvm/JMM内存模型.md](../jvm/JMM内存模型.md)，两篇互为表里。

---

## 一、面试第一题：volatile 保证什么？（30 秒速答版）

| 特性 | 保证？ | 一句话解释 |
|:---|:---:|:---|
| **可见性** | ✅ | 一个线程写，其他线程**立刻**能看到（写刷主存 + 读失效本地缓存） |
| **有序性** | ✅ | 禁止指令重排：volatile 写之前的操作不会重排到写之后，读之后不会重排到读之前 |
| **原子性** | ❌ | **`i++` 依然不安全**——volatile 只管「读到的值是最新的」，不管「读写是否连续完成」 |

> 标准话术：「volatile 是轻量级同步机制，保证可见性和有序性，**不保证原子性**；底层靠内存屏障实现，写后插 StoreLoad、读后插 LoadLoad/LoadStore（x86 上是 lock 前缀指令）。」

---

## 二、为什么不保证原子性？（必考代码反证）

```java
private static volatile int count = 0;

public static void main(String[] args) throws Exception {
    for (int t = 0; t < 2; t++) {
        new Thread(() -> {
            for (int i = 0; i < 10000; i++) count++;   // 结果 < 20000，且每次运行都不同
        }).start();
    }
}
```

`count++` 是**三步复合操作**（字节码层面）：

```
① getstatic    读 count 到操作数栈     （volatile 保证：读到的是最新值 ✓）
② iconst_1/iadd 栈上 +1                （纯寄存器操作，volatile 管不到 ✗）
③ putstatic    写回 count              （volatile 保证：写回立即可见 ✓）
```

两个线程的丢失更新时间线：

```
线程A：读 count=5  ──────加 1──────写 count=6
线程B：      读 count=5 ──加 1──写 count=6
                                ↑ 两次自增，最终 +1（B 的写覆盖了 A 的写）
```

- volatile 保证 A、B **读的时候都是 5**（可见性），但两人在各自「栈上」加完再写回，**后写覆盖先写**；
- 可见性解决「看不见」，原子性解决「被打断」——**这是两件事**。

### 替代方案选型

| 方案 | 原理 | 适用 |
|:---|:---|:---|
| `AtomicInteger.incrementAndGet()` | **CAS 自旋**（volatile value + Unsafe.compareAndSwapInt） | 中低并发计数 |
| `LongAdder` | 分段 Cell 累加，汇总求和 | **高并发计数首选**（热点分离） |
| `synchronized` | 互斥 | 复合逻辑/多变量 |
| `ReentrantLock` | AQS | 需要超时/可中断 |

> 原子类为什么能解决？`incrementAndGet` 是「读-比较-交换」在一个 **CAS 原语**里完成，失败就重试——把三步压成一步，而不是靠 volatile。

---

## 三、volatile vs synchronized（必考对比）

| 维度 | volatile | synchronized |
|:---|:---|:---|
| 原子性 | ❌ | ✅（互斥） |
| 可见性 | ✅ | ✅（解锁刷主存/加锁失效缓存） |
| 有序性 | ✅（屏障） | ✅（解锁前/加锁后的操作不外移） |
| 阻塞 | **不阻塞线程** | 阻塞（锁升级后重量级要挂起） |
| 作用对象 | 仅变量 | 方法/代码块（任意临界区） |
| 编译器优化 | 读写不被优化 | 锁消除/锁粗化 |
| 典型场景 | 状态标志、DCL、读多写少的单变量发布 | 复合操作、临界区、多变量一致性 |

**一句话：volatile 是「可见性开关」，synchronized 是「互斥保险箱」；volatile 不能替代锁**——它保证「每次读最新」，不保证「读改写不被打断」。

---

## 四、使用场景清单（答「什么时候用 volatile」）

| 场景 | 利用的特性 |
|:---|:---|
| **状态标志**：`volatile boolean running`，一个线程停、其他线程感知 | 可见性 |
| **DCL 双检锁单例**：`private static volatile Singleton instance` | 禁止重排（new 三步：①分配 ②初始化 ③赋引用，②③重排会让别人拿到半成品） |
| **一次性安全发布**：配置对象初始化完成后用 volatile 引用发布 | 可见性 + 禁止重排 |
| **独立观察**：周期性发布最新值（如心跳时间戳），读端容忍读到稍旧值 | 可见性 |
| **volatile bean**：POJO 全部字段 volatile + 不加锁读写 | 可见性 |

### 进阶加分点：volatile 的「传递可见性」（高频追问）

```java
private volatile Config config;          // 引用 volatile
private Config instance = new Config();  // 普通写：a=1, b=2（构造函数内）

// 写线程：
config = new Config();    // volatile 写：之前的普通写（构造器内 a=1,b=2）不许重排到它之后

// 读线程：
Config c = config;        // volatile 读：之后的普通读不会提前到它之前
int a = c.a;              // 一定能看到 1 —— happens-before 传递：构造写 → volatile 写 → volatile 读 → 普通读
```

> **volatile 写之前的所有写（含普通字段），对 volatile 读之后的所有读可见**——这就是 DCL 不加 volatile 字段也安全、而引用本身必须 volatile 的原因。答出「happens-before 传递性」即 P7 信号。

---

## 五、追问链速答（面试官连环问）

| 追问 | 速答 |
|:---|:---|
| volatile 底层怎么实现？ | 编译器生成字节码带 ACC_VOLATILE → JVM 插内存屏障 → x86 上 volatile 写是 `lock` 前缀指令（如 `lock addl $0`），兼做可见性（锁缓存行）与屏障 |
| 和 MESI 什么关系？ | MESI 是硬件缓存一致性协议（保证单缓存行最终一致），volatile 屏障是**强制立即生效+禁止重排**的软件约定，两者配合（详见 [JMM 笔记](../jvm/JMM内存模型.md)） |
| happens-before 规则哪条？ | **volatile 规则：volatile 写 happens-before 后续对同一变量的 volatile 读**；再靠程序顺序规则与传递性扩大可见范围 |
| volatile 数组安全吗？ | ❌ 坑：`volatile int[] arr` 只保证**引用**可见，**元素读写不 volatile**；需要元素级原子性用 `AtomicIntegerArray` |
| long/double 要 volatile 吗？ | 64 位变量在 32 位 JVM 可能撕裂读（两次 32 位访问）；64 位 JVM 商用实现按原子处理，但规范允许非原子——跨平台严谨场景加 volatile |
| AtomicBoolean 内部是什么？ | 就是 `private volatile int value` + CAS——**volatile 保证可见，CAS 保证原子**，两者组合才是完整原子操作 |

---

## 易错点

| 坑 | 说明 |
|:---|:---|
| 「加了 volatile 线程就安全了」 | 只对**单一变量的读写**成立；`count++`/先检查后执行（check-then-act）都不行 |
| volatile 引用 ≠ 内部字段立即可见 | 修改 `volatile User u` 指向对象的**内部字段**（`u.age++`）不经过 volatile 读写，无保证；除非字段本身 volatile |
| DCL 漏 volatile | 无 volatile 时 ②③重排，拿到未初始化对象——高频手撕题陷阱 |
| 用 volatile 做「读-改-写」计数 | 该用 AtomicLong/LongAdder（见第二节选型表） |
| 认为 volatile 变量读写比普通变量「一样快」 | 读大致同价，**写更贵**（StoreLoad 全能屏障 + 缓存行失效）；热点写路径别滥用 |
| volatile 替代锁实现同步器 | 无原子性 + 无互斥，只能做状态位/发布，不能做互斥 |

---

## 一句话总结

**volatile = 可见性 + 禁止重排，不保证原子性**：`i++` 失败在「读改写三步被打断」，替代方案是 CAS 原子类/LongAdder；场景锁定「状态标志、DCL、安全发布」；底层是内存屏障（x86 `lock` 前缀），配合 happens-before 的**传递性**让 volatile 写之前的所有普通写对读方可见——这三层（特性 → 原理 → 传递性）答全就是满分。

## 相关笔记

- 内存屏障四类型与插入位置 → [../jvm/JMM内存模型.md](../jvm/JMM内存模型.md)
- CAS / LongAdder / AQS 的锁替代方案 → [JUC 并发包面试题](JUC并发包.md)
- DCL 与静态内部类单例对比 → [Object 与关键字面试题](Object与关键字面试题.md)、[../jvm/类加载机制.md](../jvm/类加载机制.md)
- ConcurrentHashMap 里的 volatile 用法（Node.val） → [ConcurrentHashMap 的应用场景和源码分析](ConcurrentHashMap应用场景和源码分析.md)
