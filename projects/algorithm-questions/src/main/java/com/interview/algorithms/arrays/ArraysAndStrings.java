package com.interview.algorithms.arrays;

import java.util.*;

/**
 * Common interview questions on Arrays and Strings
 */
public class ArraysAndStrings {

    /**
     * Find the missing number in a sequence from 1 to n
     * Using sum formula: n(n+1)/2 - actual sum
     * Time: O(n), Space: O(1)
     */
    public static int findMissingNumber(int[] nums) {
        int n = nums.length + 1;
        int expectedSum = n * (n + 1) / 2;
        int actualSum = 0;
        for (int num : nums) {
            actualSum += num;
        }
        return expectedSum - actualSum;
    }

    /**
     * Reverse a string
     * Time: O(n), Space: O(n)
     */
    public static String reverseString(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char[] chars = str.toCharArray();
        int left = 0, right = chars.length - 1;
        while (left < right) {
            char temp = chars[left];
            chars[left] = chars[right];
            chars[right] = temp;
            left++;
            right--;
        }
        return new String(chars);
    }

    /**
     * Reverse an array in place
     * Time: O(n), Space: O(1)
     */
    public static void reverseArray(int[] arr) {
        if (arr == null || arr.length == 0) {
            return;
        }
        int left = 0, right = arr.length - 1;
        while (left < right) {
            int temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
            left++;
            right--;
        }
    }

    /**
     * Check if two strings are anagrams
     * Time: O(n), Space: O(1) - assuming limited character set
     */
    public static boolean areAnagrams(String s1, String s2) {
        if (s1 == null || s2 == null || s1.length() != s2.length()) {
            return false;
        }

        int[] charCount = new int[256]; // ASCII characters

        for (char c : s1.toCharArray()) {
            charCount[c]++;
        }

        for (char c : s2.toCharArray()) {
            charCount[c]--;
            if (charCount[c] < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find duplicates in an array
     * Time: O(n), Space: O(n)
     */
    public static List<Integer> findDuplicates(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        Set<Integer> duplicates = new HashSet<>();

        for (int num : nums) {
            if (!seen.add(num)) {
                duplicates.add(num);
            }
        }

        return new ArrayList<>(duplicates);
    }

    /**
     * Find unique elements in an array
     * Time: O(n), Space: O(n)
     */
    public static List<Integer> findUnique(int[] nums) {
        Map<Integer, Integer> countMap = new HashMap<>();

        for (int num : nums) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        List<Integer> unique = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() == 1) {
                unique.add(entry.getKey());
            }
        }

        return unique;
    }

    /**
     * Merge two sorted arrays
     * Time: O(m + n), Space: O(m + n)
     */
    public static int[] mergeSortedArrays(int[] arr1, int[] arr2) {
        int m = arr1.length;
        int n = arr2.length;
        int[] result = new int[m + n];

        int i = 0, j = 0, k = 0;

        while (i < m && j < n) {
            if (arr1[i] <= arr2[j]) {
                result[k++] = arr1[i++];
            } else {
                result[k++] = arr2[j++];
            }
        }

        while (i < m) {
            result[k++] = arr1[i++];
        }

        while (j < n) {
            result[k++] = arr2[j++];
        }

        return result;
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Arrays and Strings Examples ===\n");

        // Find missing number
        int[] nums1 = { 1, 2, 4, 5, 6 };
        System.out.println("Missing number in [1,2,4,5,6]: " + findMissingNumber(nums1));

        // Reverse string
        String str = "Hello World";
        System.out.println("Reversed '" + str + "': " + reverseString(str));

        // Reverse array
        int[] arr = { 1, 2, 3, 4, 5 };
        reverseArray(arr);
        System.out.println("Reversed array: " + Arrays.toString(arr));

        // Anagrams
        System.out.println("Are 'listen' and 'silent' anagrams? " + areAnagrams("listen", "silent"));
        System.out.println("Are 'hello' and 'world' anagrams? " + areAnagrams("hello", "world"));

        // Find duplicates
        int[] nums2 = { 1, 2, 3, 2, 4, 5, 4 };
        System.out.println("Duplicates in [1,2,3,2,4,5,4]: " + findDuplicates(nums2));

        // Find unique
        System.out.println("Unique elements in [1,2,3,2,4,5,4]: " + findUnique(nums2));

        // Merge sorted arrays
        int[] sorted1 = { 1, 3, 5, 7 };
        int[] sorted2 = { 2, 4, 6, 8 };
        System.out.println("Merged arrays: " + Arrays.toString(mergeSortedArrays(sorted1, sorted2)));
    }
}
