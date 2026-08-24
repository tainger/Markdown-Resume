package org.example.od.奇偶三数之和;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/8/24 12:50
 */
public class Main {

    public static void main(String[] args) {

        int[] nums = {-1, 0, 1, 2, -1, -4};
        int target = 0;
        List<List<Integer>> lists = threeSumOdd(nums, target);
        System.out.println(lists);
    }

    public static List<List<Integer>> threeSumOdd2(int[] nums, int target) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();

        for (int i = 0; i < nums.length; i++) {
            int cur = nums[i];
            int theNextTarget = target - cur;
            int j = i - 1;              // 左指针
            int k = i + 1;              // 右指针

            while (j >= 0 && k < nums.length) {   // Bug1: 边界修正
                int leftValue = theNextTarget - nums[k];
                if (leftValue > nums[j]) {
                    k++;                // 注意:leftValue 偏大说明 nums[k] 太小,要 k++
                } else if (leftValue < nums[j]) {
                    j--;
                } else {
                    int count = ((nums[i] & 1) != 0 ? 1 : 0)
                            + ((nums[j] & 1) != 0 ? 1 : 0)
                            + ((nums[k] & 1) != 0 ? 1 : 0);
                    if (count >= 2) {
                        res.add(Arrays.asList(nums[j], nums[i], nums[k]));
                    }
//                    j--;                // Bug3: 命中后两个指针都要动
                    k++;
                    // Bug2: 跳过重复
//                    while (j >= 0 && nums[j] == nums[j + 1]) j--;
//                    while (k < nums.length && nums[k] == nums[k - 1]) k++;
                }
            }
        }
        // Bug2 补充:i 在中间导致跨 i 的重复,以及 Bug4 字典序,统一用去重+排序兜底
        return (res);
    }

    public static List<List<Integer>> threeSumOdd(int[] nums, int target) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {

            int cur = nums[i];

            int theNextTarget = target - cur;
            int k = i + 1;

            int j = i - 1;

            if (j < 0 || k > nums.length) {
                continue;
            }

            while (0 <= j && k < nums.length) {

                int leftvalue = theNextTarget - nums[k];

                if (leftvalue > nums[j]) {
                    k++;
                } else if (leftvalue < nums[j]) {
                    j--;
                } else {

                    int count = 0;
                    if (Math.abs(nums[j]) % 2 != 0) {
                        count++;
                    }
                    if (Math.abs(nums[i]) % 2 != 0) {
                        count++;
                    }
                    if (Math.abs(nums[k]) % 2 != 0) {
                        count++;
                    }
                    if(count >= 2) {
                        List<Integer> resItem = new ArrayList<>();
                        resItem.add(nums[j]);
                        resItem.add(nums[i]);
                        resItem.add(nums[k]);
                        res.add(resItem);
                    }
                    k++;
                }
            }
        }
        return res;
    }


}



    
