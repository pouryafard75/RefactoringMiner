

package diffTool.actions.model;

import diffTool.tree.Tree;

public class Insert extends Addition {
    public Insert(Tree node, Tree parent, int pos) {
        super(node, parent, pos);
    }

    @Override
    public String getName() {
        return "insert-node";
    }
}
