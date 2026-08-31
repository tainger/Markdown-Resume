package org.example.dp.chapter01;

/**
 * @author jiazhiyuan
 * @date 2026/8/31 15:59
 */
public class GetCoinBack {


    public static void main(String[] args) {
        int[] values = {5, 3};
        int total = 11;

        int minCoinCounter = new GetCoinBack().getMinCoinCounter(total, values, 2);
        System.out.println(minCoinCounter);

    }

    /**
     * 贪心去解，当前最优，拿不到全局最优。
     * @param total
     * @param values
     * @param valueCount
     * @return
     */
    private int getMinCoinCounter(int total, int[] values, int valueCount) {
        int rest= total;
        int count = 0;
        for(int i = 0;  i < valueCount; ++i) {
            int  currentCount =  rest / values[i];
            rest-= currentCount * values[i];
            count +=  currentCount;
            if(rest ==0) {
                return  count;
            }
        }
        return -1;
    }

    /**
     * 回溯去解
     */
    private int getMinCoinCountOfValue(int total, int[] values, int  valueIndex) {

        //判断使用的币种数超过了coin就终止
        int valueCount = values.length;

        if(valueIndex == valueCount) {
            return Integer.MAX_VALUE;
        }

        int minResult = Integer.MAX_VALUE;
        int currentValue = values[valueIndex];
        int maxCount = total/ currentValue;

        for(int count = maxCount; count >= 0; count --) {
            int rest = total - count * currentValue;

            if(rest == 0) {
                minResult = Math.min(minResult, count);
                break;
            }

            int restCount = getMinCoinCountOfValue(rest, values,  valueIndex + 1);


        }


        return 1;


    }
}



    
