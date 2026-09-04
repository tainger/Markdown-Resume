# 匿名内部类和 Lambda 表达式的区别

> 基础 Lambda 特性见 [Stream 与 Lambda](Stream与Lambda.md) 第二章，本篇深挖**编译原理差异、this 引用陷阱、变量捕获区别、性能对比、选型决策**——面试中"Lambda 和匿名内部类有什么区别"是送分题，但能讲到字节码层面和 this 引用的是加分题。

---

## 一、语法对比

### 1.1 匿名内部类写法

```java
// 传统写法：new 一个接口/抽象类的匿名实现
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("hello");
    }
};

// 按钮监听器
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("clicked");
    }
});
```

### 1.2 Lambda 写法

```java
// Lambda 简写
Runnable r = () -> System.out.println("hello");

// 带参数的 Lambda
button.addActionListener(e -> System.out.println("clicked"));
```

### 1.3 语法差异速览

| 维度 | 匿名内部类 | Lambda |
|:---|:---|:---|
| 代码量 | 多（类名+方法签名+body） | 少（只需参数+body） |
| 适用对象 | 接口 + 抽象类 | **只有函数式接口**（@FunctionalInterface） |
| 类型声明 | 显式 `new Runnable(){...}` | 隐式，由目标类型推断 |
| 能否有实例字段 | ✅ 可以定义成员变量 | ❌ 不能有实例字段（是方法不是类） |
| 能否有多个方法 | ✅ 可以实现多个方法 | ❌ 只能有一个抽象方法 |

---

## 二、编译原理差异（核心区别）

> 面试加分题：「Lambda 不是匿名内部类的语法糖」——这句话怎么证明？

### 2.1 匿名内部类的编译产物

```java
public class Demo {
    public void test() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("hello");
            }
        };
    }
}
```

编译后生成 **两个 class 文件**：

```
Demo.class          ← 外部类
Demo$1.class        ← 匿名内部类（独立的 class 文件）
```

`Demo$1.class` 反编译后：

```java
final class Demo$1 implements Runnable {
    Demo$1() {}
    public void run() {
        System.out.println("hello");
    }
}
```

**关键特征**：
- 每次 `new` 都会生成一个新的**类实例**
- 有独立的 class 文件，类加载器需要加载它
- `this` 指向匿名内部类自身

### 2.2 Lambda 的编译产物

```java
public class Demo {
    public void test() {
        Runnable r = () -> System.out.println("hello");
    }
}
```

编译后**只有一个 class 文件**：

```
Demo.class          ← 外部类（Lambda 没有独立 class）
```

反编译 `Demo.class` 可以看到 Lambda 被翻译成**类内的私有静态方法**：

```java
public class Demo {
    private static void lambda$test$0() {  // Lambda 被编译成静态方法
        System.out.println("hello");
    }

    public void test() {
        // invokedynamic 指令在运行时动态生成代理对象
        Runnable r = (Runnable) CallSite...;
    }
}
```

### 2.3 invokedynamic 机制

Lambda 不是通过 `new` 创建对象，而是通过 JDK 7 引入的 **`invokedynamic` 指令**在运行时动态生成：

```
invokedynamic 调用流程：
┌─────────────────────────────────────────────────┐
│ 1. 首次执行 Lambda → 触发 bootstrap 方法          │
│ 2. LambdaMetafactory.metafactory() 引导           │
│ 3. 生成 CallSite（调用点）→ 绑定到 lambda$test$0   │
│ 4. 动态生成实现函数式接口的代理对象                 │
│ 5. 后续调用直接走 CallSite，无需再次 bootstrap     │
└─────────────────────────────────────────────────┘
```

**关键优势**：
- 没有独立 class 文件，减少类加载开销
- JVM 可以对 Lambda 方法做更激进的优化（内联、逃逸分析）
- 同一个 Lambda 表达式可能被缓存复用（如果无状态）

### 2.4 编译产物对比表

| 维度 | 匿名内部类 | Lambda |
|:---|:---|:---|
| class 文件 | 生成独立 `Outer$N.class` | **不生成独立 class** |
| 字节码指令 | `new` + `invokespecial` | `invokedynamic` |
| 方法位置 | 匿名类的实例方法 | 外部类的**静态方法** `lambda$xxx` |
| 类加载 | 每次 new 都要加载匿名类 | 首次 invokedynamic 引导后缓存 |
| this 指向 | 匿名内部类实例 | **外部类实例**（关键差异，见下节） |

