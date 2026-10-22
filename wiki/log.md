---
type: log
status: stable
updated: 2026-09-12
---

# 操作日志（append-only）

> 只追加不修改。每条记录：日期 | 动作 | 摘要。

## 2026-09-12

- **[ingest]** 批量消化「剑指offer」剩余 25 篇 raw 笔记（09-05 已消化 5 篇，本次补齐至 30 篇全覆盖）：
  - 链表 3 道：#06 从尾到头打印、#18 删除节点、#22 倒数第 k 个
  - 树 6 道：#26 子结构、#27 镜像、#28 对称、#33 BST 后序、#34 路径和、#54 第 k 大
  - 栈与队列 1 道：#31 压入弹出序列（#09 已在 09-05 消化）
  - 数组与二分 3 道：#03 重复数字、#04 二维查找、#53 排序数组查找
  - 位运算与投票 2 道：#39 摩尔投票、#56 异或分组
  - 滑动窗口与单调队列 2 道：#57 和为 s 的序列、#59 窗口最大值
  - 堆 1 道：#41 数据流中位数
  - 字符串与数学递推 3 道：#58 翻转字符串、#61 扑克牌顺子、#62 约瑟夫环
  - 快排切分 1 道：#40 最小的 k 个数
  - 归并分治 1 道：#51 逆序对
- **[concept 更新]** 已有 7 个算法概念页补入剑指 Offer 例题与易错点：
  - 链表技巧（+3 题）、二叉树遍历（+7 题）、二分查找（+3 题）
  - 滑动窗口（+1 题）、单调栈与单调队列（+1 题）、动态规划（Kadane 易错点补全）
  - 回溯（+1 题）
- **[concept 新建]** 新增 6 个算法概念页（全部 growing 状态）：
  - [[wiki/concepts/栈与队列]] — 双栈实现队列 + 弹出序列验证
  - [[wiki/concepts/位运算与摩尔投票]] — 异或分组 + 摩尔投票抵消法
  - [[wiki/concepts/堆与Top-K]] — Top-K 单堆 + 中位数双堆平衡
  - [[wiki/concepts/归并排序与逆序对]] — 归并跨段计数模板
  - [[wiki/concepts/快速选择]] — Quick Select 找第 k 小
  - [[wiki/concepts/数学递推]] — 约瑟夫环 + 顺子 + 翻转字符串
- **[index 更新]** 算法概念页从 9 个扩展至 15 个，按链表/树/二分/滑窗/DP 等主线重排顺序，覆盖描述同步更新为「hot100 + 剑指 Offer」双线
- **[status 升级]** 二分查找、滑动窗口、回溯 从 seed → growing

## 2026-09-05

- **[ingest]** 批量消化 08-30 后新增的 13 篇 raw 笔记（停更一周补回）：
  - AI应用开发 5 篇：openclaw/#15-16、deepseek-harness/#17-18、6.1 RAG查询全链路
  - 剑指offer 5 篇：#09/#24/#25/#32/#42
  - 量化交易 3 篇：仓位管理/风险管理/策略回测框架
- **[concept 更新]** RAG.md 补入 6.1 查询全链路 6 阶段；ContextEngineering.md 补入 OpenClaw + dsh 双框架对比 + 十大设计模式；Agent.md 补入 OpenClaw vs dsh 对比表
- **[concept 更新]** 算法页补入剑指 offer 引用：链表技巧→#24/#25、二叉树遍历→#32、动态规划→#42、单调栈→#09
- **[concept 新建]** 量化交易.md（仓位管理/风险管理/策略回测三骨架 + 核心指标表）
- **[index 更新]** raw 层目录地图补入 剑指offer/、量化交易/、AI应用开发子目录说明；概念页索引补入 量化交易
- **[status 升级]** RAG/ContextEngineering/Agent/链表技巧/二叉树遍历/动态规划/单调栈 从 seed → growing

## 2026-08-30

