# NIO 三大件与多路复用

> NIO 的高性能不是「非阻塞」三个字能解释的——真正的开关是 **Selector 背后的 epoll**：把「一个线程盯一万条连接」变成现实。

按「**NIO 三大件 → select/poll/epoll 演进 → LT/ET → Reactor/Proactor 线程模型**」组织，这是 P7 网络编程面试的核心战场。

---

## 一、NIO 三大件：Channel、Buffer、Selector

```
                 ┌──────────── Selector（选择器：一个线程）────────────┐
                 │   OP_ACCEPT / OP_READ / OP_WRITE / OP_CONNECT       │
                 └───────┬─────────────┬──────────────┬────────────────┘
                         ▼             ▼              ▼
              SocketChannel   SocketChannel   ServerSocketChannel
              （Channel：双向读写通道，替代单向的 Stream）
                         │
                         ▼
                   Buffer（数据容器：写满 flip() 切换读）
```

### 1. Channel（通道）——双向的流

- 与 Stream 区别：**可读可写**（流是单向的 InputStream/OutputStream）；
- 常用实现：`SocketChannel` / `ServerSocketChannel` / `DatagramChannel` / `FileChannel`；
- 支持直接内存读写与 scatter/gather（一次读写多个 Buffer）。

### 2. Buffer（缓冲区）——读写共用，靠指针切换

三个指针满足 `capacity ≥ limit ≥ position`：

| 方法 | 作用 | 易错 |
|:---|:---|:---|
| `flip()` | 写模式 → 读模式（`limit=position, position=0`） | 忘调 flip 读到残留数据 |
| `clear()` | 清空，回到写模式（数据未真删） | — |
| `compact()` | 把未读完数据挪到开头再切写模式 | 半包场景用它 |

**堆内 vs 堆外（DirectByteBuffer）**：Socket 写堆内 Buffer 时 JVM 会先拷到堆外再交给 OS；直接用堆外内存**省这一次拷贝**，代价是分配慢、受 `-XX:MaxDirectMemorySize` 限制（详见 [../jvm/内存结构.md](../jvm/内存结构.md)）。

### 3. Selector（选择器）——多路复用的 Java 抽象

- 一个线程把多个 Channel 注册上来，监听 4 种事件；
- `select()` 返回就绪集合，按事件类型分发处理；
- 底层依赖操作系统的 **select / poll / epoll**（Linux 默认 epoll）。

---

## 二、select / poll / epoll（必背，P7 必考）

### 1. select：位图 + 全量扫描

- fd 集合用**位图**表示，**默认上限 1024**；
- 每次调用都要把整个 fd 集合**从用户态拷到内核态**；
- 返回后线程要**线性遍历 O(n)** 找出就绪的 fd。

### 2. poll：数组版 select

- 用 pollfd 数组替代位图，**突破 1024 限制**；
- 但「全量拷贝 + O(n) 遍历」两大痛点依旧。

### 3. epoll：红黑树 + 就绪链表（Linux 2.6+）

三个 API 各司其职：

```java
epoll_create()  // 创建 epoll 实例（内核事件表）
epoll_ctl()     // 增/删/改关注的 fd（只在这一步拷贝一次 fd 进内核）
epoll_wait()    // 只返回「就绪链表」里的 fd，O(1)
```

高效的三点原因（必背）：

1. **红黑树管理 fd**：增删查 O(log n)，fd 只在 `ctl` 时拷一次，不用每次调用全量传入；
2. **事件回调 + 就绪链表**：fd 就绪时内核回调把它挂入就绪链表，`wait` 直接取结果，**不需要遍历全部 fd**；
3. 适合海量连接中**活跃比例低**的场景（长连接居多，就绪的永远是少数）。

**LT vs ET（高频追问）**：

| 触发模式 | 行为 | 要求 |
|:---|:---|:---|
| LT（水平触发，默认） | 只要还有数据没读完，**每次 wait 都会通知** | 编程简单 |
| ET（边缘触发） | 状态**变化时只通知一次** | 必须非阻塞 fd + 循环读到 `EAGAIN` |

### 三者对比（必背）

