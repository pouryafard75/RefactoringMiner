

package actions.model;

import tree.Tree;

public abstract class Action {
    protected Tree node;

    public Action(Tree node) {
        this.node = node;
    }

    public Tree getNode() {
        return node;
    }

    public void setNode(Tree node) {
        this.node = node;
    }

    public abstract String getName();

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        if (o.getClass() != this.getClass())
            return false;

        return node == ((Action) o).node;
    }
}
