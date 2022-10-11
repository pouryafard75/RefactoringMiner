package diffTool.matchers;

import java.util.*;

import diffTool.tree.Tree;
import diffTool.utils.Pair;

public class GreedySubtreeMatcher extends AbstractSubtreeMatcher {
    public GreedySubtreeMatcher(int minP) {
        super(minP);
    }

    @Override
    public void handleAmbiguousMappings(List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings) {
        MappingComparators.FullMappingComparator comparator = new MappingComparators.FullMappingComparator(mappings);
        ambiguousMappings.sort(new AmbiguousMappingsComparator());
        ambiguousMappings.forEach((pair) -> {
            List<Mapping> candidates = convertToMappings(pair);
            candidates.sort(comparator);
            candidates.forEach(mapping -> {
                if (mappings.areBothUnmapped(mapping.first, mapping.second))
                    mappings.addMappingRecursively(mapping.first, mapping.second);
            });
        });
    }

    public static final List<Mapping> convertToMappings(Pair<Set<Tree>, Set<Tree>> ambiguousMapping) {
        List<Mapping> mappings = new ArrayList<>();
        for (Tree src : ambiguousMapping.first)
            for (Tree dst : ambiguousMapping.second)
                mappings.add(new Mapping(src, dst));
        return mappings;
    }

    public static class AmbiguousMappingsComparator implements Comparator<Pair<Set<Tree>, Set<Tree>>> {
        @Override
        public int compare(Pair<Set<Tree>, Set<Tree>> m1, Pair<Set<Tree>, Set<Tree>> m2) {
            int s1 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
            int s2 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
            return Integer.compare(s2, s1);
        }
    }
}
