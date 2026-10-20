# AQS 源码分析（AbstractQueuedSynchronizer）

> AQS 是 JUC 的「地基」——`ReentrantLock`、`Semaphore`、`CountDownLatch`、`ReentrantReadWriteLock`、`ThreadPoolExecutor.Worker` 全是它的子类。它把「**线程排队等待**」这件所有同步器都要做的事抽出来：**一个 volatile 状态位 + 一个 CLH 双向队列 + CAS**，剩下的「能不能拿到锁」交给子类用 `tryAcquire`/`tryRelease` 决定。理解了 AQS，JUC 一半源码就都通了。概览版见 [JUC并发包.md](JUC并发包.md)。

---

## 一、AQS 是什么 / 解决什么问题

**一句话**：AQS 是一个**同步器框架**，封装了「获取不到就排队、被唤醒后重试」的通用流程，让子类只需专注「获取/释放的判定规则」。

| 没有 AQS 之前 | 有了 AQS 之后 |
|:---|:---|
| 每个锁/同步器自己写线程挂起、唤醒、排队逻辑 | 通用骨架在 AQS，子类只填 `tryAcquire`/`tryRelease` 等 5 个钩子 |
| 容易写错（park/unpark 时机、竞态） | 框架保证正确性 |
| 代码重复 | 复用 |

```java
// 子类的最小工作示例（互斥锁）
class MyLock {
    private static class Sync extends AbstractQueuedSynchronizer {
        protected boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {                    // CAS 抢状态
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
        protected boolean tryRelease(int releases) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
    }
    private final Sync sync = new Sync();
    public void lock()   { sync.acquire(1); }                  // 模板方法在 AQS
    public void unlock() { sync.release(1); }
}
```

> 💡 **模板方法模式**：`acquire`/`release` 是模板（在 AQS 里），`tryAcquire`/`tryRelease` 是钩子（在子类）。AQS 控制「流程」，子类控制「规则」。

---

## 二、底层结构：state + CLH 队列 + 三个核心组件

```
┌─────────────────────────────────────────────┐
│  AbstractQueuedSynchronizer                  │
│                                              │
│  ① volatile int state       ← 同步状态        │
│       含义由子类定义：                          │
│       ReentrantLock: 持有次数（0 表示空闲）     │
│       Semaphore:      剩余许可数              │
│       CountDownLatch: 剩余倒数                 │
│                                              │
│  ② CLH 同步队列（FIFO 双向链表）               │
│       head ─→ Node ─→ Node ─→ ... ─→ tail    │
│       获取失败的线程在这里排队等待              │
│                                              │
│  ③ exclusiveOwnerThread     ← 独占模式拥有者  │
│       （在父类 AbstractOwnableSynchronizer）  │
└─────────────────────────────────────────────┘
```

### Node 节点（CLH 队列的元素）

```java
static final class Node {
    static final Node SHARED = new Node();       // 共享模式标记
    static final Node EXCLUSIVE = null;          // 独占模式标记（就是 null）

    // 等待状态（waitStatus）
    static final int CANCELLED =  1;             // 已取消（超时/中断）
    static final int SIGNAL    = -1;             // 后继节点需要被唤醒（重要！）
    static final int CONDITION = -2;             // 在条件队列里等
    static final int PROPAGATE = -3;             // 共享模式下传播唤醒

    volatile int waitStatus;
    volatile Node prev;                          // 前驱
    volatile Node next;                          // 后继
    volatile Thread thread;                      // 排队的是哪个线程
    Node nextWaiter;                             // 条件队列的下一个 / 模式标记
}
```

> **CLH 队列本质**：是一个**双向链表**，但 `prev` 用于「找前驱来挂到自己后面」，`next` 用于「前驱唤醒后继」。**SIGNAL 状态是协议核心**：一个节点的 `waitStatus = SIGNAL` 意味着「我后面的节点睡着了，我释放时得负责叫醒它」。

---

## 三、核心源码分析（独占模式）

### 1. acquire：拿不到就排队

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&                                  // ① 先试试直接拿（子类实现）
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))       // ② 拿不到 → 入队 + 挂起
        selfInterrupt();                                     // ③ 排队期间被中断过 → 补中断标记
}
```

三步走：**先试一次（快速路径）→ 入队 → 在队里循环 park 等待**。

### 2. addWaiter：把当前线程封装成 Node 挂到队尾

```java
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {                 // CAS 抢尾指针
            pred.next = node;
            return node;
        }
    }
    enq(node);                                               // 队列为空或 CAS 失败 → 自旋入队
    return node;
}

