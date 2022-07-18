
package tree;

/**
 * Class dedicated to check the constraints on the ASTs.
 * The checked constraints are:
 * 1. Only leaves may have a label
 * 2. A child node test position must be contained in the position of its parent
 * 3. The text position of a given node should be strictly greater than
 *    the position of its preceding sibling (if any).
 * An instantiated tree validator should be stateless and can then be reused
 * to validate several ASTs.
 */
public class TreeValidator {

    private TreeContext context;

    /**
     * Launch the validation on the tree contained in the provided context.
     * In case of violation of a constraint, an runtime exception is raised.
     * @see TreeException
     */
    public void validate(TreeContext context) {
        this.context = context;
        validate(context.getRoot());
    }

    private void validate(Tree root) {
        for (Tree t : root.preOrder()) {
            if (!t.isLeaf()) {
                if (!t.getLabel().equals(Tree.NO_LABEL))
                    throw new TreeException(String.format("%s : %s\n%s",
                            "Inner node with label",
                            t.toString(),
                            t.toTreeString()));

                if (t.getChildren().get(0).getPos() < t.getPos())
                    throw new TreeException(String.format("%s : %s\n%s",
                            "Children begin position before node begin position",
                            t.getChildren().get(0).toString(),
                            t.toTreeString()));

                if (t.getChildren().get(t.getChildren().size() - 1).getEndPos() > t.getEndPos())
                    throw new TreeException(String.format("%s : %s\n%s",
                            "Children end position after node end position",
                            t.getChildren().get(t.getChildren().size() - 1).toString(),
                            t.toTreeString()));

                if (t.getChildren().size() > 1) {
                    for (int i = 1; i < t.getChildren().size(); i++) {
                        Tree b = t.getChild(i -  1);
                        Tree c = t.getChild(i);
                        if (c.getPos() < b.getEndPos())
                            throw new TreeException(String.format("%s : %s\n%s",
                                    "Sibling begin position before node end position",
                                    c.toString(),
                                    t.toTreeString()));
                    }
                }
            }

        }

    }

    /**
     * Exception class for errors w.r.t. to AST constraints.
     */
    public static class TreeException extends RuntimeException {
        public TreeException(String message) {
            super(message);
        }
    }

}
