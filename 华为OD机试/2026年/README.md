# 华为 OD 机试 · 2026 年真题整理

本目录收录 **2026 年**华为 OD 机试真题索引 + 题解，风格与 [../2025年/](../2025年/) 及上级 [../README.md](../README.md) 保持一致：每题含「题目描述 → 示例 → 提示 → 思路 → 伪代码 → 题解 → 个人总结」。

> ⚠️ 题目根据流传真题整理，**题面/分值可能随卷次变化**，以实际考试为准；重点掌握每题背后的**考点**。
>
> 📌 **2026-04-01 起华为 OD 机试全面切换新系统（力扣模式）**，无需自己处理输入输出，只需实现函数签名。本目录所有题目按新系统格式整理。
>
> 📚 **题库来源**：[CSDN 2026最新华为OD机试新系统卷 + 双机位C卷 真题题库目录](https://blog.csdn.net/banxia_frontend/article/details/160346662)（更新至 2026-09-06）

## 题目索引（按月分组）

### 9 月（共 2 场 6 题，已整理 6 题）

| # | 日期 | 题目 | 分值 | 考点 | 状态 |
|:---:|:---:|:---|:---:|:---|:---:|
| 2026-09-06a | 09-06 | [仓储货物有序出库](2026-09-06a.%20仓储货物有序出库.md) | 100 | 模拟（双栈货物搬运） | ✅ |
| 2026-09-06b | 09-06 | [少儿爬山闯关积分游戏](2026-09-06b.%20少儿爬山闯关积分游戏.md) | 100 | 动态规划（不能连续跳 2 关的爬楼梯） | ✅ |
| 2026-09-06c | 09-06 | [嵌入式传感器寄存器数据解析](2026-09-06c.%20嵌入式传感器寄存器数据解析.md) | 200 | 位运算（32 位寄存器位域解析 + 校验） | ✅ |
| 2026-09-02a | 09-02 | [查找幸运数](2026-09-02a.%20查找幸运数.md) | 100 | 字符串（6/8 连续段 + TreeSet 去重排序） | ✅ |
| 2026-09-02b | 09-02 | [螳螂领地争霸](2026-09-02b.%20螳螂领地争霸.md) | 100 | 排序、区间合并（闭区间边界接触合并） | ✅ |
| 2026-09-02c | 09-02 | [无人机巡检航线规划](2026-09-02c.%20无人机巡检航线规划.md) | 200 | 动态规划（状态压缩 TSP，曼哈顿距离） | ✅ |

### 8 月（共 9 场 27 题，已整理 27 题）

| # | 日期 | 题目 | 分值 | 考点 | 状态 |
|:---:|:---:|:---|:---:|:---|:---:|
| 2026-08-30a | 08-30 | [小花获胜的奶茶](2026-08-30a.%20小花获胜的奶茶.md) | 100 | 滑动窗口（定长窗口最大和） | ✅ |
| 2026-08-30b | 08-30 | [快递驿站计费系统](2026-08-30b.%20快递驿站计费系统.md) | 100 | 模拟计算（哈希聚合计费 + 双关键字排序） | ✅ |
| 2026-08-30c | 08-30 | [单词搜索计数](2026-08-30c.%20单词搜索计数.md) | 200 | DFS（四方向回溯路径计数） | ✅ |
| 2026-08-26a | 08-26 | [仓库查询](2026-08.%20仓库查询.md) | 100 | 排序算法（前缀排序取第 j 位） | ✅ |
| 2026-08-26b | 08-26 | [字符串回文判](2026-08-26b.%20字符串回文判.md) | 100 | 双指针（失配点 + 可删下标） | ✅ |
| 2026-08-26c | 08-26 | [部门绩效汇总](2026-08-26c.%20部门绩效汇总.md) | 200 | 二叉树（后序遍历子树求和） | ✅ |
| 2026-08-23a | 08-23 | [直线冲刺](2026-08-23a.%20直线冲刺.md) | 100 | 模拟（弹簧/陷阱规则移动） | ✅ |
| 2026-08-23b | 08-23 | [小牛牛超市选品](2026-08-23b.%20小牛牛超市选品.md) | 100 | 枚举（连续区间约束最短长度） | ✅ |
| 2026-08-23c | 08-23 | [抗洪救灾](2026-08-23c.%20抗洪救灾.md) | 200 | DFS（8 连通连通块计数） | ✅ |
| 2026-08-19a | 08-19 | [基站低能耗时段统计](2026-08-19a.%20基站低能耗时段统计.md) | 100 | 滑动窗口 | ✅ |
| 2026-08-19b | 08-19 | [大整数位序反转](2026-08-19b.%20大整数位序反转.md) | 100 | 进制转换、位运算（BigInteger 二进制反转） | ✅ |
| 2026-08-19c | 08-19 | [点亮战争迷雾](2026-08-19c.%20点亮战争迷雾.md) | 200 | 二叉树、树形 DP（三状态守卫） | ✅ |
| 2026-08-16a | 08-16 | [电池充电时间计算](2026-08-16a.%20电池充电时间计算.md) | 100 | 模拟（三段充电区间求交） | ✅ |
| 2026-08-16b | 08-16 | [信道资源分配与掩码运算](2026-08-16b.%20信道资源分配与掩码运算.md) | 100 | 位运算（掩码合并 + 最长连续 1） | ✅ |
| 2026-08-16c | 08-16 | [智能家居模式调度优化](2026-08-16c.%20智能家居模式调度优化.md) | 200 | 递归回溯（DFS 枚举 + 剪枝） | ✅ |
| 2026-08-12a | 08-12 | [LLM推理批次最大化](2026-08-12a.%20LLM推理批次最大化.md) | 100 | 贪心（区间调度，按右端点排序） | ✅ |
| 2026-08-12b | 08-12 | [获取二叉树第k层的数值](2026-08-12b.%20获取二叉树第k层的数值.md) | 100 | 二叉树、BFS（建邻接表 + 按层遍历） | ✅ |
| 2026-08-12c | 08-12 | [末世分配资源包](2026-08-12c.%20末世分配资源包.md) | 200 | 二分答案 + 贪心分段 | ✅ |
| 2026-08-09a | 08-09 | [查找最佳充电策略](2026-08-09a.%20查找最佳充电策略.md) | 100 | 滑动窗口（定长窗口最小和） | ✅ |
| 2026-08-09b | 08-09 | [灯带颜色变换](2026-08-09b.%20灯带颜色变换.md) | 100 | 逻辑分析（状态压缩 + 循环节） | ✅ |
| 2026-08-09c | 08-09 | [云南菌子加工](2026-08-09c.%20云南菌子加工.md) | 200 | 贪心、动态规划（位掩码 DP） | ✅ |
| 2026-08-05a | 08-05 | [最长不连续子串](2026-08-05a.%20最长不连续子串.md) | 100 | 双指针（一次遍历） | ✅ |
| 2026-08-05b | 08-05 | [智能广播合并台号](2026-08-05b.%20智能广播合并台号.md) | 100 | 双指针（桶去重 + 连续段合并） | ✅ |
| 2026-08-05c | 08-05 | [IPv4等长子网划分与自动分配系统](2026-08-05c.%20IPv4等长子网划分与自动分配系统.md) | 200 | 模拟（CIDR 位运算） | ✅ |
| 2026-08-02a | 08-02 | [计算电动车续航里程](2026-08-02a.%20计算电动车续航里程.md) | 100 | 模拟（分段系数 + 四舍五入） | ✅ |
| 2026-08-02b | 08-02 | [语义化版本号对比](2026-08-02b.%20语义化版本号对比.md) | 100 | 模拟（版本解析 + 多级比较） | ✅ |
| 2026-08-02c | 08-02 | [战士技能规划设计](2026-08-02c.%20战士技能规划设计.md) | 200 | 贪心、滑动窗口（前后缀分解） | ✅ |

### 7 月（共 9 场 27 题，已整理 27 题）

| # | 日期 | 题目 | 分值 | 考点 | 状态 |
|:---:|:---:|:---|:---:|:---|:---:|
| 2026-07-29a | 07-29 | [能量对撞](2026-07-29a.%20能量对撞.md) | 100 | 模拟计算（栈消除） | ✅ |
| 2026-07-29b | 07-29 | [基于上报数据的实时最大值](2026-07-29b.%20基于上报数据的实时最大值.md) | 100 | 暴力枚举（回溯窗口最大值） | ✅ |
| 2026-07-29c | 07-29 | [路口等待时间](2026-07-29c.%20路口等待时间.md) | 200 | 模拟计算（红绿灯周期 + 4 方向排队） | ✅ |
| 2026-07-26a | 07-26 | [区分奇偶的数组排序](2026-07-26a.%20区分奇偶的数组排序.md) | 100 | 排序算法（拆分 + 交替合并） | ✅ |
| 2026-07-26b | 07-26 | [数字系统转换器](2026-07-26b.%20数字系统转换器.md) | 100 | 数学逻辑（任意进制 BigInteger） | ✅ |
| 2026-07-26c | 07-26 | [炸弹人的雷区数量](2026-07-26c.%20炸弹人的雷区数量.md) | 200 | DFS（连通块计数） | ✅ |
| 2026-07-22a | 07-22 | [不同Tag类型统计](2026-07-22a.%20不同Tag类型统计.md) | 100 | 模拟（TLV 解析 + 4 字节对齐 + 格式校验） | ✅ |
| 2026-07-22b | 07-22 | [统计能源使用时段](2026-07-22b.%20统计能源使用时段.md) | 100 | 双指针（至多 2 种最长子数组） | ✅ |
| 2026-07-22c | 07-22 | [最小代价完成论文评审](2026-07-22c.%20最小代价完成论文评审.md) | 200 | 暴力枚举（集合覆盖 + 位掩码枚举） | ✅ |
| 2026-07-19a | 07-19 | [酒店服务记录分析](2026-07-19a.%20酒店服务记录分析.md) | 100 | 集合（按首次出现顺序输出重复字符） | ✅ |
| 2026-07-19b | 07-19 | [物流仓储多维度成本利润综合查询系统](2026-07-19b.%20物流仓储多维度成本利润综合查询系统.md) | 100 | 模拟计算（扁平数组索引 + 整数风控判定） | ✅ |
| 2026-07-19c | 07-19 | [小明的顺风车](2026-07-19c.%20小明的顺风车.md) | 200 | 动态规划（加权区间调度） | ✅ |
| 2026-07-15a | 07-15 | [谁没交卷](2026-07-15a.%20谁没交卷.md) | 100 | 集合（差集 + 升序枚举） | ✅ |
| 2026-07-15b | 07-15 | [日期格式统一与排序](2026-07-15b.%20日期格式统一与排序.md) | 100 | 字符串（日期解析 + 闰年校验 + 去重排序） | ✅ |
| 2026-07-15c | 07-15 | [毕业旅行](2026-07-15c.%20毕业旅行.md) | 200 | 图（Dijkstra 单源最短路 + 预算约束） | ✅ |
| 2026-07-12a | 07-12 | [字符串压缩编码](2026-07-12a.%20字符串压缩编码.md) | 100 | 双指针（连续段压缩） | ✅ |
| 2026-07-12b | 07-12 | [查找满足条件的数据包](2026-07-12b.%20查找满足条件的数据包.md) | 100 | 单调栈（按 weight 分组 + 右侧首个更大 priority） | ✅ |
| 2026-07-12c | 07-12 | [软件依赖树](2026-07-12c.%20软件依赖树.md) | 200 | BFS（依赖解析 + 路径 exclusion 传递） | ✅ |
| 2026-07-08a | 07-08 | [魔法咒语组合](2026-07-08a.%20魔法咒语组合.md) | 100 | 递归（纯净过滤 + 全排列 + 字典序） | ✅ |
| 2026-07-08b | 07-08 | [计算碳排放减少量](2026-07-08b.%20计算碳排放减少量.md) | 100 | 并查集（连通分量 + 分组聚合） | ✅ |
| 2026-07-08c | 07-08 | [迷宫相遇](2026-07-08c.%20迷宫相遇.md) | 200 | BFS（双动点 + 周期状态压缩） | ✅ |
| 2026-07-05a | 07-05 | [省超足球比赛获胜队伍计算](2026-07-05a.%20省超足球比赛获胜队伍计算.md) | 100 | 哈希（积分统计 + 净胜球排序） | ✅ |
| 2026-07-05b | 07-05 | [SQL记录拆分](2026-07-05b.%20SQL记录拆分.md) | 100 | 并查集（同行约束 + 同事务约束 + 传递合并 + 贪心分配） | ✅ |
| 2026-07-05c | 07-05 | [二叉树两节点间的最小跳数](2026-07-05c.%20二叉树两节点间的最小跳数.md) | 200 | 树（完全二叉树数组表示 + LCA + 跳数公式） | ✅ |
| 2026-07-01a | 07-01 | [仓库盘点](2026-07-01a.%20仓库盘点.md) | 100 | 哈希（计数 + 首次位置排序） | ✅ |
| 2026-07-01b | 07-01 | [奇偶三数之和](2026-07-01b.%20奇偶三数之和.md) | 100 | 双指针（排序 + 三数之和 + 奇偶过滤） | ✅ |
| 2026-07-01c | 07-01 | [收集灵草](2026-07-01c.%20收集灵草.md) | 200 | 暴力枚举（连续子序列 + 丢弃最负 M 个负数） | ✅ |

### 6 月（共 7 场 21 题）

| # | 日期 | 题目 | 分值 | 考点 | 状态 |
|:---:|:---:|:---|:---:|:---|:---:|
| 2026-06-28a | 06-28 | [最小极差分组](https://blog.csdn.net/banxia_frontend/article/details/162642702) | 100 | 贪心算法 | ⬜ |
| 2026-06-28b | 06-28 | [统计不重叠区间的个数](https://blog.csdn.net/banxia_frontend/article/details/162643207) | 100 | 暴力枚举 | ⬜ |
| 2026-06-28c | 06-28 | [盘丝洞破阵寻珠](https://blog.csdn.net/banxia_frontend/article/details/162643379) | 200 | DFS | ⬜ |
| 2026-06-24a | 06-24 | [终端设备档位差异统计](https://blog.csdn.net/banxia_frontend/article/details/162641896) | 100 | 统计去重 | ⬜ |
| 2026-06-24b | 06-24 | [设计能量管理系统](https://blog.csdn.net/banxia_frontend/article/details/162642062) | 100 | 模拟计算 | ⬜ |
| 2026-06-24c | 06-24 | [云服务安全策略最优选择](https://blog.csdn.net/banxia_frontend/article/details/162642626) | 200 | 暴力枚举 | ⬜ |
| 2026-06-22a | 06-22 | [日志关键词统计](https://blog.csdn.net/banxia_frontend/article/details/162213000) | 100 | 模拟计算 | ⬜ |
| 2026-06-22b | 06-22 | [预测新能源发电量](https://blog.csdn.net/banxia_frontend/article/details/162213502) | 100 | 模拟计算 | ⬜ |
| 2026-06-22c | 06-22 | [数据包分段传输的最小最大延迟](https://blog.csdn.net/banxia_frontend/article/details/162213618) | 200 | 二分查找 | ⬜ |
| 2026-06-17a | 06-14 | [字符串格式调整](https://blog.csdn.net/banxia_frontend/article/details/162077705) | 100 | 字符串 | ⬜ |
| 2026-06-17b | 06-14 | [进制转换后自定义排序](https://blog.csdn.net/banxia_frontend/article/details/162077769) | 100 | 字符串排序 | ⬜ |
| 2026-06-17c | 06-14 | [分析电网负载均衡](https://blog.csdn.net/banxia_frontend/article/details/162077934) | 200 | 并查集 | ⬜ |
| 2026-06-10a | 06-10 | [设备重排SN](https://blog.csdn.net/banxia_frontend/article/details/162047000) | 100 | 模拟 | ⬜ |
| 2026-06-10b | 06-10 | [查找温度记录统计信息](https://blog.csdn.net/banxia_frontend/article/details/162077476) | 100 | 滑动窗口 | ⬜ |
| 2026-06-10c | 06-10 | [双系统资源类型调配](https://blog.csdn.net/banxia_frontend/article/details/162077589) | 200 | 字符串 | ⬜ |
| 2026-06-07a | 06-07 | [网络数据包收发处理](https://blog.csdn.net/banxia_frontend/article/details/162045891) | 100 | 队列模拟 | ⬜ |
| 2026-06-07b | 06-07 | [内网IP有效性校验](https://blog.csdn.net/banxia_frontend/article/details/162046190) | 100 | 筛选排序 | ⬜ |
| 2026-06-07c | 06-07 | [最佳任务统筹](https://blog.csdn.net/banxia_frontend/article/details/162046789) | 200 | 状态压缩 DP | ⬜ |
| 2026-06-03a | 06-03 | [统计盈利目标区间](https://blog.csdn.net/banxia_frontend/article/details/161697391) | 100 | 前缀和 | ⬜ |
| 2026-06-03b | 06-03 | [返回所有加载的AGENTS.md文件ID列表](https://blog.csdn.net/banxia_frontend/article/details/161697510) | 100 | DFS | ⬜ |
| 2026-06-03c | 06-03 | [资源二分类隔离判定](https://blog.csdn.net/banxia_frontend/article/details/161697795) | 200 | 二分图 | ⬜ |

### 5 月（共 7 场 21 题）

| # | 日期 | 题目 | 分值 | 考点 | 状态 |
|:---:|:---:|:---|:---:|:---|:---:|
| 2026-05-30a | 05-30 | [链表数字游戏](https://blog.csdn.net/banxia_frontend/article/details/161602621) | 100 | 模拟计算 | ⬜ |
| 2026-05-30b | 05-30 | [企业内部部门的最大层级](https://blog.csdn.net/banxia_frontend/article/details/161602691) | 100 | BFS | ⬜ |
| 2026-05-30c | 05-30 | [魔法阵的能量收集](https://blog.csdn.net/banxia_frontend/article/details/161602739) | 200 | 前缀和 | ⬜ |
| 2026-05-27a | 05-27 | [小学生班长选举](https://blog.csdn.net/banxia_frontend/article/details/161602256) | 100 | 统计 | ⬜ |
| 2026-05-27b | 05-27 | [Skill执行链完整性检测](https://blog.csdn.net/banxia_frontend/article/details/161602310) | 100 | 动态规划 | ⬜ |
| 2026-05-27c | 05-27 | [充电桩最优布局规划](https://blog.csdn.net/banxia_frontend/article/details/161602438) | 200 | 动态规划 | ⬜ |
| 2026-05-24a | 05-24 | [简单表达式运算](https://blog.csdn.net/banxia_frontend/article/details/161495339) | 100 | 模拟计算 | ⬜ |
| 2026-05-24b | 05-24 | [最小请求间隔限流策略](https://blog.csdn.net/banxia_frontend/article/details/161601625) | 100 | 枚举 | ⬜ |
| 2026-05-24c | 05-24 | [优化充电桩调度算法](https://blog.csdn.net/banxia_frontend/article/details/161602069) | 200 | 模拟计算 | ⬜ |
| 2026-05-20a | 05-20 | [小学英语老师批改作文](https://blog.csdn.net/banxia_frontend/article/details/161462589) | 100 | 滑动窗口 | ⬜ |
| 2026-05-20b | 05-20 | [等距二进制判断](https://blog.csdn.net/banxia_frontend/article/details/161463074) | 100 | 逻辑题 | ⬜ |
| 2026-05-20c | 05-20 | [多模型版本的最优调度](https://blog.csdn.net/banxia_frontend/article/details/161463326) | 200 | 动态规划 | ⬜ |
| 2026-05-17a | 05-17 | [IP地址分类识别](https://blog.csdn.net/banxia_frontend/article/details/161235313) | 100 | 模拟计算 | ⬜ |
| 2026-05-17b | 05-17 | [麻将基本胡牌型判断](https://blog.csdn.net/banxia_frontend/article/details/161235546) | 100 | 递归 | ⬜ |
| 2026-05-17c | 05-17 | [输出二叉树后序遍历结果](https://blog.csdn.net/banxia_frontend/article/details/161495148) | 200 | 二叉树 | ⬜ |
| 2026-05-13a | 05-13 | [查找能被整除的最大整数](https://blog.csdn.net/banxia_frontend/article/details/161263852) | 100 | 模拟计算 | ⬜ |
| 2026-05-13b | 05-13 | [数据包优先级窗口查找](https://blog.csdn.net/banxia_frontend/article/details/161264025) | 100 | 单调栈 | ⬜ |
| 2026-05-13c | 05-13 | [社交网络相同爱好好友查询](https://blog.csdn.net/banxia_frontend/article/details/161494588) | 200 | — | ⬜ |
| 2026-05-10a | 05-10 | [美观的灯笼](https://blog.csdn.net/banxia_frontend/article/details/161264779) | 100 | 模拟计算 | ⬜ |
| 2026-05-10b | 05-10 | [循环内存存取计算](https://blog.csdn.net/banxia_frontend/article/details/161463544) | 100 | 模拟计算 | ⬜ |
| 2026-05-10c | 05-10 | [寻找孤立水站](https://blog.csdn.net/banxia_frontend/article/details/161463745) | 200 | BFS | ⬜ |
| 2026-05-06a | 05-06 | [匹配命令行前缀关键字](https://blog.csdn.net/banxia_frontend/article/details/161026914) | 100 | 模拟计算 | ⬜ |
| 2026-05-06b | 05-06 | [物流仓库货物调货优化](https://blog.csdn.net/banxia_frontend/article/details/161091412) | 100 | 并查集 | ⬜ |
| 2026-05-06c | 05-06 | [寻找重复子数据](https://blog.csdn.net/banxia_frontend/article/details/161092402) | 200 | DFS | ⬜ |

### 4 月（共 7 场 21 题，新系统首月）

| # | 日期 | 题目 | 分值 | 考点 | 状态 |
|:---:|:---:|:---|:---:|:---|:---:|
| 2026-04-29a | 04-29 | [操作历史管理器的撤销/重做能力](https://blog.csdn.net/banxia_frontend/article/details/160995076) | 100 | 模拟计算 | ⬜ |
| 2026-04-29b | 04-29 | [日志文件异常检测](https://blog.csdn.net/banxia_frontend/article/details/161025603) | 100 | 排序 | ⬜ |
| 2026-04-29c | 04-29 | [获取大写字母瓷砖拼出独特图案数量](https://hydro.ac/d/hwod/p/596) | 200 | DFS | ⬜ |
| 2026-04-26a | 04-26 | [端口流量统计](https://blog.csdn.net/banxia_frontend/article/details/160569223) | 100 | 单调栈 | ⬜ |
| 2026-04-26b | 04-26 | [最大化游戏试玩资格分发](https://blog.csdn.net/banxia_frontend/article/details/160601860) | 100 | 贪心算法 | ⬜ |
| 2026-04-26c | 04-26 | [项目模块依赖构建顺序规划](https://blog.csdn.net/banxia_frontend/article/details/160602542) | 200 | DFS | ⬜ |
| 2026-04-22a | 04-22 | [文档特征提取](https://blog.csdn.net/banxia_frontend/article/details/160450406) | 100 | 频率统计 | ⬜ |
| 2026-04-22b | 04-22 | [计费时段计算](https://blog.csdn.net/banxia_frontend/article/details/160451120) | 100 | 模拟计算 | ⬜ |
| 2026-04-22c | 04-22 | [小学生班长选举增强版](https://blog.csdn.net/banxia_frontend/article/details/160451541) | 200 | 模拟计算 | ⬜ |
| 2026-04-19a | 04-19 | [8位LED控制器](https://blog.csdn.net/banxia_frontend/article/details/160382843) | 100 | 模拟 | ⬜ |
| 2026-04-19b | 04-19 | [分辨率排序](https://blog.csdn.net/banxia_frontend/article/details/160382971) | 100 | 排序 | ⬜ |
| 2026-04-19c | 04-19 | [WIFI设备网络规划](https://blog.csdn.net/banxia_frontend/article/details/160383120) | 200 | DFS | ⬜ |
| 2026-04-15a | 04-15 | [失灵的键盘](https://blog.csdn.net/banxia_frontend/article/details/160347306) | 100 | 排序 | ⬜ |
| 2026-04-15b | 04-15 | [API请求日志去重分析](https://blog.csdn.net/banxia_frontend/article/details/160347824) | 100 | 双指针 | ⬜ |
| 2026-04-15c | 04-15 | [小猫钓鱼纸牌游戏](https://blog.csdn.net/banxia_frontend/article/details/160348490) | 200 | 模拟计算 | ⬜ |
| 2026-04-08a | 04-08 | [准备生日礼物](https://blog.csdn.net/banxia_frontend/article/details/160348543) | 100 | 模拟计算 | ⬜ |
| 2026-04-08b | 04-08 | [配置操作失败数量统计](https://blog.csdn.net/banxia_frontend/article/details/160382054) | 100 | 模拟计算 | ⬜ |
| 2026-04-08c | 04-08 | [直捣黄龙](https://blog.csdn.net/banxia_frontend/article/details/160382706) | 200 | BFS | ⬜ |
| 2026-04-01a | 04-01 | [空间占用计算](https://blog.csdn.net/banxia_frontend/article/details/160384087) | 100 | 模拟计算 | ⬜ |
| 2026-04-01b | 04-01 | [计算数列位置N的值](https://blog.csdn.net/banxia_frontend/article/details/160383323) | 100 | 模拟计算 | ⬜ |
| 2026-04-01c | 04-01 | [勇攀数字高峰](https://blog.csdn.net/banxia_frontend/article/details/160383979) | 200 | DFS | ⬜ |

> 状态图例：⬜ 待整理 · 🟡 整理中 · ✅ 已完成

## 双机位 C 卷题库（独立索引）

> 双机位 C 卷为 2025 年起沿用卷次，与 2026 新系统并行使用。完整目录见 [CSDN 题库目录页](https://blog.csdn.net/banxia_frontend/article/details/160346662)，下表仅登记 100 分题（44 题），200/300 分题目录在来源页被截断，待后续补全。

| # | 题目 | 考点 | 来源 |
|:---:|:---|:---|:---|
| C-100-01 | [螺旋数字矩阵](https://blog.csdn.net/banxia_frontend/article/details/148458505) | 模拟计算 | [OJ](https://hydro.ac/d/hwod/p/423) |
| C-100-02 | [运维日志排序](https://blog.csdn.net/banxia_frontend/article/details/142746741) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/85) |
| C-100-03 | [评委评分](https://blog.csdn.net/banxia_frontend/article/details/148747693) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/129) |
| C-100-04 | [查找接口成功率最优时间段](https://blog.csdn.net/banxia_frontend/article/details/153651927) | 前缀和 | [OJ](https://hydro.ac/d/hwod/p/411) |
| C-100-05 | [补种未成活胡杨](https://blog.csdn.net/banxia_frontend/article/details/141814723) | 双指针 | [OJ](https://hydro.ac/d/hwod/p/356) |
| C-100-06 | [整数编码](https://blog.csdn.net/banxia_frontend/article/details/130816655) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/4) |
| C-100-07 | [构成正方形的数量](https://blog.csdn.net/banxia_frontend/article/details/130172005) | 数学 | [OJ](https://hydro.ac/d/hwod/p/360) |
| C-100-08 | [最长的顺子](https://blog.csdn.net/banxia_frontend/article/details/129793342) | 逻辑分析 | [OJ](https://hydro.ac/d/hwod/p/127) |
| C-100-09 | [比赛的冠亚季军](https://blog.csdn.net/banxia_frontend/article/details/129288857) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/198) |
| C-100-10 | [手机App防沉迷系统](https://blog.csdn.net/banxia_frontend/article/details/141832127) | 排序比较 | [OJ](https://hydro.ac/d/hwod/p/359) |
| C-100-11 | [流量波峰](https://blog.csdn.net/banxia_frontend/article/details/155285312) | 线性搜索 | [OJ](https://hydro.ac/d/hwod/p/529) |
| C-100-12 | [敏感字段加密](https://blog.csdn.net/banxia_frontend/article/details/130042378) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/350) |
| C-100-13 | [斗地主之顺子](https://blog.csdn.net/banxia_frontend/article/details/141298145) | 数据结构/栈 | [OJ](https://hydro.ac/d/hwod/p/469) |
| C-100-14 | [异常的打卡记录](https://blog.csdn.net/banxia_frontend/article/details/129347985) | 逻辑分析 | [OJ](https://hydro.ac/d/hwod/p/500) |
| C-100-15 | [统计射击比赛成绩](https://blog.csdn.net/banxia_frontend/article/details/129998971) | 排序 | [OJ](https://hydro.ac/d/hwod/p/99) |
| C-100-16 | [热点网站统计](https://blog.csdn.net/banxia_frontend/article/details/142468225) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/424) |
| C-100-17 | [国际移动用户识别码](https://blog.csdn.net/banxia_frontend/article/details/149488289) | 模拟计算 | [OJ](https://hydro.ac/d/hwod/p/515) |
| C-100-18 | [恢复数字序列](https://blog.csdn.net/banxia_frontend/article/details/148124590) | 滑动窗口 | [OJ](https://hydro.ac/d/hwod/p/418) |
| C-100-19 | [流水线](https://blog.csdn.net/banxia_frontend/article/details/156027568) | 逻辑分析 | [OJ](https://hydro.ac/d/hwod/p/96) |
| C-100-20 | [机器人活动区域](https://blog.csdn.net/banxia_frontend/article/details/141534302) | 数据结构 | [OJ](https://hydro.ac/d/hwod/p/525) |
| C-100-21 | [猜数字](https://blog.csdn.net/banxia_frontend/article/details/130310017) | 逻辑分析 | [OJ](https://hydro.ac/d/hwod/p/470) |
| C-100-22 | [查找单入口空闲区域](https://blog.csdn.net/banxia_frontend/article/details/129475856) | DFS | [OJ](https://hydro.ac/d/hwod/p/499) |
| C-100-23 | [小华地图寻宝](https://blog.csdn.net/banxia_frontend/article/details/134901578) | DFS | [OJ](https://hydro.ac/d/hwod/p/268) |
| C-100-24 | [贪心的商人](https://blog.csdn.net/banxia_frontend/article/details/141390711) | 贪心思维 | [OJ](https://hydro.ac/d/hwod/p/345) |
| C-100-25 | [打印机队列](https://blog.csdn.net/banxia_frontend/article/details/146452918) | 优先队列 | [OJ](https://hydro.ac/d/hwod/p/204) |
| C-100-26 | [货币单位换算](https://blog.csdn.net/banxia_frontend/article/details/156543825) | 逻辑分析 | [OJ](https://hydro.ac/d/hwod/p/412) |
| C-100-27 | [小明减肥](https://blog.csdn.net/banxia_frontend/article/details/156544063) | 动态规划 | [OJ](https://hydro.ac/d/hwod/p/496) |
| C-100-28 | [微服务的集成测试](https://blog.csdn.net/banxia_frontend/article/details/156576657) | 分治递归 | [OJ](https://hydro.ac/d/hwod/p/478) |
| C-100-29 | [完美走位](https://blog.csdn.net/banxia_frontend/article/details/156576750) | 滑动窗口 | [OJ](https://hydro.ac/d/hwod/p/170) |
| C-100-30 | [高矮个子排队](https://blog.csdn.net/banxia_frontend/article/details/156576790) | 滑动窗口 | [OJ](https://hydro.ac/d/hwod/p/366) |
| C-100-31 | [最佳信号覆盖问题](https://blog.csdn.net/banxia_frontend/article/details/156578061) | 模拟 | [OJ](https://hydro.ac/d/hwod/p/534) |
| C-100-32 | [精准核酸检测](https://blog.csdn.net/banxia_frontend/article/details/156577033) | DFS | [OJ](https://hydro.ac/d/hwod/p/275) |
| C-100-33 | [字符串计数匹配](https://blog.csdn.net/banxia_frontend/article/details/156577064) | 滑动窗口 | [OJ](https://hydro.ac/d/hwod/p/535) |
| C-100-34 | [挑选宝石](https://blog.csdn.net/banxia_frontend/article/details/156577664) | 暴力 | [OJ](https://hydro.ac/d/hwod/p/536) |
| C-100-35 | [开心消消乐](https://blog.csdn.net/banxia_frontend/article/details/156577242) | BFS/并查集 | [OJ](https://hydro.ac/d/hwod/p/443) |
| C-100-36 | [风险投资计划](https://blog.csdn.net/banxia_frontend/article/details/156577818) | 贪心 | [OJ](https://hydro.ac/d/hwod/p/537) |
| C-100-37 | [压缩日志查询](https://blog.csdn.net/banxia_frontend/article/details/156577343) | 模拟 | [OJ](https://hydro.ac/d/hwod/p/533) |
| C-100-38 | [停车场收入统计](https://blog.csdn.net/banxia_frontend/article/details/156617094) | — | [OJ](https://hydro.ac/d/hwod/p/538) |
| C-100-39 | [寻找密码](https://blog.csdn.net/banxia_frontend/article/details/156577476) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/156) |
| C-100-40 | [网上商城优惠活动](https://blog.csdn.net/banxia_frontend/article/details/156577844) | 模拟 | [OJ](https://hydro.ac/d/hwod/p/513) |
| C-100-41 | [分苹果](https://blog.csdn.net/banxia_frontend/article/details/156577915) | 位运算 | [OJ](https://hydro.ac/d/hwod/p/C0E19) |
| C-100-42 | [池化资源共享](https://blog.csdn.net/banxia_frontend/article/details/156577969) | 模拟 | [OJ](https://hydro.ac/d/hwod/p/510) |
| C-100-43 | [矩阵扩散](https://blog.csdn.net/banxia_frontend/article/details/156578032) | 图论/多源BFS | [OJ](https://hydro.ac/d/hwod/p/527) |
| C-100-44 | [字符串摘要](https://blog.csdn.net/banxia_frontend/article/details/156578044) | 字符串 | [OJ](https://hydro.ac/d/hwod/p/114) |

> ⚠️ 双机位 C 卷 200 分、300 分题在 CSDN 目录页被截断（页面 30KB 限制），待后续单独抓取补全。

## 考点分布统计（基于 4-8 月新系统真题）

| 考点 | 100 分题出现次数 | 200 分题出现次数 | 备考优先级 |
|:---|:---:|:---:|:---:|
| 模拟计算 | 25+ | 5+ | ⭐⭐⭐⭐⭐ |
| DFS / 回溯 | 3 | 9+ | ⭐⭐⭐⭐⭐ |
| BFS | 2 | 6+ | ⭐⭐⭐⭐ |
| 动态规划 | 2 | 4+ | ⭐⭐⭐⭐ |
| 并查集 | 3 | 2 | ⭐⭐⭐⭐ |
| 双指针 / 滑动窗口 | 5 | 0 | ⭐⭐⭐ |
| 单调栈 | 3 | 0 | ⭐⭐⭐ |
| 排序 | 4 | 0 | ⭐⭐⭐ |
| 哈希 / 集合 | 4 | 0 | ⭐⭐⭐ |
| 二分查找 / 二分答案 | 0 | 2 | ⭐⭐ |
| 贪心 | 2 | 0 | ⭐⭐ |
| 字符串 | 3 | 1 | ⭐⭐⭐ |
| 二分图 | 0 | 1 | ⭐⭐ |
| 树 | 0 | 2 | ⭐⭐ |
| 前缀和 | 1 | 1 | ⭐⭐ |
| 状态压缩 DP | 0 | 1 | ⭐⭐ |

> **备考建议**：100 分题**模拟计算**占绝对主流（25+ 题），务必熟练模板；200 分题**DFS/BFS** 是绝对主力（15+ 题），其次**动态规划**。优先刷最近 2 个月真题命中率最高。

## 使用说明

- 新增一题：参考 [../2025年/_模板.md](../2025年/_模板.md)，重命名为 `2026-MM. 题目名.md`（按月编号，同月多题加 a/b/c 后缀），填写各段落后在对应月份表登记。
- 难度图例：🟢 简单（≈100 分）· 🟡 中等（≈200 分）· 🔴 困难（≈300 分）。
- 标签沿用主目录常见分类：数组、字符串、图（BFS/DFS/并查集/拓扑排序）、贪心、动态规划、二分答案、树、栈/队列、哈希、模拟等。
- 整理顺序建议：**8 月 → 7 月 → 6 月** 倒序整理，最新题目命中率最高。

## 备考重点（沿用主目录经验）

1. **100 分题必拿**（字符串、哈希计数、模拟、排序模板题），零失误保底。
2. **200 分题决胜**：图论（并查集/BFS/拓扑）、区间贪心、二分答案、树形 DP、状态压缩 DP 是高频拉分点。
3. **溢出优先想 `long`**：累加/乘积/前缀和是最隐蔽的失分点。
4. **边界与空输入**：空数组、`n=1`、`k=0`、目标不存在——写完先补判空。
5. 新系统（力扣模式）**先看函数签名**再动手，别写 `Scanner`。
6. **2026 新系统双机位 C 卷** 监考严格，每场出新题，优先刷最近 3 个月真题提分效率最高。

## 待补充清单

- [x] 2026 年 9 月新题（9.2 / 9.6 共 6 题已整理完成）
- [ ] 双机位 C 卷 200 分、300 分题（目录页被截断，需单独抓取补全）
- [ ] 4-6 月新系统真题正文整理（7 月 27 题、8 月 27 题、9 月 6 题已全部整理完成）

## 相关

- 主目录题库与机考经验：[../README.md](../README.md)
- 机考注意事项：[../0.OD机考注意事项.md](../0.OD机考注意事项.md)
- 2025 年真题目录：[../2025年/README.md](../2025年/README.md)
- CSDN 题库来源：[2026最新华为OD机试新系统卷 + 双机位C卷 真题题库目录](https://blog.csdn.net/banxia_frontend/article/details/160346662)
