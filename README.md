# Markdown Resume

个人简历与面试备战仓库 — 简历维护、JD 追踪、算法刷题笔记，全部用 Markdown 管理。

## 目录结构

```
├── resume.md               # 简历（Markdown 源文件）
├── resume.html             # 简历（HTML 渲染版）
├── assets/                 # 简历用到的图标、头像等静态资源
├── jd/                     # 意向岗位 JD 收集
├── wiki/                   # LLM Wiki 知识编译层（AI Agent 维护，详见下方「LLM Wiki」）
├── AGENTS.md               # wiki 层维护规范（所有权 / 页面类型 / 双链 / 工作流）
├── leetcode-hot100/        # Hot 100 刷题笔记（含题解、代码、踩坑总结）
├── 华为OD机试/              # 华为 OD 机试高频真题 + 题解（100/200/300 分）
├── 分布式/                 # 分布式系统面试笔记（事务、一致性、高可用、服务治理、锁 — P7 备战）
├── 系统设计/              # 系统设计面试笔记（短链、秒杀、ID 生成器、Feed 流 — P7 备战）
├── 权限设计/              # 权限设计面试笔记（权限模型、RBAC 落地、认证会话、数据权限、微服务鉴权 — P7 备战）
├── AI应用开发/             # AI 应用落地经验（RAG、Agent框架、大模型基础、安全工程 — 差异化杀手锏）
├── DeepSeek Harness/      # DeepSeek 学习路径与面试资料（模型选型、API应用、RAG/Agent集成、核心原理、部署成本）
├── mysql/                  # MySQL 面试题笔记（索引、事务锁、存储引擎、SQL 优化）
├── redis/                  # Redis 面试题笔记（数据类型、持久化、内存淘汰、高可用集群、缓存实战 — P7 备战）
├── redisson/               # Redisson 面试题笔记（分布式锁全家桶/看门狗源码/二级缓存/延迟队列/Spring 集成事务坑 — P7 备战）
├── jvm/                    # JVM 面试笔记（内存、GC、类加载、JMM、调优 — P7 备战）
├── 计算机网络/              # 计算机网络面试题笔记（分层、TCP、HTTP/HTTPS、IP、DNS/CDN — P7 备战）
├── Mybatis/                # MyBatis 面试题笔记（架构执行流程、缓存、动态SQL、插件 — P7 备战）
├── dubbo/                  # Dubbo 面试题笔记（架构流程、SPI、注册发现、容错负载均衡、通信线程模型 — P7 备战）
├── elasticsearch/          # ElasticSearch 面试题笔记（倒排索引、写入近实时、搜索评分、深分页调优、高可用 — P7 备战）
├── io/                     # IO 面试题笔记（IO模型、NIO多路复用、零拷贝、Netty — P7 备战）
├── java/                   # Java 技术笔记
├── agentscope-java/        # AgentScope Java 2.0 设计思想源码解读
├── 自媒体/                # 自媒体技术分享文章（微信公众号、微博）
│   └── 微信公众号/探小虎/ # AI Agent 设计哲学、OpenSpec 技术分享等
├── 每日记录/              # 每日学习记录（按月分组，方便复习回看）
├── lover/                  # 其他
├── repo/                   # 其他
├── mdconvert               # 文档转换工具（见下方说明）
├── .venv/                  # Python 虚拟环境（gitignore）
└── .gitignore
```

## 工具

### mdconvert — 任意文档转 Markdown

