package org.example.basic.graph.unionfind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 19:04
 */
public class UnionFind {
    int[] parent;
    int[] rank; // 或 size
    int count;  // 连通分量数量

    UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        count = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
    }

    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // 路径压缩
        }
        return parent[x];
    }

    void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) return;

        // 按秩合并
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        count--; // 连通分量减1
    }

    boolean connected(int x, int y) {
        return find(x) == find(y);
    }

    // 方法1: 打印parent数组的详细信息
    void printParent() {
        System.out.print("Parent数组: [");
        for (int i = 0; i < parent.length; i++) {
            System.out.print(parent[i]);
            if (i < parent.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }

    // 方法2: 格式化打印，显示索引和对应的值
    void printParentWithIndex() {
        System.out.println("=== Parent数组详细信息 ===");
        System.out.print("索引:  ");
        for (int i = 0; i < parent.length; i++) {
            System.out.printf("%3d ", i);
        }
        System.out.println();
        System.out.print("值:    ");
        for (int i = 0; i < parent.length; i++) {
            System.out.printf("%3d ", parent[i]);
        }
        System.out.println();
    }

    // 方法3: 打印每个节点的根节点
    void printRoots() {
        System.out.println("=== 每个节点的根节点 ===");
        for (int i = 0; i < parent.length; i++) {
            int root = find(i);
            System.out.println("节点 " + i + " -> 根节点: " + root);
        }
    }

    // 方法4: 打印连通分量分组
    void printGroups() {
        System.out.println("=== 连通分量分组 ===");
        boolean[] visited = new boolean[parent.length];
        for (int i = 0; i < parent.length; i++) {
            if (!visited[i]) {
                int root = find(i);
                System.out.print("组 " + root + ": ");
                for (int j = i; j < parent.length; j++) {
                    if (!visited[j] && find(j) == root) {
                        System.out.print(j + " ");
                        visited[j] = true;
                    }
                }
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
//        unionFindDemo1();
//        System.out.println("\n" + "=".repeat(50) + "\n");
        unionFindDemo8();
    }

     // ========== 新增：最大不平衡度计算 ==========
    /**
     * Demo 8: 计算连通分量的最大不平衡度
     * 定义：不平衡度 = (最大值 - 最小值) * 节点数
     * 要求：节点数 >= 2 才有效
     * 
     * 应用场景：负载均衡、资源分配、网络分区评估等
     */
    public static void unionFindDemo8() {
        System.out.println("=== 计算连通分量最大不平衡度 ===");
        
        // 测试用例1: 多个连通分量
        System.out.println("\n--- 测试1: 多个连通分量 ---");
        int[] loads1 = {10, 20, 30, 40, 50};
        int[][] edges1 = {{0, 1}, {1, 2}, {3, 4}};
        int result1 = maxImbalance(loads1, edges1);
        System.out.println("最大不平衡度: " + result1);
        // 组0: {0,1,2} -> max=30,min=10,count=3 -> (30-10)*3=60
        // 组1: {3,4} -> max=50,min=40,count=2 -> (50-40)*2=20
        // 最大: 60

        // 测试用例2: 单节点（无效）
        System.out.println("\n--- 测试2: 单节点（无效） ---");
        int[] loads2 = {100};
        int[][] edges2 = {};
        int result2 = maxImbalance(loads2, edges2);
        System.out.println("最大不平衡度: " + result2); // -1

        // 测试用例3: 所有节点连通
        System.out.println("\n--- 测试3: 所有节点连通 ---");
        int[] loads3 = {5, 15, 25, 35};
        int[][] edges3 = {{0, 1}, {1, 2}, {2, 3}};
        int result3 = maxImbalance(loads3, edges3);
        System.out.println("最大不平衡度: " + result3);
        // max=35,min=5,count=4 -> (35-5)*4=120

        // 测试用例4: 复杂情况
        System.out.println("\n--- 测试4: 复杂情况 ---");
        int[] loads4 = {8, 3, 12, 6, 20, 15};
        int[][] edges4 = {{0, 1}, {1, 2}, {3, 4}, {4, 5}};
        int result4 = maxImbalance(loads4, edges4);
        System.out.println("最大不平衡度: " + result4);
        // 组0: {0,1,2} -> max=12,min=3,count=3 -> (12-3)*3=27
        // 组1: {3,4,5} -> max=20,min=6,count=3 -> (20-6)*3=42
        // 最大: 42
        
        // 测试用例5: 无连接（每个节点独立）
        System.out.println("\n--- 测试5: 无连接（每个节点独立） ---");
        int[] loads5 = {1, 5, 3, 9, 7};
        int[][] edges5 = {};
        int result5 = maxImbalance(loads5, edges5);
        System.out.println("最大不平衡度: " + result5); // -1 (因为没有分组达到2个节点)
    }

    /**
     * 计算最大不平衡度（基础版本）
     * @param loads 每个节点的负载值
     * @param edges 边的关系
     * @return 最大不平衡度，如果没有有效分组返回 -1
     */
    static int maxImbalance(int[] loads, int[][] edges) {
        int n = loads.length;
        UnionFind uf = new UnionFind(n);
        
        // 1. 建立连通关系
        for (int[] e : edges) {
            uf.union(e[0], e[1]);
        }
        
        // 2. 按根分组统计: root -> [max, min, count]
        Map<Integer, long[]> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            long[] g = groups.computeIfAbsent(root, 
                k -> new long[]{Long.MIN_VALUE, Long.MAX_VALUE, 0});
            g[0] = Math.max(g[0], loads[i]);  // max
            g[1] = Math.min(g[1], loads[i]);  // min
            g[2]++;                            // count
        }
        
        // 3. 打印分组信息（便于调试）
        System.out.println("分组统计:");
        for (Map.Entry<Integer, long[]> entry : groups.entrySet()) {
            int root = entry.getKey();
            long[] g = entry.getValue();
            long imbalance = (g[0] - g[1]) * g[2];
            System.out.printf("  组%d: max=%d, min=%d, count=%d, 不平衡度=%d%n",
                root, g[0], g[1], g[2], imbalance);
        }
        
        // 4. 计算最大不平衡度
        long ans = -1;
        for (long[] g : groups.values()) {
            if (g[2] < 2) continue;
            long imbalance = (g[0] - g[1]) * g[2];
            ans = Math.max(ans, imbalance);
        }
        return (int) ans;
    }

    /**
     * 增强版：计算最大不平衡度并返回详细信息
     */
    static int maxImbalanceWithDetails(int[] loads, int[][] edges) {
        int n = loads.length;
        UnionFind uf = new UnionFind(n);
        
        // 建立连通关系
        for (int[] e : edges) {
            uf.union(e[0], e[1]);
        }
        
        // 分组统计：[max, min, count, maxIndex, minIndex]
        Map<Integer, long[]> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            long[] g = groups.computeIfAbsent(root, 
                k -> new long[]{Long.MIN_VALUE, Long.MAX_VALUE, 0, -1, -1});
            
            if (loads[i] > g[0]) {
                g[0] = loads[i];
                g[3] = i; // 记录最大值索引
            }
            if (loads[i] < g[1]) {
                g[1] = loads[i];
                g[4] = i; // 记录最小值索引
            }
            g[2]++;
        }
        
        // 计算并找出最大不平衡度
        long maxImbalance = -1;
        int bestRoot = -1;
        System.out.println("分组详细信息:");
        for (Map.Entry<Integer, long[]> entry : groups.entrySet()) {
            int root = entry.getKey();
            long[] g = entry.getValue();
            if (g[2] < 2) {
                System.out.printf("  组%d: count=%d (节点数<2, 跳过)%n", root, g[2]);
                continue;
            }
            long imbalance = (g[0] - g[1]) * g[2];
            System.out.printf("  组%d: max=%d(节点%d), min=%d(节点%d), count=%d, 不平衡度=%d%n",
                root, g[0], g[3], g[1], g[4], g[2], imbalance);
            
            if (imbalance > maxImbalance) {
                maxImbalance = imbalance;
                bestRoot = root;
            }
        }
        
        if (bestRoot != -1) {
            System.out.printf(">>> 最大不平衡度: 组%d, 值=%d%n", bestRoot, maxImbalance);
        }
        return (int) maxImbalance;
    }


    public static void unionFindDemo7() {
        System.out.println("=== 路径压缩演示 ===");

        UnionFind uf = new UnionFind(10);

        // 构建一条链: 0-1-2-3-4
        for (int i = 0; i < 4; i++) {
            uf.union(i, i + 1);
        }

        System.out.println("查找前，节点4的父节点链:");
        printPath(uf, 4);

        // 执行查找，触发路径压缩
        int root = uf.find(4);
        System.out.println("根节点: " + root);

        System.out.println("查找后，节点4的父节点链(已压缩):");
        printPath(uf, 4);
    }

    static void printPath(UnionFind uf, int x) {
        int current = x;
        while (true) {
            System.out.print(current);
            int parent = uf.parent[current];
            if (parent == current) {
                System.out.println(" (根)");
                break;
            }
            System.out.print(" -> ");
            current = parent;
        }
    }

    public static void UnionFindDemo6() {
        System.out.println("=== 冗余连接 ===");
        int[][] edges = {{1, 2}, {2, 3}, {3, 4}, {1, 4}, {1, 5}};
        // 节点从1开始，需要n+1大小

        int[] result = findRedundantConnection(5, edges);
        System.out.println("多余的边: " + Arrays.toString(result)); // [1, 4]
    }

    static int[] findRedundantConnection(int n, int[][] edges) {
        UnionFind uf = new UnionFind(n + 1); // 节点从1开始
        for (int[] edge : edges) {
            if (uf.find(edge[0]) == uf.find(edge[1])) {
                return edge;
            }
            uf.union(edge[0], edge[1]);
        }
        return new int[0];
    }

    public static void unionFindDemo5() {
        System.out.println("=== 等式方程可满足性 ===");

        String[] equations1 = {"a==b", "b==c", "a==c"};
        System.out.println("equations1 是否可满足: " + equationsPossible(equations1)); // true

        String[] equations2 = {"a==b", "b!=c", "c==a"};
        System.out.println("equations2 是否可满足: " + equationsPossible(equations2)); // false
    }

    static boolean equationsPossible(String[] equations) {
        UnionFind uf = new UnionFind(26); // 26个小写字母

        // 处理所有 ==
        for (String eq : equations) {
            if (eq.charAt(1) == '=') {
                int x = eq.charAt(0) - 'a';
                int y = eq.charAt(3) - 'a';
                uf.union(x, y);
            }
        }

        // 检查所有 !=
        for (String eq : equations) {
            if (eq.charAt(1) == '!') {
                int x = eq.charAt(0) - 'a';
                int y = eq.charAt(3) - 'a';
                if (uf.find(x) == uf.find(y)) {
                    return false;
                }
            }
        }
        return true;
    }


    public static void unionFindDemo4() {
        System.out.println("=== 岛屿数量 ===");
        char[][] grid = {
                {'1', '1', '0', '0', '0'},
                {'1', '1', '0', '0', '0'},
                {'0', '0', '1', '0', '0'},
                {'0', '0', '0', '1', '1'}
        };

        System.out.println("岛屿数量: " + numIslands(grid)); // 3
    }

    static int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        int m = grid.length, n = grid[0].length;
        UnionFind uf = new UnionFind(m * n);

        int water = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '0') {
                    water++;
                    continue;
                }
                // 检查右方和下方
                if (i + 1 < m && grid[i + 1][j] == '1') {
                    uf.union(i * n + j, (i + 1) * n + j);
                    uf.printParent();
                    uf.printParentWithIndex();
                }
                if (j + 1 < n && grid[i][j + 1] == '1') {
                    uf.union(i * n + j, i * n + (j + 1));
                    uf.printParent();
                    uf.printParentWithIndex();
                }
            }
        }
        return uf.count - water;
    }

    /**
     * 检测是否有环
     */
    public static void unionFindDemo3() {
        System.out.println("=== 环检测 ===");

        // 无环图: 3个节点2条边
        int[][] edges1 = {{0, 1}, {1, 2}};
        System.out.println("图1是否有环: " + hasCycle(3, edges1)); // false

        // 有环图: 3个节点3条边
        int[][] edges2 = {{0, 1}, {1, 2}, {0, 2}};
        System.out.println("图2是否有环: " + hasCycle(3, edges2)); // true

    }

    static boolean hasCycle(int n, int[][] edges) {
        UnionFind uf = new UnionFind(n);
        for (int[] edge : edges) {
            if (uf.find(edge[0]) == uf.find(edge[1])) {
                return true; // 发现环
            }
            uf.union(edge[0], edge[1]);
        }
        return false;
    }

    /**
     * 图连通分量统计
     */
    public static void unionFindDemo2() {
        System.out.println("=== 图连通分量统计 ===");
        // 5个节点，3条边
        int n = 5;
        int[][] edges = {{0, 1}, {1, 2}, {3, 4}};

        UnionFind uf = new UnionFind(n);
        for (int[] edge : edges) {
            System.out.printf("\n执行操作: union(%s, %s)%n", edge[0], edge[1]);
            uf.union(edge[0], edge[1]);
            uf.printParent();
        }

        System.out.println("连通分量数: " + uf.count); // 2: {0,1,2} 和 {3,4}

        // 统计每个分量的节点
        System.out.println("各节点所属根:");
        for (int i = 0; i < n; i++) {
            System.out.println("节点" + i + " -> 根" + uf.find(i));
        }
    }

    /**
     * 基本操作
     */
    private static void unionFindDemo1() {
        System.out.println("=== 基础功能测试 ===");
        UnionFind uf = new UnionFind(10);

        System.out.println("初始连通分量数: " + uf.count); // 10
        System.out.println("初始状态:");
        uf.printParent();
        uf.printParentWithIndex();

        // 连接一些节点
        System.out.println("\n执行操作: union(0, 1)");
        uf.union(0, 1);
        uf.printParent();

        System.out.println("\n执行操作: union(2, 3)");
        uf.union(2, 3);
        uf.printParent();

        System.out.println("\n执行操作: union(1, 2) // 0-1-2-3 全部连通");
        uf.union(1, 2);
        uf.printParent();

        System.out.println("\n连接后连通分量数: " + uf.count); // 8 (0-3一组，4-9各自独立)

        System.out.println("\n0和3是否连通: " + uf.connected(0, 3)); // true
        System.out.println("0和4是否连通: " + uf.connected(0, 4)); // false

        // 查看父节点关系
        System.out.println("\n节点0的根: " + uf.find(0));
        System.out.println("节点3的根: " + uf.find(3));

        // 打印更详细的信息
        uf.printRoots();
        uf.printGroups();
    }


}