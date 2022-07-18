

package tree;


import io.TreeIoUtils;
import java.util.*;

public abstract class AbstractTree implements Tree {
    protected Tree parent;

    protected List<Tree> children;

    protected TreeMetrics metrics;

    @Override
    public String toString() {
        if (hasLabel())
            return String.format("%s: %s [%d,%d]",
                    getType(), getLabel(), getPos(), getEndPos());
        else
            return String.format("%s [%d,%d]",
                    getType(), getPos(), getEndPos());
    }

    @Override
    public String toTreeString() {

        return TreeIoUtils.toShortText(this).toString();
    }

    @Override
    public Tree getParent() {
        return parent;
    }

    @Override
    public void setParent(Tree parent) {
        this.parent = parent;
    }

    @Override
    public void setParentAndUpdateChildren(Tree parent) {
        if (this.parent != null)
            this.parent.getChildren().remove(this);
        this.parent = parent;
        if (this.parent != null)
            parent.getChildren().add(this);
    }

    @Override
    public List<Tree> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<Tree> children) {
        this.children = children;
        for (Tree c : children)
            c.setParent(this);
    }

    @Override
    public void addChild(Tree t) {
        children.add(t);
        t.setParent(this);
    }

    @Override
    public void insertChild(Tree t, int position) {
        children.add(position, t);
        t.setParent(this);
    }

    public TreeMetrics getMetrics() {
        if (metrics == null) {
            Tree root = this;
            if (!this.isRoot()) {
                List<Tree> parents = this.getParents();
                root = parents.get(parents.size() - 1);
            }
            TreeVisitor.visitTree(root, new TreeMetricComputer());
        }

        return metrics;
    }

    public void setMetrics(TreeMetrics metrics) {
        this.metrics = metrics;
    }

    protected static class EmptyEntryIterator implements Iterator<Map.Entry<String, Object>> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Map.Entry<String, Object> next() {
            throw new NoSuchElementException();
        }
    }
}
