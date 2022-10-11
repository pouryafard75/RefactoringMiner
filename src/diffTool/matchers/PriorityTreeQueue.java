package ASTDiff.matchers;

import ASTDiff.tree.Tree;

import java.util.List;
import java.util.function.Function;

public interface PriorityTreeQueue {
    Function<Tree, Integer> HEIGHT_PRIORITY_CALCULATOR = (Tree t) -> t.getMetrics().height;
    Function<Tree, Integer> SIZE_PRIORITY_CALCULATOR = (Tree t) -> t.getMetrics().size;

    static Function<Tree, Integer> getPriorityCalculator(String name) {
        if ("size".equals(name))
            return SIZE_PRIORITY_CALCULATOR;
        else if ("height".equals(name))
            return HEIGHT_PRIORITY_CALCULATOR;
        else
            return HEIGHT_PRIORITY_CALCULATOR;
    }

    /**
     * Return the list of trees with the greatest priority, and place all
     * their children in the queue.
     */
    List<Tree> popOpen();

    /**
     * Set the function that computes the priority on a given tree.
     */
    void setPriorityCalculator(Function<Tree, Integer> calculator);

    /**
     * Return the list of trees with the greatest priority.
     */
    List<Tree> pop();

    /**
     * Put the child of the tree into the priority queue.
     */
    void open(Tree tree);

    /**
     * Return the current greatest priority.
     */
    int currentPriority();

    /**
     * Set the minimum priority of a Tree to enter the queue.
     */
    void setMinimumPriority(int priority);

    /**
     * Return the minimum priority of a Tree to enter the queue.
     */
    int getMinimumPriority();

    /**
     * Return if there is any tree in the queue.
     */
    boolean isEmpty();

    /**
     * Empty the queue.
     */
    void clear();

    /**
     * Pop the provided queues until their current priorities are equals.
     * @return true if there are elements with a same priority in the queues
     *     false as soon as one queue is empty, in which case both queues are cleared.
     */
    static boolean synchronize(PriorityTreeQueue q1, PriorityTreeQueue q2) {
        while (!(q1.isEmpty() || q2.isEmpty()) && q1.currentPriority() != q2.currentPriority()) {
            if (q1.currentPriority() > q2.currentPriority())
                q1.popOpen();
            else
                q2.popOpen();
        }

        if (q1.isEmpty() || q2.isEmpty()) {
            q1.clear();
            q2.clear();
            return false;
        }
        else
            return true;
    }
}
