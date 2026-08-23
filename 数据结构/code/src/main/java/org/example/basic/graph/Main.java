package org.example.graph;

import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 10:54
 */
public class Main {


    public static void main(String[] args) {

        int n = 5;
        int[] vals = {5, 3, 8, 3, 7};
        int[][] edges = {{0, 1}, {0, 2}, {1, 3}, {1, 4}};
        int k = 2;


        
        int[] ints = kthLevel(n, vals, edges, k);
        System.out.println(ints);


    }

    private static int[] kthLevel(int n, int[] vals, int[][] edges, int k) {
        if (n <= 0 || k < 0) return new int[0];

        // 1. 建有向邻接表
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (int[] e : edges) {
            if (e[0] >= 0 && e[0] < n && e[1] >= 0 && e[1] < n) {
                graph.get(e[0]).add(e[1]);
            }
        }
        // 2. BFS 逐层下探，直到第 k 层或队列耗尽
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(0);
        int level = 0;
        while (!queue.isEmpty() && level < k) {
            int size = queue.size();          // 锁定当前整层
            for (int i = 0; i < size; i++) {
                int cur = queue.poll();
                for (int nxt : graph.get(cur)) queue.offer(nxt);
            }
            level++;
        }

        // 3. 没到第 k 层（树没那么深）→ 空数组
        if (level != k) return new int[0];

        // 4. 队列里就是第 k 层节点，取值去重升序
        TreeSet<Integer> set = new TreeSet<>();
        while (!queue.isEmpty()) set.add(vals[queue.poll()]);

        int[] ans = new int[set.size()];
        int idx = 0;
        for (int v : set) ans[idx++] = v;
        return new int[]{1, 2,3};
    }
}



    
