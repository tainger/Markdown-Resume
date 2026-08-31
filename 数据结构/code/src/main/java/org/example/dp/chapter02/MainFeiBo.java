package org.example.dp.chapter02;

import java.util.Arrays;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 16:30
 */
public class MainFeiBo {

    public static void main(String[] args) {

        int i = new MainFeiBo().feiBo(2);
        int i2 = new MainFeiBo().feiBo2(2);
        int i22 = new MainFeiBo().feiBo3(2);
        int i222 = new MainFeiBo().feiBo4(2);
        System.out.println(i == i2);
        System.out.println(i2 == i22);
        System.out.println(i222 == i22);

        int i3 = new MainFeiBo().feiBo(12);
        int i4 = new MainFeiBo().feiBo2(12);
        int i44 = new MainFeiBo().feiBo3(12);
        int i444 = new MainFeiBo().feiBo4(12);
        System.out.println(i3 == i4);
        System.out.println(i44 == i4);
        System.out.println(i44 == i444);

        int i5 = new MainFeiBo().feiBo(15);
        int i6 = new MainFeiBo().feiBo2(15);
        int i66 = new MainFeiBo().feiBo3(15);
        int i666 = new MainFeiBo().feiBo4(15);
        System.out.println(i5 == i6);
        System.out.println(i5 == i66);
        System.out.println(i5 == i666);
    }

    public int feiBo(int n) {

        if (n == 0) {
            return 0;
        }

        if (n == 1) {
            return 1;
        }

        return feiBo(n - 2) + feiBo(n - 1);

    }

    public int feiBo2(int n) {
        if (n == 0) {
            return 0;
        }

        if (n == 1) {
            return 1;
        }
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        memo[0] = 0;
        memo[1] = 1;

        return feiBoMemo(n, memo);
    }

    private int feiBoMemo(int n, int[] memo) {

        if (memo[n] != -1) {
            return memo[n];
        }

        int res = feiBoMemo(n - 1, memo) + feiBoMemo(n - 2, memo);
        memo[n] = res;
        return res;
    }

    public int feiBo3(int n) {
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        int[] dp = new int[n + 1];
        dp[0] = 0;
        dp[1] = 1;
        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        return dp[n];
    }


    public int feiBo4(int n) {
        int last1 = 0;
        int last2 = 1;
        int cur = 0;
        for (int i = 2; i <= n; i++) {
            cur= last1  + last2;
            last1 = last2;
            last2 = cur;
        }
        return cur;
    }
}



    
