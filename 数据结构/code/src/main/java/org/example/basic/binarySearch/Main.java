package org.example.basic.binarySearch;

/**
 * @author jiazhiyuan
 * @date 2026/8/29 13:22
 */
public class Main {


    public int binarySearch(int[] nums, int target) {
        int l = 0;
        int r = nums.length - 1;
        while (l <= r) {

            int mid = l + (r - l) / 2;
            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] < target) {
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        return -1;
    }

    public static void main(String[] args) {



    }
}



    
