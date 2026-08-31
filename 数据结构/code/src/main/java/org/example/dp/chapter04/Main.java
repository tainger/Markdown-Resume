package org.example.dp.chapter04;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 17:48
 */
public class Main {


    public static void main(String[] args) {
        int[] array = new int[]{-2, 1, -3, 1, -1, 6, 2, -5, 4};
    }


    /**
     * dp[i]= max{dp[i-1] + nums[i], nums[i]}
     *
     * @param nums
     * @return
     */
    public int maxSubArray(int[] nums) {
        int n = nums.length;
        if (n == 0) {
            return -1;
        }
        if (n == 1) {
            return nums[0];
        }
        int[] dp = new int[n];
        dp[0] = nums[0];
        for(int i=1; i <= n -1 ;i++) {

            if(nums[i]> dp[i-1] + nums[i] ) {
                dp[i] = nums[i];
            }else {
                dp[i] = dp[i-1] + nums[i];
            }
        }
        return dp[n-1];
    }
}



    
