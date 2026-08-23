package org.example.basic.graph.adjList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 12:11
 */
public class GraphAdjList {

    private List<List<Integer>> adj;

    private int vertices;


    public GraphAdjList(int vertices) {
        this.vertices = vertices;
        this.adj = new ArrayList<>();
        for (int i =0; i < vertices; i++) {
            adj.add(new ArrayList<>());
        }
    }

    // 添加有向边 u -> v
    public void addDirectedEdge(int u, int v) {
        adj.get(u).add(v);
    }

    // 添加无向边 u - v
    public void addUndirectedEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    // 打印邻接表
    public void printGraph() {
        for (int i = 0; i < vertices; i++) {
            System.out.print("顶点 " + i + " 的邻居: ");
            for (int neighbor : adj.get(i)) {
                System.out.print(neighbor + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        GraphAdjList graph = new GraphAdjList(4);
        graph.addUndirectedEdge(0, 1);
        graph.addUndirectedEdge(0, 2);
        graph.addUndirectedEdge(1, 2);
        graph.addUndirectedEdge(2, 3);
        graph.printGraph();
        // 输出:
        // 顶点 0 的邻居: 1 2
        // 顶点 1 的邻居: 0 2
        // 顶点 2 的邻居: 0 1 3
        // 顶点 3 的邻居: 2
    }
 }



    
