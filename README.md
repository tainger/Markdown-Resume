# Markdown Resume

个人简历与面试备战仓库 — 简历维护、JD 追踪、算法刷题笔记，全部用 Markdown 管理。

## 目录结构

```
├── resume.md               # 简历（Markdown 源文件）
├── resume.html             # 简历（HTML 渲染版）
├── assets/                 # 简历用到的图标、头像等静态资源
├── jd/                     # 意向岗位 JD 收集
├── leetcode-hot100/        # Hot 100 刷题笔记（含题解、代码、踩坑总结）
├── 华为OD机试/              # 华为 OD 机试高频真题 + 题解（100/200/300 分）
├── 分布式/                 # 分布式系统面试笔记（事务、一致性、高可用、服务治理、锁 — P7 备战）
├── 系统设计/              # 系统设计面试笔记（短链、秒杀、ID 生成器、Feed 流 — P7 备战）
├── AI应用开发/             # AI 应用落地经验（RAG、Agent框架、大模型基础、安全工程 — 差异化杀手锏）
├── DeepSeek Harness/      # DeepSeek 学习路径与面试资料（模型选型、API应用、RAG/Agent集成、核心原理、部署成本）
├── mysql/                  # MySQL 面试题笔记（索引、事务锁、存储引擎、SQL 优化）
├── jvm/                    # JVM 面试笔记（内存、GC、类加载、JMM、调优 — P7 备战）
├── 计算机网络/              # 计算机网络面试题笔记（分层、TCP、HTTP/HTTPS、IP、DNS/CDN — P7 备战）
├── java/                   # Java 技术笔记
├── agentscope-java/        # AgentScope Java 2.0 设计思想源码解读
├── 自媒体/                # 自媒体技术分享文章（微信公众号、微博）
│   └── 微信公众号/探小虎/ # AI Agent 设计哲学、OpenSpec 技术分享等
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

## LeetCode Hot 100

刷题笔记统一模板：

```
# N. 题目名称
> **难度**：🟢/🟡/🔴 | **标签**：xxx | **企业**：—

## 题目描述 → ## 示例 → ## 提示 → ## 进阶
## 思路 → ## 代码（我的伪代码 + 解法） → ## 个人总结
```

每道题记录：伪代码复盘 + 正确解法 + 踩坑总结，方便二刷回顾。
