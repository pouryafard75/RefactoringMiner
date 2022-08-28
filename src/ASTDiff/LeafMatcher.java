package ASTDiff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import matchers.Mapping;
import matchers.MultiMappingStore;
import tree.Tree;
import tree.TreeUtils;

import java.util.HashMap;
import java.util.Iterator;

public class LeafMatcher extends BasicTreeMatcher implements TreeMatcher{
    @Override
    public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
//        if (true) return;
        super.match(src,dst,abstractCodeMapping,mappingStore);
    }

    private void leafMatcher(Tree src, Tree dst,MultiMappingStore mappingStore) {
//        super.match(src,dst,mappingStore);
    }
}
