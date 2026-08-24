package org.example.od.寻找孤立水站;


import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/24 07:54
 */
public class Main {

    public static void main(String[] args) {

        int n = 5;
        int[] sources = {1};
        int[][] pipes = {{1, 0, 0},{1, 2, 0}};



    }


    /**
     *
     * @param n
     * @param sources
     * @param pipes
     * @return
     */
    public List<Integer> findIsolated(int n, int[] sources, int[][] pipes) {

        List<List<Integer>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for(int i =0; i< pipes[0].length; i++) {
            int v = pipes[i][0];
            int e = pipes[i][1];
            int type = pipes[i][2];
            graph.get(v).add(e);

            if(type == 1) {
                graph.get(e).add(v);
            }
        }

        boolean[] visited = new boolean[n];

        Queue<Integer> queue = new ArrayDeque<>();

        //先把源头设置访问了

        for(int i = 0; i < sources.length; i++) {
            if(!visited[sources[i]]) {
                visited[sources[i]] =true;
                queue.offer(sources[i]);
            }
        }

        //开始bfs

        while(!queue.isEmpty()) {
            int u = queue.poll();
            for(int v : graph.get(u)) {
                if(!visited[v]) {
                    visited[v] =true;
                    queue.offer(v);
                }
            }
        }

        List<Integer> ans = new ArrayList<>();

        for (int i = 0; i < visited.length; i++) {
            if(! visited[i]) {
                ans.add(i);
            }
        }
        return  ans;
    }
}



    
