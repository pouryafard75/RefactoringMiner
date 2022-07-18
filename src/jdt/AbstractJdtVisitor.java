

package jdt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import tree.Type;
import org.eclipse.jdt.core.dom.*;
import tree.Tree;
import tree.TreeContext;
import static tree.TypeSet.type;

public abstract class AbstractJdtVisitor extends ASTVisitor {

    protected TreeContext context = new TreeContext();

    protected Deque<Tree> trees = new ArrayDeque<>();

    public AbstractJdtVisitor() {
        super(true);
    }

    public TreeContext getTreeContext() {
        return context;
    }

    protected void pushNode(ASTNode n, String label) {
        push(n, nodeAsSymbol(n), label, n.getStartPosition(), n.getLength());
    }

    protected void pushNode(ASTNode n, Type type, int pos, int length) {
        push(n, type, "", pos, length);
    }

    protected void pushFakeNode(EntityType n, int startPosition, int length) {
        Type type = type(n.name()); // FIXME is that consistent with AbstractJDTVisitor.type
        push(null, type,"", startPosition, length);
    }

    protected void push(ASTNode n, Type type, String label, int startPosition, int length) {
        Tree t = context.createTree(type, label);
        t.setPos(startPosition);
        t.setLength(length);

        if (trees.isEmpty())
            context.setRoot(t);
        else {
            Tree parent = trees.peek();
            t.setParentAndUpdateChildren(parent);
        }

        if (n instanceof TypeDeclaration)
            t.setMetadata("id", getId((TypeDeclaration) n));
        else if (n instanceof MethodDeclaration)
            t.setMetadata("id", getId((MethodDeclaration) n));
        else if (n instanceof FieldDeclaration)
            t.setMetadata("id", getId((FieldDeclaration) n));
        else if (n instanceof EnumDeclaration)
            t.setMetadata("id", getId((EnumDeclaration) n));

        trees.push(t);
    }

    private String getId(TypeDeclaration d) {
        return "Type " + d.getName();
    }

    private String getId(EnumDeclaration d) {
        return "Enum " + d.getName();
    }

    private String getId(MethodDeclaration d) {
        StringBuilder b = new StringBuilder();
        b.append("Method ");
        b.append(d.getName() + "(");
        for (SingleVariableDeclaration v : (List<SingleVariableDeclaration>) d.parameters())
            b.append(" " + v.getType().toString());
        b.append(")");
        return b.toString();
    }

    private String getId(FieldDeclaration d) {
        return "Field " + ((VariableDeclarationFragment) d.fragments().get(0)).getName();
    }

    protected Tree getCurrentParent() {
        return trees.peek();
    }

    protected void popNode() {
        trees.pop();
    }

    protected static Type nodeAsSymbol(ASTNode node) {
        return nodeAsSymbol(node.getNodeType());
    }

    protected static Type nodeAsSymbol(int id) {
        return type(ASTNode.nodeClassForType(id).getSimpleName());
    }
}