private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) {                                     // ① 队列空：先初始化 dummy head
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {                                             // ② 队列非空：CAS 挂到尾
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

> **注意**：队列的 `head` 是一个 **dummy 节点**（不存线程，只当占位）。真正排队的第一个线程是 `head.next`。这是 CLH 的经典设计——`head` 表示「当前持有锁的（或刚释放的）」，`head.next` 才是「下一个该被唤醒的」。

### 3. acquireQueued：在队列里循环等待（核心）

```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();               // ① 拿前驱
            if (p == head && tryAcquire(arg)) {              // ② 前驱是 head → 再试一次拿
                setHead(node);                               // ③ 拿到了 → 自己升为 head
                p.next = null;                               //    帮助 GC
                failed = false;
                return interrupted;
            }
            // ④ 拿不到 → 检查是否该 park；该 park 就 park
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);                             // 异常时取消
    }
}
```

**关键点**：

- **只有 `前驱 == head` 的节点才会重试 `tryAcquire`**——其他节点直接 park。这样保证最多只有一个线程在「积极抢锁」，避免惊群。
- `shouldParkAfterFailedAcquire` 会把前驱的 `waitStatus` 设置成 `SIGNAL`——「我马上要睡了，你释放时记得叫我」。
- `parkAndCheckInterrupt` 内部是 `LockSupport.park(this)`，被唤醒后返回是否曾被中断。**AQS 不响应中断**，只记录，最后由 `acquire` 的 `selfInterrupt()` 补标记。

### 4. shouldParkAfterFailedAcquire：决定要不要睡

```java
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        return true;                                         // 前驱已承诺会叫我 → 安心睡
    if (ws > 0) {                                            // 前驱已取消
        do {
            node.prev = pred = pred.prev;                    // 一路向前跳过所有取消节点
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);      // 给前驱打 SIGNAL 标记
    }
    return false;                                            // 这轮不睡，下轮再判
}
```

### 5. release：释放 + 唤醒后继

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {                                   // ① 子类释放状态
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);                              // ② 唤醒 head 的后继
        return true;
    }
    return false;
}

private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);                // 清掉 SIGNAL

    Node s = node.next;
    if (s == null || s.waitStatus > 0) {                     // 后继为空或已取消
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;                                       // 从尾向前找最靠前的有效节点
    }
    if (s != null)
        LockSupport.unpark(s.thread);                        // 唤醒它
}
```

> **为什么从尾部向前找**：因为 `addWaiter` 里 `pred.next = node` 不是原子的（先 CAS prev，再赋值 next），从前往后可能漏。从后往前扫 prev 链是安全的。

### 6. cancelAcquire：处理取消

```java
private void cancelAcquire(Node node) {
    if (node == null) return;
    node.thread = null;

    Node pred = node.prev;
    while (pred.waitStatus > 0)                              // 跳过前面的取消节点
        node.prev = pred = pred.prev;

    Node predNext = pred.next;
    node.waitStatus = Node.CANCELLED;

    if (node == tail && compareAndSetTail(node, pred)) {     // ① 是尾节点 → 直接摘
        compareAndSetNext(pred, predNext, null);
    } else {
        int ws;
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
                compareAndSetNext(pred, predNext, next);     // ② 中间节点 → 前驱跳过自己
        } else {
            unparkSuccessor(node);                           // ③ 自己就是 head 后继 → 直接唤醒
        }
        node.next = node;                                    // help GC
    }
}
```

---

## 四、共享模式（acquireShared / releaseShared）

共享与独占的本质区别：**拿到锁之后还要继续唤醒后面的节点**（传播）。

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)                           // 返回负数 = 拿不到
        doAcquireShared(arg);
}

private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {                                // 拿到了
                    setHeadAndPropagate(node, r);            // ⚠️ 关键：传播唤醒
                    p.next = null;
                    if (interrupted) selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed) cancelAcquire(node);
    }
}

private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head;
    setHead(node);
    // propagate > 0 表示"还有剩余许可"或"需要传播" → 继续唤醒下一个共享节点
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();
    }
}
```

