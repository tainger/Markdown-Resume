package org.example.od.OD006最长的指定瑕疵度的元音子串;

/**
 * @author jiazhiyuan
 * @date 2026/8/25 13:48
 */
public class Main {


    public static void main(String[] args) {


    }

    public int longestVowelSubstring(String s, int k) {
        int n = s.length();
        int left = 0;
        int cnt = 0;
        int ans = 0;

        for (int right = 0; right < n; right++) {
            if(!isVoel(s.charAt(right))) {
                cnt ++;
            }

            while (cnt > k) {
                if(!isVoel(s.charAt(left))){
                    cnt --;
                }
                left++;
            }

            if(cnt  == k && isVoel(s.charAt(left)) && isVoel(s.charAt(right))) {

                ans = Math.max(ans, right - left + 1);
            }
        }
        return  ans;
    
    }

    private boolean isVoel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }


}



    
