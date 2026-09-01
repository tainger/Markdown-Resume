# Spring AOP

AOP（Aspect-Oriented Programming，面向切面编程）是 Spring 里**日常用得最多、踩坑也最多**的特性之一。底层是动态代理 + 拦截器链；声明式事务 `@Transactional` 也建立在这套机制之上（事务切面的原理与场景题见 [事务.md](事务.md)）。

---

## 一、JDK 动态代理 vs CGLIB 代理（必考）

| 维度 | JDK 动态代理 | CGLIB（Code Generation Library） |
|:---|:---|:---|
| **原理** | 运行时动态生成实现目标类**所有接口**的匿名类 | 运行时 ASM 字节码生成**目标类的子类**，覆盖所有非 final 方法 |
| **是否需要接口** | ✅ 必须实现至少一个接口 | ❌ 不需要，类直接代理 |
| **能否代理非 public 方法** | 接口方法必须 public | 可以代理 protected/package 级别（不能 private/final） |
| **能否代理 final 类 / final 方法** | ❌ 类是 final 无所谓（接口不看）；方法 final 也无所谓（接口方法不能 final） | ❌ final 类**不能继承**；final 方法**不能覆盖**，CGLIB 直接忽略不代理，调用走原类逻辑 |
| **性能（代理创建）** | 快（JDK 内置 `Proxy.newProxyInstance`，反射生成字节码） | 慢（ASM 读类结构、生成新类字节码、ClassLoader 加载） |
| **性能（调用）** | 慢（每次调用走 `InvocationHandler.invoke`，两次反射） | Spring 3.2+ 集成 CGLIB 后优化：**FastClass 索引调用，比 JDK 快 2~3 倍** |
| **Spring 默认策略** | 有接口用 JDK，无接口用 CGLIB（Spring 5.x 及以前） | Spring Boot 2.0+ **默认 `spring.aop.proxy-target-class=true`** — 一律 CGLIB，不管有没有接口 |
| **Spring 强制切换开关** | `@EnableAspectJAutoProxy(proxyTargetClass = false)` | `@EnableAspectJAutoProxy(proxyTargetClass = true)`（默认） |

### 为什么 Spring Boot 2.0 开始默认开 proxy-target-class=true？

因为纯 JDK 代理在 Spring 世界里会有一个很烦人的类型坑：

```java
public interface UserService { void save(); }

@Service
public class UserServiceImpl implements UserService { ... }

// 另一个类注入
@Autowired
private UserServiceImpl userService;  // ❌ 报错！注入的是 JDK 代理 $Proxy18，类型是 UserService 接口，不是 UserServiceImpl
@Autowired
private UserService userService;      // ✅ 没问题（接口类型）
```

大量用户（尤其是新手）会写实现类注入，JDK 代理直接抛 BeanNotOfRequiredTypeException。Spring Boot 直接把默认切 CGLIB，避开所有这类问题。

### 代理陷阱：this 调用不生效

```java
@Service
public class OrderService {
    @Transactional
    public void create() {
        this.detail();  // ❌ this 指向原始对象，不是代理 → @Transactional 失效！
    }

    @Transactional
    public void detail() { ... }
}
```

Spring 事务的代理模型如下：

```
  Client（Controller）
      │
      │  注入的是 OrderService$$EnhancerBySpringCGLIB$$xxxxx（代理对象）
      ▼
Proxy.method():
  ├─ TransactionInterceptor.invoke():
  │   ├─ create connection / setAutoCommit(false)  ← 事务开启
  │   ├─ 调用 原始对象.method()                     ← 进入真实业务代码
  │   │      内部的 this.detail() → 直接走真实对象的 detail()
  │   │                                     ↑ 绕开代理！
  │   └─ commit / rollback
```

**三种解决办法**：

1. 注入自己（丑陋但可用）：
```java
@Autowired @Lazy private OrderService self;  // @Lazy 破循环依赖
public void create() { self.detail(); }
```
2. 调 AopContext.currentProxy()：
```java
public void create() { ((OrderService)AopContext.currentProxy()).detail(); }
// 需要加 @EnableAspectJAutoProxy(exposeProxy = true)
```
3. **方法拆分到另一个 Bean**（最推荐）：
```java
@Service public class OrderService {
    @Autowired private OrderDetailService detailService;
    @Transactional public void create() { detailService.save(); }
}
```

---

## 二、AOP 核心概念

