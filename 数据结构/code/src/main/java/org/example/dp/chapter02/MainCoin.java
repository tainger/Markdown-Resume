package org.example.dp.chapter02;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 17:05
 */
public class MainCoin {

    private int ans = Integer.MAX_VALUE;

    public int coinChange1(int[] coins, int amount) {
        backtrack(coins, amount, 0);
        return ans == Integer.MAX_VALUE ?  -1: ans;

    }

    /**
     *
     * @param coins
     * @param rest
     * @param count
     */
    private void backtrack(int[] coins, int rest, int count) {
        if(rest == 0) {
            ans = Math.min(ans, count);
            return;
        }
        //剪枝条件
        if(count > ans) {
            return;
        }

        for(int coin: coins) {
            if(coin < rest) {
                backtrack(coins, rest - coin, count + 1);
            }
        }

    }

    public static void main(String[] args) {


    }


}



    
