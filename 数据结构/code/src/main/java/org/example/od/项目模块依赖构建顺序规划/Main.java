package org.example.od.项目模块依赖构建顺序规划;

import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/23 14:55
 */
public class Main {

    List<String> ans = new ArrayList<>();

    //邻接表构图
    Map<String, List<String>> graph = new HashMap<>();
    Map<String, Integer> indegree = new HashMap<>();
    List<String> sorted;


    public static void main(String[] args) {


        //模块名称
        String[] modules = {"user", "auth", "database", "api"};
        //依赖关系
        String[][] deps = {{"user","auth"}, {"auth","database"}, {"api","database"}};

        List<String> strings = new Main().buildOrders(modules, deps);
        System.out.println(strings);

    }

    void dfs(List<String> chosen, int total) {
        if (chosen.size() == total) {
            ans.add(String.join(" ", chosen));
            return;
        }
        for (String m : sorted) {
            if (indegree.get(m) != 0) continue;      // 只选入度为 0
            if (chosen.contains(m)) continue;        // 未选过

            chosen.add(m);
            for (String next : graph.getOrDefault(m, List.of())) {
                indegree.put(next, indegree.get(next) - 1);
            }
            dfs(chosen, total);
            // 回溯恢复
            for (String next : graph.getOrDefault(m, List.of())) {
                indegree.put(next, indegree.get(next) + 1);
            }
            chosen.remove(chosen.size() - 1);
        }
    }

    public List<String> buildOrders(String[] modules, String[][] deps) {
        sorted = new ArrayList<>(Arrays.asList(modules));
        Collections.sort(sorted);                     // 先排序保证字典序
        for (String m : sorted) indegree.put(m, 0);

        for (String[] d : deps) {                     // d[0] 依赖 d[1] → d[1] 先
            graph.computeIfAbsent(d[1], k -> new ArrayList<>()).add(d[0]);
            indegree.put(d[0], indegree.getOrDefault(d[0], 0) + 1);
        }

        dfs(new ArrayList<>(), modules.length);
        return ans;                                   // 有环时 ans 为空 → []
    }
}



    
