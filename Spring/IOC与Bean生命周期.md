# Spring IOC 与 Bean 生命周期

IOC（Inversion of Control，控制反转）是 Spring 的基石——把对象创建/依赖注入的责任从业务代码**反转给容器**。理解 IOC = 理解 Spring 的本质。

---

## 一、BeanFactory vs ApplicationContext（必考）

这是最基础的一问，不能只答"后者是前者的超集"，要分层：

| 维度 | BeanFactory | ApplicationContext |
|:---|:---|:---|
| 加载方式 | **懒加载**（`getBean()` 时才实例化） | **启动时预实例化**所有非 lazy-init 的 singleton |
| 功能覆盖 | 仅核心 DI：Bean 注册 + 查找 + 生命周期 | BeanFactory 全部 + 国际化 + 事件发布 + 资源加载 + AOP + Web |
| 典型实现 | XmlBeanFactory（已废）、DefaultListableBeanFactory | ClassPathXmlApplicationContext、AnnotationConfigApplicationContext、SpringApplication |
| 启动速度 | 快（不实例化 Bean） | 慢（创建所有 singleton Bean） |
| 推荐使用场景 | 嵌入式/资源受限环境、BeanDefinition 调试 | **99% 生产场景都用 ApplicationContext** |

> 记忆：**BeanFactory 是骨架，ApplicationContext 是带血肉的产品**。生产永远用后者。

Spring Boot 启动后实际用的 `ApplicationContext` 实现取决于环境：
- Servlet Web → `AnnotationConfigServletWebServerApplicationContext`
- Reactive Web → `AnnotationConfigReactiveWebServerApplicationContext`
- 非 Web → `AnnotationConfigApplicationContext`

---

## 二、Bean 的作用域（Scope）

| 作用域 | 说明 | 适用场景 |
|:---|:---|:---|
| **singleton**（默认） | IOC 容器中只有 1 个实例（不是类加载器单例） | 无状态 Service/DAO/Controller |
| **prototype** | 每次 `getBean()` / 注入都创建新实例 | 有状态对象（每次请求独立数据） |
| **request**（Web） | 每次 HTTP 请求一个实例 | Request 级别有状态 Bean |
| **session**（Web） | 每次用户 Session 一个实例 | 登录态、购物车 |
| **application**（Web） | ServletContext 生命周期一个实例 | 整个 Web 应用共享的配置 |
| **websocket**（Web） | 一次 WebSocket 连接一个实例 | WebSocket 会话态 |

### ⚠️ singleton 注入 prototype 的经典陷阱

```java
@Service  // singleton
public class OrderService {
    @Autowired
    private Order order;  // prototype

    // 实际：Order 只注入了一次，永远是同一个对象（不是每次调用新对象）
    public Order create() { return order; }
}
```

**为什么？** singleton 在容器启动时注入一次，之后不会再去容器拿。

**三种解决方式**：
1. 实现 `ApplicationContextAware` 手动 `ctx.getBean("order")`
2. 用 `@Lookup` 注解（CGLIB 动态子类化，方法每次返回新 Bean）
3. 用 `ObjectFactory(T)` / `ObjectProvider(T)` 委托（Spring 官方推荐，尖括号写为代码形式避免解析为 HTML 标签）

```java
@Service
public class OrderService {
    @Autowired
    private ObjectProvider/*<*/Order/*>*/ orderProvider;

    public Order create() { return orderProvider.getObject(); }  // 每次新建
}
```

---

## 三、Bean 创建流程（生命周期）— 最核心概念

面试口述时按「实例化 → 属性填充 → 初始化 → 成品」四大段讲，每段插钩子：

