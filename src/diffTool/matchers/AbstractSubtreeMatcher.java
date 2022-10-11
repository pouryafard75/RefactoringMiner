package diffTool.matchers;

import java.util.*;
import java.util.function.Function;
import diffTool.tree.Tree;
import diffTool.utils.Pair;

public abstract class AbstractSubtreeMatcher {
    private static final int DEFAULT_MIN_PRIORITY = 1;
    protected int minPriority = DEFAULT_MIN_PRIORITY;

    private static final String DEFAULT_PRIORITY_CALCULATOR = "height";
    protected Function<Tree, Integer> priorityCalculator = PriorityTreeQueue
            .getPriorityCalculator(DEFAULT_PRIORITY_CALCULATOR);

    protected Tree src;
    protected Tree dst;
    protected MappingStore mappings;

    public AbstractSubtreeMatcher(int minPriority) {
        this.minPriority = minPriority;
    }


    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;

        List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings = new ArrayList<>();

        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, this.minPriority, this.priorityCalculator);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, this.minPriority, this.priorityCalculator);

        while (PriorityTreeQueue.synchronize(srcTrees, dstTrees)) {
            var localHashMappings = new HashBasedMapper();
            localHashMappings.addSrcs(srcTrees.pop());
            localHashMappings.addDsts(dstTrees.pop());

            localHashMappings.unique().forEach(
                    (pair) -> mappings.addMappingRecursively(
                            pair.first.stream().findAny().get(), pair.second.stream().findAny().get()));

            localHashMappings.ambiguous().forEach(
                    (pair) -> ambiguousMappings.add(pair));

            localHashMappings.unmapped().forEach((pair) -> {
                pair.first.forEach(tree -> srcTrees.open(tree));
                pair.second.forEach(tree -> dstTrees.open(tree));
            });
        }

        handleAmbiguousMappings(ambiguousMappings);
        return this.mappings;
    }

    public abstract void handleAmbiguousMappings(List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings);

    public int getMinPriority() {
        return minPriority;
    }

    public void setMinPriority(int minPriority) {
        this.minPriority = minPriority;
    }

}
