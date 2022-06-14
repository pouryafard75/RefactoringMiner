
package actions;

import ASTDiff.actions.model.*;
import actions.model.*;
import matchers.MappingStore;
import tree.Tree;

import java.util.HashMap;
import java.util.Map;

/**
 * A script generator, based upon the Chawathe algorithm,
 * that makes use of deleted and inserted subtrees actions.
 *
 * @see ChawatheScriptGenerator
 */
public class SimplifiedChawatheScriptGenerator implements EditScriptGenerator {
    @Override
    public EditScript computeActions(MappingStore ms) {
        EditScript actions = new ChawatheScriptGenerator().computeActions(ms);
        return simplify(actions);
    }

    private static EditScript simplify(EditScript actions) {
        Map<Tree, Action> addedTrees = new HashMap<>();
        Map<Tree, Action> deletedTrees = new HashMap<>();

        for (Action a: actions)
            if (a instanceof Insert)
                addedTrees.put(a.getNode(), a);
            else if (a instanceof Delete)
                deletedTrees.put(a.getNode(), a);


        for (Tree t : addedTrees.keySet()) {
            if (addedTrees.keySet().contains(t.getParent())
                    && addedTrees.keySet().containsAll(t.getParent().getDescendants()))
                actions.remove(addedTrees.get(t));
            else {
                if (t.getChildren().size() > 0 && addedTrees.keySet().containsAll(t.getDescendants())) {
                    Insert originalAction = (Insert) addedTrees.get(t);
                    TreeInsert ti = new TreeInsert(originalAction.getNode(),
                            originalAction.getParent(), originalAction.getPosition());
                    int index = actions.lastIndexOf(originalAction);
                    actions.add(index, ti);
                    actions.remove(index +  1);
                }
            }
        }

        for (Tree t : deletedTrees.keySet()) {
            if (deletedTrees.keySet().contains(t.getParent())
                    && deletedTrees.keySet().containsAll(t.getParent().getDescendants()))
                actions.remove(deletedTrees.get(t));
            else {
                if (t.getChildren().size() > 0 && deletedTrees.keySet().containsAll(t.getDescendants())) {
                    Delete originalAction = (Delete) deletedTrees.get(t);
                    TreeDelete ti = new TreeDelete(originalAction.getNode());
                    int index = actions.lastIndexOf(originalAction);
                    actions.add(index, ti);
                    actions.remove(index +  1);
                }
            }
        }

        return actions;
    }
}
