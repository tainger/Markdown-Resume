---
type: index
status: stable
updated: 2026-08-30
---

# Wiki 全局索引

> 本目录是 LLM 维护的编译层，规范见根目录 `AGENTS.md`。
> raw 层（除 `wiki/` 外所有笔记）归人类所有，Agent 只读。

## 使用方式

| 指令 | 作用 |
|:---|:---|
| `消化 <路径>` | 把一篇 raw 笔记编译进 wiki（增量） |
| `lint wiki` | 死链/矛盾/孤立页检查 |
| `从 wiki 出草稿` | 从综合页提炼文章草稿 |

> 实战 Case 手册：核心 12 个见 [[wiki/usage]]，进阶 8 个（速记包/评分器/闪卡/踩坑本/对比工厂/死链巡检/选题日历/健康度）见 [[wiki/usage-advanced]]。

---

## 一、综合主线（复习先看这里）

| 页面 | 一句话 |
|:---|:---|
| [[wiki/synthesis/P7面试复习主线]] | 面试前 24 小时的总入口 |
| [[wiki/synthesis/高频踩坑本]] | hot100 全部「个人总结」聚合的 Top 10 踩坑模式 + 分簇速查 |
| [[wiki/synthesis/高并发库存扣减]] | 秒杀主线：redis+mysql+mq+分布式锁 |
| [[wiki/synthesis/缓存体系与一致性]] | 穿透/击穿/雪崩/污染 + 一致性方案 |
| [[wiki/synthesis/分布式事务选型]] | 2PC/TCC/本地消息表/事务消息怎么选 |

## 二、概念页（算法）

| 页面 | 覆盖 |
|:---|:---|
| [[wiki/concepts/滑动窗口]] | 双指针与滑动窗口 + 4 道 hot100 |
| [[wiki/concepts/动态规划]] | 背包系列 + 树形 DP + 区间 DP |
| [[wiki/concepts/二分查找]] | 边界写法 + 3 道 hot100 |
| [[wiki/concepts/回溯]] | 排列/组合/子集模板 |
| [[wiki/concepts/单调栈与单调队列]] | 每日温度、接雨水 |
| [[wiki/concepts/前缀和与差分]] | 子数组和 + 树上前缀和 |
| [[wiki/concepts/并查集]] | 连通性问题 |
| [[wiki/concepts/链表技巧]] | 反转/快慢指针/合并 |
| [[wiki/concepts/二叉树遍历]] | 递归/层序/序列化 |

## 三、概念页（AI 应用开发）

| 页面 | 覆盖 |
|:---|:---|
| [[wiki/concepts/RAG]] | 检索增强：双混合检索、切片、重排 |
| [[wiki/concepts/Agent]] | 智能体框架、MCP、记忆、安全 |
| [[wiki/concepts/ContextEngineering]] | 上下文工程 |

## 四、实体页（后端组件）

| 页面 | 定位 |
|:---|:---|
| [[wiki/entities/HashMap]] | 面试最高频的集合源码 |
| [[wiki/entities/Redis]] | 数据结构/持久化/高可用/缓存实战 |
| [[wiki/entities/MySQL]] | 索引/事务锁/调优/分库分表 |
| [[wiki/entities/RocketMQ]] | 存储/消息类型/消费模型 |
| [[wiki/entities/JVM]] | 内存/GC/类加载/JMM |
| [[wiki/entities/Spring生态]] | IOC/AOP/事务/Cloud + MyBatis |
| [[wiki/entities/Dubbo]] | 架构/SPI/容错/通信 |
| [[wiki/entities/网络与IO]] | TCP/HTTP/HTTPS + IO模型/Netty |

## 五、对比页

| 页面 | 对比对象 |
|:---|:---|
| [[wiki/comparisons/B树与B+树]] | B树 vs B+树 vs 跳表，为什么索引用 B+树 |

## 六、raw 层目录地图

| 板块 | 目录 | 入口 |
|:---|:---|:---|
| 算法 | 算法思想 / leetcode-hot100 / 华为OD机试 / 数据结构 | 各目录 README.md |
| 后端 | 分布式 / 系统设计 / mysql / redis / rocketMq / Mybatis / dubbo | 各目录 README.md |
| 基础 | java / jvm / io / 计算机网络 | 各目录 README.md |
| 更多 | AI应用开发 / DeepSeek Harness / 面试 / 面试准备 / 英语能力 / 自媒体/微信公众号/探小虎 | 各目录 README.md |
