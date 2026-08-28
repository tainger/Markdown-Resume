# JUC 并发包面试题

JUC（`java.util.concurrent`）是 Java 并发编程的"兵器库"：显式锁、原子变量、线程池、阻塞队列、同步工具一应俱全。理解它绕不开三大基石——**CAS + AQS + volatile/JMM**。本文按面试考察频度梳理。

> JMM（可见性/有序性/`happens-before`）是并发理论基础，见 [../jvm/JMM内存模型.md](../jvm/JMM内存模型.md)，本文聚焦 JUC 工具本身。

---

## 一、JUC 全景图

| 类别 | 代表类 | 解决什么问题 |
|:---|:---|:---|
| **显式锁** | `ReentrantLock`、`ReentrantReadWriteLock`、`StampedLock` | 比 `synchronized` 更灵活（可中断/超时/公平/多条件） |
| **同步器框架** | `AQS`（抽象队列同步器） | 半成品框架，`ReentrantLock`/`Semaphore`/`CountDownLatch` 都基于它 |
| **原子类** | `AtomicInteger`、`LongAdder`、`AtomicReference` | 无锁线程安全计数/引用更新，比加锁轻 |
| **并发容器** | `ConcurrentHashMap`、`CopyOnWriteArrayList`、`ConcurrentSkipListMap` | 并发下安全读写，替代同步的 `Collections.synchronizedXxx` |
| **阻塞队列** | `ArrayBlockingQueue`、`LinkedBlockingQueue`、`SynchronousQueue`、`DelayQueue` | 生产者-消费者解耦、线程池任务队列 |
| **同步工具** | `CountDownLatch`、`CyclicBarrier`、`Semaphore`、`Exchanger` | 协调多线程的"等齐/限流/交换" |
| **执行框架** | `ThreadPoolExecutor`、`ForkJoinPool`、`CompletableFuture` | 线程池 + 异步编排 |
| **并发原语** | `ThreadLocal`、`ThreadLocalRandom` | 线程隔离变量、无竞争随机数 |

---

## 二、synchronized vs Lock（高频必考）

`synchronized` 是关键字（JVM 层面），`Lock` 是接口（API 层面，基于 AQS）。两者核心差异：

| 维度 | `synchronized` | `Lock`（以 `ReentrantLock` 为例） |
|:---|:---|:---|
| **实现** | JVM 内置，对象头 Mark Word + Monitor | AQS 队列 + CAS |
| **释放锁** | 自动释放（出代码块/异常） | **必须手动 `finally { lock.unlock(); }`** |
| **可中断** | 不可中断（等锁时不响应 `interrupt`） | `lockInterruptibly()` 可响应中断 |
| **超时获取** | 不支持 | `tryLock(time, unit)` 支持超时 |
| **公平性** | 非公平 | 可选公平 `new ReentrantLock(true)` |
| **条件变量** | 1 个（`wait/notify`） | 多个 `Condition`，分组等待唤醒 |
| **锁状态** | 不可查 | `isLocked()`、`getHoldCount()`、`hasQueuedThreads()` |
| **死锁诊断** | 只能 jstack | 可编程判断 `hasQueuedThreads()` |
| **性能** | JDK6 后经偏向锁/轻量级锁优化，差距很小 | 大量竞争下略优，但 API 开销略大 |

> 选型：**简单同步优先 `synchronized`**（简洁、JVM 优化足、不会漏 `unlock`）；**需要可中断/超时/多条件/公平/编排**时才上 `Lock`。

### synchronized 的锁升级（JDK6+）

```
无锁 ──(单线程进入)──> 偏向锁 ──(第二个线程竞争)──> 轻量级锁(自旋CAS)
                                                       │
                                                  (自旋失败/多线程持续竞争)
                                                       ▼
                                                   重量级锁(Monitor)
```

| 状态 | Mark Word 标记 | 适用 |
|:---|:---|:---|
| **偏向锁** | 存线程 ID | 单线程重复进入，几乎零成本 |
| **轻量级锁** | 指向栈中锁记录 | 两线程交替，用 CAS 避免阻塞 |
| **重量级锁** | 指向 Monitor 对象 | 多线程长竞争，OS 层互斥，最重 |

> JDK15 起偏向锁默认关闭（争议大、维护成本高）。对象头细节见 [../jvm/对象与内存分配.md](../jvm/对象与内存分配.md)。

