package Mappings;

public class TreeMetrics {

    public int size;

    public int height;

    public int hash;

    public int structureHash;

    public int depth;

    public int position;

    public TreeMetrics(int size, int height, int hash, int structureHash, int depth, int position) {
        this.size = size;
        this.height = height;
        this.hash = hash;
        this.structureHash = structureHash;
        this.depth = depth;
        this.position = position;
    }
}
