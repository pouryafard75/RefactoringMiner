package matchers;

import java.util.*;

import tree.Tree;
import utils.Pair;

public class MappingStore implements Iterable<Mapping> {
    /**
     * The immutable root of the source AST
     */
    public final Tree src;

    /**
     * The immutable root of the destination AST
     */
    public final Tree dst;

    private Map<Tree, Tree> srcToDst;
    private Map<Tree, Tree> dstToSrc;

    public Map<Tree, Tree> getSrcToDst() {
        return srcToDst;
    }

    public Map<Tree, Tree> getDstToSrc() {
        return dstToSrc;
    }

    public void mergeMappings(MappingStore addon)
    {
        if (addon == null) return;
        for (Map.Entry<Tree,Tree> entry : addon.getSrcToDst().entrySet())
        {
            this.addMapping(entry.getKey(),entry.getValue());
        }
    }

    /**
     * Instantiate a mapping store using the mappings of the provided
     * mapping store.
     * The references to the source and destination AST are also copied
     * from the provided mapping store.
     */
    public MappingStore(MappingStore ms) {
        this(ms.src, ms.dst);
        for (Mapping m : ms)
            addMapping(m.first, m.second);
    }

    /**
     * Instantiate a new empty mapping store between the provided
     * source and destination AST.
     */
    public MappingStore(Tree src, Tree dst) {
        this.src = src;
        this.dst = dst;
        srcToDst = new HashMap<>();
        dstToSrc = new HashMap<>();
    }

    /**
     * Return the number of mappings.
     */
    public int size() {
        return srcToDst.size();
    }

    /**
     * Converts the mapping store to a set of mappings.
     */
    public Set<Mapping> asSet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Mapping> iterator() {
                Iterator<Tree> it = srcToDst.keySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Mapping next() {
                        Tree src = it.next();
                        if (src == null) return null;
                        return new Mapping(src, srcToDst.get(src));
                    }
                };
            }

            @Override
            public int size() {
                return srcToDst.keySet().size();
            }
        };
    }

    /**
     * Add a mapping between the two provided nodes inside the mapping store.
     */
    public void addMapping(Tree src, Tree dst) {
        srcToDst.put(src, dst);
        dstToSrc.put(dst, src);
    }
    public void addListOfMapping(List<Pair<Tree,Tree>> pairList)
    {
        if (pairList == null) return;
        for (Pair<Tree,Tree> pair : pairList) {
            addMapping(pair.first,pair.second);
        }
    }

    /**
     * Add a mapping between the two provided nodes inside the mapping store.
     * Mappings between all descendants of the two nodes are added as well.
     * The trees rooted at the source and destination nodes are expected to be at
     * least iso structural.
     *
     * @see Tree#isIsoStructuralTo(Tree)
     */
    public void addMappingRecursively(Tree src, Tree dst) {
        addMapping(src, dst);
        if (src.getChildren() != null)
            for (int i = 0; i < src.getChildren().size(); i++)
                addMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    public void addListOfMappingRecursively(List<Pair<Tree,Tree>> pairList) {
        if (pairList == null) return;
        for(Pair<Tree,Tree> pair : pairList)
            addPairRecursively(pair);
    }
    public void addPairRecursively(Pair<Tree,Tree> pair)
    {
        if (pair == null) return;
        addMappingRecursively(pair.first,pair.second);
    }

    /**
     * Remove the mapping between the provided source and destination nodes.
     */
    public void removeMapping(Tree src, Tree dst) {
        srcToDst.remove(src);
        dstToSrc.remove(dst);
    }

    /**
     * Return the destination source node mapped to the given source node.
     * If there is no mapping involving the source node, null is returned.
     */
    public Tree getDstForSrc(Tree src) {
        return srcToDst.get(src);
    }

    /**
     * Return the source node mapped to the given destination node.
     * If there is no mapping involving the destination node, null is returned.
     */
    public Tree getSrcForDst(Tree dst) {
        return dstToSrc.get(dst);
    }

    /**
     * Return whether or not there is a mapping for the given source node.
     */
    public boolean isSrcMapped(Tree src) {
        return srcToDst.containsKey(src);
    }


    /**
     * Return whether or not there is a mapping for the given destination node.
     */
    public boolean isDstMapped(Tree dst) {
        return dstToSrc.containsKey(dst);
    }

    /**
     * Return whether or not the given source and destination nodes are unmapped.
     */
    public boolean areBothUnmapped(Tree src, Tree dst) {
        return !(isSrcMapped(src) || isDstMapped(dst));
    }

    /**
     * Return whether or not all the given source nodes are unmapped.
     */
    public boolean areSrcsUnmapped(Collection<Tree> srcs) {
        for (Tree src : srcs)
            if (isSrcMapped(src))
                return false;

        return true;
    }

    /**
     * Return whether or not all the given destination nodes are unmapped.
     */
    public boolean areDstsUnmapped(Collection<Tree> dsts) {
        for (Tree dst : dsts)
            if (isDstMapped(dst))
                return false;

        return true;
    }

    /**
     * Return whether or not the given source node has unmapped descendants.
     */
    public boolean hasUnmappedSrcChildren(Tree t) {
        for (Tree c : t.getDescendants())
            if (!isSrcMapped(c))
                return true;

        return false;
    }

    /**
     * Return whether or not the given destination node has unmapped descendants.
     */
    public boolean hasUnmappedDstChildren(Tree t) {
        for (Tree c : t.getDescendants())
            if (!isDstMapped(c))
                return true;

        return false;
    }

    /**
     * Returns whether or not there is a mapping between the given source and
     * destination nodes.
     */
    public boolean has(Tree src, Tree dst) {
        return srcToDst.get(src) == dst;
    }

    @Override
    public Iterator<Mapping> iterator() {
        return asSet().iterator();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Mapping m : this)
            b.append(m.toString()).append('\n');
        return b.toString();
    }

    /**
     * Return whether or not a mapping is possible between the provided source and
     * destination nodes.
     */
    public boolean isMappingAllowed(Tree src, Tree dst) {
        return src.hasSameType(dst) && areBothUnmapped(src, dst);
    }

    public static List<Pair<Tree,Tree>> recursivePairings(Tree src, Tree dst, List<Pair<Tree,Tree>> pairlist)
    {
        if (pairlist == null)
            pairlist = new ArrayList<>();
        pairlist.add(new Pair<>(src,dst));
        if (src.getChildren() != null)
            for (int i = 0; i < src.getChildren().size(); i++)
                recursivePairings(src.getChild(i), dst.getChild(i),pairlist);
        return pairlist;
    }
}
