package org.example.leetcode100.lee02两数相加;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 17:57
 */
public class Main {

    static class ListNode {
        int val;
        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        if (l1 == null) {
            return l2;
        }

        if (l2 == null) {
            return l1;
        }

        int sumL1 = 0;
        int countl1 = 1;
        while (l1 != null) {
            sumL1 += l1.val * countl1 + sumL1;
            l1 = l1.next;
            countl1 = countl1 * 10;
        }

        int sumL2 = 0;
        int countl2 = 1;

        while (l2 != null) {
            sumL2 = l2.val * countl2 + sumL2;
            l2 = l2.next;
            countl2 = countl2 * 10;
        }

        int sum = sumL1 + sumL2;
        ListNode yummy = new ListNode(-1);
        ListNode cur = yummy;

        while (sum > 0) {
            int yushu = sum % 10;
            ListNode yushuListNode = new ListNode(yushu);
            cur.next = yushuListNode;
            cur = cur.next;
            sum = sum / 10;

        }
        return yummy.next;

    }


    public static void main(String[] args) {
        Main main = new Main();


        main.addTwoNumbers();
    }
}



    
