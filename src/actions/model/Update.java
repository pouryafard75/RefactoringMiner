

package actions.model;

import tree.Tree;

public class Update extends Action {
    private String value;

    public Update(Tree node, String value) {
        super(node);
        this.value = value;
    }

    @Override
    public String getName() {
        return "update-node";
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("===\n%s\n---\n%s\nreplace %s by %s",
                getName(),
                node.toString(),
                node.getLabel(),
                getValue());
    }

    public boolean equals(Object o) {
        if (!(super.equals(o)))
            return false;

        Update a = (Update) o;
        return value.equals(a.value);
    }
}
