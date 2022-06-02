

package actions;

import actions.model.*;
import actions.model.Delete;
import tree.Tree;

import java.util.HashSet;
import java.util.Set;

/**
 * Partition only root (of a complete subtree) moved, inserted, updated or deleted nodes.
 */
public class OnlyRootsClassifier extends AbstractITreeClassifier {
    public OnlyRootsClassifier(ASTDiff diff) {
        super(diff);
    }

    @Override
    public void classify() {
        Set<Tree> insertedDsts = new HashSet<>();
        for (Action a: diff.getEditScript())
            if (a instanceof Insert)
                insertedDsts.add(a.getNode());

        Set<Tree> deletedSrcs = new HashSet<>();
        for (Action a: diff.getEditScript())
            if (a instanceof Delete)
                deletedSrcs.add(a.getNode());

        for (Action a: diff.getEditScript()) {
            if (a instanceof TreeDelete)
                srcDelTrees.add(a.getNode());
            else if (a instanceof Delete) {
                if (!(deletedSrcs.containsAll(a.getNode().getDescendants())
                        && deletedSrcs.contains(a.getNode().getParent())))
                    srcDelTrees.add(a.getNode());
            }
            else if (a instanceof Insert) {
                if (!(insertedDsts.containsAll(a.getNode().getDescendants())
                        && insertedDsts.contains(a.getNode().getParent())))
                    dstAddTrees.add(a.getNode());
            }
            else if (a instanceof TreeInsert )
                dstAddTrees.add(a.getNode());
            else if (a instanceof Update) {
                srcUpdTrees.add(a.getNode());
                dstUpdTrees.add(diff.mappings.getDstForSrc(a.getNode()));
            }
            else if (a instanceof Move) {
                srcMvTrees.add(a.getNode());
                dstMvTrees.add(diff.mappings.getDstForSrc(a.getNode()));
            }
        }
    }
}
