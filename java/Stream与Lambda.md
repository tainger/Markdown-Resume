# Stream 与 Lambda 面试题

函数式是 JDK8 以来最大的语言变化。面试考四块：**函数式接口与四种方法引用、Lambda 的 invokedynamic 原理、Stream 惰性与并行流的坑、Optional 正确姿势**。

---

## 一、函数式接口（地基）

只有一个抽象方法的接口，`@FunctionalInterface` 仅为编译期校验。

| 内置接口 | 抽象方法 | 语义 | 例子 |
|:---|:---|:---|:---|
| `Function<T,R>` | `R apply(T)` | 进 T 出 R | `User::getName` |
| `Consumer<T>` | `void accept(T)` | 消费不出 | `System.out::println` |
| `Supplier<T>` | `T get()` | 无中生有 | `ArrayList::new` |
| `Predicate<T>` | `boolean test(T)` | 判定 | `s -> s.isEmpty()` |

**四种方法引用**：静态方法 `Integer::parseInt`、实例方法（对象）`System.out::println`、实例方法（类名，第一个参数当接收者）`String::toUpperCase`、构造器 `User::new`。

```java
// lambda 捕获的局部变量必须 effectively final（副本语义，原因见 Object 笔记）
int base = 10;
Function<Integer, Integer> f = x -> x + base;   // base 不能再被赋值
```

---

## 二、Lambda 的实现原理（区分度题）

**不是匿名内部类的语法糖**：

| 对比 | 匿名内部类 | Lambda |
|:---|:---|:---|
| 编译产物 | 独立的 `Outer$1.class` | 类内合成方法 `lambda$main$0` |
| 创建时机 | new 时加载类 | **首次调用才通过 invokedynamic 生成** |
| 生成方式 | 字节码固定 | `LambdaMetafactory` 运行时生成函数式适配类 |
| `this` 指向 | 匿名类自身实例 | **外围类实例**（语义不同！） |
| 无状态 lambda | 每次都 new | **同一实例可复用/缓存**（non-capturing） |

> 面试话术：「Lambda 用 invokedynamic 把实现策略延迟到运行时，JVM 可以自由优化（生成类、方法引用直接绑定），比匿名类更省——这是 `String.concat` 用 invokedynamic 同款思路。」

---

## 三、Stream 流水线

### 三段式与惰性求值

```java
List<String> names = users.stream()          // 1. 源
        .filter(u -> u.getAge() > 18)        // 2. 中间操作（惰性，只记录不执行）
        .map(User::getName)                  //    （无终端操作则一行都不跑！）
        .collect(Collectors.toList());       // 3. 终端操作（触发整条链，一次遍历完成）
```

- **惰性 + 融合**：多个中间操作在一次遍历里逐元素依次执行（垂直执行），不是先 filter 完全量再 map；
- **短路操作**：`anyMatch / findFirst / limit(n)` 可提前终止——无限流靠它才可用（`Stream.iterate` + limit）；
- **一次性**：流只能消费一次，重用抛 `IllegalStateException`。

### 常用 API 速查

| 场景 | API |
|:---|:---|
| 分组 | `groupingBy(User::getDept)`（可嵌套 `groupingBy + mapping`） |
| 二分组 | `partitioningBy(u -> u.getAge() >= 18)` |
| 转换收集 | `toMap(k, v)`——**key 冲突必须给 merge 函数**，否则 IllegalStateException |
| 拍平 | `flatMap(List::stream)`（嵌套集合展开） |
| 数值统计 | `mapToInt` → `summaryStatistics()`（min/max/avg 一次拿） |
| 拼接 | `Collectors.joining(", ")` |
| 派生并行 | `parallelStream()` |

---

## 四、并行流的坑（P7 考察点）

```java
list.parallelStream().forEach(...);   // 底层 ForkJoinPool.commonPool()
```

| 要点 | 说明 |
|:---|:---|
| 线程池共享 | commonPool 全 JVM 共享（核数-1），**IO 阻塞任务会拖垮全局**（连 parallelStream 之外的任务） |
| 适用 | CPU 密集 + 数据量大（万级以上）+ 无共享可变状态 |
| 不适用 | IO 密集、顺序敏感、小数据量（拆分开销 > 收益） |
| 顺序性 | forEach 并行不保序；要序用 `forEachOrdered`（牺牲并行收益） |
| 正确替代 | IO 密集任务自建线程池 + CompletableFuture，别用公共池 |

**性能结论**：小数据量 for 最快（无装箱/对象开销）；Stream 可读性优先；并行流只在大数据 CPU 密集场景实测后用。

---

## 五、Optional（防 NPE 的正确姿势）

```java
// ✓ 链式 + 兜底
String city = Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .orElse("unknown");

// orElse vs orElseGet：orElse 参数永远求值（热路径别放重计算/副作用）
Optional.ofNullable(name).orElseGet(this::defaultName);
```

**反面清单**：`isPresent() + get()`（等于没用）、`Optional.get()` 裸调、把 Optional 做**字段/方法参数/集合元素**（官方明确反对，仅作返回值）。

---

## 易错点

| 坑 | 说明 |
|:---|:---|
| 中间操作没生效 | 忘了终端操作，整条链不执行（惰性） |
| `toMap` 不给 merge | key 重复直接炸；`toMap(k, v, (a, b) -> a)` |
| `peek` 不生效 | 无终端操作或被 JIT/实现优化跳过；调试用 `peek` 不是可靠手段 |
| 状态式 lambda | filter/map 里改外部集合 → 并行流数据竞争 |
| 并行流做 IO | 占死公共 ForkJoinPool，全局遭殃 |
| 流是「视图」 | 不修改源集合；`list.removeIf` 才改源（它不是 Stream API） |
| 装箱开销 | `Stream<Integer>` 高频数值计算用 `IntStream` |

---

## 一句话总结

**Lambda = invokedynamic 延迟生成**（this 指向外围类、无状态可复用）；**Stream = 惰性中间操作 + 终端触发一次遍历**（短路可终止、一次性消费）；`toMap` 必备 merge 函数、并行流公共池**只给 CPU 密集**；Optional 只做返回值，`orElse` 恒求值用 `orElseGet` 兜底重计算。

## 相关笔记

- effectively final 与值传递原理 → [Object 与关键字面试题](Object与关键字面试题.md)
- ForkJoinPool 深入 → [JUC 并发包面试题](JUC并发包.md)
- 方法引用与 Mapper 接口风格 → [../Mybatis/动态SQL与参数绑定.md](../Mybatis/动态SQL与参数绑定.md)
