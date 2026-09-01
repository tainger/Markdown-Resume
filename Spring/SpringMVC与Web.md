# Spring MVC 与 Web

Spring MVC 是基于 Servlet 规范（传统）的 Web 框架，Spring Boot Web 默认内嵌 Tomcat 启动时自动装配；Spring Boot 3.x 同时提供 WebFlux（响应式）选项，面试会问 MVC vs WebFlux 选型。

---

## 一、Servlet、Spring IoC、Spring MVC 三者关系

面试容易被问「**什么是父子容器**？」「**DispatcherServlet 为什么能拿到 Service 的 Bean**？」

### 历史：Spring MVC 以前的双容器

Spring 5.x 以前，传统 web.xml 部署（打 war 放外部 Tomcat）是**父+子两个 ApplicationContext**：

```
        ┌──────────────────────────────────────────────┐
        │  父 Context（ContextLoaderListener 创建）     │
        │  - 配置文件：application-context.xml         │
        │  - Bean：Service / DAO / DataSource / Tx Mgr │ ← 业务层、无 Web 依赖
        │  - 不能访问子容器的 Bean                      │
        └──────────────────────▲───────────────────────┘
                               │ parent
        ┌──────────────────────┴───────────────────────┐
        │  子 Context（DispatcherServlet 自己创建）     │
        │  - 配置文件：spring-mvc-servlet.xml          │
        │  - Bean：Controller / ViewResolver / Handler │ ← Web 层
        │  - 可以访问父容器 Bean（@Service 能注入）     │
        └──────────────────────────────────────────────┘
                        Tomcat
```

### 现在：Spring Boot 单容器

Spring Boot 启动内嵌 Tomcat 时，**所有 Bean（Service + Controller + DataSource）都放在同一个 AnnotationConfigServletWebServerApplicationContext 里**，不再分父子——因为现在没有 web.xml 时代的 ContextLoaderListener 了。

```
         ┌───────────────────────────────────────────────┐
         │  AnnotationConfigServletWebServerAppContext   │
         │   （单容器，没有父子）                        │
         │                                               │
         │  Service Bean        Controller Bean          │
         │  DAO Bean             ViewResolver Bean       │    Tomcat（内嵌）
         │  DataSource Bean      HandlerMapping Bean     │    │
         │  TransactionManager   HandlerAdapter Bean     │    │
         └───────────────────────────────────────────────┘
```

> 但 DispatcherServlet 内部仍然维护着一套「自己的 Web 组件」（HandlerMappings / HandlerAdapters / ViewResolvers 等），当有请求时，先从自己的 WebApplicationContext 找组件，找不到再去 parent ApplicationContext（现在其实就是同一个容器）找。

---

## 二、DispatcherServlet 9 步请求处理流程

面试口述提纲：

