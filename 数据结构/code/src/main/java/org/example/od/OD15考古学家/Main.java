package org.example.od.OD15考古学家;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 11:23
 */
public class Main {

    private List<String> res = new ArrayList<>();

    public static void main(String[] args) {
        char chars[] = {'a', 'b', 'c'};
        List<String> strings = new Main().permuteUnique(chars);
        System.out.println(strings);
    }

    public List<String> permuteUnique(char[] chars) {
        Arrays.sort(chars);
        permuteUniqueIndex(chars, new StringBuilder(), 0);
        return this.res;
    }

    public void permuteUniqueIndex(char[] chars, StringBuilder stringBuilder, int cur) {
        if (cur > chars.length - 1) {
            this.res.add(stringBuilder.toString());
            return;
        }
        //字典序号
        for (int i = 0; i < chars.length; i++) {
            if (i != cur) {
                stringBuilder.append(chars[i]);
                permuteUniqueIndex(chars, stringBuilder, i + 1);
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
        }

    }
}



    
