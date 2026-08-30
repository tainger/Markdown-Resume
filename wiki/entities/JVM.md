---
type: entity
status: seed
updated: 2026-08-30
---

# JVM

> 一句话：运行时数据区 + 类加载 + GC 三大件；调优的前提是先会诊断。

## 核心笔记

- [[jvm/内存结构]]（五大区域）
- [[jvm/对象与内存分配]]（创建过程/逃逸分析/TLAB）
- [[jvm/垃圾回收算法]]（标记清除/复制/整理）
- [[jvm/垃圾收集器]]（CMS/G1/ZGC 演进）
- [[jvm/类加载机制]]（双亲委派）
- [[jvm/JMM内存模型]]（happens-before/可见性）
- [[jvm/性能调优与诊断]]（工具链 + GC 日志）

## 高频追问链

对象什么时候进老年代 → G1 分区模型 → CMS 为什么被废弃 → 三色标记 + 增量更新/原始快照 → 双亲委派能不能破坏 → volatile 与 JMM（Java 层见 [[java/volatile关键字]]）

## 关联

- 内存泄漏实战：[[java/ThreadLocal源码分析]]（弱引用 key + Value 泄漏）
- [[java/JUC并发包]]
