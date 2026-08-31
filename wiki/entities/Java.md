---
type: entity
status: seed
updated: 2026-08-31
---

# Java 语言核心

> 一句话：JVM 之上的语言层——类库、并发、IO、函数式、工程化。与 [[wiki/entities/JVM]]（底层运行时）形成上下层关系。

## 核心笔记

- [[java/HashMap的应用场景和源码分析]]（数组+链表+红黑树、扰动哈希、扩容、树化）
- [[java/TreeMap源码分析]]（红黑树、插入/删除平衡、导航与范围查询）
- [[java/TreeSet的应用场景和源码分析]]（委托 TreeMap、有序去重、导航方法）
- [[java/ConcurrentHashMap应用场景和源码分析]]（CAS+synchronized 桶级锁、协助扩容、LongAdder）
- [[java/JUC并发包]]（显式锁/AQS/CAS/原子类/并发容器/阻塞队列/同步工具/ThreadLocal）
- [[java/线程面试题]]（创建方式、6 状态、wait/notify、sleep/wait/yield/join、synchronized 锁升级、死锁、中断、daemon、单例）
- [[java/线程池面试题]]（队列选型深度、动态调参、监控告警、Spring Boot @Async、ForkJoinPool 对比、线程池隔离、生产事故）
- [[java/volatile关键字]]（三大特性、原子性反证、DCL、传递可见性、内存屏障速答）
- [[java/ThreadLocal源码分析]]（Thread/ThreadLocalMap、线性探测、被动清理、泄漏、InheritableThreadLocal/TTL）
- [[java/String面试题]]（不可变设计、常量池、拼接原理、Compact Strings）
- [[java/Object与关键字面试题]]（equals/hashCode 契约、值传递、clone、final/static、包装类缓存、内部类）
- [[java/异常体系面试题]]（checked vs unchecked、finally 语义、try-with-resources、异常性能）
- [[java/泛型与类型擦除]]（擦除规则、Signature 属性、桥方法、PECS、数组协变）
- [[java/反射与动态代理]]（反射性能、JDK Proxy vs CGLIB、Spring AOP 选型、事务失效）
- [[java/Stream与Lambda]]（函数式接口、invokedynamic、惰性求值、并行流的坑、Optional）
- [[java/jdk21增加了哪些新内容？]]（虚拟线程、模式匹配、分代 ZGC 等）

## 高频追问链

HashMap 树化阈值 → concurrent 8 为什么用 synchronized + CAS → AQS 双向队列 → volatile 禁止重排原理（见 [[wiki/entities/JVM]]）→ synchronized 锁升级 → wait 为什么要在 synchronized 里 → 线程池队列选型影响最大线程数 → ForkJoinPool Work-Stealing → CompletableFuture 默认 commonPool 被 IO 阻塞

## 关联

- 底层运行时：[[wiki/entities/JVM]]（对象头 Mark Word / 类加载 / GC / JMM）
- 框架层：[[wiki/entities/Spring生态]]（Spring AOP 依赖动态代理、@Async 默认线程池坑）
- 并发理论：[[jvm/JMM内存模型]]（happens-before 是 volatile/synchronized 的理论基础）
