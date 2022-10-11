

package ASTDiff.actions.model;
import ASTDiff.tree.Tree;

public abstract class Addition extends Action {
    protected Tree parent;

    protected int pos;

    public Addition(Tree node, Tree parent, int pos) {
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
                node.toString(),
                (parent != null) ? parent.toString() : "root",
                pos);
    }

    public boolean equals(Object o) {
        if (!(super.equals(o)))
            return false;

        Addition a = (Addition) o;
        return parent == a.parent && pos == a.pos;
    }
}
