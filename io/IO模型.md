# IO 模型

> 面向工作 6 年、冲击 P7 的 Java 工程师——IO 是所有中间件的底座，Redis/Netty/Kafka 的高性能故事都从「IO 模型」讲起。

按「**阻塞/非阻塞 × 同步/异步 → 五种 IO 模型 → BIO/NIO/AIO 代码对比 → 高频追问**」的顺序，把这道「Java 后端第一道网络编程题」一次讲透。

---

## 一、两组基本概念（必背，先分维度再谈模型）

### 1. 阻塞 vs 非阻塞——看「等数据」阶段

- **阻塞**：调用 `read()` 时数据没准备好，线程被内核挂起（让出 CPU），直到数据就绪才醒。
- **非阻塞**：数据没准备好**立即返回**（`-1` + `EWOULDBLOCK`），线程可以干别的或继续轮询。

### 2. 同步 vs 异步——看「搬数据」阶段

- **同步**：数据从**内核空间拷贝到用户空间**这一步，由线程自己发起 read 完成（拷贝期间线程要参与）。
- **异步**：内核把数据**拷好放进用户缓冲区后**才通知线程（回调/信号），线程全程不参与 IO。

| 组合 | 含义 | 对应 |
|:---|:---|:---|
| 同步阻塞 | 等 + 搬都卡着线程 | BIO |
| 同步非阻塞 | 等不卡（轮询/多路复用），搬还得自己来 | NIO |
| 异步 | 等 + 搬都由内核托管，完成后回调 | AIO |

> 面试话术：**「阻塞与否看等数据时线程挂不挂起；同步异步看数据从内核到用户空间谁来搬」**——两个独立维度，别混着答。

---

## 二、五种 IO 模型（《UNIX 网络编程》经典分类）

```
① 阻塞IO      线程发起read ──► 数据未就绪，挂起等待 ──► 就绪后内核拷贝 ──► 返回
② 非阻塞IO    线程反复read ──► 未就绪立即返回 ──► 轮询直到就绪 ──► 自己拷贝
③ IO多路复用  线程阻塞在select/epoll_wait上监听N个fd ──► 某fd就绪 ──► 自己read拷贝
④ 信号驱动    发起sigaction注册SIGIO ──► 内核数据就绪发信号 ──► 自己read拷贝
⑤ 异步IO      发起aio_read立即返回 ──► 内核等待+拷贝全托管 ──► 完成后通知线程
```

| 模型 | 等数据阶段 | 搬数据阶段 | 线程:连接 | 典型应用 |
|:---|:---|:---|:---|:---|
| ① 阻塞 IO | 阻塞 | 同步（自己拷） | 1:1 | 传统 Socket 服务 |
| ② 非阻塞 IO | 非阻塞（轮询） | 同步（自己拷） | 1:1 | 少见（CPU 空转） |
| ③ IO 多路复用 | 阻塞在 select/epoll_wait | 同步（自己拷） | **N:1** | Redis、Nginx、Netty |
| ④ 信号驱动 | 非阻塞（信号通知） | 同步（自己拷） | — | 实际很少用 |
| ⑤ 异步 IO | 非阻塞 | **异步（内核拷）** | 0:1（回调） | Windows IOCP、io_uring |

> 面试话术：**前四种的「拷贝阶段」都是线程自己做的，本质是同步；只有 AIO 把等待和拷贝全部交给内核**——这是同步与异步的分水岭。

---

## 三、BIO / NIO / AIO：Java 世界的三种实现

### 1. BIO（JDK 1.4 之前唯一选择）：同步阻塞，一连接一线程

```java
// BIO：每个连接占用一个线程，accept() 和 read() 都会阻塞
ServerSocket server = new ServerSocket(8080);
while (true) {
    Socket socket = server.accept();          // 阻塞①：等连接
    new Thread(() -> {                        // 一连接一线程
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {  // 阻塞②：等数据
            // 处理请求...
        }
    }).start();
}
```

**致命伤**：1 万个连接 = 1 万个线程；每个线程栈约 1MB → 内存爆炸；大量线程上下文切换开销。Tomcat 早期、老版 Dubbo 都吃过这个亏。

### 2. NIO（JDK 1.4）：同步非阻塞，靠 Selector 多路复用

