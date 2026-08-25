# Spring 面试题笔记

面向 P7 备战——从**容器 → AOP/事务 → 自动配置启动 → Web → Cloud** 的完整 Spring 体系八股，每篇含对比表、ASCII 图解、易错点、一句话总结。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🧩 IOC 与 Bean 生命周期 | [IOC与Bean生命周期.md](IOC与Bean生命周期.md) | BeanFactory/ApplicationContext、作用域、循环依赖三级缓存、实例化 vs 初始化、生命周期钩子 |
| 2 | 🔒 AOP 与事务 | [AOP与事务.md](AOP与事务.md) | JDK/CGLIB 代理、@Transactional、7 种传播行为、失效场景、自调用陷阱 |
| 3 | 🏗️ 自动配置与启动流程 | [自动配置与启动流程.md](自动配置与启动流程.md) | @SpringBootApplication 拆解、SpringFactories / AutoConfiguration.imports、starter 原理、后置处理器扩展点 |
| 4 | 🌐 Spring MVC 与 Web | [SpringMVC与Web.md](SpringMVC与Web.md) | DispatcherServlet 9 步流程、拦截器 vs Filter、参数解析、异常处理、SpringBoot WebFlux 对比 |
| 5 | ⚙️ Spring Cloud 微服务 | [SpringCloud微服务.md](SpringCloud微服务.md) | 注册/配置中心（Nacos/Eureka/Consul/CAP）、Feign、Gateway、Sentinel、分布式事务 Seata |

## 高频「必背」清单（速查）

- **BeanFactory vs ApplicationContext**：前者懒加载、后者启动时预实例化所有非懒加载单例；后者是 BeanFactory 的超集，多了国际化/事件/资源加载/AOP/Web 特性
- **Bean 创建流程**：实例化（构造器）→ 属性注入 → 前置处理 → 初始化（@PostConstruct / afterPropertiesSet / init-method）→ 后置处理 → AOP 代理 → 放入单例池 → 销毁
- **三级缓存解决循环依赖**：earlySingletonObjects（半成品对象）、singletonFactories（代理钩子）、singletonObjects（成品）；**构造器注入的循环依赖无法解决**
- **AOP 底层**：Spring 默认 JDK 动态代理（接口）+ CGLIB（类）；SpringBoot 2.0 后默认 `spring.aop.proxy-target-class=true` 即 CGLIB 优先
- **7 种事务传播行为核心**：`REQUIRED`（默认，有则加入/无则新建）、`REQUIRES_NEW`（挂起外层/新建事务）、`NESTED`（savepoint 嵌套）
- **@Transactional 失效场景 Top 5**：① 非 public 方法 ② 类内部自调用（this 绕过代理）③ 捕获异常没回抛 ④ 数据库引擎不支持事务（MyISAM）⑤ 多线程/事务方法被 final 修饰
- **@SpringBootApplication 三件套**：`@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
- **自动配置核心开关**：Spring Boot 2.7 前 `META-INF/spring.factories`；Spring Boot 3.0+ 改为 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **DispatcherServlet 9 步简化记**：请求到 → HandlerMapping 找 Handler → HandlerAdapter 适配 → 拦截器 pre → 调用 Controller → 响应返回前 post → 渲染视图 → 触发 afterCompletion
- **Nacos vs Eureka vs Consul CAP 选择**：Nacos AP+CP 可选（AP 默认）、Eureka 纯 AP、Consul CP 强一致注册；**服务注册宁可旧可用，不要新不可用** → 中大型生产选 Nacos

## 学习建议

1. 按 1→5 顺序：容器是底层 → AOP/事务是日常最多坑 → 自动配置是 SpringBoot 魔法之源 → MVC 是 Web 基本功 → Cloud 是分布式进阶
2. 每篇「一句话总结」当作口述提纲，能复述即过关
3. 「易错点」是 P7 面试的细节陷阱（比如自调用不生效、SpringBoot 3.0 改了自动配置文件路径）
4. 结合真实项目讲案例：写 1 个循环依赖踩坑实例 + 1 个事务失效排查实例，STAR 结构讲

## 相关笔记

- JVM 双亲委派打破：Spring Boot LaunchedURLClassLoader 的双亲委派打破策略与自定义类加载（可串联 [JVM/类加载机制.md](../jvm/类加载机制.md)）
- MySQL 事务隔离级别与 MVCC：与 [MySQL/事务与锁.md](../mysql/事务与锁.md) 联动，Spring `@Transactional(isolation = DEFAULT)` 继承 DB 默认隔离
- 分布式事务 Seata AT/TCC 与 MySQL 两阶段提交：可串联 [MySQL/存储引擎与架构.md](../mysql/存储引擎与架构.md) 的 redo/binlog 两阶段提交
