package org.example.graph.adjMatrix;

import java.util.Arrays;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 11:43
 */
public class WeightedGraphAdjMatrix {
    private int[][] adj;   // 邻接矩阵，存储权重
    private int vertices;  // 顶点数
    private final int INF = Integer.MAX_VALUE; // 用极大值表示无边

    public WeightedGraphAdjMatrix(int vertices) {
        this.vertices = vertices;
        this.adj = new int[vertices][vertices];
        // 初始化：所有边为 INF（无边）
        for (int[] row : adj) {
            Arrays.fill(row, INF);
        }
        // 对角线设为 0（自己到自己距离为0）
        for (int i = 0; i < vertices; i++) {
            adj[i][i] = 0;
        }
    }

    // 添加有向带权边 u -> v，权重 w
    public void addDirectedEdge(int u, int v, int w) {
        adj[u][v] = w;
    }

    // 添加无向带权边 u - v，权重 w
    public void addUndirectedEdge(int u, int v, int w) {
        adj[u][v] = w;
        adj[v][u] = w;
    }

    // 打印邻接矩阵
    public void printMatrix() {
        for (int[] row : adj) {
            for (int val : row) {
                System.out.print((val == INF ? "INF" : val) + "\t");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        WeightedGraphAdjMatrix graph = new WeightedGraphAdjMatrix(4);
        graph.addUndirectedEdge(0, 1, 5);
        graph.addUndirectedEdge(0, 2, 3);
        graph.addUndirectedEdge(1, 2, 2);
        graph.addUndirectedEdge(2, 3, 4);
        graph.printMatrix();
        // 输出:
        // 0    5    3    INF
        // 5    0    2    INF
        // 3    2    0    4
        // INF  INF  4    0
    }
}
