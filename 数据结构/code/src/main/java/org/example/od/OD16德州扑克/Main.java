package org.example.od.OD16德州扑克;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 12:44
 */
public class Main {

    public static void main(String[] args) {
        Main solution = new Main();

        // 同花顺
        String[] cards1 = {"A红桃", "K红桃", "Q红桃", "J红桃", "10红桃"};
        System.out.println(solution.handType(cards1));  // 1

        // 四条
        String[] cards2 = {"A红桃", "A黑桃", "A梅花", "A方块", "K红桃"};
        System.out.println(solution.handType(cards2));  // 2

        // 葫芦
        String[] cards3 = {"A红桃", "A黑桃", "A梅花", "K红桃", "K黑桃"};
        System.out.println(solution.handType(cards3));  // 3

        // 同花
        String[] cards4 = {"2红桃", "4红桃", "6红桃", "8红桃", "J红桃"};
        System.out.println(solution.handType(cards4));  // 4

        // 顺子（A2345）
        String[] cards5 = {"A红桃", "2黑桃", "3梅花", "4方块", "5红桃"};
        System.out.println(solution.handType(cards5));  // 5

        // 三条
        String[] cards6 = {"A红桃", "A黑桃", "A梅花", "K红桃", "Q红桃"};
        System.out.println(solution.handType(cards6));  // 6

        // 两对
        String[] cards7 = {"A红桃", "A黑桃", "K梅花", "K方块", "Q红桃"};
        System.out.println(solution.handType(cards7));  // 7

        // 对子
        String[] cards8 = {"A红桃", "A黑桃", "K梅花", "Q方块", "J红桃"};
        System.out.println(solution.handType(cards8));  // 8


    }

    public int handType(String[] cards) {
        // 1. 解析牌面大小和花色
        int[] ranks = new int[5];
        String[] suits = new String[5];

        for (int i = 0; i < 5; i++) {
            String card = cards[i];
            // 提取牌面：如果是 "10" 需要特殊处理
            if (card.startsWith("10")) {
                ranks[i] = 10;
                suits[i] = card.substring(2);  // "10红桃" → 从索引2开始
            } else {
                char rankChar = card.charAt(0);
                ranks[i] = getRankValue(rankChar);
                suits[i] = card.substring(1);
            }
        }

        // 2. 排序牌面（用于顺子判断）
        int[] sortedRanks = ranks.clone();
        Arrays.sort(sortedRanks);

        // 3. 统计牌面频率
        Map<Integer, Integer> rankCount = new HashMap<>();
        for (int r : ranks) {
            rankCount.put(r, rankCount.getOrDefault(r, 0) + 1);
        }

        // 4. 统计花色频率
        Map<String, Integer> suitCount = new HashMap<>();
        for (String s : suits) {
            suitCount.put(s, suitCount.getOrDefault(s, 0) + 1);
        }

        boolean isSameSuit = suitCount.size() == 1;  // 是否同花

        // 5. 判断顺子
        boolean isStraight = isStraight(sortedRanks);

        // 6. 按优先级从高到低判断
        if (isSameSuit && isStraight) return 1;  // 同花顺
        if (isFourOfKind(rankCount)) return 2;   // 四条
        if (isFullHouse(rankCount)) return 3;    // 葫芦
        if (isSameSuit) return 4;                // 同花
        if (isStraight) return 5;                // 顺子
        if (isThreeOfKind(rankCount)) return 6;  // 三条
        if (isTwoPairs(rankCount)) return 7;     // 两对
        if (isOnePair(rankCount)) return 8;      // 对子
        return 9;                                // 高牌
    }

    // 牌面字符转数字
    private int getRankValue(char c) {
        switch (c) {
            case 'J': return 11;
            case 'Q': return 12;
            case 'K': return 13;
            case 'A': return 14;
            default: return c - '0';  // '2'~'9'
        }
    }

    // 判断顺子（A 可以当 1）
    private boolean isStraight(int[] sorted) {
        // 情况1：普通顺子（如 2,3,4,5,6）
        if (sorted[4] - sorted[0] == 4) {
            // 检查是否连续
            for (int i = 1; i < 5; i++) {
                if (sorted[i] != sorted[i-1] + 1) return false;
            }
            return true;
        }

        // 情况2：A2345（A 当 1 用）
        // 排序后是 [2,3,4,5,14]，需要特殊判断
        if (sorted[0] == 2 && sorted[1] == 3 && sorted[2] == 4 &&
                sorted[3] == 5 && sorted[4] == 14) {
            return true;
        }

        return false;
    }

    // 判断四条
    private boolean isFourOfKind(Map<Integer, Integer> rankCount) {
        return rankCount.containsValue(4);
    }

    // 判断葫芦（三张 + 两张）
    private boolean isFullHouse(Map<Integer, Integer> rankCount) {
        return rankCount.containsValue(3) && rankCount.containsValue(2);
    }

    // 判断三条
    private boolean isThreeOfKind(Map<Integer, Integer> rankCount) {
        return rankCount.containsValue(3) && !rankCount.containsValue(2);
    }

    // 判断两对
    private boolean isTwoPairs(Map<Integer, Integer> rankCount) {
        int pairCount = 0;
        for (int count : rankCount.values()) {
            if (count == 2) pairCount++;
        }
        return pairCount == 2;
    }

    // 判断对子
    private boolean isOnePair(Map<Integer, Integer> rankCount) {
        int pairCount = 0;
        for (int count : rankCount.values()) {
            if (count == 2) pairCount++;
        }
        return pairCount == 1;
    }

}



    
