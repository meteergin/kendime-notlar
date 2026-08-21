package com.interview.algorithms.linkedlists;

/**
 * Common interview questions on Linked Lists
 */
public class LinkedLists {

    /**
     * ListNode class definition
     */
    public static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    /**
     * Detect a cycle in a linked list using Floyd's Cycle Detection (Tortoise and
     * Hare)
     * Time: O(n), Space: O(1)
     */
    public static boolean detectCycle(ListNode head) {
        if (head == null || head.next == null) {
            return false;
        }

        ListNode slow = head;
        ListNode fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;

            if (slow == fast) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reverse a linked list iteratively
     * Time: O(n), Space: O(1)
     */
    public static ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode current = head;

        while (current != null) {
            ListNode nextTemp = current.next;
            current.next = prev;
            prev = current;
            current = nextTemp;
        }

        return prev;
    }

    /**
     * Reverse a linked list recursively
     * Time: O(n), Space: O(n) due to recursion stack
     */
    public static ListNode reverseListRecursive(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        ListNode newHead = reverseListRecursive(head.next);
        head.next.next = head;
        head.next = null;

        return newHead;
    }

    /**
     * Merge two sorted linked lists
     * Time: O(m + n), Space: O(1)
     */
    public static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;

        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                current.next = l1;
                l1 = l1.next;
            } else {
                current.next = l2;
                l2 = l2.next;
            }
            current = current.next;
        }

        current.next = (l1 != null) ? l1 : l2;

        return dummy.next;
    }

    /**
     * Find the nth node from the end using two pointers
     * Time: O(n), Space: O(1)
     */
    public static ListNode findNthFromEnd(ListNode head, int n) {
        if (head == null || n <= 0) {
            return null;
        }

        ListNode fast = head;
        ListNode slow = head;

        // Move fast n nodes ahead
        for (int i = 0; i < n; i++) {
            if (fast == null) {
                return null; // n is larger than the list
            }
            fast = fast.next;
        }

        // Move both pointers until fast reaches the end
        while (fast != null) {
            slow = slow.next;
            fast = fast.next;
        }

        return slow;
    }

    // Helper methods for demonstration
    private static ListNode createList(int... values) {
        if (values.length == 0)
            return null;

        ListNode head = new ListNode(values[0]);
        ListNode current = head;

        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
        }

        return head;
    }

    private static void printList(ListNode head) {
        ListNode current = head;
        while (current != null) {
            System.out.print(current.val);
            if (current.next != null) {
                System.out.print(" -> ");
            }
            current = current.next;
        }
        System.out.println();
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Linked Lists Examples ===\n");

        // Detect cycle
        ListNode cycleList = createList(1, 2, 3, 4, 5);
        ListNode node = cycleList;
        while (node.next != null) {
            node = node.next;
        }
        node.next = cycleList.next.next; // Create cycle to node 3
        System.out.println("Cycle detected: " + detectCycle(cycleList));

        ListNode noCycleList = createList(1, 2, 3, 4, 5);
        System.out.println("Cycle detected in linear list: " + detectCycle(noCycleList));

        // Reverse list
        ListNode list = createList(1, 2, 3, 4, 5);
        System.out.print("Original list: ");
        printList(list);
        ListNode reversed = reverseList(list);
        System.out.print("Reversed list: ");
        printList(reversed);

        // Merge two sorted lists
        ListNode l1 = createList(1, 3, 5, 7);
        ListNode l2 = createList(2, 4, 6, 8);
        System.out.print("List 1: ");
        printList(l1);
        System.out.print("List 2: ");
        printList(l2);
        ListNode merged = mergeTwoLists(l1, l2);
        System.out.print("Merged list: ");
        printList(merged);

        // Find nth from end
        ListNode list2 = createList(1, 2, 3, 4, 5, 6, 7);
        int n = 3;
        ListNode nthNode = findNthFromEnd(list2, n);
        System.out.println(n + "rd node from end: " + (nthNode != null ? nthNode.val : "null"));
    }
}