```java
// NIO：一个线程 + Selector 监视成千上万条连接
Selector selector = Selector.open();
ServerSocketChannel ssc = ServerSocketChannel.open();
ssc.configureBlocking(false);                       // 关键①：非阻塞模式
ssc.register(selector, SelectionKey.OP_ACCEPT);     // 注册关注事件

while (true) {
    selector.select();                              // 阻塞②：阻塞在多路复用器上（非阻塞在 IO）
    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
    while (it.hasNext()) {
        SelectionKey key = it.next();
        it.remove();                                // 别忘移除，否则重复处理
        if (key.isAcceptable()) {
            SocketChannel ch = ssc.accept();        // 此刻 accept 不会阻塞
            ch.configureBlocking(false);
            ch.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            SocketChannel ch = (SocketChannel) key.channel();
            ByteBuffer buf = ByteBuffer.allocate(1024);
            ch.read(buf);                           // 就绪后才读，不会白等
            // flip() 读数据、业务处理...
        }
    }
}
```

**代价**：事件驱动 + 状态维护，编程模型复杂（谁注册了什么、读到一半怎么办），所以才有 Netty。

### 3. AIO（JDK 7，NIO.2）：异步 IO，内核全托管

```java
// AIO：发起请求立即返回，内核完成等待+拷贝后回调
AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()
        .bind(new InetSocketAddress(8080));
server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
    @Override
    public void completed(AsynchronousSocketChannel ch, Void att) {
        server.accept(null, this);                  // 继续接下一个连接
        ByteBuffer buf = ByteBuffer.allocate(1024);
        ch.read(buf, buf, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer n, ByteBuffer b) {
                // 数据已从内核拷到用户缓冲区，直接处理
            }
            @Override
            public void failed(Throwable e, ByteBuffer b) { /* 异常处理 */ }
        });
    }
    @Override
    public void failed(Throwable e, Void att) { /* 异常处理 */ }
});
```

**为什么没火**：Linux 的 AIO 底层仍靠 epoll 模拟（真正异步只有 Windows IOCP），性能相比 NIO 无明显优势；Netty 曾支持 AIO 后在 4.x **主动移除**。Linux 5.1+ 的 `io_uring` 才是真异步，值得关注但生态尚早。

### 三者对比（必背）

| 维度 | BIO | NIO | AIO |
|:---|:---|:---|:---|
| 全称 | Blocking IO | New IO（非阻塞） | Asynchronous IO |
| JDK | 1.4 之前 | 1.4 | 7 |
| 阻塞性 | 阻塞 | 非阻塞（select 阶段阻塞） | 非阻塞 |
| 同步/异步 | 同步 | 同步 | **异步** |
| 线程:连接 | 1:1 | N:1 | 0:1（回调） |
| 适用场景 | 连接少且固定 | 高并发长连接（Netty/中间件） | 文件操作为主 |

---

## 四、高频追问

**Q1：NIO 的 N 到底是 New 还是 Non-Blocking？**
官方是 New IO（相对老 IO API 而言），其特性是**同步非阻塞**；两个说法都对，关键是答出「同步非阻塞 + 多路复用」。

**Q2：IO 多路复用到底阻塞不阻塞？**
阻塞在 `select()/epoll_wait()` 上（一个线程挂起等事件），但**不阻塞在具体 IO 上**——阻塞点从「每条连接的 read」集中转移到「一个 select」，这是 N:1 的关键。

**Q3：BIO 是不是一无是处？**
连接数少、请求处理快的场景（如内部管理端），BIO 模型简单、易调试、无线程切换浪费，不必无脑上 NIO。

---

## 易错点

1. **「NIO 非阻塞」≠「NIO 异步」**：NIO 的数据拷贝仍是线程自己 read，属于**同步**非阻塞——把 NIO 答成异步直接扣分。
2. **多路复用不是没有阻塞**：阻塞点转移到 select/epoll_wait，一个线程阻塞监视 N 个 fd。
3. **五种模型里只有 AIO 是异步**：信号驱动虽然「等」不阻塞，但拷贝仍要自己做。
4. **一连接一线程的账要会算**：1 万连接 × 1MB 线程栈 = 10GB，再加上下文切换——量化说出来才有说服力。
5. **AIO 在 Linux 是模拟的**：别答成「Linux AIO 原生高性能」；顺带提 io_uring 是加分项。

---

## 一句话总结

**五种 IO 模型按两个维度切分：等数据的方式（阻塞/非阻塞/多路复用）+ 谁搬数据（同步/异步）**。Java 里 BIO=同步阻塞一连接一线程，NIO=同步非阻塞靠 Selector 盯 N 条连接，AIO=内核全托管回调通知；Linux 生态下 NIO(+epoll) 是高并发主流，Netty 是它的工业级封装。
