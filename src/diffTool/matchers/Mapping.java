package diffTool.matchers;

import diffTool.tree.Tree;
import diffTool.utils.Pair;

public class Mapping extends Pair<Tree, Tree> {
    public Mapping(Tree a, Tree b) {
        super(a, b);
    }
}
