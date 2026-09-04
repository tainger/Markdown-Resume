# 如何编写一份 skill.md

> 基于 AgentMate 智能体平台（阿里云 MSE 团队）的 Skill 体系实战经验，结合 Trae Skill 规范，讲透「什么是 Skill、怎么写、怎么注册调度、怎么沉淀复用」。
> 面试价值：简历中「Skill 体系与回归测试守门」这条职责的展开，能聊 20 分钟的 P7 深度话题。

---

## 一、Skill 是什么

### 1.1 定义

**Skill 是「给 Agent 用的可复用操作手册」**——把一类高频任务的触发条件、所需工具、执行步骤、边界约束、输出格式沉淀成一份结构化文档，Agent 遇到对应场景时自动加载并按步骤执行。

```
Tool（工具）= 一个原子能力（如 curl 请求、读文件）
Skill（技能）= 一组 Tool 的编排 + 业务逻辑 + 约束条件（如「查询值班人员并钉钉推送」）
Agent（智能体）= 能自主决策用哪个 Skill / Tool 的执行主体
```

### 1.2 Skill vs Tool vs Prompt

| 维度 | Tool | Skill | Prompt |
|:---|:---|:---|:---|
| 粒度 | 原子操作 | 任务编排 | 单次指令 |
| 复用性 | 高（通用） | 高（场景化） | 低（一次性） |
| 谁执行 | Agent 直接调 | Agent 加载后按步骤调 | LLM 直接响应 |
| 示例 | HTTP GET | 值班查询技能 | "帮我查下今天谁值班" |

### 1.3 什么时候该写成 Skill

满足以下**任意两条**就应该沉淀为 Skill：

1. **高频**：同类任务每周出现 3 次以上
2. **多步**：需要 2 个以上工具调用 + 条件分支
3. **易错**：人工操作容易漏步骤/顺序错
4. **有边界**：需要明确的输入输出约束、权限控制
5. **可复用**：不同团队/场景都能用（如「发钉钉卡片」是通用 Skill）

---

## 二、skill.md 标准结构

一份完整的 skill.md 由 **frontmatter（元数据）+ body（执行手册）** 两部分组成：

```markdown
---
name: duty-query                    # Skill 唯一标识（kebab-case）
description: 查询当前值班人员并钉钉推送   # 一句话描述（Agent 靠这个判断是否加载）
version: 1.0.0
author: zhaozhiyuan
tags: [duty, dingtalk, ops]          # 标签，用于技能市场分类
triggers:                            # 触发条件（用户说什么时加载）
  - "谁值班"
  - "今天值班"
  - "值班查询"
tools:                               # 依赖的工具（Agent 需先注册这些 Tool）
  - http-client
  - dingtalk-send-card
inputs:                              # 输入参数
  - name: date
    type: string
    required: false
    default: today
    desc: 查询日期，格式 YYYY-MM-DD
outputs:                             # 输出格式约束
  type: dingtalk-card
  template: duty-card                # 引用卡片模板
permissions:                         # 权限控制
  roles: [ops-engineer, sre]
---

# 值班查询技能

## 适用场景

当用户询问「谁值班」「今天值班」「明天值班」等时触发，自动查询值班系统并推送钉钉卡片。

## 执行步骤

1. **解析日期**：从用户输入提取日期，默认今天
2. **查询值班系统**：
   ```
   GET /api/duty/query?date={date}
   Header: Authorization: Bearer {token}
   ```
3. **结果校验**：
   - 若返回空 → 走异常分支「无值班安排」
   - 若返回多人 → 按优先级排序（主班 > 备班）
4. **渲染卡片**：用 duty-card 模板填充值班人员、班次、联系方式
5. **钉钉推送**：发送卡片到指定群 + @值班人

## 边界与约束

- ⚠️ 不允许跨天查询超过 7 天（防止批量拉取）
- ⚠️ 查询失败重试 3 次，间隔指数退避（1s → 2s → 4s）
- ⚠️ 值班人电话脱敏显示（中间四位 *）
- ⛔ 非 ops/sre 角色触发时，返回「无权限」

## 异常处理

| 异常 | 处理策略 |
|:---|:---|
| 值班系统 500 | 重试 3 次，仍失败返回「值班系统暂时不可用」 |
| 日期格式错误 | 提示用户正确格式，不执行 |
| 钉钉推送失败 | 记录日志，重试 1 次，仍失败告警给管理员 |

## 输出示例

```json
{
  "card_type": "duty-card",
  "title": "今日值班通知",
  "content": "主班：张三 138****1234\n备班：李四 139****5678",
  "at_users": ["zhangsan", "lisi"]
}
```
```

