# Arthas 面试笔记（P7 备战）

> 官网：https://arthas.aliyun.com/
>
> 面向工作 6 年、冲击 P7 的 Java 工程师——Arthas 是面试中「线上排障能力」最直接的证明：不仅要会用命令，还要讲得清字节码增强原理，说得出真实排查案例。

按「**原理 → 命令 → 实战**」的主线组织，每篇均含对比表、图解、易错点、一句话总结。

## 目录

| # | 主题 | 笔记 | 核心考点 |
|:---:|:---|:---|:---|
| 1 | ⚙️ 核心原理与命令速查 | [核心原理与命令速查.md](核心原理与命令速查.md) | Attach 机制、动态 agentmain、Instrumentation + ByteBuddy 字节码增强、retransform 限制、quit vs stop、命令地图 |
| 2 | 🔍 核心命令详解 | [核心命令详解.md](核心命令详解.md) | watch 四观测点与 OGNL 变量、trace/stack/monitor、tt 录制重放、jad/sc/sm、vmtool/heapdump、logger、mc/redefine、profiler |
| 3 | 🚨 线上排障实战 | [线上排障实战.md](线上排障实战.md) | CPU 高、RT 高下钻、异常抓参、死锁、热修复、动态日志、内存泄漏、偶现录制 10 大场景 + 生产使用规范 |

## P7 必背清单（速查）

- **定位**：阿里开源 Java 在线诊断工具，**不重启、不改代码**，attach 到运行中的 JVM 做运行时诊断
- **原理三件套**：`VirtualMachine.attach(pid)` 附着 → 动态 **agentmain** 加载 arthas-agent → **Instrumentation** 的 `retransformClasses` + ByteBuddy（ASM）字节码增强，在方法前后织入 AdviceListener 回调
- **agent 跑在哪**：arthas-boot 只是启动器，诊断逻辑运行在**目标 JVM 内部**；Telnet 默认 3658、Web Console 默认 8563，只监听 127.0.0.1
- **watch**：四个观测点 `-b` 前 / `-e` 异常 / `-s` 正常返回 / `-f` 结束（默认）；表达式变量 `params / returnObj / throwExp / target / #cost(ms)`；`-x` 展开深度（默认 1）、条件表达式、`-n` 限次数
- **观测四件套分工**：watch 看一个点的数据、trace 往下看一层调用链耗时、stack 往上看谁调的、monitor 周期统计成功率/RT
- **trace 两个限制**：只追**一层直接调用**、**不跨线程**；慢点子调用要再 trace
- **tt 时间隧道**：`-t` 录制现场、`-i` 查看、`-p` 重放（真实调用，写操作慎用）
- **thread**：`-n 3` 最忙线程（CPU 高首选）、`-b` 死锁/阻塞源、`--state BLOCKED`；CPU 高先分清业务线程还是 GC 线程
- **版本确认**：`sc -d`（ClassLoaderHash、来源 jar）+ `jad` 反编译，回答「线上跑的是不是新代码」
- **热更新**：`jad → mc -c <classLoaderHash> → retransform`；JVM 限制**只能改方法体**，不能加字段/方法、改签名；重启失效，必须补发版
- **内存排查**：`dashboard` 看 GC、`vmtool forceGc` 看回不回落、`heapdump --live` 导堆给 MAT、`vmtool getInstances` 直接看堆内实例
- **logger**：`--name xxx --level DEBUG` 动态调日志级别，不用重启
- **退出红线**：`quit` 只断连接、增强还在（开销持续）；**`stop` 才 reset 所有增强并退出**
- **生产规范**：高频方法必加条件过滤 + `-n` 限次；heapdump 有 STW 低峰做；ognl/redefine/tt 重放属危险操作；Arthas 是现场显微镜，不替代常态监控

## 学习/复习建议

1. 先读第 1 篇建立原理框架——面试先问「Arthas 怎么实现的」，能讲 Attach + Instrumentation 字节码增强是分水岭。
2. 第 2 篇命令以 **watch / trace / tt / jad** 为重点，参数不用全背，但 watch 的观测点和变量要能默写。
3. 第 3 篇挑 2 个场景准备成 STAR 案例（如 CPU 飙高定位、接口慢 trace 下钻），P7 面试必问「你用 Arthas 解决过什么线上问题」。
4. 「易错点」章节是面试细节陷阱（trace 只追一层、quit vs stop、redefine 限制），重点记忆。

## 相关笔记

| 主题 | 笔记 |
|:---|:---|
| jstack/jmap/jstat/MAT 传统工具链与 OOM/CPU/GC 排查套路 | [../jvm/性能调优与诊断.md](../jvm/性能调优与诊断.md) |
| 类加载机制、ClassLoader（理解 mc -c / retransform） | [../jvm/类加载机制.md](../jvm/类加载机制.md) |
| 线上排障的 STAR 项目表达 | [../面试/项目深挖与自我介绍模板.md](../面试/项目深挖与自我介绍模板.md) |
