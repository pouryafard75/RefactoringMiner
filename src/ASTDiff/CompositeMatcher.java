package ASTDiff;

import gr.uom.java.xmi.decomposition.*;
import matchers.Mapping;
import matchers.MultiMappingStore;
import tree.DefaultTree;
import tree.Tree;
import tree.TreeUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompositeMatcher extends BasicTreeMatcher implements TreeMatcher{
    Map<Tree,Tree> cpyToSrc;
    Map<Tree,Tree> cpyToDst;
    @Override
    public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        compositeMatcher(src,dst,abstractCodeMapping,mappingStore);
    }

    private void compositeMatcher(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
        CompositeStatementObject fragment1 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment1();
        CompositeStatementObject fragment2 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment2();
        cpyToSrc = new HashMap<>();
        cpyToDst = new HashMap<>();
        Tree srcFakeTree = makeFakeTree(src,fragment1,cpyToSrc);
        Tree dstFakeTree = makeFakeTree(dst,fragment2,cpyToDst);
        MultiMappingStore tempMapping = new MultiMappingStore(null,null);
        basicMatcher(srcFakeTree,dstFakeTree,tempMapping);
        for(Mapping mapping : tempMapping) {
            if (mapping.first == srcFakeTree) continue;
            mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
        }
    }

    private Tree makeFakeTree(Tree tree, CompositeStatementObject fragment, Map<Tree, Tree> cpyMap) {
        List<AbstractExpression> expressions = fragment.getExpressions();
        Tree cpy = new DefaultTree(tree);
        cpyMap.put(cpy,tree);
        for (AbstractExpression abstractExpression : expressions)
        {
            Tree expTree = Tree.findByLocationInfo(tree,abstractExpression.getLocationInfo());
            Tree expCopy =  Tree.deepCopyWithMap(expTree,cpyMap);
            cpy.addChild(expCopy);
        }
        return cpy;
    }
}
