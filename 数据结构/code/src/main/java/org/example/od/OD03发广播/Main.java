package org.example.od.OD03发广播;

import java.util.HashSet;
import java.util.Set;

/**
 * @author jiazhiyuan
 * @date 2026/8/25 11:17
 */
public class Main {

    static int[] parent;

    static  int find(int x) {
        if(parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    static  void union(int a, int b) {
        int pa = find(a);
        int pb = find(b);
        if(pa != pb) {
            parent[pb] = pa;
        }
    }

    public int broadcast(int n, String[] grid) {

        parent = new int[n];


        for(int i = 0; i < n; i++) {
            for(int  j =0 ; j < n ;j ++) {

                if(grid[i].charAt(j) == '1') {
                    union(i,j);
                }
            }
        }

        Set<Integer> roots = new HashSet<>();

        for (int i = 0; i < n; i++) {
            roots.add(find(i));
        }

        return roots.size();
    }
    public static void main(String[] args) {


    }
}



    
