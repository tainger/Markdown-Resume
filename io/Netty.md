# Netty 核心与实战

> Netty = 对 NIO 的工业级封装：**主从 Reactor 线程模型 + 池化 ByteBuf + Pipeline 责任链 + 编解码全家桶**。Dubbo、RocketMQ、Elasticsearch、gRPC、Zuul 底层全是它。

按「**为什么用 Netty → 核心组件 → 线程模型与铁律 → 粘包拆包 → 心跳与调优 → 代码骨架**」组织。

---

## 一、为什么不用原生 NIO，要用 Netty

原生 NIO 的四大痛点，Netty 一一兜住：

| 原生 NIO 痛点 | Netty 的解法 |
|:---|:---|
| API 复杂（Selector/Channel 状态机） | 封装成 BootStrap + Handler 事件驱动 |
| JDK **空轮询 bug**（CPU 100%） | 计数检测 + 自动重建 Selector |
| 粘包拆包要自己处理 | 内置编解码器（长度/分隔符/定长） |
| 内存频繁分配回收 | **池化** PooledByteBufAllocator + 引用计数 |

---

## 二、核心组件（每个一句话记住）

| 组件 | 职责 | 关键点 |
|:---|:---|:---|
| `Channel` | 连接的抽象（NioSocketChannel 等） | IO 操作的统一门面 |
| `EventLoop` / `EventLoopGroup` | 事件循环线程：IO 事件 + 定时任务 + 普通任务 | **一个 Channel 终生绑定一个 EventLoop** → handler 串行无锁化 |
| `ChannelPipeline` | Handler 的责任链 | 入站事件从头到尾、出站事件从尾到头 |
| `ChannelHandler` | 业务逻辑（Inbound/Outbound） | 跨 Channel 共享要 `@Sharable` 且无状态 |
| `ByteBuf` | 增强版 Buffer | **读写双指针**、池化、引用计数 |
| `Future` / `Promise` | 异步结果 | 推荐 `addListener` 而非 `sync()` 阻塞 |

主从 Reactor 全景图（必背）：

```
客户端 ──连接──► ┌─ BossEventLoopGroup(1) ─ 只管 accept ─┐
                │   accept 到的 SocketChannel             │
                └──────────────────┬─────────────────────┘
                                   │ 注册（轮询挑选 worker）
                                   ▼
                ┌─ WorkerEventLoopGroup(N) ─ 管所有已连接的读写 ─┐
                │  EventLoop-1: [ch1, ch4, ch7, ...]             │
                │  EventLoop-2: [ch2, ch5, ch8, ...]             │
                │  EventLoop-3: [ch3, ch6, ch9, ...]             │
                └──────────────────┬─────────────────────────────┘
                                   ▼
                    Pipeline：解码器 → 业务 Handler → 编码器
```

---

## 三、线程模型与「不要阻塞 EventLoop」（最高频考点）

EventLoop 的一次循环三步：

```
select（等 IO 事件）→ processSelectedKeys（处理读写）→ runAllTasks（跑任务队列）
```

- **无锁串行化**：一个 EventLoop 串行处理自己名下所有 Channel 的事件 → 同一个 Channel 的 handler 不用加锁；
- **铁律**：handler 里**禁止**写耗时逻辑（DB 查询、RPC 调用、大计算）——否则该 EventLoop 上**所有连接全部卡死**；
- **解法**：业务丢独立业务线程池（与 Dubbo「IO 线程派发到业务线程池」同一思想，见 [../dubbo/核心架构与执行流程.md](../dubbo/核心架构与执行流程.md)），或用 `addListener` 回调衔接。

> 面试话术：**「Boss 收连接，Worker 干 IO，业务进业务池——三层各司其职，谁也别堵谁」**。

---

## 四、粘包拆包（承接 TCP 字节流）

**根源**：TCP 是字节流没有消息边界（Nagle 合包、MSS 分段、接收缓冲区拼接），详见 [../计算机网络/TCP.md](../计算机网络/TCP.md)。

Netty 内置解码器选型：

| 解码器 | 切分依据 | 适用 |
|:---|:---|:---|
| `FixedLengthFrameDecoder` | 定长 | 报文长度固定 |
| `LineBasedFrameDecoder` / `DelimiterBasedFrameDecoder` | 换行 / 分隔符 | 文本协议 |
| `LengthFieldBasedFrameDecoder` | **长度字段** | 自定义二进制协议（最常用） |

`LengthFieldBasedFrameDecoder` 五参数（必会）：

