package org.example.leetcode100.lee03无重复字符的最长子串;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiazhiyuan
 * @date 2026/8/29 11:07
 */
public class Main {

    /**
     * 【】1.双指针 + 字母表判重 + 滑动窗口
     * [✅]2. for循环暴力
     *
     * @param args
     */
    public static void main(String[] args) {

        String s = "S";

        int i = new Main().lengthOfLongestSubstring(s);
        System.out.println(i);
    }


    /**
     * 双指针+滑动窗口
     * @param s
     * @return
     */
    public int lengthOfLongestSubstring2(String s) {
        // 用 HashMap 存储字符及其最新出现的位置
        Map<Character, Integer> map = new HashMap<>();
        int left = 0;          // 窗口左边界
        int maxLen = 0;        // 最大长度

        // right 作为窗口右边界，持续向右移动
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);

            // 如果当前字符之前出现过，且位置在窗口内（>= left）
            // 则将左边界移到重复字符的下一个位置
            if (map.containsKey(c) && map.get(c) >= left) {
                left = map.get(c) + 1;
            }

            // 更新当前字符的位置
            map.put(c, right);

            // 计算当前窗口长度，并更新最大值
            maxLen = Math.max(maxLen, right - left + 1);
        }

        return maxLen;
    }



    public int lengthOfLongestSubstring(String s) {
        if (s.length() == 0) {
            return 0;
        }
        if (s.length() == 1) {
            return 1;
        }
        char[] charArray = s.toCharArray();
        int maxLen = Integer.MIN_VALUE;
        for (int i = 0; i < charArray.length; i++) {
            Map<Character, Integer> counter = new HashMap<>();
            counter.put(charArray[i], 1);
            int length = 1;
            for (int j = i + 1; j < charArray.length; j++) {
                if (counter.get(charArray[j]) == null) {
                    length++;
                    counter.put(charArray[j], 1);
                    maxLen = Math.max(maxLen, length);
                } else {
                    maxLen = Math.max(maxLen, length);
                    break;
                }
            }
        }
        return maxLen;
    }
//    public int lengthOfLongestSubstring2(String s) {
//        char[] charArray = s.toCharArray();
//        Map<Character, Integer> counter = new HashMap<>();
//        for (int i = 0; i < charArray.length; i++) {
//            for (int j = i + 1; j < charArray.length; j++) {
//
//
//            }
//        }
//    }
}



    
