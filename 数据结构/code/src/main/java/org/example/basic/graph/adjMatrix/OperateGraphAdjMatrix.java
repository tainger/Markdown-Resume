package org.example.basic.graph.adjMatrix;

import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 11:52
 */
public class OperateGraphAdjMatrix {

    private int[][] adj;
    private int vertices;

    public OperateGraphAdjMatrix(int vertices) {
        this.vertices = vertices;
        this.adj = new int[vertices][vertices];
        // 无权图默认 0
    }

    // ---------- 添加边 ----------
    public void addEdge(int u, int v) { addEdge(u, v, 1); }
    public void addEdge(int u, int v, int weight) {
        adj[u][v] = weight;
    }
    public void addUndirectedEdge(int u, int v, int weight) {
        adj[u][v] = weight;
        adj[v][u] = weight;
    }
    // ---------- 查询 ----------
    public boolean hasEdge(int u, int v) {
        return adj[u][v] != 0;
    }
    public int getWeight(int u, int v) {
        return adj[u][v];
    }

    // ---------- 遍历邻居 ----------
    public List<Integer> getNeighbors(int u) {
        List<Integer> neighbors = new ArrayList<>();
        for (int v = 0; v < vertices; v++) {
            if (adj[u][v] != 0) {
                neighbors.add(v);
            }
        }
        return neighbors;
    }

    // ---------- 求顶点度数（无向图） ----------
    public int getDegree(int u) {
        int deg = 0;
        for (int v = 0; v < vertices; v++) {
            if (adj[u][v] != 0) deg++;
        }
        return deg;
    }

    // ---------- BFS 遍历 ----------
    public void bfs(int start) {
        boolean[] visited = new boolean[vertices];
        Queue<Integer> queue = new LinkedList<>();
        visited[start] = true;
        queue.offer(start);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            System.out.print(u + " ");
            for (int v = 0; v < vertices; v++) {
                if (adj[u][v] != 0 && !visited[v]) {
                    visited[v] = true;
                    queue.offer(v);
                }
            }
        }
        System.out.println();
    }

    // ---------- DFS 遍历 ----------
    public void dfs(int start) {
        boolean[] visited = new boolean[vertices];
        dfsRecursive(start, visited);
        System.out.println();
    }
    private void dfsRecursive(int u, boolean[] visited) {
        visited[u] = true;
        System.out.print(u + " ");
        for (int v = 0; v < vertices; v++) {
            if (adj[u][v] != 0 && !visited[v]) {
                dfsRecursive(v, visited);
            }
        }
    }

    public void printMatrix() {
        for (int[] row : adj) {
            System.out.println(Arrays.toString(row));
        }
    }


    public static void main(String[] args) {
        OperateGraphAdjMatrix graph = new OperateGraphAdjMatrix(5);
        graph.addUndirectedEdge(0, 1, 1);
        graph.addUndirectedEdge(0, 2, 1);
        graph.addUndirectedEdge(1, 3, 1);
        graph.addUndirectedEdge(2, 4, 1);

        System.out.println("graph is: ");
        graph.printMatrix();


        System.out.println("graph detail: ");
        graph.printMatrix();


        System.out.print("BFS from 0: ");
        graph.bfs(0); // 0 1 2 3 4

        System.out.print("DFS from 0: ");
        graph.dfs(0); // 0 1 3 2 4

        System.out.println("Neighbors of 0: " + graph.getNeighbors(0)); // [1, 2]
        System.out.println("Degree of 0: " + graph.getDegree(0));       // 2
    }


}



    