- **[bootstrap]** 初始化 wiki 层：按 AGENTS.md 规范创建 index / concepts×12 / entities×8 / comparisons×1 / synthesis×4。全部为 seed 状态（索引 + 一句话总结 + 双链），内容深度靠日常喂养。
- **[lint]** 首轮扫描发现 **raw 层同名重复笔记**（不擅动 raw，仅在 wiki 层互链，待人类决策合并或差异化）：
  - `算法思想/前缀和与差分` ↔ `数据结构/前缀和与差分`
  - `算法思想/并查集` ↔ `数据结构/并查集`
  - `算法思想/哈希表` ↔ `数据结构/哈希表`
  - `算法思想/栈` ↔ `数据结构/栈`
  - `算法思想/队列` ↔ `数据结构/队列`
  - `算法思想/链表` ↔ `数据结构/链表`
- **[note]** `自媒体/微信公众号/探小虎/ LLM Wiki 技术调研` 文件名含前导空格，站上路由已正常生成，但链接书写时需注意。
- **[ingest/raw 决策]** 用户确认处理 6 对同名笔记（raw 层变更已获授权）：
  - `算法思想/前缀和与差分`、`算法思想/并查集` → **真重复**。独有内容（437 树上前缀和提示、华为OD 25/3 题链接）并入 `数据结构/` 对应篇目后删除原文件；`算法思想/README.md` 索引重排（32→31 篇）并留痕。
  - `算法思想/{哈希表,栈,队列,链表}` ↔ `数据结构/{同名}` → **差异化双篇**（套路篇 vs 结构篇，README 尾注已注明定位），全部保留。
  - 同步修复入链 9 处：`算法思想/{循环,数组,统计,哈希表,图论专题}`、`面试/算法面试实战指南.md`、`算法思想/README.md`。
  - wiki 侧：concepts/前缀和与差分、concepts/并查集、concepts/链表技巧、concepts/单调栈与单调队列、entities/HashMap 已同步更新。
