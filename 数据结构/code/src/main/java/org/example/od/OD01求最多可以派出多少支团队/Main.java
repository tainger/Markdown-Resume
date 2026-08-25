package org.example.od.OD01求最多可以派出多少支团队;

import java.util.Arrays;

/**
 * @author jiazhiyuan
 * @date 2026/8/25 08:17
 */
public class Main {

    public static void main(String[] args) {

        int[] people = {3, 1, 5, 7, 9};
        int i = maxGroup1(people, 8);
        System.out.println(i);

    }

    public static int maxGroup1(int[] people, int n) {

        int count = 0;
        Arrays.sort(people);
        int left = 0;
        int right = people.length -1 ;

        while (left <= right) {
            if (people[right] >= n) {
                count++;
                right--;
                continue;
            }

            if(people[left] + people[right] >= n) {
                right--;
                left++;
                count++;
            }

            if(people[left] + people[right] < n) {
                left++;
            }
        }
        return count;
    }

    /**
     * 这个双重for循环会导致某些重复计算
     *
     * @param people
     * @param n
     * @return
     */
    public int maxGroup(int[] people, int n) {
        int count = 0;
        for (int i = 0; i < people.length; i++) {
            int cur = people[i];
            if (cur >= n) {
                count++;
                continue;
            }
            for (int j = i + 1; j < people.length; j++) {
                int next = people[j];
                if (cur + next >= n) {
                    count++;
                }

            }
        }
        return count;
    }
}



    
