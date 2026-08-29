# Java 技术笔记

Java 核心类库源码分析、语言特性与工程实践笔记，风格与 `leetcode-hot100/`、`数据结构/` 等目录一致：源码剖析 + 复杂度分析 + 对比表 + 易错点 + 面试重点 + 一句话总结。

## 笔记索引

| # | 笔记 | 主题 | 状态 |
|:---:|:---|:---|:---:|
| 1 | [HashMap 的应用场景和源码分析](HashMap的应用场景和源码分析.md) | 数组+链表+红黑树、扰动哈希、扩容、树化 | ✅ |
| 2 | [TreeMap 源码分析](TreeMap源码分析.md) | 红黑树、插入/删除平衡、导航与范围查询 | ✅ |
| 3 | [TreeSet 的应用场景和源码分析](TreeSet的应用场景和源码分析.md) | 委托 TreeMap、有序去重、导航方法 | ✅ |
| 4 | [JDK21 增加了哪些新内容？](jdk21增加了哪些新内容？.md) | 虚拟线程、模式匹配、分代 ZGC 等新特性 | ✅ |
| 5 | [JUC 并发包面试题](JUC并发包.md) | 显式锁、AQS、CAS、原子类、并发容器、阻塞队列、同步工具、线程池、ThreadLocal | ✅ |
| 6 | [ThreadLocal 源码分析](ThreadLocal源码分析.md) | Thread/ThreadLocalMap 结构、线性探测、被动清理、泄漏原理、InheritableThreadLocal/TTL | ✅ |
| 7 | [ConcurrentHashMap 的应用场景和源码分析](ConcurrentHashMap应用场景和源码分析.md) | CAS+synchronized 桶级锁、协助扩容、LongAdder 计数、弱一致性 | ✅ |
| 8 | [String 面试题](String面试题.md) | 不可变设计、常量池与 intern 版本差异、拼接原理、Compact Strings | ✅ |
| 9 | [Object 与关键字面试题](Object与关键字面试题.md) | equals/hashCode 契约、值传递、clone、final/static、包装类缓存、内部类 | ✅ |
| 10 | [异常体系面试题](异常体系面试题.md) | checked vs unchecked、finally 语义、try-with-resources、异常性能 | ✅ |
| 11 | [泛型与类型擦除](泛型与类型擦除.md) | 擦除规则、Signature 属性与 TypeToken、桥方法、PECS、数组协变 | ✅ |
| 12 | [反射与动态代理](反射与动态代理.md) | 反射性能与优化、JDK Proxy vs CGLIB、Spring AOP 选型、事务失效 | ✅ |
| 13 | [Stream 与 Lambda](Stream与Lambda.md) | 函数式接口、invokedynamic 原理、惰性求值、并行流的坑、Optional | ✅ |

> 状态图例：⬜ 待整理 · 🟡 整理中 · ✅ 已完成

## P7 面试重点速查（语言核心）

- **String 不可变**：final 类 + final 数组 → 安全/hashCode 缓存/常量池共享/线程安全；常量池 JDK7 在堆，intern 只记引用
- **equals 相等 ⟹ hashCode 相等**：HashMap 先 hash 定位再 equals；只重写 equals 会让 HashSet 失效
- **Java 只有值传递**：引用拷贝的也是值；方法内改内容生效、换指向无效
- **Integer 缓存 ±127**：装箱即 valueOf，包装类比较一律 equals
- **finally 改不了已暂存的基本类型返回值**；资源关闭标准答案 try-with-resources（逆序 + suppressed）
- **泛型擦除**：运行期无参数类型，但 Signature 元数据在（TypeToken 原理）；PECS：生产 extends 只读、消费 super 可写
- **JDK 代理 vs CGLIB**：接口+反射 vs 子类+FastClass；Spring Boot 默认 CGLIB；@Transactional 自调用失效 = 绕过代理
- **并行流公共 ForkJoinPool 只给 CPU 密集**；toMap 必给 merge 函数

## 集合体系速览

| 类 | 底层 | 顺序 | 增删查 | 判等依据 | 适用 |
|:---|:---|:---|:---:|:---|:---|
| `HashMap` | 数组+链表+红黑树 | 无序 | 平均 \(O(1)\) | `hashCode`+`equals` | 默认最快 |
| `LinkedHashMap` | HashMap+双向链表 | 插入/访问序 | 平均 \(O(1)\) | `hashCode`+`equals` | 保序 / LRU |
| `TreeMap` | 红黑树 | 按 key 排序 | \(O(\log n)\) | `compareTo`/`Comparator` | 有序 / 范围 / 最近邻 |
| `HashSet` | HashMap | 无序 | 平均 \(O(1)\) | `hashCode`+`equals` | 去重 |
| `TreeSet` | TreeMap（红黑树） | 排序 | \(O(\log n)\) | `compareTo`/`Comparator` | 有序去重 / 导航 |
| `ConcurrentHashMap` | 数组+链表+红黑树（CAS+桶级锁） | 无序 | 并发平均 \(O(1)\) | `hashCode`+`equals` | 并发场景正解 |

## 相关

- 红黑树 = 自平衡 BST，原理见 [数据结构/二叉搜索树.md](../数据结构/二叉搜索树.md)
- 哈希原理与刷题套路见 [数据结构/哈希表.md](../数据结构/哈希表.md)
- 堆与优先队列见 [数据结构/优先队列.md](../数据结构/优先队列.md)
