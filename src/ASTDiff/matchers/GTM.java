package ASTDiff.matchers;

import ASTDiff.tree.Tree;

public abstract class GTM implements Matcher{

    int minP;
    GTM(int minP)
    {
        this.minP = minP;
    }

    public abstract MappingStore match(Tree src, Tree dst, MappingStore mappings);
}
