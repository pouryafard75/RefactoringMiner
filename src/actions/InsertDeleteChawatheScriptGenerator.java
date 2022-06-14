
package actions;

import ASTDiff.actions.model.*;
import actions.model.*;
import matchers.MappingStore;
import tree.Tree;

/**
 * A script generator, based upon the simplified Chawathe script generator,
 * that replaces moved and updated nodes by inserted and deleted nodes.
 *
 * @see SimplifiedChawatheScriptGenerator
 */
public class InsertDeleteChawatheScriptGenerator implements EditScriptGenerator {
    private EditScript actions;
    private MappingStore origMappings;

    @Override
    public EditScript computeActions(MappingStore ms) {
        this.origMappings = ms;
        this.actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        return removeMovesAndUpdates();
    }

    private EditScript removeMovesAndUpdates() {
        EditScript actionsCpy = new EditScript();
        for (Action a: actions) {
            if (a instanceof Update) {
                Tree src = a.getNode();
                Tree dst = origMappings.getDstForSrc(src);
                actionsCpy.add(new Insert(
                        dst,
                        dst.getParent(),
                        dst.positionInParent()));
                actionsCpy.add(new Delete(a.getNode()));
            }
            else if (a instanceof Move) {
                Move m = (Move) a;
                Tree src = a.getNode();
                Tree dst = origMappings.getDstForSrc(src);
                actionsCpy.add(new TreeInsert(
                        dst,
                        dst.getParent(),
                        m.getPosition()));
                actionsCpy.add(new TreeDelete(a.getNode()));
            }
            else
                actionsCpy.add(a);
        }

        return actionsCpy;
    }
}
