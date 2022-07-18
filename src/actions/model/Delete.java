

package actions.model;

import tree.Tree;

public class Delete extends Action {
    public Delete(Tree node) {
        super(node);
    }

    @Override
    public String getName() {
        return "delete-node";
    }

    @Override
    public String toString() {
        return String.format("===\n%s\n---\n%s\n===",
                getName(),
                node.toString());
    }
}
