# SPI机制与自适应扩展

「Dubbo 的可扩展性为什么强？」——答案就两个字：**SPI**。Dubbo 几乎所有组件（Protocol、LoadBalance、Registry…）都是 SPI 扩展点，这是它「微内核 + 插件」设计的地基，也是 P6→P7 面试的分水岭题。

---

## 一、Java SPI vs Dubbo SPI（必考）

**SPI（Service Provider Interface）**：JDK 内置的服务发现机制——接口 + 实现方在 `META-INF/services/接口全限定名` 文件里声明实现类。

| 对比 | Java SPI | Dubbo SPI |
|:---|:---|:---|
| 配置文件 | `META-INF/services/接口名`，内容 = 实现类全限定名 | `META-INF/dubbo/接口名`，内容 = **`key=实现类全限定名`** |
| 实例化 | **一次性全部实例化**（`ServiceLoader.load` 遍历） | **按 key 懒加载**（`getExtension("dubbo")`） |
| 按名获取 | 不支持 | ✅ `getExtensionLoader(X.class).getExtension(name)` |
| IOC 依赖注入 | ❌ | ✅ setter 注入其他扩展实例 |
| AOP 包装 | ❌ | ✅ Wrapper 包装链 + Filter 链 |
| 自适应扩展 | ❌ | ✅ `@Adaptive` 运行时按 URL 参数选实现 |
| 缓存 | 无 | cachedClasses / cachedInstances / cachedAdaptiveClass 等 |

> Java SPI 例子：JDBC 4.0 的 `Driver` 加载；缺点实例：只想用 MySQL 驱动却把 classpath 里所有驱动全部实例化。

---

## 二、Dubbo SPI 三大注解

### 1. `@SPI`：标记扩展点接口 + 默认值

```java
@SPI("dubbo")                       // 默认扩展实现：key=dubbo
public interface Protocol {
    int getDefaultPort();
    <T> Exporter<T> export(Invoker<T> invoker);
    <T> Invoker<T> refer(Class<T> type, URL url);
}
```

### 2. `@Adaptive`：自适应扩展（动态选实现，最核心）

在**方法级**打注解，Dubbo 启动时生成 Adaptive 类源码并编译：先从 `URL` 里找**参数名对应的值**当作扩展 key，再 `getExtension(key)` 拿实现。

```java
public interface Protocol {
    // url.getParameter("protocol", "dubbo")：URL 里 protocol 参数决定用哪个实现
    @Adaptive                       // 未指定参数名时，默认取「接口名小写」即 protocol
    <T> Exporter<T> export(Invoker<T> invoker);
}
```

生成的 Adaptive 类核心逻辑（面试可手写伪码）：

```java
public class Protocol$Adaptive implements Protocol {
    public Exporter export(Invoker invoker) {
        URL url = invoker.getUrl();
        // ① 从 URL 取扩展 key：找 protocol 参数，没有用 @SPI 默认值
        String key = url.getProtocol() == null ? "dubbo" : url.getProtocol();
        // ② 按名拿真实扩展实现，委托给它执行
        Protocol p = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(key);
        return p.export(invoker);
    }
}
```

> 这就是 Dubbo「**运行时按 URL 动态路由到具体实现**」的机制：同一个 Protocol 接口，URL 是 `dubbo://` 走 DubboProtocol，`registry://` 走 RegistryProtocol——**多重协议、多注册中心就这么统一进来的**。

### 3. `@Activate`：条件激活（Filter 链用）

```java
@Activate(group = {CONSUMER, PROVIDER}, order = -10000)
public class ConsumerContextFilter implements Filter { ... }
// group：消费端/服务端激活；value：URL 中出现某参数才激活
```

---

## 三、ExtensionLoader 核心逻辑（源码级要点）

```java
ExtensionLoader<Protocol> loader = ExtensionLoader.getExtensionLoader(Protocol.class);
Protocol protocol = loader.getAdaptiveExtension();   // 拿自适应扩展（编译生成的类）

// getExtension(name) 的三级缓存：
// ① cachedInstances  → name → 持有实例的 Holder（实例级缓存）
// ② createExtension：
//    ├─ cachedClasses：name → 实现类（类级缓存，首次从文件解析）
//    ├─ 反射 newInstance()
//    ├─ ★ IOC：injectExtension(instance) —— 遍历 setter，按「扩展接口类型」再取扩展注入
//    └─ ★ AOP：cachedWrapperClasses 逐层包装 ——
//         编译期扫描「构造函数只有一个扩展接口参数」的类（如 ProtocolFilterWrapper）
//         + 按 @Activate order 排序组装 Filter 链
```

**IOV + AOP 一句话**：**IOC = setter 注入扩展依赖；AOP = Wrapper 包装类 + Filter 链层层包裹**。`ProtocolFilterWrapper` 就是在这里把 Filter 链织入调用。

---

## 四、写一个自定义扩展的步骤（工程加分项）

```
① 实现接口，如 custom LoadBalance：
   public class MyBalance implements LoadBalance { ... }
② 建文件：resources/META-INF/dubbo/org.apache.dubbo.rpc.cluster.LoadBalance
   内容：myBalance=com.example.MyBalance
③ 使用：@DubboReference(loadbalance = "myBalance")
   或 URL 参数 loadbalance=myBalance（@Adaptive 就按这个值选实现）
```

> 生产级案例：自定义 Filter 做统一 traceId 透传 / 自定义 Router 做灰度分流 / 自定义 LoadBalance 做机房亲和——面试讲出一个即可证明「扩展点」理解到位。

---

## 易错点

1. **文件路径三套**：`META-INF/dubbo/`（Dubbo SPI）、`META-INF/dubbo/internal/`（内置）、`META-INF/services/`（兼容 Java SPI），文件名都是**接口全限定名**。
2. **`@Adaptive` 打在类上 vs 方法上**：类上 = 手写自适应类（如 `AdaptiveExtensionFactory`）；方法上 = 自动生成源码编译。大多数接口是方法级。
3. **自适应方法必须有 URL 参数**：没有 URL 就无法从参数里取 key，生成类直接抛 `UnsupportedOperationException`。
4. **Wrapper 判定条件**：**构造函数只有一个扩展接口类型的参数**，且无 `@Adaptive`——这是「包装类」而非「实现类」的判定标准。
5. **`getExtension("true")`**：等价于拿默认实现（@SPI 的 value），别当特殊 key 背。

---

## 一句话总结

**Dubbo SPI = Java SPI 的 key-value 懒加载增强版**：`@SPI` 定扩展点、`@Adaptive` 生成自适应类按 **URL 参数**动态选实现、`@Activate` 条件激活 Filter；配合 IOC（setter 注入）与 AOP（Wrapper + Filter 链）构成「微内核 + 插件」——**面试白板画出 Adaptive 生成类的「取参数→选实现→委托」三步即满分**。