- **[output]** 根目录 README.md 新增「LLM Wiki（Agent 知识编译层）」章节（三层架构/四条指令/纪律），目录树补入 `wiki/` 与 `AGENTS.md`。
- **[output]** 新建 [[wiki/usage]] 使用手册：7 个实战 Case（Ingest/Query/Drill/Lint/Output/Plan/Deep-dive）+ 指令速查 + 日常节奏；index、README、AGENTS.md 三处已挂入口。
- **[output]** usage.md 扩充至 12 个 Case，新增：复盘回流 Backflow（真题喂回实体页）、JD Gap 分析（三档覆盖清单）、Locator 定位器、简历背书审计、新主题播种 Seed；日常节奏表同步扩充。
- **[output]** 按「单页 ≤200 行」纪律拆分手册：新建 [[wiki/usage-advanced]] 进阶卷，新增 8 个 Case：考前速记包、答案评分器（五段式）、闪卡+遗忘曲线、踩坑本聚合（hot100 个人总结复用）、对比页工厂、全站死链巡检（补 ignoreDeadLinks 盲区）、选题日历、健康度报告；usage.md 改名「核心卷」。
- **[ingest]** 执行 Case 16 踩坑本聚合：扫 leetcode-hot100 全部 75 个含「## 个人总结」文件（有效 59 / 占位 16）→ 产出 [[wiki/synthesis/高频踩坑本]]（Top 10 高频模式 + 13 簇速查 + 待补坑清单）。已挂载：index 综合主线、P7面试复习主线 刷题保温、usage-advanced Case 16 标记已执行。**lint 发现**：16 篇个人总结为占位符待补，列表见踩坑本末节。
- **[ingest]** 消化 [[AI应用开发/MCP协议]]（经人类授权代起草的 raw 成稿，217 行：M×N→M+N 接入经济学 / 三角色与生命周期 / 六大原语 / JSON-RPC 与 Streamable HTTP / Spring AI @McpTool 落地 / 三态 Hook 复用 / 追问链 / 易错点）→ 新建 [[wiki/entities/MCP]] 实体页（含追问链 + 扣分项）；挂载 [[wiki/concepts/Agent]] 工具调用行与关联区、index 实体表（标题扩为「后端组件 + AI」）。口径锚点：T14 骨架、SpringAI 篇版本基线（SSE 废弃 / 1.1 原生注解）。
- **[ingest]** 消化 [[mysql/笛卡尔积]]（经人类授权代整理的 raw 成稿，10 题面试题：意外产生场景 / CROSS JOIN 正经用途 / JOIN 语义=笛卡尔积+过滤 vs 物理永远避免 / JOIN 放大分页与 COUNT「先收敛再放大」/ 防线机制化）→ 挂载 [[wiki/entities/MySQL]] 核心笔记。联动：深分页优化（分页错乱根因区分）、SQL优化与调优（JOIN 算法）。
- **[output]** 经授权更新 [[mysql/README]] 索引：目录表新增「🔗 笛卡尔积」行（置于深分页之后）、板块描述改八大、必背清单补「JOIN 放大：先收敛再放大」。
- **[ingest]** 消化 [[AI应用开发/提示词工程]]（经人类授权代起草的 raw 成稿，10 题面试题：提示词 ⊂ 上下文工程 / 五段模板与指令-数据物理隔离 / CoT 有效原理（输出 token = 额外计算时间）与有害场景 / System 信任边界 + 前缀缓存 / 结构化输出三层 / prompt 即代码（版本化+评测集回归+灰度）/ 注入防御最终靠权限）→ 挂载 [[wiki/concepts/ContextEngineering]] 核心笔记与知识骨架、index 覆盖描述。口径锚点：T2/T21/T22/T23、LLM安全与工程化（Judge 固化、三层防线）。
- **[ingest]** 消化 [[mysql/SQL优化的实际场景]]（经人类授权代整理的 raw 成稿，10 题场景化面试题：万能五步排查骨架 / 索引失效实战场景（区分写法失效 vs 优化器代价弃用）/ 时快时慢分诊（Buffer Pool 冷热/执行计划漂移/刷脏/锁等待）/ 大表批量导入 / CPU 100% 定位 / Lock wait timeout 排查 / 动态筛选联合索引设计 / 报表实时化分层（预聚合）/ 2000 万扫描逐层优化 / 慢 SQL 治理体系）→ 挂载 [[wiki/entities/MySQL]] 核心笔记与追问链。定位：与 SQL优化与调优（手段层）互为犄角的「场景层」。
- **[output]** 经授权更新 [[mysql/README]] 索引：目录表新增「🩺 SQL 优化实际场景」行、板块描述改九大、必背清单补「场景排查五步」。
- **[fix]** 修复 [[自媒体/微信公众号/探小虎/万物皆可markdown]] 第 13 行失效图片引用：`markdown-everything.jpg`（不存在）→ `context-engineering.jpg`（实际存在），alt 改为「Markdown 是人与 AI 的通用协议」。Vercel 构建坏链阻断已解除。
- **[ingest]** 消化 [[java/线程面试题]]（经人类授权代整理的 raw 成稿，10 题：三种创建方式 / 6 状态转换 + 三种阻塞态区别 / wait/notify + 为什么必须 synchronized 内 / sleep/wait/yield/join 四者对比 / synchronized 锁升级（底层详见 JUC 第二节）/ 死锁四条件 / volatile 写后读 happens-before / daemon 守护线程 / 中断机制 + stop 废弃原因 / 多线程单例五种写法）→ 新建 [[wiki/entities/Java]] 实体页核心笔记区挂载。定位：JUC 工具层之外的「基础线程机制层」，volatile 互引 JVM/JMM。
- **[ingest]** 消化 [[java/线程池面试题]]（经人类授权代整理的 raw 成稿，10 题：三种队列在线程池里的行为差异 + 选型 / 核心参数公式 + 经验值 / 哪些参数能热更新哪些不能 / 监控指标 + 拒绝次数统计 / Spring Boot @Async 默认坑 + 正确配置 / ForkJoinPool vs ThreadPoolExecutor / 线程池隔离舱壁模式 / 任务堆积 OOM 排查 / submit 超时后 cancel(true) 三细节 / 生产必配 5 参数）→ 挂载 [[wiki/entities/Java]] 核心笔记区。定位：与 JUC 第十二节（七大参数/执行流程）互为分工的「生产配置实践 + 场景排查」层。
- **[output]** 经授权新建 [[wiki/entities/Java]] 实体页（Java 语言核心层），覆盖 java 目录 16 篇核心笔记，关联 [[wiki/entities/JVM]]（底层运行时）与 [[wiki/entities/Spring生态]]（框架层）。同时更新 [[java/README]] 索引表：线程面试题 + 线程池面试题状态 ✅，P7 速查区补两条（synchronized 锁升级链路 + 线程池生产必做清单）。