---

## 三、ReentrantLock：可重入、可中断、公平

`Reentrant` = 同一线程可多次获取同一把锁（计数器累加），需等量释放。`ReentrantLock` 是 AQS 独占模式的典型实现。

```java
ReentrantLock lock = new ReentrantLock(false);  // true=公平
lock.lock();
try {
    // 临界区：同线程再次 lock() 不会死锁，holdCount++
} finally {
    lock.unlock();   // 必须 finally 释放，否则死锁
}
```

### 公平 vs 非公平

| 模式 | 加锁方式 | 优点 | 缺点 |
|:---|:---|:---|:---|
| **非公平**（默认） | 上来直接 CAS 抢，抢不到才入队 | 吞吐高（省一次唤醒开销） | 可能"饿死"队列线程 |
| **公平** | 先看队列有没有前驱，有就排队 | 不饿死，稳定 | 每次多一次 `hasQueuedPredecessors()` 检查，吞吐略低 |

> 非公平吞吐高是因为**线程刚释放锁时，下一个线程恰好唤醒期间，新线程能"插队"**，省了唤醒切换的等待。

### 三种加锁 API

| API | 行为 | 典型场景 |
|:---|:---|:---|
| `lock()` | 阻塞等待，不响应中断 | 普通临界区 |
| `lockInterruptibly()` | 等待时被中断抛 `InterruptedException` | 可取消任务 |
| `tryLock()` / `tryLock(t,u)` | 立即返回 / 超时返回，不阻塞 | 试探性加锁、限时等待 |

---

## 四、读写锁与 StampedLock

读多写少场景，读读不互斥可大幅提升并发。

### 1. ReentrantReadWriteLock

| 模式 | 互斥关系 | 可重入 |
|:---|:---|:---|
| **读锁（共享）** | 读-读不互斥，读-写互斥 | 同线程可多次 `readLock().lock()` |
| **写锁（独占）** | 写-任何互斥 | 同线程可多次 `writeLock().lock()` |

- **支持锁降级**：持有写锁 → 再获取读锁 → 释放写锁。常用于"写完立刻读自己写的数据，避免被别的写线程抢走"。
- **不支持锁升级**：持读锁再申请写锁会死锁（写锁要等所有读锁释放，包括自己）。

```java
ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
rwl.writeLock().lock();
try { /* 写 */ } finally { rwl.writeLock().unlock(); }
rwl.readLock().lock();
try { /* 读 */ } finally { rwl.readLock().unlock(); }
```

### 2. StampedLock（JDK8+，乐观读）

`ReentrantReadWriteLock` 的痛点：**读锁会阻塞写锁**，写饥饿风险。`StampedLock` 增加"乐观读"——不加读锁，事后校验写期间没人改过，没改直接用。

```java
StampedLock sl = new StampedLock();
long stamp = sl.tryOptimisticRead();      // 乐观读，不阻塞写
int x = data;
if (!sl.validate(stamp)) {               // 校验期间是否有写
    stamp = sl.readLock();                // 失败则升级悲观读锁
    try { x = data; } finally { sl.unlockRead(stamp); }
}
```

| 特性 | ReentrantReadWriteLock | StampedLock |
|:---|:---|:---|
| 乐观读 | ❌ | ✅ 读不加锁，吞吐高 |
| 可重入 | ✅ | ❌（同线程重复加锁会死锁） |
| 支持 Condition | ✅ | ❌ |
| 适用 | 通用读多写少 | 读极多写极少、追求极致吞吐 |

> `StampedLock` 不可重入，不能在中递归调用里用；不要调用 `tryOptimisticRead()` 后修改数据。

---

## 五、AQS（AbstractQueuedSynchronizer）—— 核心考点

AQS 是 JUC 同步器的**半成品框架**：把"等锁的线程排成 FIFO 队列 + 用 `volatile int state` 表示同步状态"这套骨架做好，子类只需实现"怎么抢/怎么释放"。

### 1. 三大组件

```
              state (volatile int)         CLH 变体等待队列（双向）
                 │                              │
                 │  含义随子类变化                 │  head → null (dummy) ⇄ Node ⇄ Node ⇄ ... ⇄ tail
                 │  ReentrantLock: 重入次数       │  每个 Node 封装一个等待线程 + 状态（CANCELLED/
                 │  Semaphore: 剩余许可            │           SIGNAL/CONDITION/PROPAGATE）
                 │  CountDownLatch: 剩余计数      │
```

