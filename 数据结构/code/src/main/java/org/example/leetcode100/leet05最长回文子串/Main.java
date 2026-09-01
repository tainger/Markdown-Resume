package org.example.leetcode100.leet05最长回文子串;

import java.util.Arrays;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 11:58
 */
public class Main {

    int  maxlen = 1;

    int start = 0;
    public static void main(String[] args) {

        String s = "abcbabad";

        String s1 = new Main().longestPalindrome3(s);

        System.out.println(s1);


    }

    /**
     * 暴力求解
     * @param s
     * @return
     */
    public String longestPalindrome2(String s) {

        for(int i = 0;i < s.length(); i++) {
            for(int j  = i + 1; j < s.length(); j++) {
                if(isPal(s, i, j) && j - i + 1 > maxlen) {
                    maxlen = j-i + 1;
                    start = i;
                }
            }
        }
        return s.substring(start, start + maxlen);
    }

    private boolean isPal(String s, int i, int j) {
        //终止条件
        if(j  - i < 3) {
            return s.charAt(i) == s.charAt(j);
        }
        return s.charAt(i) == s.charAt(j) && isPal(s, i+1, j-1);
    }


    /**
     * 由暴力递归推出状态方程
     *
     *
     * 由暴力退出状态转移方程
     *
     * dp[i][j] = {
     *     i = j,      dp[i][j] =true
     *     j = i + 1,  dp[i][j] =true
     *     j- i >= 3,            dp[i[j] = dp[i+1][j-1] && s.charAt(i) == s.charAt(j)
     * }
     * @param s
     * @return
     */
    public String longestPalindrome3(String s) {

        //初始化哨兵
        int n = s.length();
        boolean [][] dp = new boolean[n][n];
        int maxLen = 1;
        int start = 0;


        //长度1
        for(int i = 0; i < n ; i++) {
            dp[i][i] = true;
        }

        //长度2
        for(int i = 0; i < n-1 ; i++) {
            if(s.charAt(i) == s.charAt(i+1)) {
                dp[i][i+1] = true;
            }
        }

        // 长度≥3
        for (int len = 3; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j) && dp[i+1][j-1]) {
                    dp[i][j] = true;

                    System.out.println(Arrays.toString(dp));
                    if (len > maxLen) {
                        maxLen = len;
                        start = i;
                    }
                }
            }
        }
        return s.substring(start, start + maxLen);
    }

        /***
             * 自己手搓的双指针版本
             * @param s
             * @return
             */
    public String longestPalindrome(String s) {
        char[] charArray = s.toCharArray();
        int n = charArray.length - 1;

        for (int i = 0; i < charArray.length; i++) {
             int j =  i -1;
             int k =  i + 1;
             int len = 1;
             while (j >= 0 && k <= n) {
                 if (charArray[j] == charArray[k]) {
                     maxlen = Math.max(maxlen, len + 2);
                     start = j;
                     j--;
                     k++;
                 }else {
                     break;
                 }
             }

        }
        return s.substring(start, maxlen+1);
    }
}



    
