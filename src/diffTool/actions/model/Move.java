

package diffTool.actions.model;

import diffTool.tree.Tree;

public class Move extends TreeAddition {
    public Move(Tree node, Tree parent, int pos) {
        super(node, parent, pos);
    }

    @Override
    public String getName() {
        return "move-tree";
    }
}