```
           用户请求 http://localhost:8080/order/123
                        │
                        ▼
         ┌─ Tomcat（或 Jetty / Undertow）线程池 ──┐
         │  1. 连接解析：TCP 包 → HTTP Request       │
         │  2. 根据 web.xml / filter 映射执行 Filter 链
         │  3. 找 Servlet：匹配 *.do 或 / → DispatcherServlet
         └──────────────────┬───────────────────────┘
                            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ DispatcherServlet.doDispatch()：                             │
   │                                                              │
   │  ┌─ Step 1  checkMultipart：判断是不是文件上传 multipart     │
   │  │     是 → 用 MultipartResolver 把 HttpServletRequest 包成 │
   │  │             MultipartHttpServletRequest（里面有多文件）  │
   │  │                                                          │
   │  ├─ Step 2  ★ getHandler（找 Handler）：                    │
   │  │     遍历所有 HandlerMapping：                            │
   │  │     ├─ RequestMappingHandlerMapping：                    │
   │  │     │   匹配 @RequestMapping 注解 → HandlerMethod        │
   │  │     │   → 找到 OrderController.order(Long id) 方法       │
   │  │     │   → 包装成 HandlerExecutionChain（带拦截器列表）   │
   │  │     ├─ BeanNameUrlHandlerMapping：过时                    │
   │  │     └─ SimpleUrlHandlerMapping：静态资源映射             │
   │  │     找不到 → send 404 NoHandlerFoundException            │
   │  │                                                          │
   │  ├─ Step 3  ★ getHandlerAdapter（找适配器）：               │
   │  │     根据 Step 2 返回的 Handler 类型找对应的 Adapter：     │
   │  │     - HandlerMethod → RequestMappingHandlerAdapter       │
   │  │       （Spring MVC 3.1+ 默认，注解驱动核心）             │
   │  │     - Controller（接口）→ SimpleControllerHandlerAdapter │
   │  │     - HttpRequestHandler → HttpRequestHandlerAdapter    │
   │  │     找不到 → 抛 ServletException                         │
   │  │                                                          │
   │  ├─ Step 4  ★ 拦截器 preHandle：                           │
   │  │     HandlerExecutionChain.applyPreHandle(request, response)│
   │  │     → 按 order 升序遍历所有 HandlerInterceptor.preHandle │
   │  │     → 有一个 return false 就 triggerAfterCompletion 并结束│
   │  │                                                          │
   │  ├─ Step 5  ★ HandlerAdapter.handle（核心，执行业务逻辑）： │
   │  │     RequestMappingHandlerAdapter 做的事情：              │
   │  │     1. 把 HandlerMethod（要调用哪个类的哪个方法）绑定参数  │
   │  │        → HandlerMethodArgumentResolverComposite（26+种） │
   │  │           解析 @PathVariable / @RequestParam /           │
   │  │           @RequestBody / @Valid / Model / Map /          │
   │  │           HttpServletRequest / @RequestHeader 等        │
   │  │     2. 调用实际的 Controller 方法（反射 invoke）         │
   │  │        → return "order/view" 或 Order 对象 或 ResponseEntity │
   │  │     3. 包装成 ModelAndView（即使 @ResponseBody 也是 MAV  │
   │  │        只是 model 里有特殊标记 RequestResponseBodyMethodProcessor）│
   │  │     4. 调用 HandlerMethodReturnValueHandlerComposite    │
   │  │        处理返回值（@ResponseBody 走 Jackson 写回 JSON）   │
   │  │                                                          │
   │  ├─ Step 6  拦截器 postHandle（注意：视图渲染前，这时已经有  │
   │  │     MAV 了，postHandle 可以再给 Model 插数据）           │
   │  │                                                          │
   │  ├─ Step 7  render 渲染视图：                               │
   │  │     （如果 @ResponseBody 已在 Step 5 写回，这步被跳过）  │
   │  │     viewResolver.viewName("order/view")                  │
   │  │     → View render(Model, request, response)             │
   │  │     → Thymeleaf / Freemarker / JSP 模板渲染              │
   │  │                                                          │
   │  └─ Step 8  拦截器 afterCompletion：                        │
   │       （无论成功失败，finally 级别的）                     │
   │       → 按 order 降序调用所有 HandlerInterceptor.afterCompletion │
   │       清理 ThreadLocal、埋点 traceId、计算耗时              │
   └─────────────────────────────┬──────────────────────────────┘
                                 ▼
                       写回 Response → 关闭连接（或 Keep-alive）
```

### 异步请求处理（Callable / DeferredResult / WebAsyncTask）

如果 Controller 方法返回 `Callable(T)` 或 `DeferredResult(T)`，Step 5 内部会触发异步：

```java
@GetMapping("/async/long-task")
public DeferredResult/*<String>*/ longTask() {
    DeferredResult/*<String>*/ result = new DeferredResult(30000L);  // 30s 超时，泛型写为注释避免 Markdown 被解析为 HTML 标签
    threadPool.submit(() -> {
        Thread.sleep(5000);  // 这里不会占 Tomcat 的请求线程！
        result.setResult("done");  // 通知 Spring 继续处理
    });
    return result;  // 立即返回，Tomcat 请求线程还给连接池
}
```

