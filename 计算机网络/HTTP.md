# HTTP：应用层的「通用语言」

HTTP 是基于 TCP 的**请求-响应**式文本协议（HTTP/2 起为二进制）。面试重点：报文结构、方法幂等性、状态码、缓存机制、Cookie/Session/Token、版本演进（1.0→1.1→2→3）。

---

## 一、报文结构

```
请求报文                              响应报文
──────────────────────              ──────────────────────
GET /index.html HTTP/1.1   ←请求行   HTTP/1.1 200 OK       ←状态行
Host: example.com          ┐         Content-Type: text/html ┐
User-Agent: curl/8.4       ├请求头    Content-Length: 1024    ├响应头
Accept: */*                ┘         Cache-Control: max-age=60┘
                           （空行）                           （空行）
（body 可选）              ←请求体    <html>...</html>      ←响应体
```

常见头速查：

| 头 | 说明 | 面试要点 |
|:---|:---|:---|
| `Host` | 目标主机+端口 | HTTP/1.1 **必带**，支撑一台服务器多站点（虚拟主机） |
| `Content-Length` / `Transfer-Encoding: chunked` | 定长 / 分块传输 | 动态生成内容用 chunked |
| `Connection: keep-alive` | 长连接 | HTTP/1.1 默认开启 |
| `User-Agent` / `Referer` / `Origin` | 客户端标识 / 来源 | Origin 用于 CORS |
| `Accept-Encoding: gzip` | 压缩协商 | 减少传输体积 |

---

## 二、方法与幂等性

| 方法 | 语义 | 幂等 | 安全（不改数据） |
|:---|:---|:---:|:---:|
| GET | 获取资源 | ✅ | ✅ |
| POST | 提交/创建（非幂等典型） | ❌ | ❌ |
| PUT | 全量更新（幂等：反复 PUT 结果一致） | ✅ | ❌ |
| PATCH | 部分更新 | 不保证 | ❌ |
| DELETE | 删除（删一次和删多次一样） | ✅ | ❌ |
| HEAD / OPTIONS | 要响应头 / 问服务端支持哪些方法（CORS 预检） | ✅ | ✅ |

**GET vs POST 高频追问**：

- 语义：GET 取资源（幂等、可缓存、可收藏），POST 提交数据。
- 参数位置：GET 在 URL（长度受浏览器限制，约 2KB~8KB），POST 在 body。
- 编码：GET 只支持 URL 编码，POST 支持多种（表单/JSON/multipart 上传文件）。
- 本质都是 TCP 上的报文，POST 也可以带 URL 参数——区别是**约定**而非能力。

---

## 三、状态码（面试必背）

| 类别 | 含义 | 常见 |
|:---|:---|:---|
| 1xx | 信息 | 101 Switching Protocols（WebSocket 升级） |
| 2xx | 成功 | 200、**204**（成功无内容）、**206**（断点续传 Range） |
| 3xx | 重定向 | **301 永久 / 302 临时 / 304 Not Modified（缓存命中）** |
| 4xx | 客户端错误 | 400 参数错、**401 未认证**、**403 无权限**、404、**429** 限流 |
| 5xx | 服务端错误 | 500 内部异常、**502 Bad Gateway**、**504 Gateway Timeout** |

易混对比：

- **301 vs 302**：永久迁移（浏览器会缓存，SEO 权重转移）vs 临时跳转（不改收藏夹）。
- **401 vs 403**：401 = 「你是谁？先登录」（未认证）；403 = 「我知道你是谁，但你没权限」（已认证）。
- **502 vs 504**：网关收到**非法/无响应**（后端挂了）vs 等后端**超时**（后端太慢）。排查：网关日志 → 后端是否 OOM/死锁/线程池打满。

---

## 四、HTTP 缓存（两板斧）

```
        浏览器请求资源
             │
   有 Cache-Control/Expires 且未过期？ ──是──→ 强缓存命中：直接用本地（不发请求）
             │否
   带 If-None-Match / If-Modified-Since 发请求（协商缓存）
             │
   服务端比对 ETag / Last-Modified：
   ├─ 一致 → 304 Not Modified（无 body，浏览器用本地副本，并刷新强缓存时间）
   └─ 不一致 → 200 + 新资源 + 新缓存标识
```

| 类型 | 字段 | 生效方式 |
|:---|:---|:---|
| **强缓存** | `Cache-Control: max-age=3600, no-cache, no-store, private/public`、`Expires`（HTTP/1.0，受本地时间影响已过时） | 不发请求，最快 |
| **协商缓存** | `ETag / If-None-Match`、`Last-Modified / If-Modified-Since` | 发请求比对，304 则无 body |

高频追问：