```
                        BeanDefinition（XML/注解/扫描）
                                │
                                ▼
                      ┌──────────────────────┐
                      │ ① 实例化 Instantiation│ ← InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation
                      │   （构造器/工厂方法）  │
                      └──────────┬───────────┘
                                 ▼
                           ↘ 半成品对象，放入三级缓存
                                 │
                                 ▼
                      ┌──────────────────────┐
                      │ ② 属性赋值 Populate   │ ← 循环依赖时从三级缓存取对象填充属性
                      │   (@Autowired / set)   │
                      └──────────┬───────────┘
                                 ▼
                      ┌──────────────────────┐
│ Aware 回调          │ ③ BeanNameAware /      │
│ BeanClassLoaderAware│    BeanFactoryAware     │
│ ApplicationContext...│   ...                  │
                      └──────────┬───────────┘
                                 ▼
                      ┌──────────────────────┐
                      │ ④ 初始化前置           │ ← BeanPostProcessor.postProcessBeforeInitialization
                      │   （AOP 注解解析、    │    @PostConstruct 在此之前？不，@PostConstruct 是 InitDestroyAnnotationBeanPostProcessor 干的，在 beforeInitialization 里）
                      │    @PostConstruct 在这里触发）
                      └──────────┬───────────┘
                                 ▼
                      ┌──────────────────────┐
                      │ ⑤ 初始化 Initializing   │ ← afterPropertiesSet（接口）
                      │   Bean / init-method     │   init-method（XML/注解）
                      └──────────┬───────────┘
                                 ▼
                      ┌──────────────────────┐
                      │ ⑥ 初始化后置           │ ← BeanPostProcessor.postProcessAfterInitialization
                      │   （AOP 代理在这里   │    ★ AOP 代理生成！
                      │    织入，wrap 成 Proxy）│
                      └──────────┬───────────┘
                                 ▼
                    ★ 成品 Bean，放入 singletonObjects（一级缓存）
                                 │
                                 ▼
                      ┌──────────────────────┐
                      │ ⑦ 业务使用 / 销毁     │ ← @PreDestroy + DisposableBean.destroy()
                      └──────────────────────┘
```

### 生命周期钩子的执行顺序（按调用先后）

| 阶段 | 钩子 | 说明 |
|:---|:---|:---|
| 属性赋值之前 | `InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()` | 实例化前可直接返回自定义 Bean（绕过构造器） |
| 属性赋值之后 | `BeanNameAware.setBeanName()`、`BeanClassLoaderAware`、`BeanFactoryAware` | 依次注入元信息 |
| **容器级 Aware**（ApplicationContextAwareProcessor） | `EnvironmentAware`、`ApplicationContextAware`、`ApplicationEventPublisherAware` 等 7 个 | 注意：这是 BeanPostProcessor 触发的，不是原生 Aware 接口链 |
| 初始化前 | `BeanPostProcessor.postProcessBeforeInitialization()` | `@PostConstruct` 在此触发（`InitDestroyAnnotationBeanPostProcessor` 实现） |
| 初始化中 | `InitializingBean.afterPropertiesSet()` → `init-method` | 两个都执行，接口先 XML 后 |
| 初始化后 | `BeanPostProcessor.postProcessAfterInitialization()` | **AOP 动态代理在这里生成**（AbstractAutoProxyCreator） |
| 销毁前 | `@PreDestroy` → `DisposableBean.destroy()` → `destroy-method` | 和初始化顺序对应 |

### 一个最容易被问翻的问题

> **`@PostConstruct` 和 `afterPropertiesSet` 谁先执行？`postProcessBeforeInitialization` 又在哪？**

顺序是：
1. `postProcessBeforeInitialization()` 外层包
2. └─ 里面找 `@PostConstruct` 注解调用
3. 然后才是 `afterPropertiesSet()`（直接接口调用）
4. 然后才是 `init-method`
5. 最后 `postProcessAfterInitialization()`（AOP 代理生成）

所以：**`@PostConstruct` 先于 `afterPropertiesSet`**。因为 `@PostConstruct` 是 `postProcessBeforeInitialization` 触发的，在 `afterPropertiesSet` 前面一个阶段。

---

## 四、循环依赖与三级缓存

### 什么情况下出现循环依赖

```java
@Component
public class A { @Autowired private B b; }

@Component
public class B { @Autowired private A a; }
```

### Spring 能解决循环依赖的前提（缺一不可）

