package org.example.leetcode100.leet05最长回文子串;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 11:58
 */
public class Main {

    int  maxlen = 1;

    int start = 0;
    public static void main(String[] args) {

        String s = "babad";
        String s2 = "cbbd";

        String s1 = new Main().longestPalindrome(s);
        String s3 = new Main().longestPalindrome(s2);

        System.out.println(s1);
        System.out.println(s3);


    }

    public String longestPalindrome(String s) {
        char[] charArray = s.toCharArray();
        int n = charArray.length - 1;

        for (int i = 0; i < charArray.length; i++) {
             int j =  i -1;
             int k =  i + 1;
             int len = 1;
             while (j >= 0 && k <= n) {
                 if (charArray[j] == charArray[k]) {
                     maxlen = Math.max(maxlen, len + 2);
                     start = j;
                     j--;
                     k++;
                 }else {
                     break;
                 }
             }

        }
        return s.substring(start, maxlen+1);
    }
}



    
