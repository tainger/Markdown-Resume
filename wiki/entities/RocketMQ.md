---
type: entity
status: seed
updated: 2026-08-30
---

# RocketMQ

> 一句话：名字路由的分布式消息队列；面试三大件：存储刷盘、消息类型、消费模型。

## 核心笔记

- [[rocketMq/架构与角色]]（NameServer/Broker/Producer/Consumer）
- [[rocketMq/存储机制与刷盘]]（CommitLog/ConsumeQueue/刷盘与主从）
- [[rocketMq/消息类型与发送]]（普通/顺序/事务/延迟）
- [[rocketMq/消费模型与负载均衡]]（集群/广播、Rebalance、offset）
- [[rocketMq/可靠性与高可用]]（丢失三环节 + 兜底）

## 高频追问链

消息不丢（生产/存储/消费三端）→ 顺序消息怎么保证 → 事务消息半消息机制 → 重复消费 → 消息堆积处理 → Rebalance 风暴

## 关联

- [[wiki/synthesis/分布式事务选型]]（事务消息是最终一致性的主力方案）
- [[wiki/synthesis/高并发库存扣减]]（异步削峰）