| 组件 | 作用 |
|:---|:---|
| **`state`** | `volatile int`，表示同步状态。子类用 `getState()/compareAndSetState()` 安全修改 |
| **CLH 队列** | 变体的双向链表，抢锁失败的线程封装成 `Node` 入队，自旋 + `LockSupport.park()` 阻塞 |
| **CAS** | `Unsafe.compareAndSwapInt` 无锁修改 `state`，保证原子入队/抢锁 |

### 2. 两种模式

| 模式 | 子类钩子方法 | 代表 |
|:---|:---|:---|
| **独占**（Exclusive） | `tryAcquire/tryRelease` | `ReentrantLock`、`ReentrantReadWriteLock`（写锁） |
| **共享**（Shared） | `tryAcquireShared/tryReleaseShared` | `Semaphore`、`CountDownLatch`、读锁、`CyclicBarrier`（内部用 `ReentrantLock+Condition`） |

### 3. ReentrantLock 非公平抢锁流程（简化）

```java
// 非公平 tryAcquire
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {                                  // 无人占用
        if (compareAndSetState(0, acquires)) {    // CAS 抢
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {   // 重入
        setState(c + acquires);                    // 重入计数+1
        return true;
    }
    return false;                                  // 抢失败，AQS 框架会入队阻塞
}
```

> **公平 vs 非公平的唯一差别**：公平的 `tryAcquire` 在 `c == 0` 分支前多一句 `if (hasQueuedPredecessors()) return false;`——队列有人就乖乖排队，不插队。

### 4. 基于AQS的同步工具一览

| 工具 | 模式 | state 语义 |
|:---|:---|:---|
| `ReentrantLock` | 独占 | 重入次数 |
| `ReentrantReadWriteLock` | 独占+共享 | 高16位读次数 / 低16位写次数 |
| `Semaphore` | 共享 | 剩余许可数 |
| `CountDownLatch` | 共享 | 剩余计数（减到0唤醒所有等待者） |

> 面试常被追问"AQS 原理"，回答框架：①`volatile state` ②CLH 双向队列 ③CAS 抢锁入队 ④`park/unpark` 阻塞唤醒 ⑤独占/共享两种模式 + 钩子方法。

---

## 六、Condition 条件变量

`Condition` 是 `Object.wait/notify` 的升级版——一个锁可挂**多个等待队列**，分组唤醒，避免"虚假唤醒/唤醒错对象"。

```java
Lock lock = new ReentrantLock();
Condition notFull = lock.newCondition();   // 队列未满
Condition notEmpty = lock.newCondition(); // 队列未空

lock.lock();
try {
    while (队列满) notFull.await();       // 释放锁 + 等待
    入队;
    notEmpty.signal();                    // 唤醒一个等待"非空"的线程
} finally { lock.unlock(); }
```

| 对比 | `wait/notify` | `Condition` |
|:---|:---|:---|
| 依赖 | `synchronized` | `Lock` |
| 等待队列数量 | 1 个 | 多个（每 `newCondition()` 一个） |
| 精确唤醒 | ❌ 只能随机/全部 | ✅ 按 `Condition` 分组唤醒 |
| 超时/不响应中断 | 有限支持 | `await(time)`、`awaitUninterruptibly()` 更全 |

> `BlockingQueue` 的实现（如 `ArrayBlockingQueue`）正是用 `ReentrantLock + 两个 Condition` 精确控制"满了等 notFull，空了等 notEmpty"。

---

## 七、CAS 与原子类

### 1. CAS（Compare And Swap）

```
compareAndSwap(obj, offset, expected, newValue)
  if (obj 的该字段 == expected)  原子地写入 newValue，返回 true
  else                          返回 false（说明有人改过）
```

- 无锁原子操作，靠 CPU `cmpxchg` 指令保证原子性。
- Java 通过 `sun.misc.Unsafe`（JDK9+ 是 `VarHandle`）调用。
- 典型模式：**自旋 CAS** 直到成功。

### 2. 原子类家族

