

package actions;

import actions.model.*;
import actions.model.Delete;
import actions.model.Move;
import actions.model.Update;
import matchers.MappingStore;

/**
 * Partition all moved, inserted, updated or deleted nodes.
 */
public class AllNodesClassifier extends AbstractITreeClassifier {
    public AllNodesClassifier(ASTDiff diff) {
        super(diff);
    }

    @Override
    public void classify() {
        for (Action a: diff.getEditScript()) {
            if (a instanceof Delete)
                srcDelTrees.add(a.getNode());
            else if (a instanceof TreeDelete) {
                srcDelTrees.add(a.getNode());
                srcDelTrees.addAll(a.getNode().getDescendants());
            }
            else if (a instanceof Insert)
                dstAddTrees.add(a.getNode());
            else if (a instanceof TreeInsert) {
                dstAddTrees.add(a.getNode());
                dstAddTrees.addAll(a.getNode().getDescendants());
            }
            else if (a instanceof Update) {
                srcUpdTrees.add(a.getNode());
                dstUpdTrees.add(diff.mappings.getDstForSrc(a.getNode()));
            }
            else if (a instanceof Move) {
                srcMvTrees.add(a.getNode());
                srcMvTrees.addAll(a.getNode().getDescendants());
                dstMvTrees.add(diff.mappings.getDstForSrc(a.getNode()));
                dstMvTrees.addAll(diff.mappings.getDstForSrc(a.getNode()).getDescendants());
            }
        }
    }
}
