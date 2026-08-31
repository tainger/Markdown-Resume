package org.example.leetcode100.lee152乘积最大子数组;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 19:30
 */
public class Main {

    public static void main(String[] args) {
        int[] nums = {-2,3,-4};
        int res = new Main().maxProduct(nums);
        System.out.println(res);
    }

    public int maxProduct(int[] nums) {
        int n = nums.length;
        int endRes = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            int res = nums[i];
            for (int j = i + 1; j < n; j++) {
                res = res * nums[j];
                endRes = Math.max(endRes, res);
            }
        }
        return endRes;
    }
}



    
