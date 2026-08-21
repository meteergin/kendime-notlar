# Interview Algorithm Questions

A comprehensive collection of common interview algorithm questions implemented in Java, organized by topic.

## 📚 Categories

### 1. Arrays and Strings
- Find the missing number in a sequence
- Reverse a string or array
- Check if two strings are anagrams
- Find duplicates or unique elements
- Merge two sorted arrays

### 2. Linked Lists
- Detect a cycle in a linked list (Floyd's Algorithm)
- Reverse a linked list
- Merge two sorted linked lists
- Find the nth node from the end

### 3. Trees and Graphs
- Tree traversal (inorder, preorder, postorder)
- Lowest common ancestor in a binary tree
- Check if a binary tree is balanced
- Graph traversal algorithms (BFS, DFS)
- Detect a cycle in a graph

### 4. Sorting and Searching
- Quick sort
- Merge sort
- Binary search
- Search in rotated sorted array

### 5. Dynamic Programming
- Fibonacci sequence (memoization, tabulation, optimized)
- Coin change problem
- Longest common subsequence
- 0/1 Knapsack problem

### 6. Recursion and Backtracking
- Generate all subsets of a set
- Solve the N-Queens problem
- Permutations of a string or array

### 7. Stacks and Queues
- Implement a queue using stacks
- Evaluate expressions (postfix and infix)
- Next greater element problem

### 8. Hashing
- Two sum problem
- Group anagrams
- Longest substring without repeating characters

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Build the Project
```bash
mvn clean compile
```

### Run Examples

Each algorithm module has a `main()` method with working examples. Run any module using:

```bash
# Arrays and Strings
mvn exec:java -Dexec.mainClass="com.interview.algorithms.arrays.ArraysAndStrings"

# Linked Lists
mvn exec:java -Dexec.mainClass="com.interview.algorithms.linkedlists.LinkedLists"

# Trees and Graphs
mvn exec:java -Dexec.mainClass="com.interview.algorithms.treesgraphs.TreesAndGraphs"

# Sorting and Searching
mvn exec:java -Dexec.mainClass="com.interview.algorithms.sortingsearching.SortingSearching"

# Dynamic Programming
mvn exec:java -Dexec.mainClass="com.interview.algorithms.dp.DynamicProgramming"

# Recursion and Backtracking
mvn exec:java -Dexec.mainClass="com.interview.algorithms.recursion.RecursionBacktracking"

# Stacks and Queues
mvn exec:java -Dexec.mainClass="com.interview.algorithms.stacksqueues.StacksQueues"

# Hashing
mvn exec:java -Dexec.mainClass="com.interview.algorithms.hashing.Hashing"
```

## 📁 Project Structure

```
src/main/java/com/interview/algorithms/
├── arrays/
│   └── ArraysAndStrings.java
├── linkedlists/
│   └── LinkedLists.java
├── treesgraphs/
│   └── TreesAndGraphs.java
├── sortingsearching/
│   └── SortingSearching.java
├── dp/
│   └── DynamicProgramming.java
├── recursion/
│   └── RecursionBacktracking.java
├── stacksqueues/
│   └── StacksQueues.java
└── hashing/
    └── Hashing.java
```

## 💡 Code Examples

### Two Sum Problem
```java
int[] nums = {2, 7, 11, 15};
int target = 9;
int[] result = Hashing.twoSum(nums, target);
// Returns: [0, 1]
```

### Binary Search
```java
int[] sorted = {1, 3, 5, 7, 9, 11, 13, 15};
int index = SortingSearching.binarySearch(sorted, 7);
// Returns: 3
```

### Fibonacci (Optimized)
```java
int result = DynamicProgramming.fibonacciOptimized(10);
// Returns: 55
```

## ⏱️ Complexity Analysis

Each algorithm includes detailed comments on:
- **Time Complexity**: Big O notation for runtime
- **Space Complexity**: Memory usage analysis

Example:
```java
/**
 * Binary Search on sorted array
 * Time: O(log n), Space: O(1)
 */
public static int binarySearch(int[] arr, int target) {
    // Implementation
}
```

## 🎯 Features

✅ All algorithms include working implementations  
✅ Time and space complexity documented  
✅ Runnable examples with test cases  
✅ Clean, well-commented code  
✅ Multiple solution approaches where applicable (iterative/recursive)  
✅ Bonus algorithms included in each category  

## 📖 Learning Resources

Each module demonstrates common interview patterns:
- **Two Pointers**: Array reversal, linked list problems
- **Sliding Window**: Longest substring problems
- **Dynamic Programming**: Bottom-up and top-down approaches
- **Backtracking**: N-Queens, permutations, subsets
- **Graph Algorithms**: BFS, DFS, cycle detection

## 🔧 Development

### Running Tests
```bash
mvn test
```

### Package the Application
```bash
mvn clean package
```

## 📝 License

This project is for educational purposes.

## 🤝 Contributing

Feel free to add more algorithms or optimize existing implementations!

---

**Happy Coding! 🚀**
