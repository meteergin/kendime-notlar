package com.interview.algorithms.treesgraphs;

import java.util.*;

/**
 * Common interview questions on Trees and Graphs
 */
public class TreesAndGraphs {

    /**
     * TreeNode class definition
     */
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * Inorder traversal (Left, Root, Right)
     * Time: O(n), Space: O(h) where h is height
     */
    public static List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderHelper(root, result);
        return result;
    }

    private static void inorderHelper(TreeNode node, List<Integer> result) {
        if (node == null)
            return;
        inorderHelper(node.left, result);
        result.add(node.val);
        inorderHelper(node.right, result);
    }

    /**
     * Preorder traversal (Root, Left, Right)
     * Time: O(n), Space: O(h)
     */
    public static List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderHelper(root, result);
        return result;
    }

    private static void preorderHelper(TreeNode node, List<Integer> result) {
        if (node == null)
            return;
        result.add(node.val);
        preorderHelper(node.left, result);
        preorderHelper(node.right, result);
    }

    /**
     * Postorder traversal (Left, Right, Root)
     * Time: O(n), Space: O(h)
     */
    public static List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        postorderHelper(root, result);
        return result;
    }

    private static void postorderHelper(TreeNode node, List<Integer> result) {
        if (node == null)
            return;
        postorderHelper(node.left, result);
        postorderHelper(node.right, result);
        result.add(node.val);
    }

    /**
     * Lowest Common Ancestor in a binary tree
     * Time: O(n), Space: O(h)
     */
    public static TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) {
            return root;
        }

        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);

        if (left != null && right != null) {
            return root;
        }

        return left != null ? left : right;
    }

    /**
     * Check if a binary tree is balanced
     * Time: O(n), Space: O(h)
     */
    public static boolean isBalanced(TreeNode root) {
        return checkHeight(root) != -1;
    }

    private static int checkHeight(TreeNode node) {
        if (node == null)
            return 0;

        int leftHeight = checkHeight(node.left);
        if (leftHeight == -1)
            return -1;

        int rightHeight = checkHeight(node.right);
        if (rightHeight == -1)
            return -1;

        if (Math.abs(leftHeight - rightHeight) > 1) {
            return -1;
        }

        return Math.max(leftHeight, rightHeight) + 1;
    }

    /**
     * Breadth-First Search (BFS) - Level Order Traversal
     * Time: O(n), Space: O(w) where w is max width
     */
    public static List<List<Integer>> bfs(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null)
            return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);

                if (node.left != null)
                    queue.offer(node.left);
                if (node.right != null)
                    queue.offer(node.right);
            }

            result.add(level);
        }

        return result;
    }

    /**
     * Depth-First Search (DFS) on a tree
     * Time: O(n), Space: O(h)
     */
    public static List<Integer> dfs(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        dfsHelper(root, result);
        return result;
    }

    private static void dfsHelper(TreeNode node, List<Integer> result) {
        if (node == null)
            return;
        result.add(node.val);
        dfsHelper(node.left, result);
        dfsHelper(node.right, result);
    }

    /**
     * Detect cycle in a directed graph using DFS
     * Time: O(V + E), Space: O(V)
     */
    public static boolean detectGraphCycle(int numNodes, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
        }

        int[] visited = new int[numNodes]; // 0: unvisited, 1: visiting, 2: visited

        for (int i = 0; i < numNodes; i++) {
            if (visited[i] == 0) {
                if (hasCycleDFS(i, graph, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasCycleDFS(int node, List<List<Integer>> graph, int[] visited) {
        if (visited[node] == 1)
            return true; // Back edge found
        if (visited[node] == 2)
            return false; // Already processed

        visited[node] = 1; // Mark as visiting

        for (int neighbor : graph.get(node)) {
            if (hasCycleDFS(neighbor, graph, visited)) {
                return true;
            }
        }

        visited[node] = 2; // Mark as visited
        return false;
    }

    /**
     * Graph BFS traversal
     * Time: O(V + E), Space: O(V)
     */
    public static List<Integer> graphBFS(int numNodes, int[][] edges, int start) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
        }

        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[numNodes];
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(start);
        visited[start] = true;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            result.add(node);

            for (int neighbor : graph.get(node)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }

        return result;
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Trees and Graphs Examples ===\n");

        // Create a binary tree:
        // 1
        // / \
        // 2 3
        // / \
        // 4 5
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.left = new TreeNode(4);
        root.left.right = new TreeNode(5);

        System.out.println("Inorder traversal: " + inorderTraversal(root));
        System.out.println("Preorder traversal: " + preorderTraversal(root));
        System.out.println("Postorder traversal: " + postorderTraversal(root));

        System.out.println("BFS (Level order): " + bfs(root));
        System.out.println("DFS: " + dfs(root));

        System.out.println("Is balanced: " + isBalanced(root));

        TreeNode lca = lowestCommonAncestor(root, root.left.left, root.left.right);
        System.out.println("LCA of 4 and 5: " + (lca != null ? lca.val : "null"));

        // Graph cycle detection
        int[][] graphEdges = { { 0, 1 }, { 1, 2 }, { 2, 0 } }; // Has cycle
        System.out.println("\nGraph has cycle: " + detectGraphCycle(3, graphEdges));

        int[][] acyclicEdges = { { 0, 1 }, { 1, 2 }, { 0, 2 } }; // No cycle (DAG)
        System.out.println("Acyclic graph has cycle: " + detectGraphCycle(3, acyclicEdges));

        // Graph BFS
        int[][] edges = { { 0, 1 }, { 0, 2 }, { 1, 3 }, { 2, 4 } };
        System.out.println("Graph BFS from node 0: " + graphBFS(5, edges, 0));
    }
}
