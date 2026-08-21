package com.interview.algorithms.stacksqueues;

import java.util.*;

/**
 * Common interview questions on Stacks and Queues
 */
public class StacksQueues {

    /**
     * Implement a Queue using two Stacks
     */
    public static class QueueUsingStacks {
        private Stack<Integer> stack1;
        private Stack<Integer> stack2;

        public QueueUsingStacks() {
            stack1 = new Stack<>();
            stack2 = new Stack<>();
        }

        /**
         * Push element to the back of queue
         * Time: O(1)
         */
        public void enqueue(int x) {
            stack1.push(x);
        }

        /**
         * Remove element from front of queue
         * Time: Amortized O(1)
         */
        public int dequeue() {
            if (stack2.isEmpty()) {
                while (!stack1.isEmpty()) {
                    stack2.push(stack1.pop());
                }
            }

            if (stack2.isEmpty()) {
                throw new NoSuchElementException("Queue is empty");
            }

            return stack2.pop();
        }

        /**
         * Get the front element
         * Time: Amortized O(1)
         */
        public int peek() {
            if (stack2.isEmpty()) {
                while (!stack1.isEmpty()) {
                    stack2.push(stack1.pop());
                }
            }

            if (stack2.isEmpty()) {
                throw new NoSuchElementException("Queue is empty");
            }

            return stack2.peek();
        }

        /**
         * Check if queue is empty
         * Time: O(1)
         */
        public boolean isEmpty() {
            return stack1.isEmpty() && stack2.isEmpty();
        }
    }

    /**
     * Evaluate postfix expression using stack
     * Time: O(n), Space: O(n)
     */
    public static int evaluatePostfix(String expression) {
        Stack<Integer> stack = new Stack<>();
        String[] tokens = expression.split(" ");

        for (String token : tokens) {
            if (isOperator(token)) {
                int b = stack.pop();
                int a = stack.pop();
                int result = applyOperation(a, b, token);
                stack.push(result);
            } else {
                stack.push(Integer.parseInt(token));
            }
        }

        return stack.pop();
    }

    /**
     * Evaluate infix expression using two stacks
     * Time: O(n), Space: O(n)
     */
    public static int evaluateInfix(String expression) {
        Stack<Integer> values = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == ' ')
                continue;

            if (Character.isDigit(c)) {
                StringBuilder num = new StringBuilder();
                while (i < expression.length() && Character.isDigit(expression.charAt(i))) {
                    num.append(expression.charAt(i++));
                }
                i--;
                values.push(Integer.parseInt(num.toString()));
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (operators.peek() != '(') {
                    values.push(applyOp(operators.pop(), values.pop(), values.pop()));
                }
                operators.pop();
            } else if (isOperatorChar(c)) {
                while (!operators.isEmpty() && hasPrecedence(c, operators.peek())) {
                    values.push(applyOp(operators.pop(), values.pop(), values.pop()));
                }
                operators.push(c);
            }
        }

        while (!operators.isEmpty()) {
            values.push(applyOp(operators.pop(), values.pop(), values.pop()));
        }

        return values.pop();
    }

    private static boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')')
            return false;
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-'))
            return false;
        return true;
    }

    private static int applyOp(char op, int b, int a) {
        switch (op) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                return a / b;
        }
        return 0;
    }

    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    private static boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static int applyOperation(int a, int b, String operator) {
        switch (operator) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
            case "/":
                return a / b;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }

    /**
     * Next Greater Element
     * Time: O(n), Space: O(n)
     */
    public static int[] nextGreaterElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
                int index = stack.pop();
                result[index] = nums[i];
            }
            stack.push(i);
        }

        return result;
    }

    /**
     * Valid Parentheses - Check if string has valid parentheses
     * Time: O(n), Space: O(n)
     */
    public static boolean isValidParentheses(String s) {
        Stack<Character> stack = new Stack<>();
        Map<Character, Character> pairs = new HashMap<>();
        pairs.put(')', '(');
        pairs.put('}', '{');
        pairs.put(']', '[');

        for (char c : s.toCharArray()) {
            if (pairs.containsValue(c)) {
                stack.push(c);
            } else if (pairs.containsKey(c)) {
                if (stack.isEmpty() || stack.pop() != pairs.get(c)) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    // Demo/Test methods
    public static void main(String[] args) {
        System.out.println("=== Stacks and Queues Examples ===\n");

        // Queue using stacks
        QueueUsingStacks queue = new QueueUsingStacks();
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        System.out.println("Queue peek: " + queue.peek());
        System.out.println("Queue dequeue: " + queue.dequeue());
        System.out.println("Queue dequeue: " + queue.dequeue());
        queue.enqueue(4);
        System.out.println("Queue dequeue: " + queue.dequeue());
        System.out.println("Queue dequeue: " + queue.dequeue());

        // Evaluate postfix expression
        String postfix = "2 3 + 5 *";
        System.out.println("\nPostfix '" + postfix + "' = " + evaluatePostfix(postfix));

        // Evaluate infix expression
        String infix = "10 + 2 * 6";
        System.out.println("Infix '" + infix + "' = " + evaluateInfix(infix));

        // Next greater element
        int[] nums = { 4, 5, 2, 10, 8 };
        System.out.println("\nArray: " + Arrays.toString(nums));
        System.out.println("Next greater elements: " + Arrays.toString(nextGreaterElement(nums)));

        // Valid parentheses
        String valid = "{[()]}";
        String invalid = "{[(])}";
        System.out.println("\n'" + valid + "' is valid: " + isValidParentheses(valid));
        System.out.println("'" + invalid + "' is valid: " + isValidParentheses(invalid));
    }
}
