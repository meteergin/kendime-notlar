package com.interview.algorithms.dp;

import java.util.*;

/**
 * Common interview questions on Dynamic Programming
 */
public class DynamicProgramming {

    /**
     * Fibonacci - Recursive with Memoization
     * Time: O(n), Space: O(n)
     */
    public static int fibonacci(int n) {
        if (n <= 1)
            return n;
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        return fibHelper(n, memo);
    }

    private static int fibHelper(int n, int[] memo) {
        if (n <= 1)
            return n;
        if (memo[n] != -1)
            return memo[n];

        memo[n] = fibHelper(n - 1, memo) + fibHelper(n - 2, memo);
        return memo[n];
    }

    /**
     * Fibonacci - Bottom-up DP
     * Time: O(n), Space: O(n)
     */
    public static int fibonacciDP(int n) {
        if (n <= 1)
            return n;

        int[] dp = new int[n + 1];
        dp[0] = 0;
        dp[1] = 1;

        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }

        return dp[n];
    }

    /**
     * Fibonacci - Space optimized
     * Time: O(n), Space: O(1)
     */
    public static int fibonacciOptimized(int n) {
        if (n <= 1)
            return n;

        int prev2 = 0, prev1 = 1;

        for (int i = 2; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }

        return prev1;
    }

    /**
     * Coin Change Problem - Minimum coins needed
     * Time: O(amount * coins.length), Space: O(amount)
     */
    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }

        return dp[amount] > amount ? -1 : dp[amount];
    }

    /**
     * Coin Change 2 - Number of ways to make amount
     * Time: O(amount * coins.length), Space: O(amount)
     */
    public static int coinChangeWays(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        dp[0] = 1;

        for (int coin : coins) {
            for (int i = coin; i <= amount; i++) {
                dp[i] += dp[i - coin];
            }
        }

        return dp[amount];
    }

    /**
     * Longest Common Subsequence
     * Time: O(m * n), Space: O(m * n)
     */
    public static int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * Get the actual LCS string
     */
    public static String getLCS(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack to find the LCS
        StringBuilder lcs = new StringBuilder();
        int i = m, j = n;

        while (i > 0 && j > 0) {
            if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                lcs.append(text1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        return lcs.reverse().toString();
    }

    /**
     * 0/1 Knapsack Problem
     * Time: O(n * capacity), Space: O(n * capacity)
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            for (int w = 1; w <= capacity; w++) {
                if (weights[i - 1] <= w) {
                    dp[i][w] = Math.max(
                            dp[i - 1][w],
                            dp[i - 1][w - weights[i - 1]] + values[i - 1]);
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        return dp[n][capacity];
    }

    /**
     * Knapsack - Space optimized
     * Time: O(n * capacity), Space: O(capacity)
     */
    public static int knapsackOptimized(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];

        for (int i = 0; i < weights.length; i++) {
            for (int w = capacity; w >= weights[i]; w--) {
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
            }
        }

        return dp[capacity];
    }

    /**
     * Climbing Stairs - Count ways to reach top
     * Time: O(n), Space: O(1)
     */
    public static int climbStairs(int n) {
        if (n <= 2)
            return n;

        int prev2 = 1, prev1 = 2;

        for (int i = 3; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }

        return prev1;
    }

    /**
     * House Robber - Max money without robbing adjacent houses
     * Time: O(n), Space: O(1)
     */
    public static int rob(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;
        if (nums.length == 1)
            return nums[0];

        int prev2 = 0, prev1 = 0;

        for (int num : nums) {
            int curr = Math.max(prev1, prev2 + num);
            prev2 = prev1;
            prev1 = curr;
        }

        return prev1;
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Dynamic Programming Examples ===\n");

        // Fibonacci
        int n = 10;
        System.out.println("Fibonacci(" + n + ") = " + fibonacci(n));
        System.out.println("Fibonacci DP(" + n + ") = " + fibonacciDP(n));
        System.out.println("Fibonacci Optimized(" + n + ") = " + fibonacciOptimized(n));

        // Coin Change
        int[] coins = { 1, 2, 5 };
        int amount = 11;
        System.out.println("\nCoins: " + Arrays.toString(coins) + ", Amount: " + amount);
        System.out.println("Min coins needed: " + coinChange(coins, amount));
        System.out.println("Number of ways: " + coinChangeWays(coins, amount));

        // Longest Common Subsequence
        String text1 = "abcde";
        String text2 = "ace";
        System.out.println("\nLCS of '" + text1 + "' and '" + text2 + "': ");
        System.out.println("Length: " + longestCommonSubsequence(text1, text2));
        System.out.println("String: " + getLCS(text1, text2));

        // Knapsack
        int[] weights = { 1, 3, 4, 5 };
        int[] values = { 1, 4, 5, 7 };
        int capacity = 7;
        System.out.println("\nKnapsack - Weights: " + Arrays.toString(weights));
        System.out.println("Values: " + Arrays.toString(values));
        System.out.println("Capacity: " + capacity);
        System.out.println("Max value: " + knapsack(weights, values, capacity));
        System.out.println("Max value (optimized): " + knapsackOptimized(weights, values, capacity));

        // Climbing Stairs
        int stairs = 5;
        System.out.println("\nWays to climb " + stairs + " stairs: " + climbStairs(stairs));

        // House Robber
        int[] houses = { 2, 7, 9, 3, 1 };
        System.out.println("House values: " + Arrays.toString(houses));
        System.out.println("Max money to rob: " + rob(houses));
    }
}
