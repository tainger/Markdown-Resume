package org.example.od.OD17勇攀数字高峰;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 12:56
 */
public class Main {


    private int rows, cols;
    private int [][] grid;
    private int k;
    private boolean [][] visited;
    private int pathCount;
    private int[] start, end;


    public int countPaths(int[][] grid, int k) {
        this.grid  = grid;
        this.k = k;
        this.rows = grid.length;
        this.cols = grid[0].length;

        this.visited = new boolean[rows][cols];

        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        //找到起始点
        for (int i = 0; i <  grid.length; i ++) {

            for(int j = 0; j < grid[0].length; j ++) {

                if(grid[i][j]  <  minVal) {
                    minVal = grid[i][j];
                    start = new int[]{i,j};
                }

                if(grid[i][j] > maxVal) {
                    maxVal = grid[i][j];
                    end = new int[]{i, j};
                }
            }
        }
        //找到终点

        visited[start[0]][ start[1]] = true;

        dfs(start[0], start[1]);
        return pathCount;
    }

    private void dfs(int x, int y) {

        //终止条件 到达终点
        if(x == end[0] && y == end[1]) {
            pathCount++;
            return;
        }

        //四方向探索

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};


        for(int d = 0; d < 4; d++) {
            int nx = x + dx[d];
            int ny = y + dy[d];

            //检查边界
            if(nx < 0 || nx >= rows || ny < 0 || ny >= cols) continue;

            //检查是否已经访问了
            if(visited[nx][ny]) {
                continue;
            }

            //检查高度
            if(Math.abs(grid[nx][ny] - grid[x][y]) >  k) {
                continue;
            }
            visited[nx][ny] = true;
            dfs(nx, ny);
            visited[nx][ny] = false;
        }
        //四个方向探索
    }


    public static void main(String[] args) {

        int[][] grid  = {{4, 3},{3, 2}};

        int k  = 1;

        int i = new Main().countPaths(grid, k);

        System.out.println(i);

    }



}



    
