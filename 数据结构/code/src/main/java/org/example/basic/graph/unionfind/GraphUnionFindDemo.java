package org.example.basic.graph.unionfind;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 18:37
 */
import java.util.*;

/**
 * 邻接表图 + 并查集 (Union-Find) 综合演示
 * 功能：构建图、使用并查集查找连通分量、按集合分组输出
 */
public class GraphUnionFindDemo {

    // ========== 并查集内部类 ==========
    static class UnionFind {
        private int[] parent;
        private int[] rank;
        private int count;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            count = n;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // 路径压缩
            }
            return parent[x];
        }

        public boolean union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX == rootY) return false;

            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
            count--;
            return true;
        }

        public boolean connected(int x, int y) {
            return find(x) == find(y);
        }

        public int getCount() {
            return count;
        }

        public int[] getParent() {
            return parent;
        }

        // 获取每个元素的根节点
        public int[] getRoots() {
            int[] roots = new int[parent.length];
            for (int i = 0; i < parent.length; i++) {
                roots[i] = find(i);
            }
            return roots;
        }
    }

    // ========== 图数据结构 ==========
    private int vertexCount;          // 顶点数
    private List<List<Integer>> adjList;  // 邻接表
    private Map<Integer, String> vertexNames; // 顶点名称映射（可选）

    public GraphUnionFindDemo(int vertexCount) {
        this.vertexCount = vertexCount;
        adjList = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            adjList.add(new ArrayList<>());
        }
        vertexNames = new HashMap<>();
        // 默认顶点名称为 A, B, C, ...
        for (int i = 0; i < vertexCount; i++) {
            vertexNames.put(i, String.valueOf((char)('A' + i)));
        }
    }

    // 添加无向边
    public void addEdge(int u, int v) {
        validateVertex(u);
        validateVertex(v);
        adjList.get(u).add(v);
        adjList.get(v).add(u); // 无向图
    }

    // 添加有向边
    public void addDirectedEdge(int u, int v) {
        validateVertex(u);
        validateVertex(v);
        adjList.get(u).add(v);
    }

    private void validateVertex(int v) {
        if (v < 0 || v >= vertexCount) {
            throw new IllegalArgumentException("顶点 " + v + " 超出范围 [0, " + (vertexCount-1) + "]");
        }
    }

    // 设置顶点名称
    public void setVertexName(int v, String name) {
        validateVertex(v);
        vertexNames.put(v, name);
    }

    // 获取顶点名称
    public String getVertexName(int v) {
        return vertexNames.getOrDefault(v, String.valueOf(v));
    }

    // 打印邻接表
    public void printGraph() {
        System.out.println("===== 邻接表图 =====");
        for (int i = 0; i < vertexCount; i++) {
            System.out.print("  " + getVertexName(i) + " -> ");
            if (adjList.get(i).isEmpty()) {
                System.out.println("(无邻接点)");
            } else {
                List<String> neighbors = new ArrayList<>();
                for (int neighbor : adjList.get(i)) {
                    neighbors.add(getVertexName(neighbor));
                }
                System.out.println(String.join(", ", neighbors));
            }
        }
        System.out.println();
    }

    // ========== 核心方法：使用并查集分析图的连通性 ==========
    public UnionFind analyzeConnectivity() {
        UnionFind uf = new UnionFind(vertexCount);

        // 遍历所有边，合并端点
        for (int u = 0; u < vertexCount; u++) {
            for (int v : adjList.get(u)) {
                // 只合并 u < v 的边避免重复（无向图）
                if (u < v) {
                    uf.union(u, v);
                }
            }
        }
        return uf;
    }

    // 按连通分量分组
    public Map<Integer, List<Integer>> groupByComponent(UnionFind uf) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < vertexCount; i++) {
            int root = uf.find(i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(i);
        }
        return groups;
    }

    // 打印连通分量分析结果
    public void printComponents(UnionFind uf) {
        System.out.println("===== 连通分量分析 =====");
        System.out.println("  顶点数: " + vertexCount);
        System.out.println("  连通分量数: " + uf.getCount());
        System.out.println("  父节点数组: " + Arrays.toString(uf.getParent()));

        Map<Integer, List<Integer>> groups = groupByComponent(uf);
        System.out.println("  分量分组:");
        int compId = 1;
        for (Map.Entry<Integer, List<Integer>> entry : groups.entrySet()) {
            List<String> names = new ArrayList<>();
            for (int v : entry.getValue()) {
                names.add(getVertexName(v));
            }
            System.out.println("    分量 " + compId++ + " (根=" + getVertexName(entry.getKey()) + "): " + names);
        }
        System.out.println();
    }

    // 验证两个顶点是否连通
    public void checkConnection(UnionFind uf, int u, int v) {
        boolean connected = uf.connected(u, v);
        System.out.println("  " + getVertexName(u) + " 和 " + getVertexName(v) +
                (connected ? " ✅ 连通 (同一分量)" : " ❌ 不连通 (不同分量)"));
    }

    // ========== 演示主程序 ==========
    public static void main(String[] args) {
        System.out.println("===== 邻接表图 + 并查集 综合演示 =====\n");

        // ---------- 场景1: 简单图 ----------
        System.out.println("--- 场景1: 简单无向图 (6个顶点) ---");
        GraphUnionFindDemo graph1 = new GraphUnionFindDemo(6);
        // 添加边: A-B, B-C, D-E, E-F, C-D (形成大分量 A-B-C-D-E-F)
        graph1.addEdge(0, 1); // A-B
        graph1.addEdge(1, 2); // B-C
        graph1.addEdge(3, 4); // D-E
        graph1.addEdge(4, 5); // E-F
        graph1.addEdge(2, 3); // C-D (连接两个子图)
        graph1.printGraph();

        UnionFind uf1 = graph1.analyzeConnectivity();
        graph1.printComponents(uf1);
        graph1.checkConnection(uf1, 0, 5); // A-F
        graph1.checkConnection(uf1, 0, 3); // A-D

        System.out.println("\n--- 场景2: 有孤立顶点的图 (8个顶点) ---");
        GraphUnionFindDemo graph2 = new GraphUnionFindDemo(8);
        // 设置自定义名称
        graph2.setVertexName(0, "V0");
        graph2.setVertexName(1, "V1");
        graph2.setVertexName(2, "V2");
        graph2.setVertexName(3, "V3");
        graph2.setVertexName(4, "V4");
        graph2.setVertexName(5, "V5");
        graph2.setVertexName(6, "V6");
        graph2.setVertexName(7, "V7");

        // 添加边: V0-V1, V2-V3, V4-V5, V6孤立
        graph2.addEdge(0, 1);
        graph2.addEdge(2, 3);
        graph2.addEdge(4, 5);
        // V6 和 V7 是孤立点
        graph2.printGraph();

        UnionFind uf2 = graph2.analyzeConnectivity();
        graph2.printComponents(uf2);
        graph2.checkConnection(uf2, 0, 1); // V0-V1
        graph2.checkConnection(uf2, 0, 6); // V0-V6
        graph2.checkConnection(uf2, 6, 7); // V6-V7 (两个孤立点)

        System.out.println("\n--- 场景3: 复杂图 + 动态添加边 ---");
        GraphUnionFindDemo graph3 = new GraphUnionFindDemo(10);
        // 初始化名称
        for (int i = 0; i < 10; i++) {
            graph3.setVertexName(i, "N" + i);
        }
        // 初始边: 0-1, 2-3, 4-5, 6-7, 8-9
        int[][] initialEdges = {{0,1}, {2,3}, {4,5}, {6,7}, {8,9}};
        for (int[] edge : initialEdges) {
            graph3.addEdge(edge[0], edge[1]);
        }
        System.out.println("初始图:");
        graph3.printGraph();

        UnionFind uf3 = graph3.analyzeConnectivity();
        graph3.printComponents(uf3);

        System.out.println("--- 动态添加边 (连接分量) ---");
        // 添加跨分量边: 1-2, 5-6, 3-4
        int[][] newEdges = {{1,2}, {5,6}, {3,4}};
        for (int[] edge : newEdges) {
            System.out.println("  添加边: " + graph3.getVertexName(edge[0]) + "-" + graph3.getVertexName(edge[1]));
            graph3.addEdge(edge[0], edge[1]);
        }
        System.out.println("\n更新后的图:");
        graph3.printGraph();

        UnionFind uf3Updated = graph3.analyzeConnectivity();
        graph3.printComponents(uf3Updated);
        graph3.checkConnection(uf3Updated, 0, 4); // N0-N4 (应连通)
        graph3.checkConnection(uf3Updated, 0, 8); // N0-N8 (应不连通)

        System.out.println("\n--- 场景4: 自定义交互式输入 ---");
        runInteractiveDemo();
    }

    // ========== 交互式演示 ==========
    private static void runInteractiveDemo() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入顶点个数 (推荐 5~8): ");
        int n = scanner.nextInt();
        if (n < 2) n = 5;

        GraphUnionFindDemo graph = new GraphUnionFindDemo(n);
        // 设置名称
        for (int i = 0; i < n; i++) {
            graph.setVertexName(i, "V" + i);
        }

        System.out.println("请输入边 (格式: u v, 输入 -1 结束):");
        while (true) {
            System.out.print("  边 (u v): ");
            int u = scanner.nextInt();
            if (u == -1) break;
            int v = scanner.nextInt();
            try {
                graph.addEdge(u, v);
                System.out.println("  添加边: " + graph.getVertexName(u) + "-" + graph.getVertexName(v));
            } catch (IllegalArgumentException e) {
                System.out.println("  ❌ " + e.getMessage());
            }
        }

        graph.printGraph();
        UnionFind uf = graph.analyzeConnectivity();
        graph.printComponents(uf);

        // 查询连通性
        System.out.println("\n查询连通性 (输入 -1 退出):");
        while (true) {
            System.out.print("  查询 (u v): ");
            int u = scanner.nextInt();
            if (u == -1) break;
            int v = scanner.nextInt();
            try {
                graph.checkConnection(uf, u, v);
            } catch (IllegalArgumentException e) {
                System.out.println("  ❌ " + e.getMessage());
            }
        }
        scanner.close();
    }
}


    
