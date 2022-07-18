

package tree;

/**
 * Class containing several metrics information regarding a node of an AST.
 * The metrics are immutables but lazily computed.
 *
 * @see Tree#getMetrics()
 */
public class TreeMetrics {
    /**
     * The number of nodes in the subtree rooted at the node.
     */
    public final int size;

    /**
     * The size of the longer branch in the subtree rooted at the node.
     */
    public final int height;

    /**
     * The hashcode of the subtree rooted at the node.
     */
    public final int hash;

    /**
     * The hashcode of the subtree rooted at the node, excluding labels.
     */
    public final int structureHash;

    /**
     * The number of ancestors of a node.
     */
    public final int depth;

    /**
     * An absolute position for the node. Usually computed via the postfix order.
     */
    public final int position;

    public TreeMetrics(int size, int height, int hash, int structureHash, int depth, int position) {
        this.size = size;
        this.height = height;
        this.hash = hash;
        this.structureHash = structureHash;
        this.depth = depth;
        this.position = position;
    }
}