| 术语 | 说明 | 举栗子 |
|:---|:---|:---|
| **JoinPoint（连接点）** | 程序执行过程中可以织入切面的点。Spring 中只能是**方法执行** | `UserService#save`、`OrderService#cancel` 的执行前后 |
| **Pointcut（切点）** | 匹配一组 JoinPoint 的规则（表达式） | `execution(* com.example.service.*.*(..))` 所有 service 包下所有方法 |
| **Advice（通知/增强）** | 在切点上执行的动作 | 前置 @Before、后置 @AfterReturning、异常 @AfterThrowing、最终 @After、**环绕 @Around**（功能最强，覆盖其他所有） |
| **Aspect（切面）** | Pointcut + Advice 的组合 | 「事务切面」「日志切面」「权限切面」 |
| **Weaving（织入）** | 将切面逻辑融合到目标对象代码里的过程 | 编译期（AspectJ 编译器）、类加载期（Load-time Weaving）、**运行期（Spring 默认，动态代理）** |
| **Introduction（引介）** | 给现有类动态加新接口/方法 | 让没有 `Auditable` 接口的类也能有审计方法 |

### 5 种 Advice 执行顺序

假设方法正常执行且切面内 5 种 Advice 都配了：

```
@Around 前置部分（如 preHandle）
   │
@Before
   │
方法目标调用（目标方法本身）
   │
@AfterReturning（方法正常 return 后执行，能拿 return value）
   │
@After（不管成功失败，finally 级别的执行）
   │
@Around 后置部分（如 postHandle / 返回值处理）
```

### 多切面的执行顺序

没有显式控制时 Spring 顺序不确定，**依赖类加载器加载顺序**，非常容易出 Bug。正确做法：

1. 切面类实现 `org.springframework.core.Ordered` 接口 / 加 `@Order(N)` 注解
2. `N` 越小优先级越高（越外层）；`@Transactional` 默认 `Ordered.LOWEST_PRECEDENCE`

```
@Order(1) 切面（外层）:
   ├─ @Around before
   │   @Order(2) 切面（内层）:
   │      ├─ @Around before
   │      │   目标方法
   │      └─ @Around after
   └─ @Around after
```

> **生产应用**：自定义 `@DistributedLock` 切面要让锁包住事务，必须 `@Order(0)` 比 `@Transactional` 的 `LOWEST_PRECEDENCE` 更外层——详见 [事务.md](事务.md) Q11「分布式锁 + @Transactional 混用导致超卖」。

---

## 三、易错点

| 易错点 | 说明 |
|:---|:---|
| **以为 CGLIB 代理一定比 JDK 快** | 代理创建 CGLIB 慢很多（ASM 字节码生成 + 类加载）；调用时 CGLIB FastClass 确实更快。短生命周期 Bean 用 JDK 反而综合更快。Spring Boot 选 CGLIB 是因为类型兼容不是性能 |
| **同一个类里两个 @Transactional 自调用** | 不生效。理由是 this 绕过代理（见上文章节「代理陷阱」）；除非在 A 方法里用 `AopContext.currentProxy()` 调 B 方法，或者拆类 |
| **@Transactional 加在接口上** | 可以加，JDK 代理会继承；但 CGLIB 类代理时接口上的注解有时不生效（取决于具体版本）。生产一律**写在 public 类方法上**。另外 `private`/`static`/`final` 都不行 |
| **方法被 final 修饰（CGLIB 场景）** | JDK 代理不受影响（接口方法不能 final）；但 Spring Boot 默认 CGLIB，final 方法直接忽略代理逻辑（详见 [事务.md](事务.md) 失效场景 5） |

---

## 四、一句话总结

Spring AOP 在 Bean 初始化后阶段用 **JDK 动态代理（接口）/ CGLIB 子类代理（类，Spring Boot 默认 `proxy-target-class=true`）** 织入切面，围绕目标方法建拦截器链；5 种 Advice 按 `@Around 前置 → @Before → 目标方法 → @AfterReturning → @After → @Around 后置` 执行，多切面用 `@Order(N)` 控制（N 越小越外层）。**最大的坑是同类自调用走 this 绕过代理**，解决办法 3 选 1：注入自己（`@Lazy`）/ `AopContext.currentProxy()` / 拆类；事务切面 `@Transactional` 默认 `Ordered.LOWEST_PRECEDENCE`，自定义切面要包住事务时 `@Order` 必须 < 它（典型：分布式锁包事务，见 [事务.md](事务.md) Q11）。

---

## 五、相关笔记

| 主题 | 笔记 |
|:---|:---|
| 事务原理层：@Transactional 底层 ThreadLocal、7 种传播行为、8 大失效场景 | [事务.md](事务.md) |
| Bean 生命周期中 BeanPostProcessor 何时生成 AOP 代理 | [IOC与Bean生命周期.md](IOC与Bean生命周期.md) |
| 自动配置与启动流程（AOP 后置处理器扩展点） | [自动配置与启动流程.md](自动配置与启动流程.md) |
