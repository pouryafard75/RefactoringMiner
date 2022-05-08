package ASTDiff;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;

public class BaseTree{
    private Type type;
    private BaseTree parent;
    private int pos;
    private int length;
    private TreeMetrics treeMetrics;
    private ArrayList<BaseTree> children = new ArrayList<>();
    ASTVisitor astVisitor = new ASTVisitor() {

    };
    BaseTree(Type type) {
        this.type = type;
    }

    BaseTree(ASTNode astNode)
    {
//        this.parent = astNode.getParent()

        System.out.println("test");
    }
}