1. **必须是 singleton scope**（prototype 每次新建，无法缓存半成品）
2. **必须是属性/setter 注入**（构造器注入拿不到半成品对象）
3. **必须是非 final / 非 AOP 代理方式的类**（部分 AOP 场景更严格，见下）

### 三级缓存设计

| 缓存 | 变量名 | 存什么 | 什么时候放 |
|:---|:---|:---|:---|
| **一级缓存**（成品） | `singletonObjects` | 完整可使用的单例 Bean | 全部生命周期跑完之后 |
| **二级缓存**（半成品） | `earlySingletonObjects` | **实例化完成、属性未赋值**的早期 Bean 引用 | 当循环依赖需要提前暴露时，从三级缓存取后放进来 |
| **三级缓存**（工厂） | `singletonFactories` | `ObjectFactory` lambda：调用它会拿到 Bean 的早期引用 —— **AOP 场景返回代理、非 AOP 返回原对象** | **构造器刚调用完就放**（任何单例 Bean 都放，不管有没有循环依赖） |

> 核心认知：**所有 singleton Bean 都在实例化后立即塞入三级缓存**，不管有没有循环依赖。只有真的出现循环依赖（另一个 Bean 属性注入需要它）时，才从三级缓存拿，提前暴露。

### 循环依赖完整流程（A ↔ B）

```
创建 A:
  实例化 A → 放入 singletonFactories（三级缓存）
  属性注入 → 需要 B → 发现 B 还没创建
      ↓
创建 B:
  实例化 B → 放入 singletonFactories
  属性注入 → 需要 A → getSingleton("a")
    ├─ 一级缓存 singletonObjects 没 a
    ├─ 二级缓存 earlySingletonObjects 没 a
    └─ 三级缓存 singletonFactories 有 a 的 ObjectFactory
       └─ 调用 ObjectFactory.getObject()
          → 非 AOP：返回 A 原始对象引用
          → AOP：返回 A 已织入 advice 的代理对象引用 ✅（这是三级缓存存在的真正意义）
       └─ 把 A 从三级缓存搬到 earlySingletonObjects（二级缓存）
    └─ 拿到了 A（原始 or 代理），赋值给 B.a
   完成 B 初始化（afterPropertiesSet / init-method / AOP）
   B 放入一级缓存 singletonObjects
      ↓
回到 A 属性注入继续:
  拿到了 B 成品 → 赋值给 A.b
  A 完成初始化（afterPropertiesSet / AOP）
  ★ 检查：A 在二级缓存里的早期引用和最终成品是不是同一个对象
    ├─ 非 AOP：是同一个 → 直接搬去一级缓存
    └─ AOP：不是一个（早期返回代理，但 beforeInitialization 又想再包一次代理）
        → 抛 BeanCurrentlyInCreationException ❌ （除非提前暴露出的代理就是最终对象）
```

### 什么时候 Spring 解决不了循环依赖

| 场景 | 能否解决 | 原因 |
|:---|:---:|:---|
| singleton + 属性注入（非 AOP） | ✅ | 经典场景，完美处理 |
| singleton + 属性注入（AOP） | ⚠️ 大部分情况能 | 取决于 AbstractAutoProxyCreator 是否把代理提前生成在三级缓存钩子内 |
| prototype Bean + 循环依赖 | ❌ | prototype 不进缓存 |
| 构造器注入的循环依赖 | ❌ | 实例化阶段（构造器）就卡住了，还没到放三级缓存那一步 |
| `@Async` / `@Transactional` 混用造成早期引用不一致 | ❌ | @Async 用 AsyncAnnotationBeanPostProcessor（在 afterInitialization 才生成代理），和三级缓存钩子不同步 |

> **`@Async` 为什么特别容易炸循环依赖？** 因为 `@Async` 的代理不是 `AbstractAutoProxyCreator`（通用 AOP）这条链路生成的，它用的是独立的 `AsyncAnnotationBeanPostProcessor`，没有参与三级缓存的 `getEarlyBeanReference` 钩子。所以循环依赖时从三级缓存拿到的是原始 A 对象，但 B 初始化完回到 A，AsyncAnnotationBeanPostProcessor 在 `afterInitialization` 又要把 A 替换成代理 → 最终对象和早期引用不一致，直接抛异常。