## 2026-10-20 · RAG 定向完善

- **[ingest]** 工作区无待提交变更；以近期新增的 [[AI项目/RAG/README]] 导航为增量入口，定向读取关联原理与面试题，未全库重扫，未修改 raw。
- **[更新]** [[wiki/concepts/RAG]] 接入系列笔记与 66 题入口；新建 [[wiki/synthesis/RAG面试答题主线]]、[[wiki/comparisons/RAG检索方案选型]]，同步全局索引。
- **[更新/标过期]** [[wiki/synthesis/RAG资料审校与补齐清单]] 显式记录 ANN 必选、HNSW 代价、重排必选、评测 GT 冲突、无版本框架能力与固定耗时等证据问题；无法由 raw 解决的标为待补，不写成已核实结论。
- **[合并]** 同主题 raw 仅在 wiki 统一导航：[[AI项目/RAG/19.检索增强生成]] 与 [[AI项目/RAG/37.检索增强生成]] 保留独立，未合并或删改原文。
- **[lint]** 检查本次维护的 5 个页面：双链目标存在、页面不超过 200 行、知识页 frontmatter 完整、索引具备入链；两个空 raw 文件已标缺失。检查范围仅为本次改动。


## 2026-10-22 · gogo-agent 面试题库定向完善

- **[output]** 用户明确指定 `AI项目/gogo-agent/interview`；该目录起初为空，新建 8 个主题页共 64 题、README 阅读导航与 [[AI项目/gogo-agent/interview/09.源码核对与待补证据]]。每题带口述答案、追问/边界、raw 双链和本地源码定位；未修改已有项目笔记或源码。
- **[ingest]** 先查看工作区差异（开始时干净），再按题库主题定向读取项目材料及源码；源码 HEAD 为 `ae3f19dce5972d9f8c76f7e4e63f85c9be785e7c`，未全库重扫。新建 [[wiki/synthesis/gogo-agent面试答题主线]]，挂载 [[wiki/concepts/Agent]] 和全局索引。
- **[更新/标过期]** 13 组口径核对集中记入 [[AI项目/gogo-agent/interview/09.源码核对与待补证据]]：普通直跳停用、业务 Agent 注册/报销边界、工具审核与 LLM 仲裁、0 分不等于剔除、两轮上限为提示词、审批前规划规则冲突、审批检查缓存/归属问题、ECB/GCM 路径差异、熔断真实错误识别及白名单、外部幂等与本地去重、集群恢复非原子消费、方案缓存覆盖/TTL、L2 配置与正文去重保证。静态发现未做运行复现，原文与代码保留。
- **[合并]** 同主题 raw 只在新题库统一导航与解释，不移动/合并/删除原笔记；实现差异明确留痕。缺失的个人贡献、压测、准确率、延迟、成本与故障注入记录标为“⚠️ raw 缺失，待补充”，改进方案不写成已实现。
- **[lint]** 对新题库及 wiki 主线共 11 页检查：64 题连续无重号、每题答案及来源完整、207 处双链目标存在、198 处源码文件/行号有效、frontmatter 完整、代码围栏成对、页面最多 120 行。另核对新增索引入链与日志链接；未执行全库 lint。源码子仓库工作区保持干净；本次未运行应用/原测试，未调用真实供应商。


