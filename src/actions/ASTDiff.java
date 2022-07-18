
package actions;
import gr.uom.java.xmi.diff.UMLClassDiff;
import matchers.MappingStore;
import tree.Tree;
import tree.TreeContext;

import java.io.IOException;

/**
 * Class to facilitate the computation of diffs between ASTs.
 */
public class ASTDiff {

//    public String srcPath;
//    public String dstPath;

    public final TreeContext srcTC;

    public final TreeContext dstTC;

    public final MappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    public final EditScript editScript;

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public ASTDiff(TreeContext src, TreeContext dst,
                MappingStore mappings, EditScript editScript) {
//        this.srcPath = srcPath;
//        this.dstPath = dstPath;
        this.srcTC = src;
        this.dstTC = dst;
        this.mappings = mappings;
        this.editScript = editScript;
    }
    /**
     * Compute and return a all node classifier that indicates which node have
     * been added/deleted/updated/moved.
     */
    public TreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    /**
     * Compute and return a root node classifier that indicates which node have
     * been added/deleted/updated/moved. Only the root note is marked when a whole
     * subtree has been subject to a same operation.
     */
    public TreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
