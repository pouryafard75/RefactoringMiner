
package ASTDiff.actions.model;

import ASTDiff.tree.Tree;

public class TreeDelete extends TreeAction {
    public TreeDelete(Tree node) {
        super(node);
    }

    @Override
    public String getName() {
        return "delete-tree";
    }

    @Override
    public String toString() {
        return String.format("===\n%s\n---\n%s",
                getName(),
                node.toTreeString());
    }
}