## 2026-10-22 · AgentScope 1.0 面试题库与源码版本核对

- **[output]** 用户指定 `AI项目/AgentScope1.0/interview`，目录初始为空；新建 8 个主题页共 64 题、README 导航与 [[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]]。每题提供口述答案、追问/边界、原笔记双链和本地源码行号，未修改原 14 篇文档。
- **[版本切换]** 初始源码 main 为 `01056bf63f1a5d8cd8a50430d856dcd57324373c`（2.0.3-SNAPSHOT），工作区无待提交修改。用户明确要求切到 `v1.0.12` 后，检出标签提交 `b9080f27a8384403284fcc225f5e074373cb05e9`（detached HEAD）；跟踪文件无改动，保留切换后显现的原 node_modules 等未跟踪生成产物，未清理或更改忽略配置。
- **[ingest]** 先查看工作区差异（主库仅已有未跟踪的源码目录），再定向阅读本系列 14 篇笔记与相关实现/测试，不全库重扫。新增 [[wiki/synthesis/AgentScope1.0面试答题主线]]，挂载 [[wiki/concepts/Agent]] 与全局索引，并关联 gogo-agent 应用题库。
- **[更新/标过期]** 14 组差异集中在 [[AI项目/AgentScope1.0/interview/09.版本口径与源码核对]]：版本不可混用、Pending 恢复默认关闭、Toolkit 复制引用范围、静态长期记忆 PreCall/USER、Generic RAG PreCall、Hook 优先级与并发通知、工具异常提前转结果影响重试、Toolkit/McpTool 预设优先级差异、MCP enable/disable 及空列表语义、默认卸载为实例 HashMap、迭代/近期保护/估算边界、statefulToolsManaged 未见对应遍历、PostActing 批结果最后停止位、Session 原子性与宕机恢复边界。源码静态发现未运行复现，不改原文或实现。
- **[合并]** 同主题仅在题库整合导航与答题口径，不合并/移动 raw；缺失的个人实践、性能、成本、质量和故障注入结果统一标“⚠️ raw 缺失，待补充”，不把改进方案写成成果。
- **[lint]** 检查题库及新 wiki 主线共 11 页：Q01–Q64 连续无重号，每题答案与来源完整，190 处双链、204 处源码位置有效；66 个不同源码文件逐一与 v1.0.12 提交对象比对一致。frontmatter、代码围栏、导航与行数检查通过，页面最多 114 行；新增 wiki 入链/本段日志链接存在。源码跟踪文件无 diff；未运行原测试、真实模型或 MCP 服务，未执行全库 lint。


## 2026-10-22 · AgentScope 2.0 教程与面试题库维护