| 维度 | select | poll | epoll |
|:---|:---|:---|:---|
| 底层结构 | 位图 | 数组 | 红黑树 + 就绪链表 |
| fd 上限 | 1024 | 无硬上限 | 无硬上限（受内存） |
| fd 拷贝 | 每次全量 | 每次全量 | **ctl 时一次** |
| 就绪检测 | O(n) 遍历 | O(n) 遍历 | **O(1) 取就绪链表** |
| 触发模式 | LT | LT | LT / ET |
| 典型使用者 | 跨平台兼容 | 少用 | Redis、Nginx、Netty（Linux） |

> 面试话术：**select/poll 是「把一万个人名交给内核，内核喊一嗓子后你自己挨个问谁到了」；epoll 是「到了的自动站进就绪队列，你只看队列」**。

---

## 三、Reactor 与 Proactor（设计模式视角）

### Reactor：就绪通知（同步 IO）

事件驱动三步：**注册事件 → select 等就绪 → 分发给 handler**。按并发度三级演进：

```
① 单线程 Reactor（Redis 6.0 之前）
   accept + 读写 + 业务 全在一个线程
   缺点：一个慢业务阻塞所有连接

② 多线程 Reactor
   Reactor 线程只管 IO 事件，业务丢工作线程池

③ 主从 Reactor（Netty 默认 boss / worker）
   MainReactor：只管 accept
   SubReactor(多个)：管已接入连接的读写
                        │
   客户端 ──连接──► Boss ──accept──► 注册到某个 Worker EventLoop
                                        │
                              Worker: 读写事件 → Pipeline → 业务线程池
```

### Proactor：完成通知（异步 IO）

- 内核**完成等待 + 拷贝**后通知 handler 处理结果；
- 对应 AIO：Windows IOCP / Linux io_uring。

| 维度 | Reactor | Proactor |
|:---|:---|:---|
| 通知时机 | 「可以开始读了」（就绪） | 「已经读完了」（完成） |
| 拷贝由谁做 | 用户线程自己 | 内核 |
| IO 类型 | 同步 | 异步 |
| 实现 | epoll/kqueue | IOCP、io_uring |
| 用户 | Netty、Redis、Nginx | Windows 服务端、新生态 |

---

## 四、高频追问

**Q1：epoll 用了 mmap 共享内存吗？**
**没有**——高频错误答案。fd 与数据仍在内核，epoll 快靠的是「一次注册 + 回调就绪链表」减少拷贝和扫描，与 mmap 无关（mmap 是零拷贝话题，见 [零拷贝.md](零拷贝.md)）。

**Q2：Redis 单线程为什么快？**
内存操作 + 单线程 Reactor（epoll 多路复用，无锁无切换）+ 6.0 后 IO 线程并行读写、命令执行仍单线程。

**Q3：JDK Selector 有什么著名 bug？**
**空轮询 bug**：少量 fd 就绪却 `select()` 立即返回 0，导致 while 死循环 CPU 100%；Netty 用计数器（512 次阈值）检测后重建 Selector 规避。

---

## 易错点

1. **epoll ≠ mmap**：两个不同话题的答案被面试官当「背错答案」的典型，千万别混。
2. **ET 模式三件套**：非阻塞 fd + 循环读到 EAGAIN + 只能配 epoll；漏一条就是丢数据或死循环。
3. **selectedKeys 迭代要 remove**：不 remove 会导致同一事件被重复处理。
4. **flip 忘调**：读到上一轮的残留数据，半包/粘包问题雪上加霜。
5. **直接内存 OOM 类型不同**：`Direct buffer memory`，不是堆 OOM；`MaxDirectMemorySize` 默认≈堆大小。
6. **select 1024 是默认位图限制**：现代内核可调，但「每次全量拷贝 + O(n) 扫描」的结构性问题才是根本。

---

## 一句话总结

**NIO = Channel（双向通道）+ Buffer（指针切换的读写容器）+ Selector（多路复用器）**；多路复用演进主线是「select/poll 的全量拷贝 + O(n) 扫描」→「epoll 的一次注册 + 回调就绪链表 O(1)」，线程模型从单 Reactor 演进到主从 Reactor——Netty 就是主从 Reactor 的工业级实现。