---

## 三、编写核心原则

### 3.1 description 是灵魂（Agent 靠它判断是否加载）

```markdown
# ❌ 差：太笼统
description: 查询

# ✅ 好：具体到场景 + 动作
description: 查询当前值班人员并钉钉推送通知卡片

# ❌ 差：像功能清单
description: 一个支持增删改查的用户管理技能

# ✅ 好：像用户会说的话
description: 当用户问谁值班、今天谁 oncall 时，自动查值班系统并发送钉钉卡片
```

### 3.2 步骤要「可执行」而不是「描述性」

```markdown
# ❌ 差：描述性步骤
## 执行步骤
1. 去查一下值班
2. 然后发个消息

# ✅ 好：可执行步骤（含具体 API、参数、判断）
## 执行步骤
1. 解析用户输入的日期参数，默认 today
2. 调用 GET /api/duty/query?date={date}，带 Authorization Header
3. 判断返回值：空 → 异常分支；多人 → 按主备排序
4. 用 duty-card 模板渲染
5. 调用 dingtalk-send-card 推送
```

### 3.3 边界与约束必须写

```markdown
## 边界与约束
- ⚠️ 单次查询不超过 7 天范围
- ⚠️ 电话脱敏（中间四位 *）
- ⛔ 非 ops 角色无权限触发
```

Agent 没有边界意识，**你不写它就会乱来**。边界约束是 Skill 安全性的核心。

### 3.4 异常处理要覆盖「如果...怎么办」

```markdown
## 异常处理
| 异常 | 策略 |
|:---|:---|
| 值班系统超时 | 重试 3 次 + 指数退避 |
| 日期格式错误 | 反问用户正确格式，不瞎猜 |
| 推送失败 | 重试 1 次 + 告警管理员 |
```

---

## 四、AgentMate 实战案例

### 4.1 案例一：值班查询技能（高频简单）

**背景**：运维群里每天有人问「今天谁值班」，人工回复效率低

**Skill 设计**：
- `triggers`: ["谁值班", "今天值班", "oncall"]
- `tools`: [duty-api, dingtalk-card]
- 步骤：解析日期 → 查值班 API → 脱敏 → 渲染卡片 → 推送
- **关键设计**：电话脱敏、非工作时间不推送打扰

**效果**：每天自动处理 20+ 次查询，运维群噪音减少 60%

### 4.2 案例二：研发周报技能（异步长任务）

**背景**：每周一需要生成研发周报，涉及多个数据源（Git 提交、Aone 需求、SLB 告警），耗时 30s+，同步等待会超时

**Skill 设计**（异步模式）：
```markdown
---
name: weekly-report
description: 生成研发周报并推送到钉钉
triggers: ["周报", "研发周报", "本周总结"]
async: true                        # 标记为异步长任务
notify_channel: dingtalk
---

# 研发周报技能

## 执行步骤
1. **立即受理**：先返回「周报生成中，完成后自动推送」（防超时）
2. **后台执行**：
   - 拉取本周 Git 提交记录（按人汇总）
   - 拉取 Aone 需求完成情况
   - 拉取 SLB 告警统计
   - LLM 生成自然语言总结
3. **异步推送**：完成后发钉钉卡片到群 + @相关人
```