- **[output]** 用户明确要求维护 `AI项目/AgentScope2.0` 并参考 1.0；以原空 README 为入口，经授权补齐 14 篇新版教程、8 个主题共 Q01–Q64、题库导航及 [[AI项目/AgentScope2.0/interview/09.版本口径与源码核对]]，每题含答案、追问、边界、教程双链与源码位置。1.0 原笔记、题库、上游源码/文档均未修改。
- **[ingest]** 开始时主库工作区干净，2.0 只有空 README、源码与带前导空格空目录 ` interview/`；定向对照 1.0 结构及 2.0 文档/实现，未全库重扫。新建标准 `interview/`，保留原空目录。源码已位于 `v2.0.4` / `0b6743b263ba8e39eb417e35048026f1e0196548`（Java 17），未切换标签；核对官方同标签发布记录，不以 main 解释此快照。
- **[更新]** 新建 [[wiki/synthesis/AgentScope2.0面试答题主线]] 与 [[wiki/comparisons/AgentScope1.0与2.0迁移对照]]，挂载 [[wiki/index]] 与 [[wiki/concepts/Agent]]。教程按 RuntimeContext / AgentState / Middleware / 权限与请求工具视图 / Harness 重新组织，未逐字复制旧 API。
- **[更新/标过期]** 16 组口径核对集中记入核对页：版本与模块、Core/Harness 默认保存、实例队列与 CAS、Middleware 顺序/系统提示词入口、Harness 默认开启压缩及 eviction、动态预算、工具并发默认 true、RAGMode.STATIC 不存在及旧兼容入口、maxRetries 实际传入 maxAttempts、技能晋升显式 ctx、Pending 恢复默认关闭与 ASKING 优先、审批 ID 校验与应用授权边界、cancel/interrupt、returnDirect 批次条件与 APPEND_MERGE 范围。上游残留文档只标明差异，不改原文或实现。
- **[合并]** 同主题只整合导航与版本对照，1.0 与 2.0 保持独立；未合并/移动 raw。第 6 篇配置示例与验收步骤明确标为待执行；真实模型/MCP、重启、多副本、竞态/故障、性能成本、质量与个人贡献标“⚠️ raw 缺失，待补充”，不虚构已完成成果。
- **[lint]** 检查本次 29 个教程/题库/wiki 页面：Q01–Q64 连续，每题四字段与来源完整；354 处双链、211 处本机文件/行号引用有效，32 个不同引用源码文件与固定提交对象逐一比对一致。frontmatter、代码围栏、导航和行数检查通过，最多 132 行；示例导入与依赖 artifact 静态存在性核对通过。日志本段增量双链有效，原日志保留前缀不变；上游子仓库干净。未运行 Java 示例、原测试或真实外部服务，未执行全库 lint。

## 2026-10-22 · ReActAgent 上下文管理职责查询

