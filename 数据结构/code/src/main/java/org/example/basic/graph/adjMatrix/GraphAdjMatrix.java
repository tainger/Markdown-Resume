package org.example.basic.graph.adjMatrix;

import java.util.Arrays;

/**
 * 邻接矩阵建图
 * 无权图
 * @author jiazhiyuan
 * @date 2026/8/23 11:38
 */
public class GraphAdjMatrix {

    private int[][] adj;

    private int vertices;

    public GraphAdjMatrix(int vertices) {
        this.vertices = vertices;
        this.adj = new int[vertices][vertices];
    }

    // 添加有向边 u -> v
    public void addDirectedEdge(int u, int v) {
        adj[u][v] = 1;
    }

    // 添加无向边 u - v
    public void addUndirectedEdge(int u, int v) {
        adj[u][v] = 1;
        adj[v][u] = 1;
    }

    // 打印邻接矩阵
    public void printMatrix() {
        for (int[] row : adj) {
            System.out.println(Arrays.toString(row));
        }
    }

    // 测试
    public static void main(String[] args) {
        GraphAdjMatrix graph = new GraphAdjMatrix(4);
        // 添加无向边: 0-1, 0-2, 1-2, 2-3
        graph.addUndirectedEdge(0, 1);
        graph.addUndirectedEdge(0, 2);
        graph.addUndirectedEdge(1, 2);
        graph.addUndirectedEdge(2, 3);
        graph.printMatrix();
        // 输出:
        // [0, 1, 1, 0]
        // [1, 0, 1, 0]
        // [1, 1, 0, 1]
        // [0, 0, 1, 0]
    }


}



    