| 类别 | 类 | 用途 |
|:---|:---|:---|
| 基本类型 | `AtomicInteger`、`AtomicLong`、`AtomicBoolean` | 计数/标志 |
| 引用 | `AtomicReference`、`AtomicStampedReference` | 对象引用更新 + 版本号 |
| 数组 | `AtomicIntegerArray` 等 | 数组元素的原子更新 |
| 字段更新器 | `AtomicIntegerFieldUpdater` | 用反射原子更新 `volatile` 字段 |
| 高性能计数 | `LongAdder`、`LongAccumulator` | 高并发计数替代 `AtomicLong` |
| 累加器 | `DoubleAdder` | 双精度累加 |

### 3. AtomicInteger 源码片段

```java
private volatile int value;   // volatile 保证可见性

public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);  // 自旋 CAS
}
// 内部：do { v = getInt(...); } while (!compareAndSwapInt(this, offset, v, v+1));
```

> 记忆：**`volatile` 保证可见性，`CAS` 保证原子性**，二者配合才安全。

---

## 八、ABA 问题与 LongAdder

### 1. ABA 问题

CAS 只判断"值是否变了"，但"值从 A 变 B 再变回 A"也会被当作"没变"。若业务关心"中间是否被动过"，ABA 会出错。

```
线程1 读到 A，准备 CAS(A→C)
线程2 把 A → B → A（中途动过）
线程1 CAS(A→C) 成功，但忽略了"中间被改"的事实
```

**解决**：加版本号/邮戳。`AtomicStampedReference` 每次更新带 `stamp++`：

```java
AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(100, 0);
int stamp = ref.getStamp();
Integer old = ref.getReference();
// ... 判断逻辑
ref.compareAndSet(old, 200, stamp, stamp + 1);  // 同时比对值和版本
```

> 实际工程：纯计数场景 ABA 无害；**栈/链表节点复用、事务版本**等关心历史的场景必须加版本号。

### 2. LongAdder：高并发计数神器

`AtomicLong` 高并发下所有线程 CAS 同一个 `value`，失败率高、自旋严重。`LongAdder` 把计数**分散到多个 `Cell`**，最后求和。

```
        LongAdder
   ┌──────────┬──────────┬──────────┐
   │  base    │ Cell[0]  │ Cell[1]  │ ...   (按 hash 路由)
   └──────────┴──────────┴──────────┘
   sum() = base + Σ cell[i]
```

- 写：先尝试 CAS `base`，失败就路由到某个 `Cell` 上 CAS，热点分散。
- 读：`sum()` 遍历求和（非原子，仅适合"最终一致"的统计场景）。

| 维度 | `AtomicLong` | `LongAdder` |
|:---|:---|:---|
| 低并发 | 略快 | 略慢（多一次路由） |
| 高并发 | 自旋严重，吞吐下降 | 热点分散，吞吐数倍提升 |
| 读取 | \(O(1)\) 原子 | \(O(n)\) 非原子求和 |
| 场景 | 计数 + 精确读 | 纯计数统计（QPS/PV） |

---

## 九、ConcurrentHashMap

并发版的 `HashMap`。JDK7 vs JDK8 实现差异巨大，是面试重灾区。

### 1. JDK7：Segment 分段锁

```
ConcurrentHashMap
 └ Segment[16]（每个 Segment 是一把 ReentrantLock）
     └ HashEntry[]（每个桶）
         └ 链表节点
```

- 并发度 = Segment 数（默认16），同一Segment内串行，不同Segment并行。
- 写：定位 Segment → 加锁 → 改 → 解锁。

### 2. JDK8：CAS + synchronized + 链表/红黑树

抛弃 Segment，回归"数组+链表+红黑树"，但用 **CAS + 桶头节点 synchronized** 控制并发。

| 操作 | 加锁策略 |
|:---|:---|
| `put` 空桶 | **CAS** 直接写入桶头，无锁 |
| `put` 非空桶 | `synchronized` 锁住桶头节点，链表/树上插入 |
| `扩容` | 多线程协助迁移（`ForwardingNode` 标志，遇 `fwd` 帮忙搬） |
| `get` | **完全无锁**（`val` 和 `next` 是 `volatile`） |
| `size` | 基值 + `CounterCell[]` 累加（类似 `LongAdder` 思路） |

```
    table[i]
       │
       ▼
   Node(null)  ──put──> CAS 写入新 Node        （空桶，无锁）
   Node(x)     ──put──> synchronized(头节点)    （非空桶，锁粒度=单桶）
```

