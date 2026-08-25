# Spring AOP 与事务

AOP（Aspect-Oriented Programming，面向切面编程）和声明式事务 `@Transactional` 是 Spring 里**日常用得最多、踩坑也最多**的两块。它们底层是同一套机制（动态代理 + 拦截器链）。

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

---

## 三、@Transactional 深度解析

### 事务管理器 PlatformTransactionManager

Spring 事务本质是「把 JDBC Connection 的提交/回滚封装成声明式」。核心接口：

```java
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition definition);
    void commit(TransactionStatus status);
    void rollback(TransactionStatus status);
}
```

常见实现：
| 实现类 | 对应场景 |
|:---|:---|
| DataSourceTransactionManager | 纯 JDBC / MyBatis / Spring JdbcTemplate |
| JpaTransactionManager | Hibernate / JPA |
| HibernateTransactionManager | Hibernate（较老） |
| **DataSourceTransactionManager** | **生产 90% 用这个**（MyBatis + MySQL 最常见） |
| WebSphereUowTransactionManager | WebSphere 应用服务器事务（极少见） |

### Connection 绑定到线程 —— ThreadLocal 机制

Spring 事务保证一个事务里的多次操作共用同一个 Connection，靠的是 `TransactionSynchronizationManager` 的 ThreadLocal：

```
@Transactional 方法开始:
  PlatformTransactionManager.getTransaction():
    ↓
  DataSourceTransactionManager.doBegin():
    1. 从 DataSource.getConnection() 拿新连接
    2. conn.setAutoCommit(false)  ← 关闭 JDBC 自动提交
    3. 把 conn 放入 TransactionSynchronizationManager.resources
       └─ key=DataSource，value=ConnectionHolder
       └─ `ThreadLocal(Map(Object, Object))`  ← 保证同线程同事务同连接

MyBatis/JdbcTemplate 执行 SQL 时:
  DataSourceUtils.getConnection(dataSource):
    1. 先查 TransactionSynchronizationManager.resources 有没有绑定的 conn
    2. 有 → 复用它（在事务里）
    3. 没有 → 临时 new Connection（非事务，每句 SQL 一个连接）

方法正常 return:
  PlatformTransactionManager.commit():
    connection.commit() → connection.setAutoCommit(true) → 从 ThreadLocal 解绑
方法抛 RuntimeException / Error:
  PlatformTransactionManager.rollback():
    connection.rollback() → ... 同上解绑
```

### 回滚规则

默认只回滚：
- **`RuntimeException` 及其子类**（空指针、索引越界、业务运行时异常）
- **`Error`**（OOM 等系统错误）

**不回滚**：
- `Exception`（非 RuntimeException，如 IOException、SQLException）
- 受检异常

自定义回滚规则：
```java
// 受检异常也回滚
@Transactional(rollbackFor = {SQLException.class, IOException.class})
// 指定异常不回滚（只回滚 RuntimeException，不回滚 BizException）
@Transactional(noRollbackFor = BizException.class)
```

> **口诀**：默认**跑（RuntimeException）错（Error）回**，受检不回。生产建议**永远显式写 `rollbackFor = Exception.class`**。

---

## 四、7 种事务传播行为（Propagation）

这是 Spring 事务最精华的部分。**面试官喜欢画场景题让你选传播行为**，先把 7 种的定义和效果理清楚。

### 快速分类

| 行为 | 代码 | 一句话解释 |
|:---|:---|:---|
| **REQUIRED**（默认） | `Propagation.REQUIRED` | **有事务加入、无事务新建** — 最常用 |
| **SUPPORTS** | `SUPPORTS` | 有事务加入，没事务就非事务跑 |
| **MANDATORY** | `MANDATORY` | 强制必须在已有事务里，没事务直接抛异常 |
| **REQUIRES_NEW** | `REQUIRES_NEW` | **挂起外层事务，我新建自己的** — 次常用 |
| **NOT_SUPPORTED** | `NOT_SUPPORTED` | 挂起外层事务，我**非事务**跑（适合不想被事务拖的查询） |
| **NEVER** | `NEVER` | 不能在事务里，有事务抛异常 |
| **NESTED** | `NESTED` | **外层事务的嵌套事务**（savepoint 机制） |

### 场景化对比 —— REQUIRED vs REQUIRES_NEW vs NESTED

假设 ServiceA.methodA() 有事务，里面调用 ServiceB.methodB()：

```java
@Service
public class ServiceA {
    @Transactional(propagation = REQUIRED)  // 外层事务 Tx1
    public void methodA() {
        dao.saveA();
        serviceB.methodB();   // 决定用哪种传播行为
        dao.saveA2();
        // methodA 最后 throw RuntimeException
    }
}
```