基于微软 [MarkItDown](https://github.com/microsoft/markitdown) 的一键转换脚本，支持 PDF、Word、PPT、Excel、HTML 等格式。

#### 安装

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install markitdown
```

#### 使用

```bash
# 基本用法：输入文件 → 自动生成同名 .md
./mdconvert input.pdf

# 指定输出路径
./mdconvert input.docx output.md
```

#### 支持的格式

| 格式 | 说明 |
|:---|:---|
| `.pdf` | PDF 文档 |
| `.docx` | Word 文档 |
| `.pptx` | PowerPoint 幻灯片 |
| `.xlsx` | Excel 表格 |
| `.html` | 网页 |
| `.csv` / `.json` / `.xml` | 结构化数据 |
| `.zip` | 压缩包（递归解析内部文件） |

#### 典型场景

```bash
# 把 JD 的 PDF/Word 转成 Markdown 存入 jd/ 目录
./mdconvert ~/Downloads/Java高级工程师.pdf jd/Java高级工程师.md

# 简历 HTML → Markdown 双向维护
./mdconvert resume.html resume.md
```

## LLM Wiki（Agent 知识编译层）

灵感来自 Karpathy 的 LLM Wiki 模式（完整调研见 [LLM Wiki 技术调研](自媒体/微信公众号/探小虎/%20LLM%20Wiki%20技术调研.md)）：知识让 LLM **编译一次、持续积累**，而不是每次查询从零推导。本仓库落地为三层：

| 层 | 位置 | 所有者 | 说明 |
|:---|:---|:---|:---|
| Schema | `AGENTS.md` | 共同 | wiki 层维护规范：所有权划分、页面类型、双链语法、工作流 |
| raw | 除 `wiki/` 外所有笔记目录 | 人类 | LLM **只读**，禁止修改任何现有笔记 |
| wiki | `wiki/` | LLM | 知识编译层：索引 + 一句话总结 + 双链，人类可随时推翻 |

### wiki 目录结构

```
wiki/
├── index.md        # 全局索引（入口先看这里）
├── log.md          # 操作日志（append-only，记录每次 ingest/lint/决策）
├── concepts/       # 概念页：算法思想与技术机制（滑动窗口、动态规划、RAG、Agent…）
├── entities/       # 实体页：具体组件（HashMap、Redis、MySQL、RocketMQ、JVM…）
├── comparisons/    # 对比页（B树 vs B+树 vs 跳表…）
└── synthesis/      # 综合页：跨目录面试主线（P7 复习主线、高并发库存扣减、缓存一致性…）
```

### 怎么用：对 AI 编码助手说四条指令

AI 助手（Trae / Claude Code 等）会自动读取根目录 `AGENTS.md` 获知规范：

| 指令 | 场景 | Agent 动作 |
|:---|:---|:---|
| `消化 <笔记路径>` | 写完/改完一篇笔记 | 读 raw → 更新相关 wiki 页 → 维护双链 → 记 log（增量，git diff 驱动） |
| 直接提问 | 平时问技术问题 | 优先基于 wiki 已编译知识回答并附来源双链，有价值的结论沉淀回 wiki |
| `lint wiki` | 定期体检 | 死链 / 矛盾 / 孤立页 / 过期检查，输出「更新/合并/标过期」三动作 |
| `从 wiki 出草稿` | 写公众号文章 | 从 synthesis/comparison 蒸馏草稿，**新建文件**，不动已定稿旧文 |

> 实战 Case 演示：核心 12 个见 [wiki/usage.md](wiki/usage.md)，进阶 8 个见 [wiki/usage-advanced.md](wiki/usage-advanced.md)。

### 纪律（为什么它不会把仓库搞乱）

1. **所有权隔离**：LLM 永不改 raw 笔记，只写 `wiki/`
2. **不虚构**：wiki 每个论断都有 `[[双链]]` 指回 raw 原文，raw 里没有的标「待补充」
3. **不发布**：`wiki/**` 与 `AGENTS.md` 已加入 VitePress `srcExclude`，站上不渲染，仅作 AI 工作记忆

### 首批资产（2026-08-30 初始化）

26 个页面：index/log + 12 concept + 8 entity + 1 comparison + 4 synthesis。首轮 lint 处理了 `算法思想/` 与 `数据结构/` 的 6 对同名笔记（2 对真重复已合并删除、4 对确认为「结构 vs 套路」差异化双篇），决策记录见 `wiki/log.md`。

## LeetCode Hot 100

刷题笔记统一模板：

```
# N. 题目名称
> **难度**：🟢/🟡/🔴 | **标签**：xxx | **企业**：—

## 题目描述 → ## 示例 → ## 提示 → ## 进阶
## 思路 → ## 代码（我的伪代码 + 解法） → ## 个人总结
```

每道题记录：伪代码复盘 + 正确解法 + 踩坑总结，方便二刷回顾。
