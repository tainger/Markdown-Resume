---
type: comparison
status: growing
updated: 2026-10-22
---

# AgentScope 1.0 与 2.0 迁移对照

> 比较范围：本库 v1.0.12 与 v2.0.4 两份快照，不把所有 2.x 当成同一 API；细节回链原笔记与版本核对。

| 维度 | 1.0 学习主线 | 2.0 学习主线 | 来源与迁移结论 |
|:---|:---|:---|:---|
| 01 | ReActAgent、聚合依赖和旧模型包 | Core / Harness 与独立模型扩展 | 先替换依赖、import，再核对 Builder；[[AI项目/AgentScope1.0/1.AgentScope初探]]、[[AI项目/AgentScope2.0/1.AgentScope初探与1.0迁移]] |
| 03 | Memory 与 Session | RuntimeContext + AgentState + AgentStateStore | 拆分瞬态元数据和可恢复状态；[[AI项目/AgentScope1.0/3.AgentScope Java特性：多轮会话&会话持久化]]、[[AI项目/AgentScope2.0/3.多轮会话、RuntimeContext与状态持久化]] |
| 04 | 旧 LongTermMemory 模式 | Harness 工作区长期事实与日志 | 旧接口为兼容层，新代码按工作区机制组织；[[AI项目/AgentScope1.0/4.AgentScope Java特性：长期记忆]]、[[AI项目/AgentScope2.0/4.长期记忆与工作区]] |
| 05 | 六级 CompressionController | Compaction / eviction / 溢出恢复 | 机制不一一替换，重核动态预算与默认值；[[AI项目/AgentScope1.0/5.AgentScope Java特性：智能上下文压缩]]、[[AI项目/AgentScope2.0/5.智能上下文压缩与工具结果卸载]] |
| 07 | Hook 回调与 priority | Middleware 主线，Hook 兼容 | order 越大越外；按关注点渐进迁移；[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]、[[AI项目/AgentScope2.0/7.Middleware与Hook迁移]] |
| 08 | Agent 实例执行约束 | CallExecution 与 slot 级队列 | 同实例会话串行不等于分布式锁；[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/AgentScope2.0/8.ReActAgent执行原理与会话并发]] |
| 09 | Toolkit 注册与执行 | 请求工具视图、权限、并发声明 | 不要通过临时改写共享注册表隔离请求；[[AI项目/AgentScope1.0/9.AgentScope Java特性：工具集成]]、[[AI项目/AgentScope2.0/9.工具集成、权限治理与请求隔离]] |
| 10 | MCP 连接与工具注册 | 连接 / 可见视图 / 权限分别核验 | 复查 SDK、客户端所有权与元数据传播；[[AI项目/AgentScope1.0/10.AgentScope Java特性：MCP接入]]、[[AI项目/AgentScope2.0/10.MCP接入与工具可见性]] |
| 11 | Knowledge / RAGMode | 应用层先检索再调用普通 Agent | 旧接口未全删；STATIC 示例不适用此标签；[[AI项目/AgentScope1.0/11.AgentScope Java特性：RAG]]、[[AI项目/AgentScope2.0/11.应用层RAG与旧接口迁移]] |
| 12 | SkillBox / Skill 资源 | 仓库与用户作用域动态装配 | 晋升显式带 ctx；读取技能不等于执行脚本；[[AI项目/AgentScope1.0/12.AgentScope Java进阶：Skill]]、[[AI项目/AgentScope2.0/12.Skill装配、作用域与自学习]] |
| 13 | 工具挂起与人工回填 | ConfirmResult / 外部结果 / AgentRun | 分开审批、外部执行、interrupt 和 cancel；[[AI项目/AgentScope1.0/13.AgentScope Java进阶：Human-in-the-Loop]]、[[AI项目/AgentScope2.0/13.Human-in-the-Loop与AgentRun执行控制]] |
| 14 | Plan-and-Execute 与编排 | Plan Mode / todo / 子 Agent / 文件与任务后端 | 计划正文、状态与后台任务分别恢复；[[AI项目/AgentScope1.0/14.AgentScope Java进阶：Plan-and-Execute]]、[[AI项目/AgentScope2.0/14.Plan模式、子Agent与Harness部署]] |

## 选用与迁移结论

| 情境 | 结论 | 来源 |
|:---|:---|:---|
| 复习旧项目或回答旧 API | 保持 v1.0.12 口径，不把新版能力说成旧项目实现 | [[AI项目/AgentScope1.0/interview/README]] |
| 学习 2.0 裸执行机制 | 先从 Core 与显式 RuntimeContext 入手 | [[AI项目/AgentScope2.0/1.AgentScope初探与1.0迁移]]、[[AI项目/AgentScope2.0/3.多轮会话、RuntimeContext与状态持久化]] |
| 学习长期运行工程装配 | 从 Harness 继续看工作区、压缩、计划与子任务 | [[AI项目/AgentScope2.0/4.长期记忆与工作区]]、[[AI项目/AgentScope2.0/5.智能上下文压缩与工具结果卸载]]、[[AI项目/AgentScope2.0/14.Plan模式、子Agent与Harness部署]] |
| 迁移已有应用 | 逐项验证依赖、状态、工具、审批和副作用；不承诺仅改版本号即可迁移 | [[AI项目/AgentScope2.0/interview/09.版本口径与源码核对]] |
| 判断哪版更快、更准或更省 | ⚠️ raw 缺失，待补充：同任务同配置的质量/成本/吞吐实测 | [[AI项目/AgentScope2.0/6.实战：三层记忆与会话恢复]] |

关联：[[wiki/synthesis/AgentScope1.0面试答题主线]]、[[wiki/synthesis/AgentScope2.0面试答题主线]]、[[wiki/concepts/Agent]]。
