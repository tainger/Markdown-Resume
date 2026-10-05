---
type: entity
status: growing
updated: 2026-10-05
---
# AgentMate Java 技术架构

⚠️ raw 缺失，待补充：本页为技术选型与接口提案，尚未验证内部基础设施、模型、版本兼容矩阵或 Aone 契约；[[AI项目/AgentMate/README]] 为空。需求见 [[AI项目/AgentMate/wiki/synthesis/AgentMate总体设计]]。

## 选型补充：AgentScope Java（2026-10-05）
- 更新：上一版基于 Spring 技术栈优先列出 Spring AI Alibaba，未充分比较 AgentScope Java；不能据此认为 AgentScope 不支持 Java 或不适合本项目。
- ⚠️ raw 缺失，待补充：以下为外部官方资料支持的候选方案，尚未完成本地工程验证。[AgentScope Java 官方仓库](https://github.com/agentscope-ai/agentscope-java)与[Java 快速入门](https://github.com/agentscope-ai/agentscope-java/blob/main/docs/v2/en/docs/quickstart.md)描述了 Java 实现、工具交互、会话持久化、子智能体与工作空间等能力。Python AgentScope 的版本号与 Java 实现不能混为一谈。
- 针对 AgentMate 的动态调查需求，建议把 Spring Boot + AgentScope Java 列为优先验证方案：前者承载业务接入，后者承载调查循环与角色协作；Spring AI Alibaba Graph 保留为流程编排候选。未做同任务对照测试前，不断言框架优劣。
- 若采用 AgentScope Java，可直接使用其模型与工具能力，无需再叠加 Spring AI 与 Graph 管理同一调查循环。跨日工单状态、Aone 幂等写入、版本映射、索引与业务授权仍属于 AgentMate 自身职责。
- 验证任务：同一批历史工单、相同模型和工具，对照调查正确率、证据质量、工具成本、人工中断恢复、重复写入防护及 Spring Boot 集成难度，再锁定版本。
- 下表保留原方案便于评审，不代表已否决 AgentScope 或完成最终选型。

## 技术选型建议（原 Spring AI 方案）
| 层 | 首选方案 | 选择理由与边界 |
| :--- | :--- | :--- |
| 应用 | Java 21 + Spring Boot，版本按兼容矩阵锁定 | 主流程、接入、权限及调度都使用 Java |
| 模型接入 | Spring AI；Spring AI Alibaba 集成经兼容验证后采用 | 统一模型调用与工具封装，不混用不兼容版本 API |
| 调查编排 | Spring AI Alibaba Graph | 在调查任务内组合节点、条件路由和状态 |
| 长业务流程 | 数据库状态机 + Outbox + Worker | 管理跨日等待、人工确认、重试与外部写入；复杂度增长再评估专用流程引擎 |
| 事务存储 | 优先团队已有 MySQL 或 PostgreSQL | Case、任务、动作、审批、来源、同步游标 |
| 检索 | 沿用已有搜索设施；无现成设施可用 PostgreSQL 全文/精确检索 + pgvector 起步 | 精确错误码与符号不能仅靠向量；中文检索需验证分词与召回，规模增长再引入专用搜索服务 |
| 代码索引 | JGit + Java AST/符号解析适配器 | 按 commit 增量；非 Java 仓库独立适配，不承诺全语言精确调用图 |
| 对象存储 | OSS 或既有对象存储 | 版本快照、较大证据和报告，带生命周期与权限 |
| 异步任务 | 数据库任务队列起步；已有 RocketMQ 可复用 | 消费幂等、退避重试、死信及重放 |
| 定时任务 | 既有 SchedulerX/XXL-JOB 等择一 | 只触发同步任务，业务状态不能只保存在调度器内 |
| 缓存 | Redis，确有需求时引入 | 缓存键含租户、权限版本、commit；不能作为唯一任务存储 |
| 观测 | OpenTelemetry 接入既有 ARMS 等平台 | 跟踪任务、节点、工具、时延、成本、模型版本及失败 |

具体依赖版本在工程启动时通过 BOM、样例流程和兼容测试锁定，本方案不使用未验证的版本组合。
框架能力参考：[Spring AI 工具调用](https://docs.spring.io/spring-ai/reference/api/tools.html)、[Spring AI Alibaba Graph 核心概念](https://java2ai.com/docs/frameworks/graph-core/core/core-library/)。工具实际执行与访问控制仍由应用负责。

## 模块划分
```text
agentmate-api              对话、任务查询、流式进度、身份入口
agentmate-case             工单领域、关联关系、状态机
agentmate-orchestration    调查编排、预算、暂停与恢复
agentmate-knowledge        知识摄取、检索、审核与失效
agentmate-code             仓库快照、符号索引、跨仓与版本映射
agentmate-diagnostics      日志/指标/链路查询与证据归一
agentmate-connectors       工单、Aone、Git、观测平台适配器
agentmate-policy           对象授权、数据分级、动作审批
agentmate-worker           索引、定时同步、异步写入与补偿
agentmate-evaluation       历史样本回放与质量报表
```
初期部署 API 与 Worker 两个进程即可，共享领域模块；索引任务按资源需要独立扩容，避免起步就拆大量微服务。

## 持久化边界
- 业务状态机负责 Case 与跨日协同；Graph 负责一次调查的有界执行。前者保存后者的 runId/checkpointId，避免两套状态各自推进外部写入。
- 任务状态提交与待发送事件写入同一数据库事务，通过事务发件箱（Transactional Outbox）异步派发。
- 工具调用记录 planned/running/succeeded/failed/unknown；外部写入超时属于 unknown，先对账。
- Worker 使用租约和心跳；租约过期重领，使用版本号或 fencing token 防止旧 Worker 继续提交。
- 重试只针对可重试故障；参数错误、权限拒绝、业务冲突不可无限重试。

## 领域数据
| 对象 | 关键字段 |
| :--- | :--- |
| Case | tenantId、ticketId、产品域、环境、版本、影响、状态、owner |
| InvestigationRun | caseId、runId、state、budget、checkpoint、workflowVersion |
| Evidence / Claim | 来源、权限、版本/时间、证据关系、事实或假设状态 |
| RepoSnapshot / ReleaseMap | repoId、commit、artifactDigest、环境、有效时间 |
| KnowledgeItem | 来源、适用范围、审核状态、索引版本、失效原因 |
| ActionExecution | 幂等键、payloadHash、授权、结果、外部 ID、对账状态 |
| AoneLink / SyncCursor | caseId、workItemId、revision、水位、同步错误 |
| EvaluationCase | 问题、允许来源、参考结论、时间切分、人工评分 |

## Java 接口示意（设计契约，非可运行实现）
```java
interface SourceAnalysisPort {
    List<CodeEvidence> search(SourceQuery query, AccessContext access);
}
interface ObservabilityPort {
    LogEvidence query(LogQuery query, AccessContext access);
}
interface WorkItemPort {
    List<WorkItem> findCandidates(WorkItemQuery query, AccessContext access);
    CreateResult create(ApprovedAction action, IdempotencyKey key);
    ChangePage listChanges(SyncCursor cursor, AccessContext access);
}
interface InvestigationService {
    RunId start(CaseId caseId, AccessContext access);
    void resume(RunId runId, InvestigationEvent event);
}
```
AccessContext 来自认证上下文；ApprovedAction 由服务端验证内容哈希、权限与时效后生成，不接受模型自行构造授权。

## 对外任务 API 建议
- POST /cases/{id}/investigations：幂等启动调查，返回 runId。
- GET /runs/{id}：返回进度、结论、待补充项及证据引用。
- GET /runs/{id}/events：服务端事件流（Server-Sent Events，SSE）推送进度，断线后按事件序号恢复。
- POST /actions/{id}/approve：针对确定版本的动作确认；拒绝或过期均保持可追溯。
- POST /runs/{id}/cancel：停止后续工具调用；已经发生的外部动作不能伪装成被撤回。

## 上下文与权限
- 模型每次只接收当前问题、相关证据和必要工具；全量代码、全量日志不直接塞入上下文。
- 租户/项目/仓库/日志源权限在工具侧强制执行；读取缓存、历史结果及导出报告也检查当前权限。
- 提供内部详细报告与客户可见答复两种视图，先按可见性过滤证据再生成客户文本。
- 模型路由按数据分级选择允许的部署；提示词、源码注释、工单附件不能提升权限。
- 日志查询限制时间窗、行数、扫描量和并发；复现或测试执行放在无生产凭证的隔离环境。
- 审计保留输入摘要、工具参数、证据引用、动作记录与模型配置，不依赖保存模型隐式推理。

## 上线必要验证
采用真实历史工单脱敏样本，覆盖正确回答、版本不明、反证、缺权限、日志截断和恶意附件。
工程故障测试覆盖进程崩溃恢复、重复消费、Aone 写入超时、索引删除、权限撤销及模型不可用。
首期退出条件：关键回答可追溯到证据；证据不足能正确升级；写动作可恢复且不重复；无越权读取；人工确认业务收益后扩大范围。

## 高频设计追问
- 为什么需要 Agent？问题调查的下一步取决于新证据；固定接入、同步和写入仍用确定性程序。
- 为什么不只做知识库？知识回答历史共性，源码解释实现，运行证据验证当前实例。
- 为什么不一开始做完全自治多 Agent？先明确任务状态、工具权限和评测标准，角色数量不直接代表效果。
- 为什么必须做版本映射？跨仓与灰度部署可能同时存在多版本，当前主干不代表事发时运行代码。

## 关联
[[AI项目/AgentMate/wiki/concepts/多仓源码与运行证据分析]] · [[AI项目/AgentMate/wiki/concepts/工单与Aone闭环]]

## 最小集成验证结果
[[AI项目/AgentMate/wiki/entities/AgentScope-Java验证报告]] 已验证 Spring Boot 4.0.4 + AgentScope core 2.0.3 + JDK 17，5 项测试及独立 JVM 正常重启恢复通过；模型为测试替身，尚未验证业务诊断效果、HarnessAgent 或生产可靠性。
