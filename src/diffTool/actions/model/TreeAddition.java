
package diffTool.actions.model;

import diffTool.tree.Tree;

public abstract class TreeAddition extends TreeAction {
    protected Tree parent;

    protected int pos;

    public TreeAddition(Tree node, Tree parent, int pos) {
        super(node);
        this.parent = parent;
        this.pos = pos;
    }

    public Tree getParent() {
        return parent;
    }

    public int getPosition() {
        return pos;
    }

    @Override
    public String toString() {
        return String.format("===\n%s\n---\n%s\nto\n%s\nat %d",
                getName(),
                node.toTreeString(),
                (parent != null) ? parent.toString() : "root",
                pos);
    }

    public boolean equals(Object o) {
        if (!(super.equals(o)))
            return false;

        TreeAddition a = (TreeAddition) o;
        return parent == a.parent && pos == a.pos;
    }
}
