package org.example.od.SQL记录拆分;

import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/24 14:24
 *
 * # 20. SQL记录拆分（2026 新系统）
 *
 * > **难度**：🟡 中等 | **分值**：100 | **卷次**：新系统 2026-07（7.1/7.5 场次） | **标签**：并查集、贪心 | **考点**：传递约束合并 + 装箱
 *
 * ## 题目描述
 *
 * 某分布式数据库系统需要将 SQL 操作日志拆分到多个文件中。给定单个文件可保存的最大 SQL 语句行数 `split_line`，以及 SQL 语句数组 `sql_text`，请按规则将 SQL 语句拆分到不同文件中，返回**拆分后的文件数量**。
 *
 * 拆分规则：
 *
 * 1. **整行不可拆**：单个数组成员的 SQL 语句（可能含多条，按 `;` 分隔）必须作为同一行完整保存在同一文件中，不可跨文件拆分；
 * 2. **事务绑定**：部分语句带事务标签 `[Tn]`（n 为正整数），如 `[T1]A;`，**相同事务标签的语句必须保存于同一文件中**；
 * 3. **顺序贪心**：按语句组**首次出现的行号顺序**处理，优先放入当前文件，放入后超出 `split_line` 限制则新建文件；
 * 4. **约束传递**：若 A 与 B 必须同文件、B 与 C 必须同文件，则 A、B、C 必须同一文件；约束合并后语句组计数**即便超过限制也必须放在同一文件**；
 * 5. SQL 数组为空时返回 **0**。
 *
 * **补充**：
 *
 * - `split_line` 取值范围 [1, 10000]，无效值返回 0；
 * - `sql_text` 每行最多 1000 字符，总语句最多 100000 条，事务标签范围 [1, 1000]；
 * - 最后一条语句可以没有分号结尾；空语句（仅含分号）按 1 行计算。
 *
 * > 新系统（力扣模式）：核心为实现拆分逻辑；输入格式以实际题目为准。
 */
public class Main {

    static class DSU {
        int[] parent;
        DSU (int n) {
            parent = new int[n];
        }