**三种传播行为的结果对比：**

| 场景 | 传播行为 | methodB 有无新事务 | methodB 内部异常时 methodB 的 DB 操作 | methodA 异常时 methodB 的 DB 操作 |
|:---|:---|:---:|:---|:---|
| 1 | REQUIRED | ❌（加入 Tx1） | **都回滚**（和 methodA 共享事务） | 回滚 |
| 2 | REQUIRES_NEW | ✅（新事务 Tx2，挂起 Tx1） | **只回滚自己 Tx2**（Tx1 不感知） | **不回滚**（Tx2 已独立提交）✅ |
| 3 | NESTED | 部分：savepoint，不新建物理事务 | **只回滚到 savepoint**（Tx1 本身还在） | **跟着外层回滚**（savepoint 是同一事务） |

### 生产实战选型

| 业务场景 | 推荐传播行为 | 理由 |
|:---|:---|:---|
| **普通 CRUD Service 方法**（默认） | **REQUIRED** | 被上层事务包进去，不单独开事务；自己单独调用时自动开一个 |
| **写日志、审计、埋点**（失败不能影响主流程） | **REQUIRES_NEW** | 即使主流程炸了，日志也要提交进去 |
| **批量发通知、发消息**（可以在事务外） | **NOT_SUPPORTED** | 不让长事务把数据库连接占住 |
| **校验配置是否存在**（只读） | **SUPPORTS** | 有事务就读一致快照，没事务也不额外开 |
| **Dao 层更新，必须要有外层事务调用** | **MANDATORY** | 防止有人在非事务里调用导致脏数据 |
| **子流程：主流程成功了才提交，子流程自己失败回滚不影响主流程** | **NESTED** | 典型：主订单 + 子订单；子订单校验失败不影响主订单但主订单失败子订单一起回滚 |

> **NESTED 的前提条件**：需要用的是 **JDBC 3.0+ savepoint 支持**的 DataSource（HikariCP、Tomcat JDBC Pool 都支持）+ **DataSourceTransactionManager** 作为事务管理器。JTA 分布式事务通常不支持 NESTED。

---

## 五、@Transactional 失效的 8 大场景（高频中的高频）

这是面试必问 Top 3，背 8 条按套路口述 + 1 条项目真实坑 STAR 结构讲：

### 场景 1：非 public 方法

```java
@Service
public class OrderService {
    @Transactional
    void save(Order o) { ... }  // ❌ 包访问级别！
}
```

**原因**：Spring AOP（CGLIB/JDK 代理）默认只代理 public 方法。protected / package / private 不代理 → 调用直接走到原对象。

> **Spring 版本特殊**：Spring 5.3 加了 `exposeProxy` / CGLIB 有 `protected` 代理支持，但生产代码永远写 **public** 最安全。

### 场景 2：类内部自调用（this.xxx / super.xxx）

本文前面详述过的 this 绕过代理问题。**90% 的事务失效场景都是这个**。

### 场景 3：捕获异常没重新抛出

```java
@Transactional(rollbackFor = Exception.class)
public void save(Order o) {
    try {
        dao.insert(o);
        int a = 1 / 0;
    } catch (Exception e) {
        log.error("出错啦", e);
        // ❌ 吞掉了！Spring 不知道发生了异常 → 居然 commit 成功了！
    }
}
```

**原因**：Spring 事务切面靠 catch RuntimeException 才触发 rollback。你把异常吃了，切面以为方法正常 return 了 → 提交。

**正确写法**：catch 后要么 `throw e;` 要么手动 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();`

### 场景 4：数据库引擎不支持事务

```java
@Transactional
public void save() { ... }  // 业务代码看似没问题
```

但如果 `create table xxx engine=MyISAM;` —— MyISAM **根本没有事务**。Spring 切面会成功 commit / rollback，但 MySQL 层静默忽略 rollback，数据已经写进去了。

> 现在 MySQL 8.0 默认 InnoDB，不会遇到；但如果接手老库或者迁移过的库先确认存储引擎：`show create table xxx;`

### 场景 5：方法被 final 修饰（只有 CGLIB 场景）

```java
@Transactional
public final void save(Order o) { ... }  // ❌ CGLIB 子类无法覆盖 final 方法
```

JDK 代理不受影响（接口方法不能 final）。但 Spring Boot 默认 CGLIB，final 方法直接忽略代理逻辑。

### 场景 6：多线程 / 异步方法

```java
@Service
public class OrderService {
    @Transactional
    public void save(Order o) {
        new Thread(() -> dao.insertDetail(o)).start();  // 新线程不在事务 ThreadLocal 里
    }

