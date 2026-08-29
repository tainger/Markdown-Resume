# Object 与核心关键字面试题

`equals/hashCode`、值传递、final/static、包装类缓存——语言基础高频区，特点是**题不难、坑极多**，面试官靠它快速分层。

---

## 一、== vs equals vs hashCode（必考）

| 操作 | 语义 |
|:---|:---|
| `==` | 基本类型比值；引用类型比**地址** |
| `equals` | Object 默认就是 `==`；重写后比「业务相等」 |
| `hashCode` | 对象的哈希值，供哈希容器定位桶 |

### 契约与后果（必考推理题）

> **equals 相等 ⟹ hashCode 必须相等**；反之不要求。

```java
// 只重写 equals 不重写 hashCode 的后果：
Set<Point> set = new HashSet<>();
set.add(new Point(1, 2));
set.contains(new Point(1, 2));   // false！
// 原因：两个对象 hashCode 不同 → 定位到不同桶 → 根本走不到 equals 比较
// （HashMap 先 hash 定位再 equals，见 HashMap 源码笔记）
```

### 为什么重写 equals 常配 31？

```java
public int hashCode() {
    return Objects.hash(name, age);   // 内部：result = 31 * result + element.hashCode()
}
```
- **31 是奇质数**：偶数乘法溢出丢位（等价于左移）信息损失大；质数减少碰撞聚集；
- `31 * x` 可被 JVM 优化为 `(x << 5) - x`，位运算更快。

---

## 二、值传递：Java 只有值传递（必考辨析）

```java
static void change(int x) { x = 100; }                  // 原值不变：拷贝的是值
static void change(User u) { u.setName("new"); }        // 生效：拷贝的是引用，指向同一对象
static void swap(User a, User b) {                      // 失效的经典 swap：
    User t = a; a = b; b = t;                           // 只是交换了「引用副本」
}
```

一句话：**传基本类型拷贝值，传引用类型拷贝引用（的值）**。方法内能改对象内容（set 字段），不能让调用方的引用指向新对象。

---

## 三、clone 与深浅拷贝

| 概念 | 说明 |
|:---|:---|
| `Cloneable` | **空标记接口**；不实现就调 `Object.clone()` 抛 `CloneNotSupportedException` |
| 浅拷贝 | 复制对象本身，引用字段仍指向同一子对象（默认行为） |
| 深拷贝 | 引用字段也递归复制；实现：递归 clone / 拷贝构造 / **序列化+反序列化** |

> 工程建议：clone 设计饱受争议（Effective Java），生产中优先**拷贝构造函数或静态工厂**；真正需要深拷贝的领域对象考虑不可变设计。

---

## 四、核心关键字

### final（三个层面 + JMM 语义）

| 目标 | 效果 |
|:---|:---|
| 类 | 不能被继承（String、Integer） |
| 方法 | 不能被覆写；可内联优化 |
| 变量 | 基本类型值不可变；**引用不可变但对象内容可变**（`final List` 仍可 add） |

JMM 补充：**final 字段有安全发布语义**——构造函数内对 final 的写入，在对象引用发布给其他线程后可见（详见 [../jvm/JMM内存模型.md](../jvm/JMM内存模型.md)）。

### static

- 静态成员属于类，**类初始化时执行一次**（配合静态内部类单例：**利用「内部类使用时才加载」实现懒加载 + JVM 类初始化锁天然线程安全**，是最优雅的单例，详见 [../jvm/类加载机制.md](../jvm/类加载机制.md)）；
- static 方法**没有多态**：按引用的静态类型分派，不参与虚方法表。

### 包装类缓存（经典 == 陷阱）

```java
Integer a = 127, b = 127;  // a == b → true   缓存 [-128, 127]，valueOf 命中缓存
Integer c = 128, d = 128;  // c == d → false  超出缓存，各自 new
```

- `Integer.valueOf` 走缓存，`new Integer` 永远新对象；
- **自动装箱就是编译为 valueOf** → 集合里大量小数值装箱有额外对象开销；
- 面试延伸：Boolean/Character/Short/Long 都有缓存，Float/Double 没有（连续值无法缓存）。

### 内部类与 effectively final

```java
int x = 1;
Runnable r = () -> System.out.println(x);  // x 必须 effectively final
```
原因：lambda/匿名内部类捕获的是**变量的值副本**（存进合成字段），若允许变量后续修改，副本与原值不一致会引发并发语义混乱——干脆强制不可变。

---

## 五、Object 其他方法（追问区）

| 方法 | 面试要点 |
|:---|:---|
| `wait/notify/notifyAll` | 必须在 **synchronized 块内**调用（否则 IllegalMonitorStateException）；底层 Monitor 等待队列，与 [JUC](JUC并发包.md) 的 Condition 对比 |
| `getClass` | 运行时元数据入口，反射基础（见 [反射与动态代理](反射与动态代理.md)） |
| `finalize` | JDK 9 起废弃：执行时机不确定、可能复活对象、性能差 → 用 `Cleaner`/try-with-resources |
| `toString` | 默认 `类名@十六进制hash`，调试打印注意重写 |

---

## 易错点

| 坑 | 说明 |
|:---|:---|
| equals 相等但 hashCode 不同 | HashSet/HashMap 行为错乱（上面 contains false 的例子） |
| `Objects.equals(a, b)` 粗暴使用 | 能防 NPE，但两边都是包装类型时仍走 equals，注意类型（Integer 127 陷阱依旧） |
| 以为 Java 有引用传递 | 没有；swap 经典反例要能现场白板写 |
| final 引用 == 不可变对象 | `final int[] arr` 仍可 `arr[0] = 1`；不可变要 final + 不暴露修改方法 |
| 装箱比较用 `==` | 超 127 的 Integer 全是坑；包装类型比较一律 equals 或先拆箱 |
| 静态方法里用 this | 编译不过；static 上下文无实例概念 |

---

## 一句话总结

**equals 相等则 hashCode 必须相等**（HashMap 先桶后比较的根源）；**Java 只有值传递**（引用拷贝的也是值）；final 是「引用不可变」不是「对象不可变」；包装类缓存 ±127 是 `==` 陷阱总源头。

## 相关笔记

- 先 hash 定位再 equals → [HashMap 的应用场景和源码分析](HashMap的应用场景和源码分析.md)
- final 重排序与安全发布 → [../jvm/JMM内存模型.md](../jvm/JMM内存模型.md)
- 静态内部类单例原理 → [../jvm/类加载机制.md](../jvm/类加载机制.md)
- wait/notify 与 AQS 的区别 → [JUC 并发包面试题](JUC并发包.md)
