package ASTDiff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import matchers.MultiMappingStore;
import tree.Tree;

public interface TreeMatcher {
    void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore);
}
