# String 面试题

String 是 Java 面试第一道语言基础题，三连问几乎固定：**为什么不可变？常量池在哪？`+` 拼接怎么实现的？** 本文按源码 → 常量池 → 拼接原理 → 易错点组织。

---

## 一、为什么 String 设计成不可变（必考）

```java
public final class String {
    private final byte[] value;   // JDK9：char[] 改 byte[]（Compact Strings）
    private final byte coder;     // LATIN1 / UTF16，纯 ASCII 只占 1 字节
    private int hash;             // 缓存 hash，首次计算后不再重算
}
```

| 原因 | 说明 |
|:---|:---|
| **安全** | ClassLoader、网络连接参数、文件路径若可变，校验后被篡改后果严重 |
| **hashCode 缓存** | HashMap 的 key 大量使用 String，hash 只算一次（互链 [HashMap 源码分析](HashMap的应用场景和源码分析.md)） |
| **常量池共享** | 同一字面量全局唯一，多个引用指向同一对象，省内存 |
| **线程安全** | 不可变对象天然并发安全 |
| **语义稳定** | key 放进 HashMap 后内容不变，不会「找不到」 |

> JDK9 Compact Strings：`byte[] + coder`，纯拉丁字符内存直接减半——被追问「String 底层存什么」时的新答案。

---

## 二、字符串常量池（重点 + 经典题）

### 位置演进

| 版本 | 位置 | 原因 |
|:---|:---|:---|
| JDK 6 | 方法区（永久代） | — |
| JDK 7+ | **堆** | 永久代 GC 不积极、易 OOM；堆中可正常参与 GC |

### intern() 的版本差异（经典题核心）

```java
String s = new String("a") + new String("b"); // 堆上生成 "ab"，常量池中没有 "ab"
s.intern();                                    // JDK7+：池中不存在 → 只记录 s 的引用，不复制
String s2 = "ab";
System.out.println(s == s2);   // JDK7+ true；JDK6 false（池中是复制的副本）
```

| 场景 | JDK 6 | JDK 7+ |
|:---|:---|:---|
| intern 时池中已有 | 返回池中对象 | 返回池中对象 |
| intern 时池中没有 | **复制一份**进池再返回 | **记录堆引用**进池再返回 |

### 经典计数题

```java
String s = new String("ab");   // 创建几个对象？
```
- 字面量 `"ab"`：编译期检查，池里**没有**则先在池中建 1 个；
- `new String(...)`：堆上再建 1 个；
- **答案：1 或 2 个**（取决于池中是否已有 `"ab"`），答题先给「最多 2 个」再分情况。

```java
String s = "a" + "b";   // 编译期常量折叠：javac 直接合成 "ab"，运行期 0 个新对象
final String a = "a";
String s2 = a + "b";    // final 常量也参与折叠，s2 == "ab" 为 true
```

---

## 三、拼接原理（必考）

### `+` 的本质

```java
String s = a + b;
// 编译为（非编译期常量时）：
String s = new StringBuilder().append(a).append(b).toString();
```

**循环拼接为什么必须显式用 StringBuilder**：

```java
String s = "";
for (int i = 0; i < 100000; i++) {
    s += i;   // ✗ 每轮循环 new 一个 StringBuilder + toString 再复制，O(n²)
}
```

| 版本 | `+` 的实现 |
|:---|:---|
| JDK 8 | 编译器改写为 StringBuilder 链 |
| **JDK 9+** | invokedynamic + `StringConcatFactory`（按参数个数/类型选最优策略，性能更好，但循环内仍会重复拼接） |

### String vs StringBuilder vs StringBuffer（对比必背）

| 维度 | String | StringBuilder | StringBuffer |
|:---|:---|:---|:---|
| 可变性 | 不可变 | 可变（byte[]，不 final） | 可变 |
| 线程安全 | 天然安全 | **不安全** | 安全（方法 synchronized） |
| 场景 | 少量固定串 | **单线程拼接（默认选它）** | 多线程拼接（实际罕见，一般用 String 重新设计） |
| 扩容 | — | 原容量 `2n + 2`，数组复制 | 同左 |

---

## 四、常用方法的实现细节（区分度题）

| 方法 | 底层细节 |
|:---|:---|
| `split(",")` | **单字符/无正则元字符**走 `indexOf` 快速路径；多字符走 `Pattern`（重用要注意 Pattern.compile 开销） |
| `substring` | JDK 6 曾共享原数组导致内存泄漏（只记 offset/count），**JDK 7+ 改为复制**——讲清演进是加分点 |
| `replace` vs `replaceAll` | replace 字面量替换；**replaceAll 参数是正则**（`replaceAll(".", "-")` 会替换所有字符） |
| `switch(String)` | 编译为 **hashCode 分支 + equals 确认**（防 hash 碰撞），呼应 hashCode 缓存设计 |
| `equals` | 先比 `==`（同引用直接 true），再比长度，再逐段比较——短路与优化 |

---

## 易错点

| 坑 | 说明 |
|:---|:---|
| `==` 判等字符串 | 比地址；只有常量池折叠场景凑巧为 true，一律用 `equals` |
| 循环内 `+=` | O(n²)，显式 StringBuilder，可预估容量避免扩容 |
| intern 当缓存滥用 | 大量动态字符串 intern 会撑爆常量池（堆），需 `-XX:StringTableSize` 调优知识兜底 |
| `replaceAll` 当字面量替换 | 第二参数是正则，`.` `|` `$` 都是元字符 |
| 池中对象被 GC？ | JDK7+ 常量池在堆中，无引用的字面量**可以被回收**（常量池不是「永久」的同义词） |

---

## 一句话总结

**String 不可变 = final 类 + final 数组 + 不暴露修改**，换来了 hashCode 缓存、常量池共享与线程安全；常量池 JDK7 后在堆、intern 只记引用不复制；`+` 拼接编译期常量折叠、运行期 builder/indy，**循环拼接必须显式 StringBuilder**。

## 相关笔记

- hash 缓存与 key 定位 → [HashMap 的应用场景和源码分析](HashMap的应用场景和源码分析.md)
- 常量池在运行时数据区的位置 → [../jvm/内存结构.md](../jvm/内存结构.md)
- StringBuilder 线程不安全 vs StringBuffer → [JUC 并发包面试题](JUC并发包.md)
