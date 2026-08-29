# IO 面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——从 IO 模型到 Netty，理解 Redis/Netty/Kafka 高性能背后的底层开关。

按「**IO 模型 → NIO 与多路复用 → 零拷贝 → Netty**」的主线组织，每篇均含对比表、图解、易错点、一句话总结。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🧭 IO 模型 | [IO模型.md](IO模型.md) | 阻塞/非阻塞 × 同步/异步、五种 IO 模型、BIO/NIO/AIO 对比与代码 |
| 2 | 🔀 NIO 与多路复用 | [NIO与多路复用.md](NIO与多路复用.md) | Channel/Buffer/Selector、**select/poll/epoll**、LT/ET、Reactor/Proactor |
| 3 | 📠 零拷贝 | [零拷贝.md](零拷贝.md) | 4 拷贝 4 切换、mmap/sendfile、Kafka/RocketMQ 选型、Java API |
| 4 | ⚡ Netty | [Netty.md](Netty.md) | 主从 Reactor、Pipeline、ByteBuf、**LengthFieldBasedFrameDecoder**、心跳与调优 |

## P7 必背清单（速查）

- **两个维度别混淆**：阻塞/非阻塞看「等数据时线程挂不挂起」；同步/异步看「数据从内核到用户空间谁来搬」——**多路复用是同步阻塞**（阻塞在 select/epoll_wait）
- **五种 IO 模型**：阻塞、非阻塞轮询、多路复用、信号驱动、异步；**前四种拷贝阶段都是同步，只有 AIO 内核全托管**
- **BIO/NIO/AIO**：1 连接 1 线程（1 万连接=1 万线程） / Selector 同步非阻塞 / 内核完成回调（Linux 是 epoll 模拟，Netty 4 已移除 AIO）
- **epoll 快的三点**：红黑树存 fd（ctl 时拷一次）+ 事件回调进就绪链表（wait 取就绪 O(1)）+ 不用每次全量拷贝 fd；**epoll 没用 mmap**（高频错误答案）
- **LT/ET**：LT 不读完一直通知；ET 只通知一次，必须非阻塞 fd + 循环读到 EAGAIN
- **Reactor 三级演进**：单线程（Redis 6 前）→ 多线程（业务工作池）→ **主从（Netty boss/worker）**；Proactor 是「完成通知」对应 AIO
- **零拷贝路径**：read+write = 4 拷贝（2DMA+2CPU）4 切换 → mmap 省 1 次 CPU 拷贝（RocketMQ）→ sendfile 全内核态，DMA gather 后 **0 次 CPU 拷贝**（Kafka/Nginx）；**要改数据就不能零拷贝**
- **零拷贝选型口诀**：不改数据用 sendfile，要改数据用 mmap
- **Netty 铁律**：handler 不做耗时操作（阻塞 EventLoop = 该线程上全部连接卡死）→ 业务丢业务线程池，与 Dubbo IO 线程派发同源
- **一个 Channel 终生绑定一个 EventLoop** → 同一 Channel 的 handler 串行无锁化
- **粘包拆包**：TCP 字节流无边界；定长/分隔符/长度字段三种切法，自定义协议首选 `LengthFieldBasedFrameDecoder`（5 参数：maxFrameLength/offset/length/adjustment/strip）
- **心跳**：`IdleStateHandler` 写空闲发 Ping、读空闲断连；TCP keepalive 默认 2 小时太慢，应用层心跳是标配
- **ByteBuf**：读写双指针、池化（PooledByteBufAllocator）、引用计数用完 release；泄漏用 `-Dio.netty.leakDetection.level` 排查

## 学习/复习建议

1. 先按 1→4 顺序建立体系：IO 模型是纲，NIO/零拷贝/Netty 都是「同步非阻塞 + 多路复用」这条主线的展开。
2. 两张图要能白板画：**read+write 的 4 拷贝 4 切换**、**主从 Reactor 结构图**——画得出才算真会。
3. 每篇「一句话总结」当作口述提纲，能复述即过关。
4. 与 [TCP.md](../计算机网络/TCP.md) 互相印证：粘包拆包、backlog、心跳 keepalive 在两篇里是同一故事的两个视角。
5. 结合组件讲加分案例：Kafka 为什么快（顺序写+零拷贝）、Redis 6.0 IO 线程、Dubbo 线程派发模型，都是 IO 知识的落地证明。

## 相关笔记

- TCP 字节流与粘包根源、backlog 队列 → [../计算机网络/TCP.md](../计算机网络/TCP.md)
- epoll 是 Redis 单线程高并发的底座 → [../redis/README.md](../redis/README.md)
- 堆外直接内存（DirectByteBuffer）与 OOM → [../jvm/内存结构.md](../jvm/内存结构.md)
- Dubbo 基于 Netty 的通信与线程派发 → [../dubbo/核心架构与执行流程.md](../dubbo/核心架构与执行流程.md)
- RocketMQ mmap 消息存储 → [../rocketMq/存储机制与刷盘.md](../rocketMq/存储机制与刷盘.md)