**关键设计**：
- 异步模式避免同步超时（简历中「立即受理 + 后台生成 + 完成后异步推送」）
- 失败重试 + 降级（LLM 失败则用模板填充）
- 周报数据来源可配置（不同团队加不同数据源）

### 4.3 案例三：SQL 优化建议技能（专业领域）

**背景**：开发经常写慢 SQL，需要 DBA 人工 review

**Skill 设计**：
- 输入：SQL 文本 + 表结构
- 工具：[sql-parser, explain-analyzer, llm-advisor]
- 步骤：
  1. JSqlParser 解析 AST，识别表/字段/JOIN
  2. 调用 EXPLAIN 获取执行计划
  3. LLM 分析慢查询原因（全表扫描？索引失效？）
  4. 给出优化建议（加索引？改 SQL？）
  5. 输出优化前后对比

---

## 五、Skill 生命周期

```
┌─────────────────────────────────────────────────────────┐
│                  Skill 生命周期                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [1] 编写 skill.md                                       │
│       ↓                                                 │
│  [2] 注册到 Skill Registry（扫描 *.md，解析 frontmatter）│
│       ↓                                                 │
│  [3] Agent 加载（根据用户输入匹配 triggers/description）  │
│       ↓                                                 │
│  [4] 执行（按步骤调用 tools，处理分支和异常）              │
│       ↓                                                 │
│  [5] 回归测试（LLM-as-Judge 语义评判 + 接口真实校验）     │
│       ↓                                                 │
│  [6] 发布到技能市场（导入/导出，跨团队复用）               │
│       ↓                                                 │
│  [7] 监控迭代（成功率、耗时、用户反馈 → 优化 skill.md）    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 5.1 注册机制

AgentMate 用**自动扫描 + 注解**方式注册 Skill：

```java
// SkillRegistry 启动时扫描 classpath:/skills/**/*.md
// 解析 frontmatter → SkillDefinition（name/description/triggers/tools）
// 注册到内存 Map<name, SkillDefinition>

public class SkillRegistry {
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    public void loadAll(String path) {
        // 扫描所有 *.md，解析 frontmatter
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + path + "/**/*.md");
        Files.walk(Paths.get(path))
            .filter(matcher::matches)
            .forEach(this::parseAndRegister);
    }
}
```

### 5.2 调度执行器

```java
// Agent 收到用户消息 → 匹配 triggers → 加载 skill.md → 按步骤执行
public class SkillExecutor {
    public SkillResult execute(String skillName, Map<String, Object> inputs) {
        SkillDefinition skill = skillRegistry.get(skillName);

        // 1. 权限校验
        if (!permissionChecker.hasRole(skill, currentUser)) {
            return SkillResult.denied("无权限");
        }

        // 2. 异步长任务 vs 同步短任务
        if (skill.isAsync()) {
            executor.submit(() -> runSteps(skill, inputs));
            return SkillResult.accepted("已受理，完成后推送");
        }

        // 3. 同步执行步骤
        return runSteps(skill, inputs);
    }
}
```

### 5.3 回归测试（简历亮点）

针对 LLM 输出的非确定性，AgentMate 设计了**双引擎回归测试**：

```
测试用例集（每个 Skill 5-10 个典型场景）
    │
    ▼
┌──────────────────────────────────┐
│  引擎 1：LLM-as-Judge 语义评判     │
│  ├─ 输入：Skill 输出 + 期望结果    │
│  ├─ LLM 判断语义是否等价          │
│  └─ 解决：字符串匹配太严，LLM 输出 │
│     每次措辞不同但语义正确的情况   │
├──────────────────────────────────┤
│  引擎 2：接口真实校验              │
│  ├─ 输入：Skill 输出的关键参数     │
│  ├─ 调用真实接口验证               │
│  └─ 解决：LLM 可能编造不存在的 API │
└──────────────────────────────────┘
    │
    ▼
