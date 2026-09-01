package org.example.leetcode100.lee300最长递增子序列;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/9/1 10:24
 */
public class Main {

    private List<List<Integer>> res = new ArrayList<>();
    public static void main(String[] args) {
        int[] nums = new int[]{10,9,2,5,3,7,101,18};
        int i = new Main().lengthOfLIS(nums);
        System.out.println(i);
    }

    /**
     *
     *
     * dp[i][j]= {
     *     i = j ,    dp[i][j] = 1
     *     i + 1= j   nums[j], dp[i][j] = max{dp[i][j-1],  dp[i][j-1]}
     *
     * }
     * @param nums
     * @return
     */
    public int lengthOfLIS(int[] nums) {


    }


    private void dfs (int[] nums, int index) {

        if(index > nums.length) {
            return ;
        }

        for (int i = index; i < nums.length; i++) {


        }

    }



        /**
         * 第一次尝试么有暴力解，子序列，并不是
         * @param nums
         * @return
         */
    public int lengthOfLIS(int[] nums) {
        int resLen = 1;
        for(int i = 0; i< nums.length; i ++)  {
            int len = 1;
            for(int j = i + 1; j < nums.length; j++) {
                if(nums[j] > nums[j-1]) {
                    len++;
                    resLen = Math.max(resLen, len);
                }else {
                    break;
                }
            }
        }
        return resLen;
    }

}



    