### 3. 关键设计点

| 点 | 说明 |
|:---|:---|
| **锁粒度** | 从 Segment（一段）降到**单桶头节点**，并发度 = 桶数 |
| **读无锁** | `Node.val`、`Node.next` 为 `volatile`，保证可见性 |
| **size 不准** | 并发下只能"最终一致"，`size()` 是估算值 |
| **不允许 null** | `null` 和"值不存在"语义冲突，并发下歧义，直接禁止 |
| **扩容并发** | 多线程分摊迁移不同桶，避免单线程搬运全表 |

> 对比：`HashMap` 并发 `put` 会丢失数据/死循环（JDK7 头插法成环）；`ConcurrentHashMap` 才是并发正解。`HashMap` 源码细节见 [HashMap的应用场景和源码分析.md](HashMap的应用场景和源码分析.md)。
>
> 源码级深挖（putVal 全流程、transfer 协助扩容、CounterCell 计数、computeIfAbsent 的坑）见 [ConcurrentHashMap应用场景和源码分析.md](ConcurrentHashMap应用场景和源码分析.md)。

---

## 十、阻塞队列 BlockingQueue 家族

`BlockingQueue` = 线程安全的队列 + 四类操作（抛异常/返回特殊值/阻塞/超时）。

| 操作 | 抛异常 | 返回特殊值 | 阻塞 | 超时 |
|:---|:---|:---|:---|:---|
| 入队 | `add(e)` | `offer(e)` | `put(e)` | `offer(e, t, u)` |
| 出队 | `remove()` | `poll()` | `take()` | `poll(t, u)` |
| 查看队头 | `element()` | `peek()` | — | — |

### 主要实现对比

| 实现类 | 底层 | 特点 | 典型用途 |
|:---|:---|:---|:---|
| **ArrayBlockingQueue** | 数组 | 有界、FIFO、单锁 | 生产-消费者、线程池任务队列（有界） |
| **LinkedBlockingQueue** | 链表 | 可选有界（默认 `Integer.MAX_VALUE`）、两把锁（读写分离） | `Executors.newFixedThreadPool` 默认队列 |
| **SynchronousQueue** | 无容量 | 每个 put 必须等一个 take | `newCachedThreadPool` 直接交接（无缓存） |
| **PriorityBlockingQueue** | 堆 | 无界、按优先级出队 | 任务带优先级 |
| **DelayQueue** | 堆 | 元素到期才能取 | 定时任务、缓存过期 |
| **LinkedTransferQueue** | 链表 | `transfer()` 直接交付 | 高性能无锁队列 |

> **坑**：`LinkedBlockingQueue` 默认无界，任务堆积导致 OOM，是 `Executors.newFixedThreadPool` 臧大坑之一（见第十二节）。

---

## 十一、同步工具：CountDownLatch / CyclicBarrier / Semaphore / Exchanger

### 1. CountDownLatch（一次性倒计时）

```
CountDownLatch(3)
  主线程 await() ──────阻塞──────────────────── 醒来继续
       ▲                  ↓ countDown()         ↓ countDown()       ↓ countDown()
   子1线程               计数 3→2                子2线程 2→1          子3线程 1→0
```

- 一次性，计数减到0后无法重置。
- 用途：**主线程等 N 个子任务完成**（如启动时等若干服务就绪）。

```java
CountDownLatch latch = new CountDownLatch(3);
for (int i = 0; i < 3; i++) {
    new Thread(() -> { try { /* work */ } finally { latch.countDown(); } }).start();
}
latch.await();    // 等3个都完成
```

### 2. CyclicBarrier（可循环屏障）

```
线程1 await()   线程2 await()   线程3 await() ──达到 parties=3 ──> 执行屏障动作 → 释放所有 → 下一轮
```

- **可重用**（`reset()` 或自动进入下一轮），故叫 Cyclic。
- 用途：**N 个线程互等，齐了再做下一步**（多阶段并行计算）。

```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("齐了"));
// 线程内：barrier.await();
```

### 3. Semaphore（信号量/限流）

```java
Semaphore sem = new Semaphore(3);   // 3个许可
sem.acquire();
try { /* 最多3个线程同时进入 */ } finally { sem.release(); }
```

