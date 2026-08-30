---
type: entity
status: seed
updated: 2026-08-30
---

# 网络与 IO

> 一句话：分层模型是骨架，TCP 可靠性是灵魂，HTTPS 是安全的标配；IO 侧 BIO→NIO→Netty 是性能演进的换挡。

## 核心笔记

- [[计算机网络/网络分层模型]]
- [[计算机网络/TCP]]（三次握手/四次挥手/拥塞控制/滑动窗口）
- [[计算机网络/HTTP]]（1.1/2/3 演进）
- [[计算机网络/HTTPS]]（TLS 握手/证书链）
- [[计算机网络/IP与链路层]]
- [[计算机网络/DNS与CDN]]
- [[计算机网络/抓包排查实战]]（tcpdump/Wireshark + 8 个生产案例）
- [[io/IO模型]]（BIO/NIO/AIO）
- [[io/NIO与多路复用]]（select/poll/epoll）
- [[io/Netty]]（Reactor 线程模型/ByteBuf/零拷贝见 [[io/零拷贝]]）

## 高频追问链

TIME_WAIT 过多怎么办 → CLOSE_WAIT 堆积说明什么 → RST 谁发的 → TLS 握手一个 RTT 优化的演进 → epoll 边沿 vs 水平 → Netty 空轮询 bug → 零拷贝 mmap vs sendfile

## 关联

- Redis/RocketMQ/Dubbo 的通信层都是本目录知识的应用（[[wiki/entities/Dubbo]] 通信协议）
