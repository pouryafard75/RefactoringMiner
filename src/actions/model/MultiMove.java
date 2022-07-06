package actions.model;

import tree.Tree;

import java.util.ArrayList;

public class MultiMove extends TreeAddition {
    private int groupId;
    public MultiMove(Tree node, Tree parent, int pos, int groupId) {
        super(node, parent, pos);
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }

    @Override
    public String getName() {
        return "multi-move-tree";
    }
}
