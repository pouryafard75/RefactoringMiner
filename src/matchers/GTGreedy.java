package matchers;

import tree.Tree;

public class GTGreedy extends GTM {
    public GTGreedy(int minP) {
        super(minP);
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore m1 = new GreedySubtreeMatcher(minP).match(src,dst,mappings);
        MappingStore m2 = new GreedyBottomUpMatcher().match(src,dst,m1);
        return m2;
    }
}
