---
type: entity
status: seed
updated: 2026-08-30
---

# Spring 生态

> 一句话：IOC 管对象、AOP 管横切、事务管一致性；自动配置是 Boot 的魔法，Cloud 是微服务的全家桶。

## 核心笔记

- [[Spring/IOC与Bean生命周期]]
- [[Spring/AOP]]（动态代理机制与失效场景）
- [[Spring/事务]]（原理层 + 场景层：传播行为 / 失效场景 / 锁包事务）
- [[Spring/自动配置与启动流程]]（@EnableAutoConfiguration/SPI）
- [[Spring/SpringMVC与Web]]
- [[Spring/SpringCloud微服务]]

## MyBatis

- [[Mybatis/核心架构与执行流程]]（Executor/StatementHandler）
- [[Mybatis/缓存机制]]
- [[Mybatis/动态SQL与参数绑定]]（#{} vs ${} 注入）
- [[Mybatis/插件机制与高级特性]]

## 高频追问链

Bean 生命周期 → 三级缓存解决循环依赖 → AOP 代理失效 → 事务传播 REQUIRES_NEW/Nested → 自定义 Starter 怎么写 → MyBatis Mapper 为什么是动态代理（≈ AiServices 思路，见 [[wiki/concepts/Agent]]）