    @Async
    @Transactional
    public void saveAsync(Order o) {
        // @Async 在另一个线程池线程执行，事务切面和异步切面顺序也会出错
    }
}
```

**原因**：事务 Connection 绑定的是 ThreadLocal。新线程 → 没有绑定 Connection → 要么每句 SQL 单独开连接/单独事务，要么拿不到 Connection NPE。

### 场景 7：Bean 没有被 Spring 管理

```java
// 没有 @Service / 没有 @Component 注解，或者是 new 出来的
public class OrderService {
    @Transactional public void save() {}
}

// 外部调用
OrderService service = new OrderService();  // 自己 new 的，Spring 根本不知道这个类
service.save();  // ❌ 不可能有代理
```

### 场景 8：rollbackFor 配置错误（默认只回 RuntimeException）

```java
@Transactional // 默认不回滚 SQLException！
public void save() throws SQLException {
    jdbcTemplate.update("INSERT ...");
    throw new SQLException("出错");  // ❌ 不回滚！
}
```

生产最佳实践：`@Transactional(rollbackFor = Exception.class)` 当模板用。

---

## 六、易错点

| 易错点 | 说明 |
|:---|:---|
| **以为 CGLIB 代理一定比 JDK 快** | 代理创建 CGLIB 慢很多（ASM 字节码生成 + 类加载）；调用时 CGLIB FastClass 确实更快。短生命周期 Bean 用 JDK 反而综合更快。Spring Boot 选 CGLIB 是因为类型兼容不是性能 |
| **以为 @Transactional 可以加在接口上** | 可以加，JDK 代理会继承；但 CGLIB 类代理时接口上的注解有时不生效（取决于具体版本）。生产一律**写在 public 类方法上**。另外 `private`/`static`/`final` 都不行 |
| **REQUIRES_NEW 连接泄漏风险** | 挂起外层事务时外层的 Connection 被暂存在 stack，内层拿新 Connection。如果 ServiceA 嵌套 REQUIRES_NEW 调用 5 层，瞬间占用 5 个 Connection。HikariCP 连接池默认只有 10 个，并发 3 个请求就打爆了。注意层级不要太深 |
| **@Transactional 方法内调外部 RPC** | 事务持有 Connection 期间阻塞等 HTTP 响应 → 连接池被占。事务内不要做远程调用。正确做法：先 RPC 拿数据，再在事务里只做纯 DB 操作 |
| **只读事务 readOnly=true 的作用** | Hibernate 会刷 FlushMode.MANUAL，JdbcTemplate 会给 Connection 设置 readonly 标记（MySQL 路由到从库读）。性能没传说中那么大，但语义清晰——所有只做 select 的方法都加 readOnly |
| **同一个类里两个 @Transactional 自调用** | 不生效。理由同场景 2；除非在 A 方法里用 `AopContext.currentProxy()` 调 B 方法，或者拆类 |
| **NESTED 外层没事务** | NESTED 要求外层必须有事务，否则等价于 REQUIRED（新建事务）。和 REQUIRES_NEW 语义完全不同 |

---

## 七、一句话总结

Spring AOP 在初始化后阶段用 **JDK 动态代理（接口）/ CGLIB 子类代理（类，Spring Boot 默认）** 织入切面，围绕目标方法建拦截器链；`@Transactional` 通过**事务管理器 + ThreadLocal 绑定 Connection** 保证同一事务复用同一连接，7 种传播行为按「是否新建/挂起/嵌套」分层，**REQUIRED 是默认加入 / REQUIRES_NEW 独立事务不回外滚 / NESTED 是外层 savepoint 子事务**；**8 大失效场景**以「非 public / 自调用 / 吞异常 / 不支持事务引擎 / final / 多线程 / new 对象 / rollbackFor 未配置」为面试答纲，按 STAR 结构准备 1~2 个真实案例最加分。

---

## 八、相关笔记

| 主题 | 笔记 |
|:---|:---|
| DB 端事务隔离级别与 MVCC（Spring isolation=DEFAULT 时继承 DB 设置） | [MySQL/事务与锁.md](../mysql/事务与锁.md) |
| redo/undo/binlog 两阶段提交（Spring 事务底层的 commit/rollback 最终落库） | [MySQL/存储引擎与架构.md](../mysql/存储引擎与架构.md) |
| Bean 生命周期中 BeanPostProcessor 何时生成 AOP 代理 | [IOC与Bean生命周期.md](IOC与Bean生命周期.md) |
| 分布式事务 Seata AT/TCC 模式 | [SpringCloud微服务.md](SpringCloud微服务.md) |
