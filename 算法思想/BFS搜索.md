# BFS 搜索（广度优先搜索）

> 原文件名 `BFS搜素.md` 为错别字，已更正为 `BFS搜索.md`。与 [DFS搜索.md](DFS搜索.md) 成对食用。

BFS 的本质一句话：**像水波纹一样一层层向外扩散，第一次碰到目标时，走过的层数就是最短距离**。前提：**每步代价相同（无权）**。

---

## 一、核心思想：层序扩展

```
起点 S → 第 1 层（S 的邻居）→ 第 2 层（邻居的邻居）→ ……

层号 = 从起点出发的最少步数
      ┌───┬───┬───┐
      │ 1 │ 2 │ 3 │      数字 = BFS 层数
      ├───┼───┼───┤      从 S 出发按圈扩散，
      │ 2 │ S │ 2 │      谁先被染到，谁的距离就确定
      ├───┼───┼───┤      且一旦确定不再改变
      │ 3 │ 2 │ 3 │
      └───┴───┴───┘
```

**为什么 BFS 能求无权最短路**：队列保证「第 k 层的节点全部处理完才开始第 k+1 层」，所以第一次到达某节点时必然走了最少步数。这是 BFS 与 DFS 最本质的分界——**DFS 找到的是一条路，BFS 找到的是最短的那条**。

---

## 二、标准模板（网格 + 图两种形态）

```java
// ============ 形态一：网格 BFS（最常见）============
// 例：从左上角走到右下角的最少步数，1 可走 0 是墙
static int[] dx = {0, 0, 1, -1};          // 四方向偏移量数组（背下来）
static int[] dy = {1, -1, 0, 0};

static int bfs(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    boolean[][] visited = new boolean[m][n];
    Deque<int[]> queue = new ArrayDeque<>();
    queue.offer(new int[]{0, 0});
    visited[0][0] = true;                  // ★ 入队时就标记，不是出队时！
    int step = 0;                          // 当前层号 = 当前步数
    while (!queue.isEmpty()) {
        int size = queue.size();           // ★ 固定本层元素个数，再逐个出队
        for (int s = 0; s < size; s++) {
            int[] cur = queue.poll();
            if (cur[0] == m - 1 && cur[1] == n - 1) return step; // 到达终点
            for (int d = 0; d < 4; d++) {  // 枚举四个方向
                int nx = cur[0] + dx[d], ny = cur[1] + dy[d];
                if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue; // 越界
                if (visited[nx][ny] || grid[nx][ny] == 0) continue;   // 访问过/墙
                visited[nx][ny] = true;    // ★ 标记入队，防重复入队
                queue.offer(new int[]{nx, ny});
            }
        }
        step++;                            // 本层处理完，层数 +1
    }
    return -1;                             // 队列空还没到 → 不可达
}

// ============ 形态二：图 BFS（邻接表）============
static int graphBfs(List<List<Integer>> adj, int start, int target) {
    boolean[] visited = new boolean[adj.size()];
    Deque<Integer> queue = new ArrayDeque<>();
    int[] dist = new int[adj.size()];      // dist 数组顺便记录距离
    queue.offer(start);
    visited[start] = true;
    while (!queue.isEmpty()) {
        int cur = queue.poll();
        if (cur == target) return dist[cur];
        for (int next : adj.get(cur)) {    // 遍历所有出边
            if (visited[next]) continue;
            visited[next] = true;
            dist[next] = dist[cur] + 1;    // 距离 = 上一层 + 1
            queue.offer(next);
        }
    }
    return -1;
}
```

复杂度：每个节点入队一次、每条边看一次 → **网格 \(O(m \times n)\)，图 \(O(V + E)\)**，空间同阶。

---

## 三、四个高频变体

| 变体 | 改动点 | 适用 |
|:---|:---|:---|
| **多源 BFS** | 初始队列里放入**所有**源点，一起扩散 | 01 矩阵求每点到最近 0 的距离、腐烂橘子 |
| **0-1 BFS** | 边权只有 0/1，用**双端队列**：0 权加队首、1 权加队尾 | 换个方向代价不同的图 |
| **双向 BFS** | 起点 + 终点同时扩散，相遇即停 | 已知起点终点、分支因子大（单词接龙） |
| **分层 BFS / 状态 BFS** | visited 状态加维度：`[位置][剩余资源]` | 带钥匙走迷宫、带油量开车 |

> 多源 BFS 的关键认知：**多个源点等价于一个「虚拟超级源点」**，首层同时入队后，每格第一次被染到的层号就是「到最近源点的距离」——把 n 次单源 BFS 优化成 1 次。

---

## 四、BFS vs DFS 怎么选（必考分界）

| 信号 | 选择 | 原因 |
|:---|:---|:---|
| 求**最少步数 / 最短路径**（无权） | **BFS** | 层数即距离，第一次到达即最优 |
| 求**是否存在路径 / 连通块个数** | 都行 | 结果只看可达性，不看路径形态 |
| 求**所有具体路径/方案** | **DFS** | BFS 存整层状态太占内存 |
| 树的**层序遍历 / 按层处理** | **BFS** | 队列天然分层 |
| 树的深度/路径和等自底向上计算 | **DFS** | 后序位置才能拿到子树信息 |

> 图论全景（拓扑、最短路、最小生成树）见 [图论专题.md](图论专题.md)；本篇只聚焦 BFS 思想本身。

---

## 五、易错点 ⚠️

| 易错点 | 澄清 |
|:---|:---|
| **出队时才标记 visited** | 同一节点会被多个邻居重复入队 → 队列爆炸；必须**入队时标记** |
| **忘了按层截断（size 循环）** | 不按层出队就数不出步数；`int size = queue.size()` 先取固定值，出队中队列还在变 |
| **层数从 0 还是 1 起算混乱** | 统一约定：起点层数 0，`step++` 放在本层 for 结束后；样例手工验一遍 |
| ** visited 放在 while 外新建** | 多组用例/多轮调用复用数组导致脏数据；每轮新建或重置 |
| **多源 BFS 只放一个源点** | 应把**所有**源点一次性入队，再开始扩散 |
| **边权不为 1 还用普通 BFS** | 只有**无权图**第一次到达才最短；带权请 Dijkstra（或 0-1 BFS） |

---

## 六、一句话总结

BFS = **队列 + visited + 按层扩散**：无权图/网格里求「最少步数」的第一反应；起点到终点、单源变多源、状态加维度，都是「同一套模板换初始队列和 visited 定义」——记住「**入队即标记、按层出队数步数**」两条铁律，模板题就不会翻车。

---

## 七、相关笔记

| 主题 | 笔记 |
|:---|:---|
| DFS（成对记忆） | [DFS搜索.md](DFS搜索.md) |
| 拓扑排序/Kruskal/Dijkstra 全景 | [图论专题.md](图论专题.md) |
| 岛屿数量（BFS/DFS 都可解） | [../leetcode-hot100/200. 岛屿数量.md](../leetcode-hot100/200.%20岛屿数量.md) |
| 课程表（拓扑 = BFS 家族） | [../leetcode-hot100/207. 课程表.md](../leetcode-hot100/207.%20课程表.md) |
| 网格 DP（BFS 的静态版兄弟） | [1.动态规划.md](1.动态规划.md) |