- 用途：**限流**（接口并发数、连接池大小）、资源访问控制。
- 公平/非公平可选，基于 AQS 共享模式。

### 4. Exchanger（两线程交换数据）

```java
Exchanger<String> ex = new Exchanger<>();
// 线程A
String fromB = ex.exchange("A的数据");  // 阻塞直到 B 也调 exchange
// 线程B
String fromA = ex.exchange("B的数据");
```

- 用途：两个线程间交换数据（遗传算法、管道校验）。

### 5. 四者对比

| 工具 | 作用对象 | 可重用 | 核心方法 |
|:---|:---|:---|:---|
| CountDownLatch | 1 个等 N 个 | ❌ | `await/countDown` |
| CyclicBarrier | N 个互等齐 | ✅ | `await` |
| Semaphore | 限流 N 个许可 | ✅ | `acquire/release` |
| Exchanger | 2 个交换 | ✅ | `exchange` |

---

## 十二、线程池 ThreadPoolExecutor（重灾区）

### 1. 七大核心参数

```java
public ThreadPoolExecutor(
    int corePoolSize,                  // ①核心线程数
    int maximumPoolSize,               // ②最大线程数
    long keepAliveTime,                // ③空闲存活时间
    TimeUnit unit,                     // ④时间单位
    BlockingQueue<Runnable> workQueue, // ⑤任务队列
    ThreadFactory threadFactory,       // ⑥线程工厂（命名、守护等）
    RejectedExecutionHandler handler) // ⑦拒绝策略
```

### 2. 任务执行流程（必背）

```
       提交任务
         │
         ▼
  ┌────────────────┐
  │ 核心线程是否满？ │──否──> 创建核心线程执行
  └────────────────┘
         │是
         ▼
  ┌────────────────┐
  │ 队列是否满？    │──否──> 入队等待
  └────────────────┘
         │是
         ▼
  ┌────────────────────┐
  │ 是否达到最大线程数？│──否──> 创建非核心线程执行
  └────────────────────┘
         │是
         ▼
       触发拒绝策略
```

> **关键顺序**：核心 → 队列 → 最大 → 拒绝。`LinkedBlockingQueue` 无界时永远不满，最大线程数永远用不上——这是 `Executors` 坑点根源。

### 3. 四种拒绝策略

| 策略 | 行为 |
|:---|:---|
| **`AbortPolicy`**（默认） | 抛 `RejectedExecutionException` |
| **`CallerRunsPolicy`** | 让提交任务的线程自己执行（背压） |
| **`DiscardPolicy`** | 静默丢弃新任务 |
| **`DiscardOldestPolicy`** | 丢弃队列最老的任务，再 `execute` 新任务 |

### 4. Executors 工厂方法（及为什么阿里规约禁用）

| 方法 | 实际配置 | 坑 |
|:---|:---|:---|
| `newFixedThreadPool(n)` | 核心=最大=n，队列 `LinkedBlockingQueue`（无界） | 队列堆积 → OOM |
| `newSingleThreadExecutor` | 核心=最大=1，无界队列 | 同上 |
| `newCachedThreadPool` | 核心=0、最大=`Integer.MAX_VALUE`，`SynchronousQueue` | 线程数爆炸 → OOM |
| `newScheduledThreadPool` | `DelayedWorkQueue` | 任务堆积 → OOM |

> **阿里规约强制**：不用 `Executors`，直接 `new ThreadPoolExecutor(...)`，自己定参 + 有界队列 + 命名线程工厂 + 明确拒绝策略。

### 5. 线程数怎么定

| 任务类型 | CPU 密集 | IO 密集 |
|:---|:---|:---|
| 推荐核心数 | \(N_{cpu}+1\) | \(2N_{cpu}\) 或更多 |

公式：\(N_{threads} = N_{cpu} \times U_{cpu} \times (1 + W/C)\)，其中 \(W/C\) 为等待/计算时间比。

### 6. 状态流转

`ThreadPoolExecutor` 用 `ctl` 的高3位表示状态：

```
RUNNING(-1) ──shutdown()──> SHUTDOWN ──队空+核心线程停──> TIDYING ──terminated()──> TERMINATED
     │
     └─shutdownNow()──> STOP ──中断所有+清空队列──> TIDYING ──> TERMINATED
```