**为什么要异步？** —— Tomcat 请求线程默认 200 个，如果所有请求都要 5 秒 RPC，那 40 QPS 就打满线程池。用 DeferredResult，Tomcat 线程立即还回去，真正工作在业务线程池。能把 40 QPS 提升到几千 QPS（只要业务线程池够）。

---

## 三、参数解析与返回处理（HandlerMethodArgumentResolver）

Spring MVC 有 26+ 种参数解析器，面试高频问 6 种最常用的和执行顺序：

| 参数标注 / 类型 | 对应 Resolver | 说明 |
|:---|:---|:---|
| `@PathVariable` | PathVariableMethodArgumentResolver | 从 URL 路径占位符取：`/{id}` |
| `@RequestParam` | RequestParamMethodArgumentResolver | 从 QueryString 或 Form Data 取，默认值 required=true |
| `@RequestBody` | RequestResponseBodyMethodProcessor | 读整个 Request Body，用 HttpMessageConverter（Jackson/Gson）转 Java 对象；支持 `@Valid` 触发校验 |
| `HttpServletRequest` / `HttpServletResponse` | ServletRequestMethodArgumentResolver | 直接拿 Servlet 原生对象 |
| `@RequestHeader` | RequestHeaderMethodArgumentResolver | 取指定 Header 值 |
| `@CookieValue` | ServletCookieValueMethodArgumentResolver | 取 Cookie 值 |
| `Map / Model / ModelMap` | MapMethodProcessor | 给方法注入 Model 对象，Controller 可以往里面塞数据 |
| `@ModelAttribute` | ServletModelAttributeMethodProcessor | 把 Query 参数绑定到 JavaBean（不加注解时对 POJO 参数默认生效） |
| `@RequestPart` | RequestPartMethodArgumentResolver | Multipart/form-data 单个文件上传 |
| Java 8 `LocalDateTime` 等 | `@DateTimeFormat` 或配置全局 Formatter | ISO-8601 解析 |

### 最容易被问：`@ModelAttribute` vs `@RequestBody`

| 维度 | @ModelAttribute | @RequestBody |
|:---|:---|:---|
| **Content-Type** | application/x-www-form-urlencoded、multipart/form-data、GET QueryString | application/json（必须带这个 Header） |
| 绑定方式 | 字段对字段 setter 绑定（`name=Tom&age=18` → User.setName("Tom")） | Jackson/Gson JSON 反序列化（`{"name":"Tom"}` → User 对象） |
| 支持校验 | 支持 @Valid | 支持 @Valid + 支持 @Validated（分组校验） |
| 文件上传 | 能和 MultipartFile 一起用 | 不能（JSON 里塞 base64 文件很不规范） |
| 嵌套对象 / 数组 | 可以（`user.addr.city=Beijing` 这种写法麻烦） | 天生支持 JSON 嵌套结构 |
| 默认 | Controller 方法的 POJO 参数没加注解时，**默认走 @ModelAttribute** | 必须显式写 `@RequestBody` |

### 返回值处理：@ResponseBody 工作原理

1. Controller 返回对象（如 User user）
2. `HandlerMethodReturnValueHandlerComposite` 找匹配的 Handler：
   - RequestResponseBodyMethodProcessor（处理 `@ResponseBody` 注解的方法或类）
3. 选择一个 `HttpMessageConverter`：
   - 优先按请求头 Accept：`application/json` → MappingJackson2HttpMessageConverter
   - Accept: `application/xml` → MappingJackson2XmlHttpMessageConverter（如果 jackson-dataformat-xml 在 classpath）
4. 用 `ObjectMapper.writeValueAsString(user)` 转 JSON
5. 写入 `response.getOutputStream()`，同时设置响应头 `Content-Type: application/json`
6. 返回的 `ModelAndView = null` → Step 7 的 render 渲染步骤被跳过

---

## 四、拦截器 vs Filter（面试经典对比）