---

## 五、BeanPostProcessor 扩展点（P7 必会）

Spring 给用户留的 4 个常用扩展钩子，区别别搞混：

| 扩展接口 | 触发阶段 | 典型应用 |
|:---|:---|:---|
| **BeanFactoryPostProcessor** | **Bean 实例化之前**，BeanDefinition 全部加载完 | 改 BeanDefinition（如 `PropertySourcesPlaceholderConfigurer` 解析 `${}` 占位符；Mybatis `MapperScannerConfigurer` 扫包生成 Mapper） |
| **BeanDefinitionRegistryPostProcessor** | BeanFactoryPostProcessor 之前，更早期 | 动态注册新 BeanDefinition（如 `MapperScannerConfigurer` 实际是它的子类） |
| **InstantiationAwareBeanPostProcessor** | **实例化前后**（构造器前后） | `postProcessBeforeInstantiation` 可直接返回自定义代理替代原 Bean；Spring Data JPA 生成 Mapper 代理 |
| **BeanPostProcessor** | **初始化前后**（构造器+属性都完了） | AOP、@PostConstruct 解析、@Autowired 注入解析、@Async 代理、ApplicationContextAwareProcessor 回调 |

> P7 级答题角度：知道 **BeanFactoryPostProcessor 动 BD、BeanPostProcessor 动 Bean 实例**——这是分水岭。能说出 MapperScannerConfigurer 实现了 BeanDefinitionRegistryPostProcessor 就是加分项。

---

## 六、易错点

| 易错点 | 说明 |
|:---|:---|
| **认为 singleton 是 JVM 全局单例** | 只是同一个 ApplicationContext 中唯一；多类加载器/多上下文各管各的 |
| **prototype Bean 的销毁回调** | Spring 只管理 singleton 的销毁（close 时）；prototype 创建后交给调用方，Spring 不再管销毁，`@PreDestroy` 也不会触发 |
| **构造器注入没有循环依赖问题？** | 不对。构造器注入也会有循环依赖，**只是 Spring 解决不了**。属性注入是 Spring 解决循环依赖的唯一合法姿势 |
| **三级缓存非 AOP 场景存在意义？** | 非 AOP 场景其实二级缓存就够用，但 Spring 不能提前知道你有没有 AOP，所以统一走 ObjectFactory 钩子；真正必须三级缓存的是「代理提前生成后保证单例唯一性」 |
| **@Autowired 按什么顺序匹配？** | 先 byType（Class 查），若有多个再 byName（字段名当 beanName）；有多个同类型且 `@Qualifier` / `@Primary` 都没指定就抛 `NoUniqueBeanDefinitionException` |
| **懒加载 `@Lazy` 解决什么？** | 注入时先给代理对象，真正调用方法时才去容器取。可以**用 `@Lazy` 解决构造器注入的循环依赖**（因为不再需要在构造器参数解析时立即创建目标 Bean） |

---

## 七、一句话总结

Spring IOC 以 **ApplicationContext** 为生产入口，按「实例化 → 属性赋值 → Aware → 初始化前（`@PostConstruct`）→ 初始化 → 初始化后（AOP 代理）」流水线创建 Bean；**singleton 属性注入**场景下的循环依赖由「三级缓存 + ObjectFactory 提前暴露 AOP 代理」优雅解决；扩展点按粒度分四类：改 BD 用 `BeanFactoryPostProcessor`、改实例生成用 `InstantiationAwareBeanPostProcessor`、改初始化前后用 `BeanPostProcessor`。

---

## 八、相关笔记

| 主题 | 笔记 |
|:---|:---|
| AOP 动态代理（BeanPostProcessor.postProcessAfterInitialization 生成代理） | [AOP.md](AOP.md) |
| Spring Boot 启动时何时创建 ApplicationContext | [自动配置与启动流程.md](自动配置与启动流程.md) |
| Spring MVC 的 DispatcherServlet 是特殊 Bean（WebApplicationContext） | [SpringMVC与Web.md](SpringMVC与Web.md) |