---

## 三、`this` 引用差异（最重要的坑）

> 这是面试最高频的追问，也是实际开发中最容易踩的坑。

### 3.1 匿名内部类的 this

匿名内部类中的 `this` 指向**匿名内部类自身**：

```java
public class Outer {
    private int x = 10;

    public void test() {
        Runnable r = new Runnable() {
            private int x = 20;  // 匿名内部类的成员变量

            @Override
            public void run() {
                System.out.println(this.x);  // 20 ← this 指向匿名内部类
            }
        };
        r.run();
    }
}
```

### 3.2 Lambda 的 this

Lambda 中的 `this` 指向**外部类实例**（因为 Lambda 编译成外部类的静态方法）：

```java
public class Outer {
    private int x = 10;

    public void test() {
        Runnable r = () -> {
            // System.out.println(this.x);  // ❌ 编译错误，Lambda 内没有 this.x=20 的定义
            System.out.println(this.x);  // 10 ← this 指向外部类 Outer
        };
        r.run();
    }
}
```

### 3.3 完整对比例子

```java
public class ThisDemo {
    private String name = "Outer";

    public void test() {
        // 匿名内部类
        Runnable anonymous = new Runnable() {
            private String name = "Anonymous";

            @Override
            public void run() {
                System.out.println(this.name);  // 输出 "Anonymous"
                System.out.println(ThisDemo.this.name);  // 输出 "Outer"（要访问外部类需用 类名.this）
            }
        };

        // Lambda
        Runnable lambda = () -> {
            System.out.println(this.name);  // 输出 "Outer"（this 直接指向外部类）
            // System.out.println(lambda.this);  // ❌ 编译错误，Lambda 没有 this
        };
    }
}
```

**记忆口诀**：
- 匿名内部类：`this` 指向自己，访问外部类要用 `外部类名.this`
- Lambda：`this` 指向外部类，没有自己的 this

---

## 四、变量捕获（Variable Capture）差异

### 4.1 匿名内部类的变量捕获

匿名内部类可以捕获外部类的**成员变量**（可变）和局部变量（必须 effectively final）：

```java
public class CaptureDemo {
    private int count = 0;  // 成员变量：匿名内部类可以修改

    public void test() {
        int local = 10;  // 局部变量：必须 effectively final

        Runnable r = new Runnable() {
            @Override
            public void run() {
                count++;        // ✅ 可以修改成员变量
                // local = 20;  // ❌ 编译错误，局部变量必须 final
                System.out.println(count + local);
            }
        };
        r.run();
    }
}
```

### 4.2 Lambda 的变量捕获

Lambda 也遵循同样的规则，但因为编译成静态方法，捕获的局部变量是**值传递**：

```java
public class CaptureDemo {
    private int count = 0;

    public void test() {
        int local = 10;

        Runnable r = () -> {
            count++;        // ✅ 可以修改外部类成员变量（通过 this）
            // local = 20;  // ❌ 同样编译错误，局部变量必须 final
            System.out.println(count + local);
        };
        r.run();
    }
}
```

### 4.3 为什么局部变量必须 final？

因为 Java 方法调用是**值传递**，Lambda/匿名内部类捕获的是局部变量的**副本**，不是引用：

```
局部变量 local = 10（在栈帧中）
         │
         ├─ 匿名内部类捕获 → 复制一份到堆上的匿名类实例
         └─ Lambda 捕获 → 复制一份到静态方法的参数

如果允许修改：
    匿名类改的是副本，外部的 local 不变 → 语义混乱
所以 Java 强制要求 final，保证捕获值的一致性
```

> 对比 JavaScript：JS 闭包捕获的是**引用**，可以修改外部变量。Java 为了避免语义混乱，强制 final。

### 4.4 捕获变量的字节码存储位置

| 类型 | 匿名内部类 | Lambda |
|:---|:---|:---|
| 外部类引用 | 构造器传入 `this$0` 字段 | 通过 `invokedynamic` 绑定到外部类实例 |
| 捕获的局部变量 | 构造器传入，存为实例字段 | 作为 `lambda$xxx` 静态方法的参数传入 |
| 成员变量 | 通过 `this$0` 访问 | 通过外部类实例访问（this 指向外部类） |

---

## 五、功能能力对比

### 5.1 匿名内部类能做但 Lambda 不能做的事

