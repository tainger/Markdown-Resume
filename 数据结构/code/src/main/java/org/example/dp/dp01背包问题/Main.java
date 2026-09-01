package org.example.dp.dp01背包问题;

/**
 * @author jiazhiyuan
 * @date 2026/9/1 12:26
 */
public class Main {


    private int maxSum;

    public static void main(String[] args) {
        int w = 4;
        int[] weights = {1, 3, 4};
        int[] values = {15, 20, 30};
        int knapsack = new Main().knapsack2(weights, values, 4);
        System.out.println(knapsack);
    }

    public int knapsack(int[] weight, int[] value, int w) {
        dfs(weight, value, w, 0, 0);
        return maxSum;
    }

    /**
     * dp = {
     * 0 < i < weight ,dp[i] = {dp[i], dp[i-weight[j]] + value[j]}
     * }
     *
     * @param weight
     * @param value
     * @param w
     * @return
     */
    public int knapsack2(int[] weight, int[] value, int w) {
        //
        int[] dp = new int[w + 1];
        //初始化状态
        for (int j = 0; j <= w; j++) {
            for (int i = 0; i < weight.length; i++) {
                int wItem = weight[i];
                int vItem = value[i];
                if (j >= wItem) {
                    dp[j] = Math.max(dp[j], dp[j - wItem] + vItem);
                }
            }
        }
        return dp[w];
    }


    /**
     * 一维dp
     * @param W
     * @param weights
     * @param values
     * @return
     */
    public static int knapsackExactly(int W, int[] weights, int[] values) {
        int n = weights.length;
        // 初始化为 -∞，表示不可达
        int[] dp = new int[W + 1];
        for (int i = 1; i <= W; i++) {
            dp[i] = Integer.MIN_VALUE;
        }
        dp[0] = 0;  // 容量0恰好装满

        for (int i = 0; i < n; i++) {
            int w = weights[i];
            int v = values[i];
            for (int j = W; j >= w; j--) {
                if (dp[j - w] != Integer.MIN_VALUE) {
                    dp[j] = Math.max(dp[j], dp[j - w] + v);
                }
            }
        }

        return dp[W] == Integer.MIN_VALUE ? -1 : dp[W];
    }


    /**
     * dfs 暴力
     * @param weight
     * @param value
     * @param rest
     * @param index
     * @param sum
     */
    private void dfs(int[] weight, int[] value, int rest, int index, int sum) {
        if (index > weight.length - 1) {
            maxSum = Math.max(sum, maxSum);
            return;
        }
        int next = index + 1;

        dfs(weight, value, rest, next, sum);
        if (rest - weight[index] >= 0) {
            sum = sum + value[index];
            dfs(weight, value, rest - weight[index], next, sum);
        }
    }

}



    
