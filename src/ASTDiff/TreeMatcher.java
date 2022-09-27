package ASTDiff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import matchers.MultiMappingStore;
import tree.Tree;

public interface TreeMatcher {
    void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore);

    default void match(Tree src, Tree dst, AbstractStatement st1, AbstractStatement st2, MultiMappingStore mappingStore) {}
}
