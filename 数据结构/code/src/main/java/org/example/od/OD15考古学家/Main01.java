package org.example.od.OD15考古学家;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 12:00
 */
public class Main01 {


    private List<String> res = new ArrayList<>();
    public static void main(String[] args) {

        char[] chars ={'a', 'a', 'b'};
        List<String> strings = new Main01().permuteUnique(chars);
        System.out.println(strings);

    }

    public List<String> permuteUnique(char[] chars) {
        Arrays.sort(chars);
        int n = chars.length;
        boolean[] used = new boolean[n];
        StringBuilder path = new StringBuilder();
        dfs(chars, used, path);
        return res;
    }

    private void dfs(char[] chars, boolean[] used, StringBuilder path) {

        if (path.length() == chars.length) {
            res.add(path.toString());
            return;
        }
        for (int i = 0; i < chars.length; i++) {
            // 剪枝1：已使用
            if (used[i]) continue;
            // 剪枝2：同层去重（相同字符只取第一个）
            if (i > 0 && chars[i] == chars[i - 1] && !used[i - 1]) continue;

            used[i] = true;
            path.append(chars[i]);
            dfs(chars, used, path);
            path.deleteCharAt(path.length() - 1);
            used[i] = false;
        }
    }



}



    
