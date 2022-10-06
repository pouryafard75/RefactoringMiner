package ASTDiff.matchers;

import ASTDiff.tree.Tree;
import ASTDiff.utils.Pair;

public class Mapping extends Pair<Tree, Tree> {
    public Mapping(Tree a, Tree b) {
        super(a, b);
    }
}