| 维度 | Filter（Servlet 原生） | HandlerInterceptor（Spring 特有） |
|:---|:---|:---|
| **规范所属** | Java Servlet 规范（javax.servlet） | Spring MVC 框架 |
| **触发时机** | DispatcherServlet 之前（Tomcat 层） | DispatcherServlet 内部的 doDispatch 流程中（Step 4/6/8） |
| **是否能拿到 Spring 容器的 Bean** | ❌ 不行（Filter 是 Servlet 管理的）。要拿 Bean 只能通过 `ApplicationContextAware` / SpringBeanAutowiringSupport 或 DelegatingFilterProxy 桥接 | ✅ 可以直接 `@Autowired` Service / DAO |
| **能否拿到 Controller 方法信息**（注解、方法名） | ❌ 不行（Filter 只看 request/response，连要调哪个 Controller 都还没定） | ✅ 可以从 handler 对象拿到 Method（在 preHandle 里拿 `(HandlerMethod) handler` → `getMethod()` → 能读方法上的 `@RequiresAuth` 自定义注解做权限） |
| **粒度** | 粗（URL Pattern：所有 `/*` 或 `/api/*`） | 细（按 @RequestMapping 粒度，甚至可以 exclude 某些 Path） |
| **与 Spring 事务联动** | 不行（事务还没开始） | 可以——preHandle 在事务前，postHandle 在事务后 |
| **典型应用** | 编码过滤器（CharacterEncodingFilter）、CORS、安全认证（Spring Security 用的是 Filter 链 DelegatingFilterProxy）、请求体日志打印 | 登录鉴权（基于 `@RequiresAuth` 注解）、权限拦截、TraceId 设置、操作日志（preHandle 记录开始时间，afterCompletion 记耗时入库）、国际化 Locale 切换 |

### 自定义 HandlerInterceptor 三步走

```java
// Step 1: 实现 HandlerInterceptor
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        // 静态资源请求，handler 是 ResourceHttpRequestHandler，不是 HandlerMethod，跳过
        if (!(handler instanceof HandlerMethod hm)) return true;
        
        // 读方法/类上的自定义注解
        if (hm.getMethodAnnotation(RequiresAuth.class) == null
         && hm.getBeanType().getAnnotation(RequiresAuth.class) == null) {
            return true;  // 没加注解，放行
        }
        
        String token = req.getHeader("Authorization");
        if (token == null || !tokenService.validate(token)) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;  // 拦截
        }
        req.setAttribute("userId", tokenService.parse(token).userId);
        return true;
    }
}

// Step 2: 注册（实现 WebMvcConfigurer）
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")              // 拦截 API 路径
                .excludePathPatterns("/api/login", "/api/public/**")  // 放行白名单
                .order(0);
    }
}

// Step 3: 自定义注解加在 Controller 方法上
@RequiresAuth
@GetMapping("/api/orders")
public List<Order> list() { ... }
```

---

## 五、全局异常处理

Spring MVC 提供 3 层异常处理机制，优先级从高到低：

| 优先级 | 机制 | 粒度 | 典型应用 |
|:---:|:---|:---|:---|
| 1（最高） | `@ExceptionHandler` 加在具体 **Controller 类内部** | 单 Controller 内 | 专门处理订单 Controller 的库存不足异常 |
| 2 | `@ControllerAdvice` + `@ExceptionHandler`（`@RestControllerAdvice` = `@ControllerAdvice + @ResponseBody`） | **全局**，所有 Controller（可以指定 basePackage / annotations 缩小范围） | ★ **生产标准方案**，统一处理所有业务异常、校验异常、鉴权异常 |
| 3（最低） | 实现 `HandlerExceptionResolver` / 配置 `SimpleMappingExceptionResolver` | 全局，Spring MVC 原生底层 | 跳传统错误页面（JSP/Thymeleaf），现在很少用 |
| 兜底 | Spring Boot error page（默认 `/error`，BasicErrorController） | 兜底 | 框架处理不了的 Servlet 级异常（404、Filter 里抛的） |

