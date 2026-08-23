package org.example.graph.adjList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 12:18
 */
public class WeightedGraphAdjList {

    // 边对象（也可用 int[] 或 Pair）
    static class Edge {
        int to;
        int weight;
        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
        @Override
        public String toString() {
            return "(" + to + ", " + weight + ")";
        }
    }

    private List<List<Edge>> adj;
    private int vertices;

    public WeightedGraphAdjList(int vertices) {
        this.vertices = vertices;
        this.adj = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adj.add(new ArrayList<>());
        }
    }

    public void addDirectedEdge(int u, int v, int w) {
        adj.get(u).add(new Edge(v, w));
    }

    public void addUndirectedEdge(int u, int v, int w) {
        adj.get(u).add(new Edge(v, w));
        adj.get(v).add(new Edge(u, w));
    }


    public void printGraph() {
        for (int i = 0; i < vertices; i++) {
            System.out.print("顶点 " + i + " 的邻居(权重): ");
            for (Edge e : adj.get(i)) {
                System.out.print(e + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        WeightedGraphAdjList graph = new WeightedGraphAdjList(4);
        graph.addUndirectedEdge(0, 1, 5);
        graph.addUndirectedEdge(0, 2, 3);
        graph.addUndirectedEdge(1, 2, 2);
        graph.addUndirectedEdge(2, 3, 4);
        graph.printGraph();
        // 输出:
        // 顶点 0 的邻居(权重): (1, 5) (2, 3)
        // 顶点 1 的邻居(权重): (0, 5) (2, 2)
        // 顶点 2 的邻居(权重): (0, 3) (1, 2) (3, 4)
        // 顶点 3 的邻居(权重): (2, 4)
    }
}



    
