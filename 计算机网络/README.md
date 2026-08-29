# 计算机网络面试笔记（P7 备战）

> 面向工作 6 年、冲击 P7 的 Java 工程师——从分层模型到应用层协议，成体系地理解计算机网络。

按「**分层模型 → TCP → HTTP/HTTPS → IP 与链路 → DNS/CDN → 抓包实战**」的主线组织，每篇均含对比表、图解、易错点、一句话总结。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | 🧱 网络分层模型 | [网络分层模型.md](网络分层模型.md) | OSI 七层 vs TCP/IP 四层、各层职责与协议、封装、「输入 URL 后发生了什么」 |
| 2 | 🔌 TCP | [TCP.md](TCP.md) | 三次握手/四次挥手、TIME_WAIT/CLOSE_WAIT、重传、滑动窗口、拥塞控制、TCP vs UDP、粘包拆包 |
| 3 | 🌐 HTTP | [HTTP.md](HTTP.md) | 报文、方法与幂等、状态码、强/协商缓存、Cookie/Session/JWT、1.1→2→3 演进与队头阻塞 |
| 4 | 🔒 HTTPS | [HTTPS.md](HTTPS.md) | 对称/非对称混合加密、TLS 1.2/1.3 握手、证书链与中间人、前向安全 |
| 5 | 🛣️ IP 与链路层 | [IP与链路层.md](IP与链路层.md) | 子网/CIDR、私网与 NAT、ARP、ICMP/ping/traceroute、断网排查 |
| 6 | 🗺️ DNS 与 CDN | [DNS与CDN.md](DNS与CDN.md) | 递归/迭代查询、缓存 TTL、DNS 负载均衡、HTTPDNS、CDN 命中与回源 |
| 7 | 🔬 抓包排查实战 | [抓包排查实战.md](抓包排查实战.md) | tcpdump/Wireshark 命令、分层排查方法论、8 个生产 Case（DROP vs REJECT、TIME_WAIT/CLOSE_WAIT、RST、重传与 zero window、MTU、TLS 握手、DNS 缓存、curl 时延分解） |

## P7 必背清单（速查）

- **分层**：链路层（MAC，相邻节点）→ 网络层（IP，全局路由）→ 传输层（端口，进程到进程）→ 应用层（报文语义）；发送方加头封装、接收方解封装
- **三次握手**：确认双方收发能力 + 同步初始序列号；不能两次（防历史连接）；ISN 随机防预测；SYN 洪泛 → syncookies
- **四次挥手**：被动方 ACK 与 FIN 分开发（可能还有数据）；TIME_WAIT 等 2MSL（保证 ACK 重传 + 旧报文消逝）；大量 TIME_WAIT = 主动关太多，大量 CLOSE_WAIT = 代码没 close
- **TCP 可靠**：确认应答 + 超时重传/快速重传（3 个重复 ACK）/SACK；可靠 ≠ 不丢，是「丢了能补」
- **两个窗口**：滑动窗口（rwnd，流量控制，对端）+ 拥塞窗口（cwnd，拥塞控制，网络）；实际发送 = min(rwnd, cwnd)
- **拥塞控制四算法**：慢启动（指数）→ 拥塞避免（线性 +1）→ 快重传（3 重复 ACK）→ 快恢复（减半）；超时才 cwnd=1 从头来
- **粘包**：TCP 字节流无边界，应用层用定长/分隔符/长度前缀切消息（Netty/HTTP Content-Length 同理）
- **HTTP 缓存**：强缓存（Cache-Control，不发请求）→ 协商缓存（ETag/Last-Modified，回 304）；no-cache 每次协商，no-store 禁缓存
- **状态码**：301 永久 / 302 临时 / 304 缓存命中；401 未认证 / 403 无权限；502 后端挂 / 504 后端超时
- **队头阻塞三代演进**：HTTP/1.1 应用层按序 → HTTP/2 多路复用但仍受 TCP 丢包阻塞 → HTTP/3（QUIC over UDP）流独立根治
- **HTTPS** = 对称加密传数据 + 非对称（ECDHE）换密钥 + CA 证书链验身份；TLS 1.3 握手 1-RTT、只留前向安全套件
- **IP/MAC 分工**：跨网段 IP 不变、MAC 每跳都换；ARP 在同一链路把 IP 翻译成 MAC
- **NAT**：私网共享公网出口（NAPT 靠端口区分），外网不能主动进内网
- **DNS**：客户端 → 本地 DNS（递归），本地 DNS → 根/TLD/权威（迭代），结果按 TTL 层层缓存；CDN 靠 CNAME + GSLB 就近调度
- **排查第一反应**：connect 超时=被 DROP 查防火墙，refused=没监听查进程；大量 TIME_WAIT=本端主动关（上连接池），大量 CLOSE_WAIT=代码没 close；握手通但大包断=MTU/PMTUD；reset=对端重启或空闲超时后还在用死连接

## 学习/复习建议

1. 先按 1→7 顺序建立体系，篇末「相关笔记」形成交叉引用网；抓包实战篇是前六篇的「落地验收」。
2. 「输入 URL 到页面展示」是总纲：把它讲一遍，每个环节都能被追问到对应篇章。
3. 每篇「一句话总结」当作口述提纲，能复述即过关。
4. 「易错点」章节是面试细节陷阱，重点记忆（TIME_WAIT 在谁、401 vs 403、递归 vs 迭代）。
5. 结合工作实际：排查过 CLOSE_WAIT 堆积、配过 HTTPS 证书、用过 CDN 缓存刷新，都是加分案例。

## 相关笔记

- 分布式与系统设计：CDN/网关/负载均衡是系统设计接入层的基本功 → [../系统设计/README.md](../系统设计/README.md)
- Redis/RocketMQ 的长连接心跳与网络 IO → [../redis/README.md](../redis/README.md)、[../rocketMq/README.md](../rocketMq/README.md)
- Java 网络编程（Socket/NIO/Netty 拆包）可与 TCP 篇互相印证 → [../java/README.md](../java/README.md)