```java
new LengthFieldBasedFrameDecoder(
    1024 * 1024,  // maxFrameLength      最大帧长，防恶意大包
    7,            // lengthFieldOffset   长度字段偏移（如协议头 7 字节后才是长度）
    4,            // lengthFieldLength   长度字段本身占 4 字节（int）
    0,            // lengthAdjustment    长度字段的值 = 包体长度时为 0；含头长需调整
    0);           // initialBytesToStrip 解析后剥掉前 N 字节（如不要长度字段就设 7）
```

---

## 五、心跳与空闲检测

```
IdleStateHandler(60, 30, 0)
      │           │
      │           └ WRITER_IDLE：30 秒没写 → 触发写空闲事件 → 客户端发 Ping
      └ READER_IDLE：60 秒没读 → 触发读空闲事件 → 没收到 Pong 就关闭连接
```

- TCP 自带 keepalive 太慢（默认 2 小时）且只能探连通性 → **应用层心跳是标配**；
- 与 Redis/RocketMQ 的长连接心跳同源，见 [../redis/README.md](../redis/README.md)、[../rocketMq/架构与角色.md](../rocketMq/架构与角色.md)。

---

## 六、调优参数速查（P7 加分项）

| 参数 | 作用 |
|:---|:---|
| `SO_BACKLOG` | 半/全连接队列长度，短连接高并发调大（配合内核 somaxconn） |
| `TCP_NODELAY` | 禁用 Nagle，低延迟场景必开 |
| `SO_KEEPALIVE` | TCP 层保活（慢，一般用应用层心跳替代） |
| `WRITE_BUFFER_WATER_MARK` | 高低水位线：写前检查 `channel.isWritable()`，防积压 OOM |
| `SO_RCVBUF / SO_SNDBUF` | 收发缓冲区大小 |
| `-Dio.netty.leakDetection.level=PARANOID` | 堆外内存泄漏检测（测试期开） |

---

## 七、最小可用服务端骨架（中文注释）

```java
EventLoopGroup boss = new NioEventLoopGroup(1);        // 主 Reactor：1 个线程只管 accept
EventLoopGroup worker = new NioEventLoopGroup();       // 从 Reactor：默认 CPU 核数 × 2
try {
    ServerBootstrap b = new ServerBootstrap();
    b.group(boss, worker)
     .channel(NioServerSocketChannel.class)
     .option(ChannelOption.SO_BACKLOG, 1024)           // 服务端 option
     .childOption(ChannelOption.TCP_NODELAY, true)     // 已连接的 childOption
     .childHandler(new ChannelInitializer<SocketChannel>() {
         @Override
         protected void initChannel(SocketChannel ch) {
             ch.pipeline().addLast(
                 new LengthFieldBasedFrameDecoder(1 << 20, 7, 4, 0, 0), // 先解粘包
                 new BizHandler());                    // 耗时逻辑内部再丢业务线程池
         }
     });
    ChannelFuture f = b.bind(8080).sync();
    f.channel().closeFuture().sync();
} finally {
    boss.shutdownGracefully();
    worker.shutdownGracefully();
}

// 业务 handler：注意不要阻塞 EventLoop
class BizHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        businessPool.execute(() -> {                  // 耗时操作 → 业务线程池
            Object resp = handle(data);
            ctx.writeAndFlush(resp);                  // write 入队 + flush 写出
        });
    }
}
```

---

## 易错点

1. **handler 阻塞 EventLoop = 全连接卡死**：一个慢 SQL 拖垮整个 Worker，这是 Netty 生产事故第一名。
2. **`@Sharable` 陷阱**：默认每个 Channel 一个 handler 实例；跨 Channel 共享必须加注解**且保证无状态**。
3. **Pipeline 顺序**：入站按添加顺序执行，出站按逆序执行；解码器（入站）要放在业务 handler 前面。
4. **ByteBuf 引用计数**：用完 `release()`，否则堆外内存泄漏；Pooled 分配器配 leakDetection 排查。
5. **`write` ≠ `flush`**：write 只是入队，flush 才真正写出；常用 `writeAndFlush`。
6. **`sync()` 会阻塞当前线程**：服务端启动可以 sync，业务代码里推荐 `addListener` 回调。
7. **worker 线程数不是越大越好**：默认 CPU×2 足够——EventLoop 是 IO 线程，线程再多也只干 select 事件分发，瓶颈应在业务线程池。

---

## 一句话总结

**Netty = 主从 Reactor（Boss 收连接、Worker 干 IO）+ Pipeline 责任链 + 池化堆外 ByteBuf + 编解码全家桶**；两条铁律记牢：handler 不阻塞 EventLoop（耗时丢业务线程池），自定义协议用 `LengthFieldBasedFrameDecoder` 按长度切包——Dubbo/RocketMQ 的通信层都是这套打法的注脚。
