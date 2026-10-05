---
type: entity
status: growing
updated: 2026-10-05
---
# Spring Boot + AgentScope Java 验证报告

## 结论与边界
2026-10-05 完成最小集成验证：Spring Boot 4.0.4 + AgentScope Java core 2.0.3 + JDK 17.0.7 能编译、打包、启动，并完成真实框架的工具循环与文件会话恢复。可进入 AgentMate 业务 PoC，尚不能据此宣布生产可用或诊断效果达标。

⚠️ raw 缺失，待补充：未提供真实 MSE 源码、工单、日志和 Aone 契约；所有业务数据均为合成样例。工程实验记录保存在 wiki，不能替代产品 raw 知识。总体背景见 [[AI项目/AgentMate/wiki/synthesis/AgentMate总体设计]]，选型见 [[AI项目/AgentMate/wiki/entities/AgentMate-Java架构]]。

## 可复现实物
- [验证工程说明](../validation/agentscope-boot/README.md)
- [依赖配置](../validation/agentscope-boot/pom.xml)
- [集成测试](../validation/agentscope-boot/src/test/java/com/agentmate/validation/IntegrationTest.java)
- [执行摘要](../validation/agentscope-boot/evidence.txt)
- [一键验证脚本](../validation/agentscope-boot/verify.sh)

## 实测项目
| 项目 | 方法 | 结果与边界 |
| :--- | :--- | :--- |
| Spring 集成 | 创建 Spring 容器，注入 Model、工具、状态存储、ReActAgent | 通过；未添加 HTTP 服务或真实模型 SDK |
| 连续工具调用 | 模拟模型请求 read_source，再请求 query_logs，真实 Toolkit 执行 | 通过；断言调用顺序及两条证据进入结果 |
| 状态持久化 | 关闭容器，使用同一目录重建容器与 Agent | 通过；可读取历史标记且不重跑证据工具 |
| 会话隔离 | 切换 userId 或 sessionId，再请求历史标记 | 通过；限于测试的用户/会话组合，不等于生产租户授权审计 |
| 工具侧授权 | 模拟模型请求不在服务端固定白名单中的仓库 | 通过；工具拒绝，未继续查询日志；不是对框架权限引擎的完整验证 |
| 事件流 | 消费 streamEvents，检查工具调用事件和任务结果事件 | 通过；未验证 HTTP SSE 断线恢复 |
| 独立 JVM 恢复 | 第一进程写入并退出，第二进程从同目录读取 | 通过；两个 JVM 均正常退出且读回 marker-42，仅完整轮次后正常重启，不是处理中崩溃恢复 |

JUnit 共 5 个测试，0 失败、0 错误、0 跳过；文件状态恢复和隔离包含在这些测试中。

## 模拟与真实的划分
真实执行：Spring Boot 容器、AgentScope ReActAgent、工具注册/参数检查/执行、消息循环、事件流、JsonFileAgentStateStore、可执行 Jar。
模拟部分：ScriptedModel 按固定规则发出工具请求；源码和日志工具返回合成文本；没有请求外部模型，没有分析真实 MSE 代码。
因此本轮证明框架接线与状态能力可行，不证明模型能自主找到根因、减少研发穿透或具备提示注入防御能力。

## 实际依赖
- Spring Boot：4.0.4；AgentScope core：2.0.3。
- Reactor core：3.8.4；Jackson core/databind：2.21.1；JSON Schema validator：2.0.0。
- JDK：17.0.7；Maven：3.6.3；macOS x86_64。JDK 21 与 Spring Boot 3.x 未实测。
- 版本以本次打包 Jar 中实际依赖为准；未额外导入 AgentScope 全量依赖 BOM。

## 接入时发现的问题
1. ReActAgent 2.0.3 的带 RuntimeContext 调用使用 call(String, context) 或 call(List<Msg>, context)；不能直接套用 HarnessAgent 的示例签名。
2. 模拟流式工具响应必须携带原始 JSON content；只填解析后的 input Map 导致必填参数校验失败。修正适配器后工具实际执行成功。
3. 本机 LC_ALL=C 导致中文路径在测试子进程中失真；验证脚本显式使用 UTF-8。没有修改用户的全局配置。
4. 本机 Maven 镜像不可用，验证工程使用独立 settings.xml 与临时依赖缓存，没有修改全局 Maven 设置。

## 尚未验证的上线条件
- DashScope 等真实模型适配器、工具选择质量、长上下文、真实延迟和成本。
- HarnessAgent 的工作空间、子智能体、沙箱及其存储能力；本轮依赖是 agentscope-core。
- 多仓版本映射、源码索引、日志权限与脱敏、真实知识质量。
- Aone API、幂等建单、未知写入结果对账、跨天业务状态机。
- 人工批准/拒绝与恢复、执行中进程崩溃、取消、超时重试、同会话并发冲突。
- 多实例共享数据库状态、生产鉴权、权限撤销、负载与安全测试。

## 下一阶段建议
以这组精确版本为 PoC 起点，将 ScriptedModel 替换为允许使用的真实模型适配器，保留确定性集成测试。接入一个脱敏工单集、一个授权源码仓库及对应版本映射、只读日志源，验证有引用的诊断。
同一模型和工具条件下再与原框架候选做对照；本轮没有进行框架效果排名。优先验收证据正确率、合理升级率和总人工耗时，再扩展 Aone 写入与多 Agent。

## 外部核验来源
- [AgentScope Java 2.0.3 发布源码](https://github.com/agentscope-ai/agentscope-java/tree/v2.0.3)：核验 API，实际编译使用 Maven Central 已发布制品。
- [2.0.3 依赖基线](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/agentscope-dependencies-bom/pom.xml)：选取官方使用的 Spring Boot 基线；不将其视为所有组合均兼容的保证。
