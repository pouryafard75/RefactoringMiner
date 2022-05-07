package Mappings;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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