        int find(int  x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];  // 路径压缩
                x = parent[x];
            }
            return x;
        }

        void union(int a, int b) { parent[find(a)] = find(b); }
    }


    /**
     * 输入：
     * split_line = 2
     * sql_text = ["[T1]A;", "[T2]B;", "[T1]C;", "[T2]D;"]
     * 输出：
     * 2
     * @param splitLine
     * @param sqlText
     * @return
     */
    public int splitLogs(int splitLine, String[] sqlText) {
        if(splitLine < 1 || splitLine > 10000){
            return 0;
        }
        int n = sqlText.length;

        if(n==0) {
            return 0;
        }

        DSU dsu = new DSU(n);

        //tag
        // tag → 该 tag 涉及的行号集合
        Map<Integer, Integer> tagFirstRow = new HashMap<>();

        for (int i = 0; i < n; i++) {
            String line = sqlText[i];
            // 用正则提取所有 [Tn] 标签（也可手动扫描）
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("\\[T(\\d+)\\]").matcher(line);
            while (m.find()) {
                int tag = Integer.parseInt(m.group(1));
                if (tagFirstRow.containsKey(tag)) {
                    // 同 tag 的行必须同文件 → union
                    dsu.union(tagFirstRow.get(tag), i);
                } else {
                    tagFirstRow.put(tag, i);
                }
            }
        }

        // 分组: 根 → 组信息(大小, 最小行号)
        Map<Integer, int[]> groups = new HashMap<>(); // root -> {size, firstRow}
        for (int i = 0; i < n; i++) {
            int root = dsu.find(i);
            int[] g = groups.get(root);
            if (g == null) groups.put(root, new int[]{1, i});
            else { g[0]++; g[1] = Math.min(g[1], i); }
        }

        // 按首次出现行号排序
        List<int[]> list = new ArrayList<>(groups.values());
        list.sort((a, b) -> a[1] - b[1]);

        // 贪心装箱
        int files = 0, used = 0;
        for (int[] g : list) {
            if (g[0] > splitLine) {            // 组本身超限也必须同文件
                files++;                       // 独占一个文件
                used = 0;
                continue;
            }
            if (used + g[0] > splitLine) {     // 放不下 → 新开文件
                files++;
                used = g[0];
            } else {
                if (used == 0) files++;        // 首次使用也要算一个文件
                used += g[0];
            }
        }
        return files;
    }


    public static void main(String[] args) {
        Main sol = new Main();

        // ============ 边界 / 典型用例（含预期值，供实现后自测） ============
        // 用法：实现 splitLogs 后运行，PASS 表示与预期一致

        // --- 一、空 / 无效输入 ---
        // case1: 空数组 → 0（规则5）
        check("空数组", 0, sol.splitLogs(3, new String[]{}));
        // case2: splitLine = 0，无效值 → 0（下界外）
        check("splitLine=0 无效", 0, sol.splitLogs(0, new String[]{"[T1]A;"}));
        // case3: splitLine = 10001，无效值 → 0（上界外）
        check("splitLine 越上界", 0, sol.splitLogs(10001, new String[]{"[T1]A;"}));
        // case4: splitLine = 1，合法下界；单行 → 1
        check("最小单行", 1, sol.splitLogs(1, new String[]{"A;"}));

        // --- 二、题目原始示例 ---
        // case5: 示例1，一行带 T1、T2 两个 tag，行0 把 T1/T2 粘合，再和行1(T1) 传递合并 → 1 组共 2 行 ≤ 3 → 1
        check("示例1 一行多tag传递", 1,
                sol.splitLogs(3, new String[]{"[T1]A;[T2]B;", "[T1]C;"}));
        // case6: 示例2，T1={0,2} T2={1,3} 两独立组各 2 行，装箱(上限2) → 2
        check("示例2 交错事务", 2,
                sol.splitLogs(2, new String[]{"[T1]A;", "[T2]B;", "[T1]C;", "[T2]D;"}));

        // --- 三、无事务标签（纯装箱）---
        // case7: 5 行无标签，每行独立 1 行，上限 2 → ceil(5/2)=3
        check("无标签装箱", 3,
                sol.splitLogs(2, new String[]{"A;", "B;", "C;", "D;", "E;"}));
        // case8: 恰好装满一个文件，上限 = 行数 → 1
        check("恰好装满", 1,
                sol.splitLogs(3, new String[]{"A;", "B;", "C;"}));

        // --- 四、单组超限（规则4：超限也必须同文件，独占）---
        // case9: T1 有 2 行但上限=1，组大小 > 上限 → 独占 1 个文件
        check("单组超限独占", 1,
                sol.splitLogs(1, new String[]{"[T1]A;", "[T1]B;"}));
        // case10: 超限组独占后，后续小组另起文件 → 超限组(1) + 剩余2行按上限1 → 1 + 2 = 3
        check("超限组+后续", 3,
                sol.splitLogs(1, new String[]{"[T1]A;", "[T1]B;", "C;", "D;"}));

        // --- 五、传递约束的连锁合并 ---
        // case11: 行0 含 T1、行1 含 T1+T2、行2 含 T2 → 三行经 T1、T2 链式传递并入同组，共3行 ≤ 5 → 1
        check("链式传递合并", 1,
                sol.splitLogs(5, new String[]{"[T1]A;", "[T1]B;[T2]C;", "[T2]D;"}));
        // case12: 同上但上限=2，合并组共 3 行 > 2 → 独占 1 个文件
        check("链式合并超限", 1,
                sol.splitLogs(2, new String[]{"[T1]A;", "[T1]B;[T2]C;", "[T2]D;"}));

        // --- 六、混合：事务组 + 散行，按首次行号顺序装箱 ---
        // case13: 行0[T1] 行1散 行2[T1] 行3散，上限3
        //   T1组={0,2}大小2 首次行号0；散行1大小1；散行3大小1
        //   顺序(按首次行号): T1组(2) → 文件1剩1；散行1(1) → 文件1满(3/3)；散行3(1) → 文件2
        //   → 2
        check("混合装箱", 2,
                sol.splitLogs(3, new String[]{"[T1]A;", "B;", "[T1]C;", "D;"}));
        // case14: 空语句（仅含分号）按 1 行计算，上限1 → 每个占一行 → 2
        check("空语句计1行", 2,
                sol.splitLogs(1, new String[]{";", ";"}));
    }

    /** 简易断言：打印每个用例的期望 / 实际 / 是否通过 */
    private static void check(String name, int expected, int actual) {
        String flag = expected == actual ? "PASS" : "FAIL";
        System.out.printf("[%s] %-16s expected=%d actual=%d%n", flag, name, expected, actual);
    }





}
