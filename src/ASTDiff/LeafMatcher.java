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
        leafMatcher(src,dst,mappingStore);
    }

    private void leafMatcher(Tree src, Tree dst,MultiMappingStore mappingStore) {
        Tree cpySrc = src.deepCustomCopy();
        Tree cpyDst = dst.deepCustomCopy();
        HashMap<Tree,Tree> cpyToDst = new HashMap<>();
        HashMap<Tree,Tree> cpyToSrc = new HashMap<>();
        Iterator<Tree> srcTreeIterator = TreeUtils.preOrderIterator(src);
        for (Tree copyNode: TreeUtils.preOrder(cpySrc)) {
            Tree origSrcNode = srcTreeIterator.next();
            cpyToSrc.put(copyNode, origSrcNode);
        }
        Iterator<Tree> dstTreeIterator = TreeUtils.preOrderIterator(dst);
        for (Tree copyNode: TreeUtils.preOrder(cpyDst)) {
            Tree origDstNode = dstTreeIterator.next();
            cpyToDst.put(copyNode, origDstNode);
        }
        MultiMappingStore tempMapping = new MultiMappingStore(null,null);
        basicMatcher(cpySrc,cpyDst,tempMapping);
        for(Mapping mapping : tempMapping) {
            if (mapping.first == cpySrc) continue;
            mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
        }
    }
}
