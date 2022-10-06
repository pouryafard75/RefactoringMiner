
package ASTDiff.actions.model;

import ASTDiff.tree.Tree;

public class TreeInsert extends TreeAddition {
    public TreeInsert(Tree node, Tree parent, int pos) {
        super(node, parent, pos);
    }

    @Override
    public String getName() {
        return "insert-tree";
    }
}
