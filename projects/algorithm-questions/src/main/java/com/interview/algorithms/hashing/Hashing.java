package com.interview.algorithms.hashing;

import java.util.*;

/**
 * Common interview questions on Hashing
 */
public class Hashing {

    /**
     * Two Sum - Find two numbers that add up to target
     * Time: O(n), Space: O(n)
     */
    public static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }

        return new int[] { -1, -1 };
    }

    /**
     * Group Anagrams
     * Time: O(n * k log k) where k is max length of string
     * Space: O(n * k)
     */
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();

        for (String str : strs) {
            char[] chars = str.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);

            map.putIfAbsent(key, new ArrayList<>());
            map.get(key).add(str);
        }

        return new ArrayList<>(map.values());
    }

    /**
     * Group Anagrams - Optimal using character count
     * Time: O(n * k) where k is max length of string
     * Space: O(n * k)
     */
    public static List<List<String>> groupAnagramsOptimal(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();

        for (String str : strs) {
            String key = getCharCountKey(str);
            map.putIfAbsent(key, new ArrayList<>());
            map.get(key).add(str);
        }

        return new ArrayList<>(map.values());
    }

    private static String getCharCountKey(String s) {
        int[] count = new int[26];
        for (char c : s.toCharArray()) {
            count[c - 'a']++;
        }

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if (count[i] > 0) {
                key.append((char) ('a' + i)).append(count[i]);
            }
        }

        return key.toString();
    }

    /**
     * Longest Substring Without Repeating Characters
     * Time: O(n), Space: O(min(n, m)) where m is charset size
     */
    public static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> charIndex = new HashMap<>();
        int maxLength = 0;
        int start = 0;

        for (int end = 0; end < s.length(); end++) {
            char c = s.charAt(end);

            if (charIndex.containsKey(c)) {
                start = Math.max(start, charIndex.get(c) + 1);
            }

            charIndex.put(c, end);
            maxLength = Math.max(maxLength, end - start + 1);
        }

        return maxLength;
    }

    /**
     * Find First Non-Repeating Character
     * Time: O(n), Space: O(1) - limited charset
     */
    public static char firstNonRepeatingChar(String s) {
        Map<Character, Integer> charCount = new HashMap<>();

        for (char c : s.toCharArray()) {
            charCount.put(c, charCount.getOrDefault(c, 0) + 1);
        }

        for (char c : s.toCharArray()) {
            if (charCount.get(c) == 1) {
                return c;
            }
        }

        return '_'; // No non-repeating character
    }

    /**
     * Subarray Sum Equals K
     * Time: O(n), Space: O(n)
     */
    public static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> prefixSumCount = new HashMap<>();
        prefixSumCount.put(0, 1);

        int count = 0;
        int sum = 0;

        for (int num : nums) {
            sum += num;

            if (prefixSumCount.containsKey(sum - k)) {
                count += prefixSumCount.get(sum - k);
            }

            prefixSumCount.put(sum, prefixSumCount.getOrDefault(sum, 0) + 1);
        }

        return count;
    }

    /**
     * Find All Duplicates in Array (nums[i] in range [1, n])
     * Time: O(n), Space: O(1)
     */
    public static List<Integer> findDuplicates(int[] nums) {
        List<Integer> duplicates = new ArrayList<>();

        for (int i = 0; i < nums.length; i++) {
            int index = Math.abs(nums[i]) - 1;

            if (nums[index] < 0) {
                duplicates.add(index + 1);
            } else {
                nums[index] = -nums[index];
            }
        }

        // Restore array
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Math.abs(nums[i]);
        }

        return duplicates;
    }

    /**
     * Contains Duplicate within k distance
     * Time: O(n), Space: O(k)
     */
    public static boolean containsNearbyDuplicate(int[] nums, int k) {
        Map<Integer, Integer> indexMap = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            if (indexMap.containsKey(nums[i])) {
                if (i - indexMap.get(nums[i]) <= k) {
                    return true;
                }
            }
            indexMap.put(nums[i], i);
        }

        return false;
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Hashing Examples ===\n");

        // Two Sum
        int[] nums = { 2, 7, 11, 15 };
        int target = 9;
        System.out.println("Array: " + Arrays.toString(nums) + ", Target: " + target);
        System.out.println("Two sum indices: " + Arrays.toString(twoSum(nums, target)));

        // Group Anagrams
        String[] strs = { "eat", "tea", "tan", "ate", "nat", "bat" };
        System.out.println("\nGroup anagrams of " + Arrays.toString(strs) + ":");
        System.out.println(groupAnagrams(strs));

        // Longest substring without repeating chars
        String s1 = "abcabcbb";
        String s2 = "pwwkew";
        System.out.println("\nLongest substring without repeating in '" + s1 + "': " + lengthOfLongestSubstring(s1));
        System.out.println("Longest substring without repeating in '" + s2 + "': " + lengthOfLongestSubstring(s2));

        // First non-repeating character
        String s3 = "leetcode";
        System.out.println("\nFirst non-repeating char in '" + s3 + "': " + firstNonRepeatingChar(s3));

        // Subarray sum equals k
        int[] arr = { 1, 1, 1 };
        int k = 2;
        System.out.println("\nSubarrays with sum " + k + " in " + Arrays.toString(arr) + ": " + subarraySum(arr, k));

        // Contains nearby duplicate
        int[] nums2 = { 1, 2, 3, 1 };
        System.out.println("\nContains duplicate within 3 positions in " + Arrays.toString(nums2) + ": "
                + containsNearbyDuplicate(nums2, 3));
    }
}
