package org.example.od.OD19直捣黄龙;

import java.util.*;

/**
 * @author jiazhiyuan
 * @date 2026/8/28 13:52
 */
public class Main {


    private Map<Integer, Integer> counter = new HashMap<>();

    public static void main(String[] args) {

        char[][] grid = {{'S', '.', '.'}, {'#', '#', '.'}, {'.', '.', 'E'}};


    }

    public int countShortestPaths(char[][] grid) {
        int rows = grid.length, cols = grid[0].length;

        int[] start = null;

        int[] end = null;

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if ('S' == grid[i][j]) {
                    start = new int[]{i, j};
                }

                if ('E' == grid[i][j]) {
                    end = new int[]{i, j};
                }
            }
        }

        //2.初始化

        int[][] dist = new int[rows][cols];
        int[][] ways = new int[rows][cols];

        boolean[][] visited = new boolean[rows][cols];

        for (int i = 0; i < rows; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
        }

        dist[start[0]][start[1]] = 0;

        ways[start[0]][start[1]] = 1;

        visited[start[0]][start[1]] = true;


        Queue<int[]> queue = new LinkedList<>();

        queue.offer(new int[]{start[0], start[1]});


        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0];
            int y = cur[1];
            if (x == end[0] && y == end[1]) continue;

            for (int d = 0; d < 4; d++) {

                int nx = dx[d] + x;

                int ny = dy[d] + y;

                if (nx < 0 || nx >= rows || ny < 0 || ny > cols) {
                    continue;
                }

                if (grid[nx][ny] == '#') {
                    continue;
                }

                int newDist = dist[x][y] + 1;
                //情况1:
                if (newDist < dist[nx][ny]) {

                    dist[nx][ny] = newDist;
                    ways[nx][ny] = ways[x][y];

                    if (!visited[nx][ny]) {
                        visited[nx][ny] = true;
                        queue.offer(new int[]{nx, ny});
                    }
                } else if (newDist == dist[nx][ny]) {
                    ways[nx][ny] += ways[x][y];
                }
            }
        }
        return dist[end[0]][end[1]] == Integer.MAX_VALUE ? 0 : ways[end[0]][end[1]];
    }
}



    
