---
type: entity
status: seed
updated: 2026-08-30
---

# Dubbo

> 一句话：面向接口的 RPC 框架；面试三大件：SPI 自适应扩展、集群容错、通信线程模型。

## 核心笔记

- [[dubbo/核心架构与执行流程]]（Provider/Consumer/Registry/Monitor）
- [[dubbo/SPI机制与自适应扩展]]（与 JDK SPI 对比、@Adaptive）
- [[dubbo/服务注册发现与配置]]（URL 模型、配置优先级）
- [[dubbo/集群容错与负载均衡]]（failover 等 6 种 + 4 种 LB）
- [[dubbo/通信协议与线程模型]]（dubbo 协议、Dispatcher 线程派发）

## 高频追问链

和 Spring Cloud 怎么选 → 自适应扩展编译期生成代码原理 → 注册中心挂了还能不能调 → 泛化调用 → 异步调用线程切换 → 线程模型 Dispatcher 取值差异

## 关联

- [[分布式/服务治理]]
