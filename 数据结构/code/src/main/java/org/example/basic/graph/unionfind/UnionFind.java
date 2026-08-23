package org.example.basic.graph.unionfind;

import java.util.Arrays;

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
        unionFindDemo7();
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