package org.example.od.OD11按身高体重排队;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/8/25 16:08
 */
public class Main {




    public static void main(String[] args) {
        int[] heights = new int[]{100, 100, 120, 130};

        int[] weights = new int[]{40, 30, 60, 50};


        int[] ints = new Main().queueOrder(heights, weights);
        System.out.println(Arrays.toString(ints));
    }


    public int[] queueOrder(int[] heights, int[] weights) {

        int n = heights.length;

        Integer[] students = new Integer[n];

        //初始化

        for (int i = 0; i < n; i++) {
            students[i] = i;
        }

        //稳定排序
        System.out.printf("students: %s", Arrays.toString(students));

        Arrays.sort(students, (a,b)->{
            if (heights[a] != heights[b]) {
                return heights[a] - heights[b];  // 身高升序
            }
            if (weights[a] != weights[b]) {
                return weights[a] - weights[b];  // 体重升序
            }
            return a - b;  // 编号升序（保证稳定）
        });
        System.out.printf("Arrays.sort(students: %s", Arrays.toString(students));
        // 转换回 int[]（编号+1）
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = students[i] + 1;
        }
        return result;
    }
}



    
