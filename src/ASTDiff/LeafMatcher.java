package ASTDiff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import matchers.Mapping;
import matchers.MultiMappingStore;
import org.eclipse.jdt.core.dom.ASTNode;
import tree.Tree;
import tree.TreeUtils;

import java.util.HashMap;
import java.util.Iterator;

public class LeafMatcher extends BasicTreeMatcher implements TreeMatcher{
    @Override
    public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
//        if (true) return;
        super.match(src,dst,abstractCodeMapping,mappingStore);
        specialCases(src,dst,abstractCodeMapping,mappingStore);
    }

    private void specialCases(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        String EXP_STATEMENT =  "ExpressionStatement";
        String VAR_DEC_STATEMENT = "VariableDeclarationStatement";
        Tree expTree,varTree;
        if (src.getType().name.equals(EXP_STATEMENT) && dst.getType().name.equals(VAR_DEC_STATEMENT))
        {
            expTree = src;
            varTree = dst;
        }
        else if (src.getType().name.equals(VAR_DEC_STATEMENT) && dst.getType().name.equals(EXP_STATEMENT))
        {
            expTree = dst;
            varTree = src;
        }
        else
        {
            //TODO : nothing for now;
            return;
        }
//        mappingStore.addMapping(expTree,varTree);
//        mappingStore.addMapping(expTree.find);
//        mappingStore.addMapping(src,dst);
    }

    private void leafMatcher(Tree src, Tree dst,MultiMappingStore mappingStore) {
//        super.match(src,dst,mappingStore);
    }
}