通过两个引擎 → 回归 PASS
任一失败 → 回归 FAIL，阻断发布流水线
```

---

## 六、Trae Skill 系统对照

当前你在 Trae 环境中，Trae 的 Skill 机制和 AgentMate 的设计理念高度一致：

| 维度 | AgentMate Skill | Trae Skill |
|:---|:---|:---|
| 定义 | skill.md frontmatter + body | SKILL.md（skill-creator 生成） |
| 触发 | triggers 关键词匹配 | 用户指令匹配 skill description |
| 工具 | 注册的 Tool（HTTP/CLI 等） | Trae 内置工具（Read/Edit/Shell 等） |
| 注册 | 自动扫描 classpath:/skills/ | 放到 .trae/skills/ 目录 |
| 生命周期 | 编写→注册→执行→回归→市场 | 编写→加载→执行→迭代 |
| 复用 | 技能市场导入导出 | skills 目录可版本控制共享 |

### Trae Skill 快速编写

用 `skill-creator` 可以快速创建：

```markdown
---
name: code-reviewer
description: 对 MR/PR 执行通用代码审查
---

# 代码审查 Skill

## 审查维度
1. 正确性：逻辑是否正确，边界是否覆盖
2. 可维护性：命名、注释、复杂度
3. 性能：是否有 N+1 查询、不必要的循环
4. 安全：SQL 注入、XSS、敏感信息泄露

## 执行步骤
1. 读取 diff
2. 按 4 个维度逐项检查
3. 输出结构化问题列表（文件:行号 + 问题 + 建议）
4. 严重问题标红
```

---

## 七、面试怎么讲

### 7.1 一句话总结

> 我在 AgentMate 里建设了完整的 Skill 体系，从编写规范、自动注册、调度执行、双引擎回归测试到技能市场复用，累计沉淀 50+ 技能，把一线高频操作从人工变成 Agent 自动执行。

### 7.2 追问链

| 面试官追问 | 回答要点 |
|:---|:---|
| Skill 和 Tool 什么区别？ | Tool 是原子能力（HTTP GET），Skill 是 Tool 的编排 + 业务逻辑 + 约束（查值班+推送） |
| 怎么防止 Agent 乱调 Skill？ | description + triggers 匹配 + 权限角色控制 + 边界约束（7 天限制、脱敏） |
| LLM 输出不稳定怎么保证质量？ | 双引擎回归：LLM-as-Judge 语义评判（解决措辞差异）+ 接口真实校验（防止编造 API） |
| 长任务超时怎么办？ | 异步模式：立即受理返回 + 后台执行 + 完成后钉钉推送，周报技能就是这么做的 |
| Skill 怎么跨团队复用？ | 技能市场：导入/导出 skill.md + 依赖的 Tool 声明 + 版本号管理 |
| 怎么知道 Skill 好不好用？ | 监控埋点：成功率、平均耗时、用户点赞点踩反馈 → 数据驱动迭代 |

### 7.3 亮点话术

> "我们沉淀的 50+ Skill 里，值班查询每天自动处理 20+ 次，周报生成从人工 30 分钟变成自动 1 分钟。但最大的价值不是省时间，而是**把一线同学的操作经验沉淀成了可复用的资产**——新员工不用问老员工，Agent 就会用。"

---

## 八、相关笔记

- Agent 框架整体：[Agent智能体框架.md](Agent智能体框架.md)
- LLM 安全与 Hook 体系：[LLM安全与工程化.md](LLM安全与工程化.md)
- 提示词工程（Skill 中 Prompt 的设计）：[提示词工程.md](提示词工程.md)
- MCP 工具协议（Skill 依赖的 Tool 标准）：[MCP协议.md](MCP协议.md)
- 项目背景（简历原文）：[resume.md - Skill 体系与回归测试](../简历/resume.md#L72)
