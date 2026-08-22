# RocketMQ 面试题笔记

按主题整理的 RocketMQ 高频面试题，覆盖架构与角色、消息类型与发送、消费模型与负载均衡、存储机制与刷盘、可靠性与高可用五大板块。与 `redis/`、`mysql/` 同一套笔记风格。

## 目录

| 主题 | 笔记 | 核心考点 |
|:---|:---|:---|
| 🏛️ 架构与角色 | [架构与角色.md](架构与角色.md) | NameServer、Broker、Producer/Consumer、Topic/Queue、为什么不用 ZK |
| ✉️ 消息类型与发送 | [消息类型与发送.md](消息类型与发送.md) | 顺序/延迟/事务/批量消息、同步异步单向、Tag/SQL 过滤 |
| 👥 消费模型与负载均衡 | [消费模型与负载均衡.md](消费模型与负载均衡.md) | Push/Pull 长轮询、消费者组、集群/广播、Rebalance、offset、重试/死信 |
| 💾 存储机制与刷盘 | [存储机制与刷盘.md](存储机制与刷盘.md) | CommitLog、ConsumeQueue、IndexFile、mmap 零拷贝、同步/异步刷盘 |
| 🛡️ 可靠性与高可用 | [可靠性与高可用.md](可靠性与高可用.md) | 消息不丢三段论、幂等、消息堆积、主从/DLedger、对比 Kafka |

## 高频「必背」清单

- **四大角色**：NameServer（轻量路由注册中心，节点独立不通信，不参与转发）、Broker（存储转发核心）、Producer、Consumer
- **为什么不用 ZK**：NameServer 走 **AP 弱一致**，各节点独立，够用即可；ZK 是 CP 强一致，过度设计
- **Topic → MessageQueue**：Topic 逻辑分类拆成多个队列分布到多 Broker，实现高吞吐；单队列内 FIFO
- **顺序消息**：同 key 发同队列（MessageQueueSelector）+ 顺序消费监听器（MessageListenerOrderly）
- **延迟消息**：开源 4.x 只支持 **18 个固定级别**（存内部 SCHEDULE Topic 定时投递回原 Topic）；5.x 支持任意定时
- **事务消息**：half 消息（不可见）→ 本地事务 → commit/rollback → **事务回查**兜底，保证**最终一致**
- **Push 本质是 Pull**：底层用**长轮询**实现伪推
- **集群 vs 广播**：集群模式组内只消费一次（offset 存 Broker）；广播模式每实例都消费（offset 存本地）
- **Rebalance**：实例增减触发队列重分配，期间可能重复消费，是重复消费的重要来源
- **只保证 at least once**：offset 批量异步提交 + 发送重试 + Rebalance → 必然重复，业务需**幂等**
- **消费重试与死信**：失败进 `%RETRY%组名`，重试 16 次进 `%DLQ%组名`
- **CommitLog 顺序写**：所有 Topic 消息写**同一个** CommitLog（1GB/文件），磁盘始终顺序写，海量 Topic 比 Kafka 稳
- **读取路径**：先读 ConsumeQueue 索引（20 字节）拿 offset，再读 CommitLog
- **mmap + PageCache**：内存映射减少拷贝，启动**文件预热**避免缺页；默认**异步刷盘**
- **消息不丢三段论**：发送重试 + 同步刷盘 + 同步复制 + 消费成功才提交 offset
- **消息堆积**：先扩队列再扩消费者（**消费者数 ≤ 队列数**）、提并发、批量、临时转存
- **高可用**：传统主从**不能自动切换**；**DLedger** 用 Raft 多数派确认 + 自动选主（至少 3 节点）
- **对比 Kafka**：业务/事务消息选 RocketMQ（十万级），日志/大数据吞吐选 Kafka（百万级）

## 相关笔记

- Redis（分布式锁 / 幂等去重）：[redis/README.md](../redis/README.md)
- MySQL（幂等去重表 / 唯一索引）：[mysql/README.md](../mysql/README.md)