- **Cache-Control vs Expires**：前者 HTTP/1.1 优先级更高，且是相对时间；后者绝对时间受客户端时钟影响。
- **no-cache vs no-store**：no-cache = 可以缓存但**每次协商**；no-store = 禁止缓存（敏感数据）。
- **ETag vs Last-Modified**：ETag 内容哈希（1 秒内多次修改也能感知，优先级高）；Last-Modified 只到秒。
- **刷新行为**：地址栏回车走强缓存；F5 走协商缓存（带 If-* 头）；Ctrl+F5 强制刷新（不带缓存头）。

---

## 五、Cookie、Session、Token(JWT)

| 对比项 | Cookie | Session | Token（JWT） |
|:---|:---|:---|:---|
| 存哪 | **客户端**浏览器 | **服务端**（内存/Redis），Cookie 只存 sessionId | 客户端（localStorage/Cookie） |
| 安全性 | 易被窃取/XSS | sessionId 泄露即冒用；但服务端可主动失效 | 自包含签名，不可篡改但**无法主动作废** |
| 分布式支持 | — | 需集中存储（Redis）或粘性会话 | ✅ 天然无状态，适合集群 |
| 体积 | 4KB 上限 | 服务端无限制 | 较大（header.payload.signature） |
| 典型用途 | sessionId、画像追踪 | 登录态 | 前后端分离/微服务鉴权 |

JWT 结构 `header.payload.signature`：前两段 Base64（**只是编码不是加密，别放敏感信息**），signature 用服务端密钥对前两段签名防篡改。追问「JWT 怎么踢人下线」：签发黑名单存 Redis、短有效期 + refresh token 轮换。

---

## 六、版本演进：1.0 → 1.1 → 2 → 3

| 版本 | 关键特性 | 解决的问题 | 遗留问题 |
|:---|:---|:---|:---|
| **1.0** | 短连接（每请求新建 TCP） | — | 频繁握手开销大 |
| **1.1** | **长连接** keep-alive、管线化、Host 头、chunked、更多缓存字段 | 减少建连开销 | **应用层队头阻塞**（响应必须按序返回）、管线化实际没敢用 |
| **2** | **二进制分帧**、**多路复用**（一条连接并发多个流）、头部压缩 HPACK、服务器推送 | 解决 1.1 应用层队头阻塞 | **TCP 层队头阻塞**（丢一个包全部流等待）+ 建连慢 |
| **3** | 基于 **QUIC（UDP）**：流独立、**0/1-RTT 建连**、连接迁移（换网络不断线）、内置 TLS1.3 | 解决 TCP 层队头阻塞、弱网体验 | 部署/防火墙支持还在普及 |

一句话演进逻辑：**1.1 复用连接 → 2 并发流（应用层）→ 3 换掉 TCP（传输层）**，每一代都在治「队头阻塞」，只是病灶层级不同。

> HTTPS 相关（TLS 握手、证书、对称/非对称）单独成篇 → [HTTPS.md](HTTPS.md)。

---

## 七、易错点

| 易错点 | 澄清 |
|:---|:---|
| **GET 是幂等的所以不会产生副作用** | 语义约定 vs 实现自由：GET 也可能触发统计/日志副作用 |
| **304 是「没有响应」** | 304 是协商缓存命中的正常结果，无 body |
| **HTTP/2 解决了所有队头阻塞** | 只解决应用层；TCP 丢包仍会阻塞所有流，HTTP/3 才根治 |
| **JWT 放在 Cookie 和 localStorage 一样安全** | Cookie 带 HttpOnly 可防 XSS 读走；localStorage 更易被 XSS 偷 |
| **Session 在分布式下直接可用** | 需要粘性会话或集中存储（Spring Session + Redis） |
| **HTTP 端口只能是 80/443** | 只是默认约定，任意端口都可跑 HTTP |

---

## 八、一句话总结

HTTP = 请求-响应 + **无状态**（靠 Cookie/Session/JWT 记住用户）。背熟三板斧：**方法幂等性**、**状态码**（301/302/304、401/403、502/504）、**缓存**（强缓存直接用、协商缓存发 304）；版本演进主线是「治队头阻塞」：1.1 长连接 → 2 多路复用 → 3 换 QUIC。

---

## 九、相关笔记

| 主题 | 笔记 |
|:---|:---|
| TLS 握手与证书 | [HTTPS.md](HTTPS.md) |
| TCP 长连接、队头阻塞的传输层根源 | [TCP.md](TCP.md) |
| 静态资源加速（CDN） | [DNS与CDN.md](DNS与CDN.md) |
| 无状态登录与分布式 Session | [../分布式/分布式锁.md](../分布式/分布式锁.md) |
