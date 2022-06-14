
package actions;
import matchers.MappingStore;
import tree.TreeContext;

/**
 * Class to facilitate the computation of diffs between ASTs.
 */
public class ASTDiff {

//    public String srcPath;
//    public String dstPath;

    public TreeContext srcTC;

    public TreeContext dstTC;

    public MappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    private EditScript editScript;

    public EditScript getEditScript() {
        return editScript;
    }

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public ASTDiff(TreeContext src, TreeContext dst,
                MappingStore mappings) {
//        this.srcPath = srcPath;
//        this.dstPath = dstPath;
        this.srcTC = src;
        this.dstTC = dst;
        this.mappings = mappings;
    }
    public void computeEditScript()
    {
        this.editScript = new SimplifiedChawatheScriptGenerator().computeActions(this.mappings);
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