**典型例子**：`CountDownLatch.countDown()` 把 state 减到 0 时，**所有等待的线程都被依次唤醒**——靠的就是共享模式的传播性。

---

## 五、Condition：条件队列

`Condition`（`newCondition()`）是 AQS 的另一条队列——**等待队列**。和同步队列的区别：

```
同步队列（CLH）：head → Node → Node → tail       ← 拿不到锁的线程在这排队
等待队列（Condition）：firstWaiter → ... → lastWaiter   ← await() 的线程在这等信号
```

### await：释放锁 + 进等待队列

```java
public final void await() throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    Node node = addConditionWaiter();                        // ① 加入条件队列
    int savedState = fullyRelease(node);                     // ② 完全释放锁（包括重入）
    int interruptMode = 0;
    while (!isOnSyncQueue(node)) {                           // ③ 不在同步队列 → park
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    // ④ 被唤醒后重新竞争锁
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

### signal：从条件队列转移到同步队列

```java
public final void signal() {
    if (!isHeldExclusively()) throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}

private void doSignal(Node first) {
    do {
        if ((firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&                    // ⚠️ 关键：转移到同步队列
             (first = firstWaiter) != null);
}

final boolean transferForSignal(Node node) {
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;                                        // 已被取消
    Node p = enq(node);                                      // 挂到同步队列尾
    int ws = p.waitStatus;
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        LockSupport.unpark(node.thread);
    return true;
}
```

**关键理解**：`signal` **不直接唤醒线程**，而是把节点从条件队列**挪到同步队列**——线程要等拿到锁后才会真正从 `await` 返回。

---

## 六、典型实现类对照

| 类 | state 含义 | 模式 | tryAcquire 关键逻辑 |
|:---|:---|:---|:---|
| `ReentrantLock` | 持有次数（0=空闲，>0=重入） | 独占 | `compareAndSetState(0, 1)` 或当前线程重入 +1 |
| `ReentrantReadWriteLock` | 高 16 位读计数，低 16 位写计数 | 读共享 / 写独占 | 写：CAS 0→1；读：CAS 加 1（且无写锁占用） |
| `Semaphore` | 剩余许可数 | 共享 | `state - acquires >= 0` 则 CAS 减 |
| `CountDownLatch` | 剩余倒数 | 共享 | `state == 0` 才算拿到 |
| `ThreadPoolExecutor.Worker` | 是否被任务占用 | 独占 | 0→1 表示开始执行任务 |
| `FutureTask` | 任务状态 | — | 内部用 WaitNode 链表，不是标准 AQS |

> **重入的实现**：`ReentrantLock` 的 `tryAcquire` 在 CAS 失败后会检查「当前线程是不是已经是持有者」，是则 `state + 1`，这就是重入。`tryRelease` 时 `state - 1`，减到 0 才真正释放。

---

## 七、复杂度分析

| 操作 | 时间 | 说明 |
|:---|:---:|:---|
| `tryAcquire` 成功路径 | \(O(1)\) | 一次 CAS |
| `acquire` 失败路径（入队 + park） | 摊还 \(O(1)\) | CAS 入队 + 若干次状态检查 |
| `release` + 唤醒 | \(O(n)\) 最坏 | `unparkSuccessor` 从尾扫描找有效节点（但通常 \(O(1)\)） |
| `cancelAcquire` | \(O(n)\) 最坏 | 跳过取消节点 |
| 空间 | \(O(n)\) | 每个等待线程一个 Node |

> CLH 队列的优势在于**入队/出队都是 O(1)**（CAS 尾插），且通过 `prev` 引用实现**无锁化出队**（节点自己改自己的状态，不需要全局锁）。

---

## 八、易错点

### 1. 「AQS 的 head 是真实排队节点」是错的

`head` 是 **dummy 节点**（虚占位），真正排队的第一个线程是 `head.next`。当 `head.next` 拿到锁时，它会被 `setHead` 提升为新的 dummy head。

### 2. `acquire` 不响应中断，只是记录

`acquire` 在等待期间被中断，**不会立即抛异常**，而是记 `interrupted = true`，直到拿到锁后才 `selfInterrupt()` 补中断标记。要响应中断得用 `acquireInterruptibly`。

### 3. `signal` 不直接唤醒线程

`signal` 只是把节点从条件队列挪到同步队列，线程要等拿到锁才能从 `await` 返回。所以 `signal` 之后**必须先释放锁**，否则被 signal 的线程永远拿不到锁。

### 4. 共享模式的 `tryAcquireShared` 返回值有讲究

- 负数：拿不到，要排队
- 0：拿到，但**后面不要再传播**（如 Semaphore 只剩一个许可被自己拿了）
- 正数：拿到，且**需要传播唤醒**后续节点（如 CountDownLatch 倒数到 0）

返回 0 和正数的区别决定了一次 `release` 会唤醒多少线程。

### 5. `waitStatus = SIGNAL` 是「前驱对后继的承诺」

意思是「我（前驱）释放时会叫醒你（后继）」。所以 `shouldParkAfterFailedAcquire` 看到前驱是 SIGNAL 才能安心睡。

### 6. 取消节点的清理是「惰性」的

取消节点不会立即从队列删除，而是等下一次 `shouldParkAfterFailedAcquire` 或 `unparkSuccessor` 时被跳过。这是为了减少并发修改。

### 7. 别自己继承 AQS 重写 `acquire` / `release`

这两个是 `final` 的——AQS 的设计就是「流程不可改，规则可改」。你能改的只有 `tryAcquire` / `tryRelease` / `tryAcquireShared` / `tryReleaseShared` / `isHeldExclusively` 这五个钩子。

---

## 九、面试重点

| 优先级 | 考点 | 一句话答法 |
|:---:|:---|:---|
| ⭐⭐⭐ | **AQS 的核心组件** | `volatile int state` + CLH 双向队列 + CAS；独占模式的 `exclusiveOwnerThread` |
| ⭐⭐⭐ | **acquire 流程** | 先 `tryAcquire` 试一次 → 失败入队 → `acquireQueued` 循环（前驱是 head 才重试，否则 park）→ 拿到后升 head |
| ⭐⭐⭐ | **CLH 队列的 SIGNAL 状态** | 前驱节点对后继的承诺：「我释放时会叫醒你」 |
| ⭐⭐⭐ | **独占 vs 共享的区别** | 独占：拿到就完事；共享：拿到后还要传播唤醒后续节点（`setHeadAndPropagate`） |
| ⭐⭐ | **为什么用双向链表而非单向** | `prev` 用于高效入队（找尾）和取消（向前跳），`next` 用于唤醒传播 |
| ⭐⭐ | **Condition 的两条队列** | 条件队列（`await` 睡在这）+ 同步队列（`signal` 后挪到这）；`signal` 不直接唤醒，要等拿到锁 |
| ⭐⭐ | **重入的实现** | state 计数 + 持有线程 ID；释放时减到 0 才真释放 |
| ⭐ | **cancelAcquire 的作用** | 中断/超时后把节点标记为 CANCELLED 并跳过，防止后续误唤醒 |
| ⭐ | **为什么说 AQS 是 JUC 地基** | ReentrantLock/Semaphore/CountDownLatch/ReentrantReadWriteLock/ThreadPoolExecutor.Worker 都是它的子类 |

---

## 十、一句话总结

AQS 用「**`volatile state` + CLH 双向队列 + CAS**」三件套把「线程拿不到锁就排队挂起、被唤醒后重试」的通用骨架封装起来；子类只需实现 `tryAcquire/tryRelease` 定义「什么是拿到锁」（`ReentrantLock` 是 state 0→1，`Semaphore` 是许可数够，`CountDownLatch` 是倒数到 0），独占模式拿到就完事，共享模式还要传播唤醒后续节点；CLH 队列的 `SIGNAL` 状态是前驱对后继的「释放时叫醒」承诺，`head` 是 dummy 占位节点；理解 AQS 就等于理解了 JUC 大半源码。

---

## 相关链接

- 并发包整体梳理见 [JUC并发包.md](JUC并发包.md)
- 锁升级与底层 synchronized 见 [Object与关键字面试题.md](Object与关键字面试题.md)
- `volatile` 内存语义见 [volatile关键字.md](volatile关键字.md)
- 并发容器见 [ConcurrentHashMap应用场景和源码分析.md](ConcurrentHashMap应用场景和源码分析.md)
