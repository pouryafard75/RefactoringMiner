package matchers;


import tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;



public class DefaultPriorityTreeQueue implements PriorityTreeQueue {
    private Function<Tree, Integer> priorityCalculator;
    private final Int2ObjectSortedMap<List<Tree>> trees;
    private int minimumPriority;

    public DefaultPriorityTreeQueue(Tree root, int minimumPriority, Function<Tree, Integer> priorityCalculator) {
        this.trees = new Int2ObjectRBTreeMap<>();
        this.setMinimumPriority(minimumPriority);
        this.setPriorityCalculator(priorityCalculator);
        add(root);
    }

    @Override
    public List<Tree> popOpen() {
        List<Tree> pop = pop();
        for (Tree t: pop)
            open(t);
        return pop;
    }

    @Override
    public void setPriorityCalculator(Function<Tree, Integer> priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    @Override
    public List<Tree> pop() {
        return trees.remove(currentPriority());
    }

    @Override
    public void open(Tree tree) {
        for (Tree c: tree.getChildren())
            if (!c.getType().name.equals("Block"))
                add(c);
    }

    @Override
    public int currentPriority() {
        return trees.lastIntKey();
    }

    @Override
    public void setMinimumPriority(int minimumPriority) {
        this.minimumPriority = minimumPriority;
    }

    @Override
    public int getMinimumPriority() {
        return this.minimumPriority;
    }

    @Override
    public boolean isEmpty() {
        return trees.isEmpty();
    }

    @Override
    public void clear() {
        trees.clear();
    }

    private void add(Tree t) {
        int priority = priorityCalculator.apply(t);
        if (priority < this.getMinimumPriority()) {
            return;
        }

        trees.putIfAbsent(priority, new ArrayList<>());
        trees.get(priority).add(t);
    }
}





