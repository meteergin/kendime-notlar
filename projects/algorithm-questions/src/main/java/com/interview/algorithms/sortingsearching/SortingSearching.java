package com.interview.algorithms.sortingsearching;

import java.util.Arrays;

/**
 * Common interview questions on Sorting and Searching
 */
public class SortingSearching {

    /**
     * Quick Sort implementation
     * Average Time: O(n log n), Worst: O(n²), Space: O(log n)
     */
    public static void quickSort(int[] arr) {
        if (arr == null || arr.length == 0)
            return;
        quickSortHelper(arr, 0, arr.length - 1);
    }

    private static void quickSortHelper(int[] arr, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high);
            quickSortHelper(arr, low, pivotIndex - 1);
            quickSortHelper(arr, pivotIndex + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                swap(arr, i, j);
            }
        }

        swap(arr, i + 1, high);
        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * Merge Sort implementation
     * Time: O(n log n), Space: O(n)
     */
    public static void mergeSort(int[] arr) {
        if (arr == null || arr.length <= 1)
            return;
        mergeSortHelper(arr, 0, arr.length - 1);
    }

    private static void mergeSortHelper(int[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;

            mergeSortHelper(arr, left, mid);
            mergeSortHelper(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        int[] leftArr = new int[n1];
        int[] rightArr = new int[n2];

        System.arraycopy(arr, left, leftArr, 0, n1);
        System.arraycopy(arr, mid + 1, rightArr, 0, n2);

        int i = 0, j = 0, k = left;

        while (i < n1 && j < n2) {
            if (leftArr[i] <= rightArr[j]) {
                arr[k++] = leftArr[i++];
            } else {
                arr[k++] = rightArr[j++];
            }
        }

        while (i < n1) {
            arr[k++] = leftArr[i++];
        }

        while (j < n2) {
            arr[k++] = rightArr[j++];
        }
    }

    /**
     * Binary Search on sorted array
     * Time: O(log n), Space: O(1)
     */
    public static int binarySearch(int[] arr, int target) {
        if (arr == null || arr.length == 0)
            return -1;

        int left = 0, right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    /**
     * Recursive Binary Search
     * Time: O(log n), Space: O(log n)
     */
    public static int binarySearchRecursive(int[] arr, int target) {
        if (arr == null || arr.length == 0)
            return -1;
        return binarySearchHelper(arr, target, 0, arr.length - 1);
    }

    private static int binarySearchHelper(int[] arr, int target, int left, int right) {
        if (left > right)
            return -1;

        int mid = left + (right - left) / 2;

        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            return binarySearchHelper(arr, target, mid + 1, right);
        } else {
            return binarySearchHelper(arr, target, left, mid - 1);
        }
    }

    /**
     * Search in rotated sorted array
     * Example: [4,5,6,7,0,1,2], target = 0
     * Time: O(log n), Space: O(1)
     */
    public static int searchRotatedArray(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return -1;

        int left = 0, right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                return mid;
            }

            // Determine which half is sorted
            if (nums[left] <= nums[mid]) {
                // Left half is sorted
                if (target >= nums[left] && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                // Right half is sorted
                if (target > nums[mid] && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }

        return -1;
    }

    /**
     * Find the rotation point (minimum element) in rotated sorted array
     * Time: O(log n), Space: O(1)
     */
    public static int findMin(int[] nums) {
        if (nums == null || nums.length == 0)
            return -1;

        int left = 0, right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return nums[left];
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Sorting and Searching Examples ===\n");

        // Quick Sort
        int[] arr1 = { 64, 34, 25, 12, 22, 11, 90 };
        System.out.println("Original array: " + Arrays.toString(arr1));
        quickSort(arr1);
        System.out.println("Quick sorted: " + Arrays.toString(arr1));

        // Merge Sort
        int[] arr2 = { 64, 34, 25, 12, 22, 11, 90 };
        System.out.println("\nOriginal array: " + Arrays.toString(arr2));
        mergeSort(arr2);
        System.out.println("Merge sorted: " + Arrays.toString(arr2));

        // Binary Search
        int[] sorted = { 1, 3, 5, 7, 9, 11, 13, 15 };
        int target = 7;
        System.out.println("\nArray: " + Arrays.toString(sorted));
        System.out.println("Binary search for " + target + ": index " + binarySearch(sorted, target));
        System.out.println("Binary search for 10: index " + binarySearch(sorted, 10));

        // Search in rotated array
        int[] rotated = { 4, 5, 6, 7, 0, 1, 2 };
        System.out.println("\nRotated array: " + Arrays.toString(rotated));
        System.out.println("Search for 0: index " + searchRotatedArray(rotated, 0));
        System.out.println("Search for 3: index " + searchRotatedArray(rotated, 3));
        System.out.println("Minimum element: " + findMin(rotated));
    }
}
