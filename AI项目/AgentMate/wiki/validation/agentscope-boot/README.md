---
type: entity
status: growing
updated: 2026-10-05
---
# Spring Boot + AgentScope Java 最小验证工程

本工程为 [[AI项目/AgentMate/wiki/entities/AgentScope-Java验证报告]] 的可复现实验附件。
⚠️ raw 缺失，待补充：源码与日志是合成样例，不代表 MSE 实现；模型是确定性测试替身，不发出真实模型请求。

## 运行
需要 JDK 17+、Maven、可访问 Maven Central；本次验证环境为 JDK 17.0.7 / Maven 3.6.3。
在此目录执行：
```bash
./verify.sh
```
脚本执行五项集成测试、打包，然后先后启动两个 JVM 验证文件会话恢复；失败返回非零退出码。
可设置 VALIDATION_MAVEN_REPO 指定依赖缓存目录；脚本使用本目录 settings.xml，避免更改全局配置。默认缓存位于 /private/tmp/agentmate-validation/m2，可按平台调整。
所有模型与业务工具均为本地模拟，不需要 API Key；构建会下载公开 Maven 依赖。

## 文件
- ValidationApplication：Spring Bean 装配；验证 Spring Boot 与 AgentScope 的实际组合。
- EvidenceTools：固定白名单与合成代码/日志工具。
- ScriptedModel：按协议生成工具调用，验证框架执行与证据回传。
- ProbeRunner：独立进程的写入与恢复探针。
- IntegrationTest：工具调用、容器重建恢复、用户/会话隔离、拒绝越权、事件流。
- evidence.txt：本次测试与进程验证摘要。

## 使用边界
不提供面向生产的 HTTP 服务、真实身份认证、Aone 写入或模型效果评测。
文件状态存储只验证本地正常重启；不能据此证明分布式并发安全、执行中故障恢复或生产幂等。
测试替身不具备自然语言推理能力，固定工具顺序仅用于验证运行框架。
