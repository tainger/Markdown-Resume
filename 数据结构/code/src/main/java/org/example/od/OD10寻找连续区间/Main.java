package org.example.od.OD10寻找连续区间;

/**
 * @author jiazhiyuan
 * @date 2026/8/25 15:17
 */
public class Main {

    public static void main(String[] args) {

        int[] nums = {1, 2, 3, 4, 5};
        int x = 7;
        long l = new Main().countIntervals(nums, x);
        System.out.println(l);

    }

    //滑动窗口
    public long countIntervals(int[] nums, int x) {
        // ...
        int right = 0;

        int left = 0;

        int n = nums.length;

        int sum = 0;

        int countRange = 0;
        while (right < n &&  left <=right) {

            sum += nums[right];

            if (sum >= x) {
                countRange ++;
            }

            right++;

            if(right > n - 1) {
                left++;
                right = left;
                sum =0;
            }
        }

        return countRange;
    }

    public long countIntervalsForce(int[] nums, int x) {
        // ...


        return 1;
    }
}



    