- **[query/更新]** 基于现有 wiki、1.0 原笔记与本地 `v1.0.12` 源码，向 [[wiki/synthesis/AgentScope1.0面试答题主线]] 追加上下文职责说明：默认 Memory 只累积历史；自动压缩由 AutoContextMemory + AutoContextHook 接入；Session、长期记忆和 ToolExecutionContext 各有不同职责。来源挂载 [[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]、[[AI项目/AgentScope1.0/5.AgentScope Java特性：智能上下文压缩]]、[[AI项目/AgentScope1.0/6.实战：实现AgentScope的三层记忆体系]]。
- **[核对]** 静态检查消息加入、prepareMessages、PreReasoning 输入替换、默认 Memory、压缩入口与扩展依赖，并核对官方同标签 AutoContextHook 页面；未修改 raw 或源码，未调用真实模型，未全库重扫。本次说明沿用现有版本核对结论，无新增冲突覆盖。


## 2026-10-22 · AgentScope 2.0 会话标识源码审查与修复

- **[review/output]** 用户要求 review 代码并寻找可提交修复。定向审查 ReActAgent 会话键、缓存/版本/权限、持久化、队列与中断，确认拼接键碰撞及反向拆分导致错误保存。新建 [[AI项目/AgentScope2.0/review/01.会话标识碰撞与修复]]，含复现条件、原版与修复来源、测试证据和可移植补丁；未全库重扫。
- **[fix]** 原源码保持 v2.0.4 / 0b6743b263ba8e39eb417e35048026f1e0196548，另建修复工作区（最终移至 /private/tmp/agentscope-java-slot-fix-761af7f6），分支 codex/fix-session-slot-identity，本地提交 761af7f6：fix(core): preserve structured session slot identities。SlotRef 二元组取代拼接/解析键，新增 5 项回归，调整 2 个已有私有反射测试辅助代码，共 4 个 Java 文件；无公开 API/持久化格式变更，未推送远端。
- **[test/lint]** 5 个场景在原版均失败，修复后通过；8 组测试入口合计 103 项、0 失败/错误/跳过，改动文件 Spotless 与 diff --check 通过，补丁在原快照 apply --check 通过但未应用。构建使用临时镜像与 UTF-8/预载测试代理，无 POM/依赖改动；没有调用真实外部服务。报告与增量双链/代码位置有效，单页不超过 200 行。
- **[ingest]** 更新 2.0 README 与 [[wiki/synthesis/AgentScope2.0面试答题主线]] 入口，补上“逻辑隔离须有无碰撞内部键”的运行证据；历史静态核对日志保留，原 1.0 内容、2.0 标签快照与既有教程口径不覆盖。


## 2026-10-22 · Reactor Mono 与 Flux 使用查询

- **[query/更新]** 在 [[wiki/synthesis/AgentScope1.0面试答题主线]] 追加 Reactor 执行链读法，引用 [[AI项目/AgentScope1.0/2.AgentScope Java特性：流式输出、结构化输出、超时与重试、执行控制]]、[[AI项目/AgentScope1.0/7.AgentScope Java特性：Hook]]、[[AI项目/AgentScope1.0/8.AgentScope Java中ReactAgent的实现原理]]。定向核对本地 v1.0.12 的 call、stream 与 reasoning，以及 Reactor 官方 Mono/Flux、订阅、调度和 defer 文档；原笔记、源码及其他既有修改保持原状。
- **[lint]** 本次主线页 frontmatter、200 行上限及双链目标检查通过；仅静态核对示例和操作符含义，未运行模型或应用。


## 2026-10-22 · Reactor 3 从入门到精通教程

- **[output]** 用户明确指定 `AI项目/Reactor`（初始为空）并提供 Reactor 官方参考文档入口，授权在该 raw 目录新建教程。创建 [[AI项目/Reactor/README]] 与 13 章正文，包含订阅/类型、操作符组合、线程/背压、错误/超时/重试、冷热流/Sinks、Context/清理、测试/调试、批处理/生产排错和 AgentScope/WebFlux 对照；14 页共 1385 行，未修改既有 raw 笔记或上游源码。
- **[版本/来源]** 2026-10-22 官方 release 页面标示 3.8.7；实验固定 Java 17 + Core/Test 3.8.2，与本地 AgentScope v1.0.12 / b9080f27 的 Reactor BOM 2025.0.2 对齐。各章链接官方对应章节，教学片段与可完整编译工程明确区分；未宣称逐字翻译或覆盖所有 API。
- **[test]** 新建独立 examples 工程，28 项 JUnit/StepVerifier 语义实验在临时同内容副本离线运行通过：0 失败、0 错误、0 跳过。覆盖重复订阅、提前求值、顺序与并发、空流/Void、需求、恢复、重试、虚拟时间超时、Context、取消/资源、缓存错误、Sinks、Probe/TestPublisher 与窗口。因本机缓存来源标识差异使用临时 Maven settings，未更改全局配置；源码与 POM 逐字节比对一致。结果记入 [[AI项目/Reactor/13.可执行实验与面试自测]]。
- **[ingest]** 定向读取相关 AgentScope/gogo-agent 笔记及固定版本实现，新建 [[wiki/synthesis/Reactor3学习与源码主线]]，挂载 [[wiki/index]] 与 [[wiki/synthesis/AgentScope1.0面试答题主线]]；未全库重扫。用户先前“只放 wiki”的默认规则已被本次指定新教程目录的明确授权覆盖，不推广为允许修改其他 raw。
- **[更新/标过期]** 保留原 [[AI项目/gogo-agent/27.基于Reactor Context 实现多智能体之间的会话上下文传递]]，在 [[AI项目/Reactor/09.Context与资源生命周期]] 及 wiki 主线显式修正“ThreadLocal 完全不可用”“独立订阅 Context 必然为空”的绝对表述：桥接、显式 Context 和框架 Hook 会影响结果。[[AI项目/Reactor/11.AgentScope与WebFlux实战]] 另记录 v1.0.12 事件桥内部订阅/取消绑定与 BUFFER 的静态边界，未声称已做修复或端到端复现。
- **[lint]** 本次 14 个教程页面与 1 个 wiki 主线的 94 处双链、4 处本地源码位置、代码围栏、导航与 200 行上限通过检查（单页最多 117 行），wiki frontmatter 与索引入链有效。未运行真实模型/SDK、WebFlux 服务、生产压测或全库 lint；这些证据缺口已明确记录。原有未提交修改保留。