| 能力 | 匿名内部类 | Lambda |
|:---|:---:|:---:|
| 实现**抽象类**（非接口） | ✅ | ❌（只能函数式接口） |
| 实现**多个方法** | ✅ | ❌（只能一个抽象方法） |
| 定义**实例字段** | ✅ | ❌ |
| 定义**实例方法**（辅助方法） | ✅ | ❌ |
| **this** 引用指向自身 | ✅ | ❌（this 指向外部类） |
| 序列化 | ✅（可定制 writeReplace） | ⚠️ 需实现 Serializable |

### 5.2 Lambda 能做但匿名内部类做不到的事

| 能力 | Lambda | 匿名内部类 |
|:---|:---:|:---:|
| 代码更简洁 | ✅ | ❌ |
| 无独立 class 文件 | ✅ | ❌ |
| JVM 优化空间更大（内联/逃逸分析） | ✅ | ❌ |
| `invokedynamic` 动态绑定 | ✅ | ❌（编译期静态绑定） |
| 方法引用进一步简化 | ✅（`System.out::println`） | ❌ |

---

## 六、性能对比

### 6.1 理论性能

| 维度 | 匿名内部类 | Lambda |
|:---|:---|:---|
| 类加载 | 每次 new 加载匿名类（有开销） | 首次 invokedynamic 引导后缓存 |
| 对象创建 | 每次 new 创建新实例 | 无状态 Lambda 可被 JVM 优化复用 |
| JIT 优化 | 普通类，常规优化 | 编译成静态方法，**更容易被 JIT 内联** |
| 内存 | 每个实例占堆内存 | 无状态 Lambda 可共享单例 |

### 6.2 实际性能结论

**Lambda 通常比匿名内部类快**，但差距在现代 JVM 上很小（纳秒级），业务开发中**不应以性能为选型依据**。

```java
// 无状态 Lambda 可能被 JVM 优化为单例（JDK 21+）
Runnable stateless = () -> System.out.println("hello");
// JVM 可能只创建一个实例，多次复用

// 有状态 Lambda（捕获了变量）每次都会创建新实例
int x = 10;
Runnable stateful = () -> System.out.println(x);  // 捕获 x，每次新建
```

### 6.3 性能不是选型标准

> **面试加分**：Lambda 和匿名内部类的性能差距在现代 JVM 上可以忽略，选型应该看**可读性和语义**，不是性能。真正影响性能的是业务逻辑本身。

---

## 七、选型决策

### 7.1 什么时候用 Lambda

```
✅ 用 Lambda 的场景：
  ├─ 实现函数式接口（Runnable/Callable/Comparator/Consumer/Function 等）
  ├─ 只有一个抽象方法
  ├─ 不需要定义实例字段/辅助方法
  └─ 代码简洁优先（Stream API、集合遍历、回调）
```

```java
// ✅ Lambda 适合
list.forEach(item -> System.out.println(item));
Collections.sort(list, (a, b) -> a.length() - b.length());
```

### 7.2 什么时候用匿名内部类

```
✅ 用匿名内部类的场景：
  ├─ 实现抽象类（非接口）
  ├─ 实现有多个方法的接口
  ├─ 需要定义实例字段保存状态
  ├─ 需要 this 指向自身
  └─ 需要自定义序列化行为
```

```java
// ✅ 匿名内部类适合：定义有状态的 Comparator
Comparator<String> comparator = new Comparator<>() {
    private int compareCount = 0;  // 实例字段

    @Override
    public int compare(String a, String b) {
        compareCount++;  // 记录调用次数
        return a.length() - b.length();
    }
};

// ✅ 匿名内部类适合：继承抽象类
AbstractList<String> list = new AbstractList<>() {
    @Override
    public String get(int index) { return data[index]; }
    @Override
    public int size() { return data.length; }
};
```

### 7.3 决策树

```
需要创建函数式接口实例？
    │
    ├─ 是 → 只有一个抽象方法？
    │    ├─ 是 → 需要实例字段/辅助方法？
    │    │    ├─ 否 → ✅ Lambda
    │    │    └─ 是 → ✅ 匿名内部类
    │    └─ 否（多个方法）→ ✅ 匿名内部类
    │
    └─ 否 → 抽象类/普通类 → ✅ 匿名内部类
```

---

## 八、面试高频问答

### Q1：Lambda 和匿名内部类有什么区别？

