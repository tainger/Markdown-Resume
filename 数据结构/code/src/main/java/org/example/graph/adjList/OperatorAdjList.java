package org.example.graph.adjList;

import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 12:20
 */
public class OperatorAdjList {

    private List<List<Integer>> adj;
    private int vertices;

    public OperatorAdjList(int vertices) {
        this.vertices = vertices;
        this.adj = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adj.add(new ArrayList<>());
        }
    }

    // ---------- 添加边 ----------
    public void addDirectedEdge(int u, int v) {
        adj.get(u).add(v);
    }

    public void addUndirectedEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    // ---------- 查询 ----------
    public boolean hasEdge(int u, int v) {
        return adj.get(u).contains(v);
    }

    public List<Integer> getNeighbors(int u) {
        return new ArrayList<>(adj.get(u));
    }

    public int getDegree(int u) {
        return adj.get(u).size();
    }

    // ---------- BFS（广度优先遍历） ----------
    public List<Integer> bfs(int start) {
        List<Integer> order = new ArrayList<>();
        boolean[] visited = new boolean[vertices];
        Queue<Integer> queue = new LinkedList<>();
        visited[start] = true;
        queue.offer(start);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            for (int v : adj.get(u)) {
                if (!visited[v]) {
                    visited[v] = true;
                    queue.offer(v);
                }
            }
        }
        return order;
    }

    // ---------- DFS（深度优先遍历） ----------
    public List<Integer> dfs(int start) {
        List<Integer> order = new ArrayList<>();
        boolean[] visited = new boolean[vertices];
        dfsRecursive(start, visited, order);
        return order;
    }

    private void dfsRecursive(int u, boolean[] visited, List<Integer> order) {
        visited[u] = true;
        order.add(u);
        for (int v : adj.get(u)) {
            if (!visited[v]) {
                dfsRecursive(v, visited, order);
            }
        }
    }

    // ---------- 拓扑排序（Kahn 算法，仅限 DAG） ----------
    public List<Integer> topologicalSort() {
        int[] inDegree = new int[vertices];
        for (int u = 0; u < vertices; u++) {
            for (int v : adj.get(u)) {
                inDegree[v]++;
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < vertices; i++) {
            if (inDegree[i] == 0) queue.offer(i);
        }

        List<Integer> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            result.add(u);
            for (int v : adj.get(u)) {
                if (--inDegree[v] == 0) {
                    queue.offer(v);
                }
            }
        }
        return result.size() == vertices ? result : new ArrayList<>(); // 存在环则返回空
    }

    // ---------- 检测是否有环（DFS） ----------
    public boolean hasCycle() {
        boolean[] visited = new boolean[vertices];
        boolean[] recursionStack = new boolean[vertices];
        for (int i = 0; i < vertices; i++) {
            if (hasCycleDFS(i, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleDFS(int u, boolean[] visited, boolean[] recursionStack) {
        if (recursionStack[u]) return true;
        if (visited[u]) return false;

        visited[u] = true;
        recursionStack[u] = true;

        for (int v : adj.get(u)) {
            if (hasCycleDFS(v, visited, recursionStack)) {
                return true;
            }
        }
        recursionStack[u] = false;
        return false;
    }

    public void printMatrix() {
        for (int i = 0; i < adj.size(); i++) {
            System.out.print("[");
            List<Integer> row = adj.get(i);
            for (int j = 0; j < row.size(); j++) {
                System.out.print(row.get(j));
                if (j < row.size() - 1) System.out.print(", ");
            }
            System.out.println("]");
        }
    }

    public static void main(String[] args) {
        OperatorAdjList graph = new OperatorAdjList(5);
        graph.addUndirectedEdge(0, 1);
        graph.addUndirectedEdge(0, 2);
        graph.addUndirectedEdge(1, 3);
        graph.addUndirectedEdge(2, 4);

        System.out.println("graph is: ");   // [0, 1, 2, 3, 4]
        graph.printMatrix();;

        System.out.println("BFS from 0: " + graph.bfs(0));   // [0, 1, 2, 3, 4]
        System.out.println("DFS from 0: " + graph.dfs(0));   // [0, 1, 3, 2, 4]
        System.out.println("Neighbors of 0: " + graph.getNeighbors(0)); // [1, 2]
        System.out.println("Degree of 0: " + graph.getDegree(0));       // 2

        // 拓扑排序示例（有向无环图）
        OperatorAdjList dag = new OperatorAdjList(4);
        dag.addDirectedEdge(0, 1);
        dag.addDirectedEdge(0, 2);
        dag.addDirectedEdge(1, 3);
        dag.addDirectedEdge(2, 3);
        System.out.println("拓扑排序: " + dag.topologicalSort()); // [0, 1, 2, 3] 或 [0, 2, 1, 3]
        System.out.println("是否有环: " + dag.hasCycle()); // false
    }


}



    