| 状态 | 接收新任务 | 处理队列任务 |
|:---|:---:|:---:|
| RUNNING | ✅ | ✅ |
| SHUTDOWN | ❌ | ✅（处理完） |
| STOP | ❌ | ❌（中断） |
| TIDYING | ❌ | ❌（执行 `terminated()`） |
| TERMINATED | ❌ | ❌（终态） |

### 7. submit vs execute

| 维度 | `execute(Runnable)` | `submit(Callable/Runnable)` |
|:---|:---|:---|
| 返回值 | 无 | `Future<T>`，可拿结果/异常 |
| 异常 | 直接抛 | 封装在 `Future`，需 `future.get()` 才暴露 |

---

## 十三、Fork/Join 框架

分治+工作窃取（work-stealing）的并行计算框架，适合可递归拆分的 CPU 密集任务（如归并排序、大数组求和）。

```
       大任务
       ┌──┴──┐
      fork  fork         每个 worker 有自己的双端队列
      ┌──┴──┐ ┌──┴──┐    自己从一端 push/pop
     ...   ... ...  ...  空闲 worker 从别人队列另一端"窃取"
```

- `ForkJoinPool`：每个 worker 一个双端队列，自己 LIFO、窃取 FIFO（减少碰撞）。
- `RecursiveTask<V>`（有返回）/`RecursiveAction`（无返回）。

```java
class SumTask extends RecursiveTask<Long> {
    long[] a; int lo, hi;
    protected Long compute() {
        if (hi - lo < THRESHOLD) return sum(a, lo, hi);
        SumTask l = new SumTask(a, lo, (lo+hi)>>>1);
        SumTask r = new SumTask(a, (lo+hi)>>>1, hi);
        l.fork();                       // 异步执行左半
        long rr = r.compute();          // 同步执行右半
        return l.join() + rr;           // 合并
    }
}
```

> `parallelStream()` 底层就是 `ForkJoinPool.commonPool()`。

---

## 十四、CompletableFuture（异步编排）

`Future` 的痛点：只能 `get()` 阻塞等结果，不能链式组合/回调。`CompletableFuture` 提供**链式异步编排**。

```java
CompletableFuture.supplyAsync(() -> getUser(id))          // 异步起线程
    .thenApply(user -> user.getName())                   // 同步转换
    .thenCompose(name -> getAddress(name))               // 异步扁平化
    .thenCombine(otherFuture, (a, b) -> a + b)          // 合并两个
    .thenAccept(System.out::println)                    // 消费
    .exceptionally(ex -> "兜底")                         // 异常处理
    .orTimeout(2, TimeUnit.SECONDS);                    // JDK9+ 超时
```

### 关键方法分类

| 类别 | 方法 | 说明 |
|:---|:---|:---|
| 创建 | `runAsync` / `supplyAsync` | 异步执行，默认走 `ForkJoinPool.commonPool()` |
| 转换 | `thenApply`、`thenCompose` | 前者同步映射、后者返回 `CF` 扁平化 |
| 消费 | `thenAccept`、`thenRun` | 无返回值消费 |
| 组合 | `thenCombine`、`allOf`、`anyOf` | 合并多个异步结果 |
| 异常 | `exceptionally`、`handle`、`whenComplete` | 兜底/恢复/记录 |
| 异步变体 | `thenApplyAsync` 等 | 切线程池执行 |

> **坑**：默认线程池是 `commonPool`，CPU 核数少时易阻塞；IO 任务建议传自定义 `Executor`。

---

## 十五、ThreadLocal

线程隔离变量：每个线程一份副本，避免共享竞争。

### 1. 结构

```
Thread
  └ ThreadLocal.ThreadLocalMap          （Thread 持有，不是 ThreadLocal 持有！）
       └ Entry[]                        （Entry = WeakReference<ThreadLocal> → value）
            Entry(key=tl1, value=v1)
            Entry(key=tl2, value=v2)
```

- **核心**：`ThreadLocal` 自己不存值，每个 `Thread` 内部一个 `ThreadLocalMap`，`ThreadLocal` 作为 key。
- `Entry` 继承 `WeakReference<ThreadLocal>`，**key 是弱引用，value 是强引用**。

### 2. 内存泄漏隐患