### `@RestControllerAdvice` 生产模板

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. 业务异常（自定义 BizException，含 code / msg）
    @ExceptionHandler(BizException.class)
    public ResponseEntity/*<Result<?>>*/ biz(BizException e) {
        log.warn("业务异常：{}", e.getMessage());  // warn 级别，不打堆栈
        return ResponseEntity.status(400)
            .body(Result.fail(e.getCode(), e.getMessage()));
    }

    // 2. 校验异常（@Valid 校验不通过）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity/*<Result<?>>*/ valid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ":" + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", msg);
        return ResponseEntity.status(400).body(Result.fail("PARAM_ERR", msg));
    }

    // 3. 权限异常（Spring Security AccessDeniedException）
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity/*<Result<?>>*/ deny(AccessDeniedException e) {
        return ResponseEntity.status(403)
            .body(Result.fail("FORBIDDEN", "无权限访问"));
    }

    // 4. 兜底 Exception（其他所有未捕获）
    @ExceptionHandler(Exception.class)
    public ResponseEntity/*<Result<?>>*/ any(Exception e) {
        log.error("系统异常：", e);  // ★ 打 full stack，生产排查依赖这个
        return ResponseEntity.status(500)
            .body(Result.fail("SYS_ERR", "系统繁忙，请稍后重试"));  // 对用户只显示友好文案
    }
}
```

### ⚠️ `@ExceptionHandler` 的两个经典坑

1. **Filter / Interceptor 之前抛的异常抓不到** —— `@RestControllerAdvice` 是 Spring MVC DispatcherServlet 内部处理的，**Filter 里抛异常连 DispatcherServlet 都还没进**，自然拿不到。Servlet 级 Filter 抛异常要靠 Spring Boot 的 BasicErrorController 处理（可以自定义 `ErrorController`）。
2. **AOP `@Around` 捕获异常后没重新抛出** —— 如果自定义 AOP 切面在 `@Around` 里 `catch(Exception e){ log.error(e); return null; }`，那么 `@ExceptionHandler` 永远不会被触发（因为 AOP 已经把异常吃了）。正确做法：AOP 切面里只打日志，不吞异常，catch 后继续 `throw e;`。

---

## 六、Spring MVC vs Spring WebFlux（响应式）选型

| 维度 | **Spring MVC（Servlet 阻塞）** | **Spring WebFlux（Reactive 非阻塞）** |
|:---|:---|:---|
| 底层 API | Servlet（阻塞 IO）、每请求一线程 | Reactor（Flux / Mono）、Netty NIO EventLoop |
| 并发模型 | 线程池 200~500，每个请求占一个线程直到响应写完 | Netty EventLoop 默认 CPU 核数 * 2，不阻塞，万级连接少量线程搞定 |
| 适用场景 | **90% 的企业业务**（CRUD、事务、RPC）——写起来简单，ThreadLocal / DB 事务 / AOP 稳定 | 高并发长连接场景：网关、推送、聊天、SSE、流控 API、背压控制 |
| 学习曲线 | 低（同步阻塞思维，和普通 Java 一样） | 高（必须理解响应式、背压、onError 处理），用不好更差（"回调地狱 + Mono/Flux 泄漏"） |
| JDBC / 事务支持 | 完美支持（JDBC 本身就是阻塞 API） | ❌ 原生 JDBC 不支持，R2DBC 才是响应式数据库连接池，但生态不如 JDBC 成熟；事务要用 R2DBC Proxy |
| 对 Redis/消息队列支持 | 同步 lettuce / Spring Data Redis | Reactor 版 lettuce + Reactor Kafka / RabbitMQ Flux |
| 压测性能（同硬件） | QPS 中（受限于线程池大小 + RPC 阻塞时间） | **QPS 2~3 倍**（但前提是全链路都响应式——DB 用 R2DBC、Redis 用 Reactive、下游也是 WebFlux。只要中间有一个阻塞调用，EventLoop 就卡住了，性能反不如 MVC） |

> **选型口诀**：**99% 业务系统选 Spring MVC**。WebFlux 只有在你真的需要 10 万连接的网关 / 直播弹幕 / 实时推送这种长连接+高并发场景时才值得。不要为了"用新技术"硬上 WebFlux——全链路响应式改造成本极高，事务/ThreadLocal/日志全要重写。

---

## 七、易错点

| 易错点 | 说明 |
|:---|:---|
| **以为 Filter 能通过 handler 参数拿 Controller 方法** | Filter 在 DispatcherServlet 之前执行，Step 2 getHandler 还没跑，根本不知道要调哪个 Controller。需要方法级权限控制只能用 HandlerInterceptor。 |
| **`@RequestParam` 默认 required=true** | 参数没传直接 400 Bad Request，不传的加 `required=false` 或 `defaultValue` |
| **`@PathVariable` / `@RequestParam` 接收路径变量，但 URL 里没写对应占位符** | `@GetMapping("/order/{id}")` 接收 `@PathVariable("orderId")` → 空指针异常或 500，名字一定要对 |
| **`@Valid` 加在 @RequestBody 参数前才触发 Bean Validation** | 加在 @ModelAttribute 参数上也能校验，但如果 BindingResult 放在紧跟校验参数之后，会把错误收进 BindingResult 而不是抛 MethodArgumentNotValidException |
| **`@ExceptionHandler` 无法捕获 Filter 中抛的异常** | DispatcherServlet 还没进入，ControllerAdvice 管不到。Servlet 级过滤器异常只能用 ErrorController / 自定义 Filter 包 try-catch |
| **配置 `spring.mvc.throw-exception-if-no-handler-found=true` 让 404 走 ExceptionHandler** | 默认 Spring 找不到 handler 直接 forward 到 /error 走 BasicErrorController，不走 ControllerAdvice。加了这个配置后 404 会抛 NoHandlerFoundException，可以被 `@ExceptionHandler(NoHandlerFoundException.class)` 统一处理 |
| **`@ResponseBody` 的 ControllerAdvice 里返回对象，Spring 会用 Jackson 写回 JSON** | 如果加的是 `@ControllerAdvice` 而不是 `@RestControllerAdvice`，那 `@ExceptionHandler` 返回值默认当 ViewName 处理，需要加 `@ResponseBody` 在方法上才会写 JSON |

---

## 八、一句话总结

Spring Boot 现在是**单容器架构**（无父子容器），DispatcherServlet 通过 `doDispatch()` 按「`checkMultipart → getHandler(HandlerMapping 匹配 @RequestMapping + 拦截器链) → getHandlerAdapter(适配器) → 拦截器 preHandle → HandlerAdapter.handle（★ 26+ 参数解析器 → 反射调 Controller → 返回值处理器 → @ResponseBody Jackson 写 JSON）→ 拦截器 postHandle → render 视图渲染 → 拦截器 afterCompletion（finally 级）」9 步处理请求；参数解析按 Content-Type 分 `application/json` 走 `@RequestBody`、`form-urlencoded` 走 `@ModelAttribute`；全局异常优先用 `@RestControllerAdvice + @ExceptionHandler` 统一处理（Filter 级异常需另找 ErrorController）；90% 业务系统选 **Servlet 版 Spring MVC（同步阻塞好写好维护）**，只有网关、长连接、推送这类万级并发才值得上 WebFlux（前提是全链路响应式改造）。

---

## 九、相关笔记

| 主题 | 笔记 |
|:---|:---|
| Spring Boot onRefresh 阶段创建内嵌 Tomcat + WebApplicationContext | [自动配置与启动流程.md](自动配置与启动流程.md) |
| `@Transactional` 在拦截器 preHandle 之前还是之后？——AOP 代理在 Bean 创建生命周期 afterInitialization 生成，HandlerAdapter 调用时命中 | [事务.md](事务.md) |
| Bean 创建/代理流程（HandlerMethod 调用的 Controller 对象实际是 AOP 代理对象） | [IOC与Bean生命周期.md](IOC与Bean生命周期.md) |
| 微服务层 API 网关（Gateway Netty WebFlux vs Spring MVC） | [SpringCloud微服务.md](SpringCloud微服务.md) |
