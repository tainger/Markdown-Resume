# JDK 21 新特性总结

> JDK 21（2023.09）是继 JDK 17 之后的下一个 **LTS** 版本，亮点是虚拟线程正式 GA。

---

## 一、重磅特性（Final）

### 1. 虚拟线程（Virtual Threads）— JEP 444

JDK 21 最重要的特性，**协程的官方实现**。

```java
// 之前：平台线程（1:1 映射 OS 线程），昂贵
Thread.ofPlatform().start(() -> doSomething());

// 现在：虚拟线程（M:N 映射），轻量，可以创建百万级
Thread.ofVirtual().start(() -> doSomething());

// 或者用 ExecutorService
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> doSomething());
}
```

| 对比 | 平台线程 | 虚拟线程 |
|:---|:---|:---|
| 映射 | 1:1 OS 线程 | M:N（由 JVM 调度） |
| 创建成本 | ~1MB 栈空间 | ~几 KB |
| 阻塞代价 | 阻塞 OS 线程 | 自动让出 carrier 线程 |
| 适用场景 | CPU 密集型 | **IO 密集型（高并发网络请求）** |

> 对面试的价值：可以聊「虚拟线程如何简化传统异步编程（无需 CompletableFuture / WebFlux），同时保持高吞吐」。

### 2. Record Patterns（记录模式）— JEP 440

解构 Record 类型，配合 `instanceof` 使用：

```java
record Point(int x, int y) {}

// 之前
if (obj instanceof Point p) {
    int x = p.x();
    int y = p.y();
}

// JDK 21
if (obj instanceof Point(int x, int y)) {
    // 直接使用 x, y
}
```

### 3. Pattern Matching for switch — JEP 441

`switch` 终极形态：类型匹配 + 条件 + null 处理一体化。

```java
Object obj = ...;
switch (obj) {
    case null       -> System.out.println("null");
    case String s   -> System.out.println("String: " + s);
    case Integer i when i > 0 -> System.out.println("正数");
    case int[] arr  -> System.out.println("数组长度: " + arr.length);
    default         -> System.out.println("unknown");
}
```

### 4. Sequenced Collections（有序集合）— JEP 431

为 `List`、`LinkedHashSet`、`SortedSet` 等统一了「首尾操作」的接口：

```java
interface SequencedCollection<E> extends Collection<E> {
    E getFirst();
    E getLast();
    void addFirst(E e);
    void addLast(E e);
    E removeFirst();
    E removeLast();
    SequencedCollection<E> reversed();  // 反向视图
}

// 之前
list.get(0);              // 取首
list.get(list.size() - 1); // 取尾（啰嗦）

// JDK 21
list.getFirst();
list.getLast();
list.reversed();           // 反向遍历，超实用
```

### 5. Key Encapsulation Mechanism API — JEP 452

用于后量子密码学场景的密钥封装机制，加密领域的标准化 API。

---

## 二、预览/孵化特性（Preview/Incubator）

| 特性 | 状态 | 一句话 |
|:---|:---|:---|
| String Templates (JEP 430) | Preview | `STR."Hello \{name}"` 替代字符串拼接 |
| Unnamed Patterns & Variables (JEP 443) | Preview | `_` 代替不用的变量，`try { ... } catch (Exception _)` |
| Unnamed Classes & Main Methods (JEP 445) | Preview | 写 Hello World 不需要 `public class` 包裹 |
| Scoped Values (JEP 446) | Preview | ThreadLocal 的现代替代，不可变 + 可继承 |
| Structured Concurrency (JEP 453) | Preview | `StructuredTaskScope` 管理多线程子任务，类似 Go 的 errgroup |
| Foreign Function & Memory API (JEP 442) | 3rd Preview | 替代 JNI，安全调用 native 代码 |
| Vector API (JEP 448) | 6th Incubator | SIMD 向量化运算 |

---

## 三、GC 相关

### Generational ZGC — JEP 439

ZGC 从「不分代」升级为「分代」，**大幅降低年轻代对象频繁回收的开销**，吞吐量提升显著。

---

## 四、面试重点

如果面试官问「JDK 21 了解什么」，按这个优先级回答：

| 优先级 | 特性 | 为什么 |
|:---:|:---|:---|
| ⭐⭐⭐ | **虚拟线程** | 最大亮点，能展开聊协程原理、与 Go goroutine 对比 |
| ⭐⭐ | Record Patterns + switch 增强 | 日常编码直接受益，展示跟进新版本 |
| ⭐⭐ | Sequenced Collections | 实用性强，体现编码细节关注 |
| ⭐ | String Templates / Scoped Values | 加分项，展示前瞻视野 |
