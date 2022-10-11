package ASTDiff.matchers;

import ASTDiff.tree.Tree;

public class GTSimple extends GTM{

    int minP;
    public GTSimple(int minP)
    {
        super(minP);
    }
    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore m1 = new GreedySubtreeMatcher(minP).match(src,dst,mappings);
        MappingStore m2 = new SimpleBottomUpMatcher().match(src,dst,m1);
        return m2;
    }
}

