package com.interview.algorithms.recursion;

import java.util.*;

/**
 * Common interview questions on Recursion and Backtracking
 */
public class RecursionBacktracking {

    /**
     * Generate all subsets of a set (Power Set)
     * Time: O(2^n), Space: O(n) for recursion stack
     */
    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackSubsets(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackSubsets(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
        result.add(new ArrayList<>(current));

        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]);
            backtrackSubsets(nums, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /**
     * Solve N-Queens Problem
     * Time: O(N!), Space: O(N^2)
     */
    public static List<List<String>> solveNQueens(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(board[i], '.');
        }

        backtrackQueens(board, 0, result);
        return result;
    }

    private static void backtrackQueens(char[][] board, int row, List<List<String>> result) {
        if (row == board.length) {
            result.add(constructBoard(board));
            return;
        }

        for (int col = 0; col < board.length; col++) {
            if (isValidQueen(board, row, col)) {
                board[row][col] = 'Q';
                backtrackQueens(board, row + 1, result);
                board[row][col] = '.';
            }
        }
    }

    private static boolean isValidQueen(char[][] board, int row, int col) {
        int n = board.length;

        // Check column
        for (int i = 0; i < row; i++) {
            if (board[i][col] == 'Q')
                return false;
        }

        // Check upper left diagonal
        for (int i = row - 1, j = col - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j] == 'Q')
                return false;
        }

        // Check upper right diagonal
        for (int i = row - 1, j = col + 1; i >= 0 && j < n; i--, j++) {
            if (board[i][j] == 'Q')
                return false;
        }

        return true;
    }

    private static List<String> constructBoard(char[][] board) {
        List<String> result = new ArrayList<>();
        for (char[] row : board) {
            result.add(new String(row));
        }
        return result;
    }

    /**
     * Generate all permutations of an array
     * Time: O(n!), Space: O(n)
     */
    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackPermute(nums, new ArrayList<>(), new boolean[nums.length], result);
        return result;
    }

    private static void backtrackPermute(int[] nums, List<Integer> current, boolean[] used,
            List<List<Integer>> result) {
        if (current.size() == nums.length) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = 0; i < nums.length; i++) {
            if (used[i])
                continue;

            current.add(nums[i]);
            used[i] = true;
            backtrackPermute(nums, current, used, result);
            used[i] = false;
            current.remove(current.size() - 1);
        }
    }

    /**
     * Generate all permutations of a string
     */
    public static List<String> permuteString(String s) {
        List<String> result = new ArrayList<>();
        backtrackPermuteString(s.toCharArray(), 0, result);
        return result;
    }

    private static void backtrackPermuteString(char[] chars, int start, List<String> result) {
        if (start == chars.length - 1) {
            result.add(new String(chars));
            return;
        }

        for (int i = start; i < chars.length; i++) {
            swap(chars, start, i);
            backtrackPermuteString(chars, start + 1, result);
            swap(chars, start, i);
        }
    }

    private static void swap(char[] chars, int i, int j) {
        char temp = chars[i];
        chars[i] = chars[j];
        chars[j] = temp;
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Recursion and Backtracking Examples ===\n");

        // Subsets
        int[] nums = { 1, 2, 3 };
        System.out.println("Subsets of " + Arrays.toString(nums) + ":");
        System.out.println(subsets(nums));

        // N-Queens
        int n = 4;
        System.out.println("\n" + n + "-Queens solutions:");
        List<List<String>> queens = solveNQueens(n);
        for (int i = 0; i < queens.size(); i++) {
            System.out.println("Solution " + (i + 1) + ":");
            for (String row : queens.get(i)) {
                System.out.println(row);
            }
            System.out.println();
        }

        // Permutations
        int[] permuteNums = { 1, 2, 3 };
        System.out.println("Permutations of " + Arrays.toString(permuteNums) + ":");
        System.out.println(permute(permuteNums));

        String str = "abc";
        System.out.println("\nPermutations of '" + str + "':");
        System.out.println(permuteString(str));
    }
}
