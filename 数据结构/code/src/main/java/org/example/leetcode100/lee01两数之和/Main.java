package org.example.leetcode100.lee01两数之和;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 17:56
 */
public class Main {

    public static void main(String[] args) {

    }

    /**
     * 【✅】1.双重for循环暴力
     * 【✅】2.hashmap
     * 【】3.sort 之后双指针。双指针不行，排序之后会把下标换掉【不信你g】。
     */
    public int[] twoSum1(int[] nums, int target) {

        for(int i = 0; i < nums.length; i ++) {
            for(int j = i + 1; j < nums.length; j++) {
                if(target == nums[i] + nums[j]) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1, -1};

    }
    public int[] twoSum2(int[] nums, int target) {

        Map<Integer, Integer> map  = new HashMap<>();
        for (int i = 0; i <nums.length; i++) {
            map.put(nums[i], i);
        }

        for (int i =0 ; i < nums.length;i++) {
            int nextValue = target - nums[i];

            Integer integer = map.get(nextValue);
            if(integer != null && integer != i)  {
                return new int[]{i, integer};
            }
        }
        return new int[]{-1, -1};
    }
    public int[] twoSum3(int[] nums, int target) {
        Arrays.sort(nums);
        int j = 0;
        int k = nums.length -1 ;
        while (j < k) {
            if(target > nums[j] + nums[k]) {
               j++;
            }else if(target < nums[j] + nums[k]) {
                k--;
            }else {
                return new int[]{j, k};
            }
        }
        return new int[]{-1, -1};
    }
}



    