核心四点：
1. **编译产物**：匿名内部类生成独立 `Outer$N.class`，Lambda 编译成类内静态方法 `lambda$xxx` + `invokedynamic` 动态绑定
2. **this 引用**：匿名内部类的 `this` 指向自身，Lambda 的 `this` 指向外部类
3. **适用范围**：匿名内部类可实现抽象类/多方法接口，Lambda 只能用于函数式接口
4. **能力**：匿名内部类可有实例字段和辅助方法，Lambda 不能

### Q2：Lambda 是匿名内部类的语法糖吗？

**不是**。证明：
- 匿名内部类编译生成独立 `.class` 文件，Lambda 不生成
- 匿名内部类通过 `new` 创建，Lambda 通过 `invokedynamic` 运行时动态生成
- `this` 引用行为完全不同
- JVM 对 Lambda 的优化方式不同（可内联、可复用）

### Q3：为什么 Lambda 捕获的局部变量必须是 final？

因为 Java 是值传递，Lambda 捕获的是局部变量的**副本**。如果允许修改，Lambda 改的是副本，外部变量不变，语义混乱。强制 final 保证捕获值的一致性。JavaScript 闭包捕获的是引用所以可以修改，Java 为了简单和安全选了值传递 + final。

### Q4：Lambda 和匿名内部类哪个性能好？

Lambda 理论上更快（无独立 class、易内联、可复用），但现代 JVM 上差距可忽略。**选型看可读性和语义，不看性能**。

### Q5：Lambda 中的 this 和匿名内部类中的 this 有什么不同？

```java
// 匿名内部类：this 指向匿名内部类实例
new Runnable() {
    @Override
    public void run() {
        System.out.println(this);  // 匿名内部类实例
    }
}

// Lambda：this 指向外部类实例
() -> {
    System.out.println(this);  // 外部类实例
}
```

原因：匿名内部类是一个**类**，有自己的实例；Lambda 是一个**方法**，没有自己的实例，this 自然指向调用它的外部类。

### Q6：Lambda 可以序列化吗？

可以，但有限制：
- Lambda 必须实现 `Serializable` 接口（函数式接口声明 `extends Serializable`）
- 捕获的变量也必须可序列化
- 匿名内部类序列化更复杂，需要定制 `writeReplace`/`readResolve`

```java
@FunctionalInterface
interface SerializableFunction<T, R> extends Function<T, R>, Serializable {}

// 使用
SerializableFunction<String, Integer> fn = String::length;
```

### Q7：什么场景必须用匿名内部类不能用 Lambda？

1. 实现**抽象类**（Lambda 只能实现接口）
2. 实现**多个抽象方法**的接口
3. 需要**保存状态**（定义实例字段）
4. 需要 `this` 指向**自身**
5. 需要**辅助方法**（接口 default 方法之外的私有方法）

---

## 九、易错点

1. **❌ Lambda 是匿名内部类的简写** → ✅ Lambda 是 `invokedynamic` 动态生成，不是匿名内部类
2. **❌ Lambda 中可以用 this 指向 Lambda 自身** → ✅ Lambda 中 this 指向外部类
3. **❌ Lambda 可以实现抽象类** → ✅ Lambda 只能实现函数式接口（JDK 22+ 有 Lambda for 抽象类的实验特性，但主流仍只支持接口）
4. **❌ Lambda 捕获的变量可以修改** → ✅ 必须 effectively final，修改会编译错误
5. **❌ 匿名内部类和 Lambda 性能差距很大** → ✅ 现代 JVM 上差距可忽略，选型看语义
6. **❌ Lambda 不能创建对象** → ✅ Lambda 本质是对象（函数式接口的实例），只是创建方式是 invokedynamic 而非 new

---

## 十、相关笔记

- Lambda 函数式接口与 Stream 实战 → [Stream与Lambda.md](Stream与Lambda.md)
- invokedynamic 底层原理 → [反射与动态代理.md](反射与动态代理.md)
- 变量捕获与闭包概念 → [ThreadLocal源码分析.md](ThreadLocal源码分析.md)（ThreadLocal 的 set/get 也是一种变量隔离机制）
- this 与内部类基础 → [Object与关键字面试题.md](Object与关键字面试题.md)
- 方法引用（Lambda 的进一步简化）→ [Stream与Lambda.md](Stream与Lambda.md)

---

## 十一、一句话总结

> 匿名内部类是**编译期生成的独立类**（有自己的 this、字段、多方法能力），Lambda 是**运行时 invokedynamic 动态绑定的静态方法**（this 指向外部类、只能函数式接口、更简洁更易优化）；选型看语义——需要状态/抽象类/多方法用匿名内部类，纯函数式单方法用 Lambda。