| 现象 | 原因 |
|:---|:---|
| `ThreadLocal` 被回收后，`Entry.key=null`，但 `value` 还强引用着 | 线程不结束，`value` 永不回收 |
| 线程池线程复用，`ThreadLocal` 没 `remove` → 数据串号 | 残留上个任务的 `value` |

**三连正确用法**：

```java
ThreadLocal<User> tl = new ThreadLocal<>();
try {
    tl.set(user);
    // 业务逻辑
} finally {
    tl.remove();    // 必须 remove，防泄漏 + 防串号
}
```

### 3. InheritableThreadLocal / TransmittableThreadLocal

| 类 | 跨线程传递能力 |
|:---|:---|
| `ThreadLocal` | ❌ |
| `InheritableThreadLocal` | 仅子线程创建时复制父线程值 |
| `TransmittableThreadLocal`（阿里 TTL） | 线程池场景下也能正确传递，配套 `TtlExecutors` |

> 链路追踪、MDC 日志、用户上下文跨线程池传递，要用 `TransmittableThreadLocal`，`InheritableThreadLocal` 在线程池下失效。

---

## 十六、易错点

| 易错点 | 澄清 |
|:---|:---|
| **`ReentrantLock` 不 `unlock`** | 必须 `try-finally` 释放，否则死锁；`synchronized` 自动释放 |
| **`StampedLock` 重入会死锁** | 它不可重入，不能在持锁函数里递归调用 |
| **`tryLock()` 不传时间立刻返回** | 拿不到锁直接返回 false，不阻塞 |
| **`synchronized` 也能 `wait/notify`** | 但只有1个等待队列，`Condition` 可多队列精确唤醒 |
| **`AtomicInteger` 不是万能锁** | 只保证单个变量原子，复合操作仍要锁 |
| **`volatile` + CAS 才安全** | `AtomicInteger` 内部靠 `volatile value` + `Unsafe CAS` 两者配合 |
| **ABA 无害与否看场景** | 计数无害；链表节点复用、版本字段必须 `AtomicStampedReference` |
| **`LongAdder.sum()` 不精确** | 非原子求和，只适合统计而非精确控制 |
| **`ConcurrentHashMap` 不允许 null** | 并发下 null 语义歧义，`HashMap` 允许 |
| **`Executors.newFixedThreadPool` 用无界队列** | 任务堆积 OOM，禁用，直接 `new ThreadPoolExecutor` |
| **线程池先扩到最大才入队？** | ❌ 顺序是**核心→队列→最大→拒绝**，无界队列导致最大线程数用不上 |
| **`ThreadLocal` 不 `remove`** | 线程池下数据串号 + value 泄漏 |
| **`CompletableFuture` 默认走 `commonPool`** | IO 密集任务建议传自定义 `Executor`，避免阻塞 |
| **`CyclicBarrier` 可以重用** | `CountDownLatch` 一次性，别混 |
| **公平锁更快吗** | 非公平吞吐更高（减少唤醒切换），公平只在不饿死需求时选 |

---

## 十七、一句话总结

JUC 的三大基石是 **CAS（无锁原子）+ AQS（同步器骨架）+ volatile/JMM（可见性有序性）**：`ReentrantLock`/`Semaphore`/`CountDownLatch` 都基于 AQS，原子类靠 `volatile` + CAS。**显式锁比 `synchronized` 灵活但需手动释放**，`StampedLock` 用乐观读突破读写锁瓶颈；**线程池用 `ThreadPoolExecutor` 七参数 + 有界队列 + 明确拒绝策略**，禁用 `Executors`；**`ConcurrentHashMap` JDK8 用 CAS+synchronized 单桶锁，读无锁**；**`ThreadLocal` 必须 `finally remove`** 防泄漏和串号。记住这条主线，JUC 八股基本通杀。

---

## 十八、相关笔记

| 主题 | 笔记 |
|:---|:---|
| JMM / happens-before / volatile 原理 | [../jvm/JMM内存模型.md](../jvm/JMM内存模型.md) |
| 对象头 Mark Word 与锁升级 | [../jvm/对象与内存分配.md](../jvm/对象与内存分配.md) |
| HashMap 源码（与 ConcurrentHashMap 对照） | [HashMap的应用场景和源码分析.md](HashMap的应用场景和源码分析.md) |
| JDK21 虚拟线程（并发新特性） | [jdk21增加了哪些新内容？.md](jdk21增加了哪些新内容？.md) |
