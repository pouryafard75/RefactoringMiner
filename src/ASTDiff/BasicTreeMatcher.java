package ASTDiff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import matchers.*;
import tree.Tree;
import tree.TreeUtils;
import tree.Type;
import utils.Pair;
import utils.SequenceAlgorithms;

import java.util.*;
import java.util.function.Function;

public class BasicTreeMatcher implements TreeMatcher {

    private static final int MIN_ACCEPTABLE_HIGHT = 0;

    @Override
    public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        basicMatcher(src, dst, mappingStore);
    }
    public void step1(Tree src,Tree dst, MultiMappingStore mappingStore)
    {
        List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings = new ArrayList<>();
        Function<Tree, Integer> HEIGHT_PRIORITY_CALCULATOR = (Tree t) -> t.getMetrics().height;
        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);
        while (PriorityTreeQueue.synchronize(srcTrees, dstTrees)) {
            var localHashMappings = new HashBasedMapper();
            localHashMappings.addSrcs(srcTrees.pop());
            localHashMappings.addDsts(dstTrees.pop());
            localHashMappings.unique().forEach(
                    (pair) -> mappingStore.addMappingRecursively(pair.first.stream().findAny().get(), pair.second.stream().findAny().get()));
            localHashMappings.ambiguous().forEach(
                    (pair) -> ambiguousMappings.add(pair));
            localHashMappings.unmapped().forEach((pair) -> {
                pair.first.forEach(tree -> srcTrees.open(tree));
                pair.second.forEach(tree -> dstTrees.open(tree));
            });
//            handleAmbiguousMappings(ambiguousMappings);
        }
    }
    public void basicMatcher(Tree src, Tree dst, MultiMappingStore mappingStore) {
        step1(src,dst,mappingStore);
        greedyMatcher(src,dst,mappingStore);
//        nonG(src,dst,mappingStore);
//        lastChanceMatch(src,dst,mappingStore);

    }

    protected void lastChanceMatch(Tree src, Tree dst,MultiMappingStore mappings) {
        lcsEqualMatching(mappings, src, dst);
        lcsStructureMatching(mappings, src, dst);
        histogramMatching(src, dst, mappings);
    }

    protected void lcsEqualMatching(MultiMappingStore mappings, Tree src, Tree dst) {
        List<Tree> unmappedSrcChildren = new ArrayList<>();
        List<Tree> unmappedDstChildren = new ArrayList<>();
        for (Tree c : src.getChildren()) {
            if (mappings.isSrcMapped(c)) {
                boolean _flag = false;
                Set<Tree> dstForSrc = mappings.getDstForSrc(c);
                for (Tree dstMapped : dstForSrc) {
                    if (dst.getDescendantsAndItself().contains(dstMapped)) {
                        _flag = true;
                        break;
                    }
                }
                if (_flag)
                    continue;
            }
            unmappedSrcChildren.add(c);
        }

        for (Tree c : dst.getChildren()) {
            if (mappings.isDstMapped(c)) {
                boolean _flag = false;
                Set<Tree> srcForDst = mappings.getSrcForDst(c);
                for (Tree srcMapped : srcForDst) {
                    if (src.getDescendantsAndItself().contains(srcMapped)) {
                        _flag = true;
                        break;
                    }
                }
                if (_flag)
                    continue;
            }
            unmappedDstChildren.add(c);
        }

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(
                unmappedSrcChildren, unmappedDstChildren);
        for (int[] x : lcs) {
            var t1 = unmappedSrcChildren.get(x[0]);
            var t2 = unmappedDstChildren.get(x[1]);
            //TODO:
            if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1),dst) && mappings.areDstsUnmapped(
                    TreeUtils.preOrder(t2),src))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void lcsStructureMatching(MultiMappingStore mappings, Tree src, Tree dst) {
        List<Tree> unmappedSrcChildren = new ArrayList<>();
        List<Tree> unmappedDstChildren = new ArrayList<>();
        for (Tree c : src.getChildren()) {
            if (mappings.isSrcMapped(c)) {
                boolean _flag = false;
                Set<Tree> dstForSrc = mappings.getDstForSrc(c);
                for (Tree dstMapped : dstForSrc) {
                    if (dst.getDescendantsAndItself().contains(dstMapped)) {
                        _flag = true;
                        break;
                    }
                }
                if (_flag)
                    continue;
            }
            unmappedSrcChildren.add(c);
        }

        for (Tree c : dst.getChildren()) {
            if (mappings.isDstMapped(c)) {
                boolean _flag = false;
                Set<Tree> srcForDst = mappings.getSrcForDst(c);
                for (Tree srcMapped : srcForDst) {
                    if (src.getDescendantsAndItself().contains(srcMapped)) {
                        _flag = true;
                        break;
                    }
                }
                if (_flag)
                    continue;
            }
            unmappedDstChildren.add(c);
        }

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(
                unmappedSrcChildren, unmappedDstChildren);
        for (int[] x : lcs) {
            var t1 = unmappedSrcChildren.get(x[0]);
            var t2 = unmappedDstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(
                    TreeUtils.preOrder(t1),dst) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2),src))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void histogramMatching(Tree src, Tree dst, MultiMappingStore mappings) {
        Map<Type, List<Tree>> srcHistogram = new HashMap<>();
        for (var c :  src.getChildren()) {
            if (mappings.isSrcMapped(c)) {
                boolean _flag = false;
                Set<Tree> dstForSrc = mappings.getDstForSrc(c);
                for (Tree dstMapped : dstForSrc) {
                    if (dst.getDescendantsAndItself().contains(dstMapped))
                    {
                        _flag = true;
                        break;
                    }
                }
                if (_flag)
                    continue;
            }
            srcHistogram.putIfAbsent(c.getType(), new ArrayList<>());
            srcHistogram.get(c.getType()).add(c);
        }

        Map<Type, List<Tree>> dstHistogram = new HashMap<>();
        for (var c : dst.getChildren()) {
            if (mappings.isDstMapped(c)) {
                boolean _flag = false;
                Set<Tree> srcForDst = mappings.getSrcForDst(c);
                for (Tree mappedSrc : srcForDst) {
                    if (src.getDescendantsAndItself().contains(mappedSrc))
                    {
                        _flag = true;
                        break;
                    }
                }
                if (_flag)
                    continue;
            }
            dstHistogram.putIfAbsent(c.getType(), new ArrayList<>());
            dstHistogram.get(c.getType()).add(c);
        }

        for (Type t : srcHistogram.keySet()) {
            if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
                var srcChild = srcHistogram.get(t).get(0);
                var dstChild = dstHistogram.get(t).get(0);
                mappings.addMapping(srcChild, dstChild);
                lastChanceMatch(srcChild, dstChild,mappings);
            }
        }
    }

    public void nonG(Tree src, Tree dst, MultiMappingStore mappings) {
        for (Tree t : src.postOrder()) {
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(t, dst,mappings);
                break;
            }
            else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
                List<Tree> candidates = getDstCandidates(mappings, t,dst);
                Tree best = null;
                var max = -1D;
                var tSize = t.getDescendants().size();

                for (var candidate : candidates) {
                    var threshold = 1D / (1D + Math.log(candidate.getDescendants().size() + tSize));
                    var sim = SimilarityMetrics.diceSimilarity(t, candidate, mappings);
                    if (sim > max && sim >= threshold) {
                        max = sim;
                        best = candidate;
                    }
                }
                if (best != null) {
                    lastChanceMatch(t, best, mappings);
                    mappings.addMapping(t, best);
                }
            }
//            else if (mappings.isSrcMapped(t) && mappings.hasUnmappedSrcChildren(t)
//                    && mappings.hasUnmappedDstChildren(mappings.getDstForSrc(t)))
//                lastChanceMatch(mappings, t, mappings.getDstForSrc(t));
        }
    }

    public void greedyMatcher(Tree src, Tree dst, MultiMappingStore mappings) {
        double simThreshold = 0.5;
        for (Tree t : src.postOrder()) {
            boolean _flag = true;
            if (mappings.isSrcMapped(t)) {
                ArrayList<Tree> temp = new ArrayList<>(mappings.getDstForSrc(t));
                for (Tree tree : temp) {
                    if (dst.getDescendantsAndItself().contains(tree)) {
                        _flag = false;
                        break;
                    }
                }
            }
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(t,dst,mappings);
                break;
            } else if (!(mappings.isSrcMapped(t) || t.isLeaf()) || (!t.isLeaf() && _flag)) {
                List<Tree> candidates = getDstCandidates(mappings, t, dst);
                Tree best = null;
                double max = -1D;
                for (Tree cand : candidates) {
                    double sim = SimilarityMetrics.diceSimilarity(t, cand, mappings);
                    if (sim > max && sim >= simThreshold) {
                        max = sim;
                        best = cand;
                    }
                }

                if (best != null) {
                    if (!mappings.isDstMapped(best)) {
                        mappings.addMapping(t, best);
                        lastChanceMatch(t, best, mappings);
                    }
                    else
                    {
                        Set<Tree> srcForDst = mappings.getSrcForDst(best);
                        boolean _check = true;
                        for (Tree srcMapped : srcForDst)
                            if (src.getDescendants().contains(srcMapped))
                            {
                                _check = false;
                                break;
                            }
                        if (_check) {
                            mappings.addMapping(t, best);
                            lastChanceMatch(t, best, mappings);
                        }
                    }
                    // TODO: 8/2/2022 Might be mapped from other trees but it must be discarded
                }
            }
        }
    }

    protected List<Tree> getDstCandidates(MultiMappingStore mappings, Tree src, Tree checkingInsideDst) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            if (mappings.isSrcMapped(c)) {
                for (Tree t : mappings.getDstForSrc(c)) {
                    if (checkingInsideDst.getDescendantsAndItself().contains(t))
                        seeds.add(t);
                }
            }
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (Tree seed : seeds) {
            while (seed.getParent() != null) {
                Tree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !parent.isRoot())
                    if (!(mappings.isDstMapped(parent)))
                        candidates.add(parent);
                    else {
                        List<Tree> mappedParent = new ArrayList<>(mappings.getSrcForDst(parent));
                        boolean flag = true;
                        for (Tree mp : mappedParent) {
                            if (checkingInsideDst.getDescendantsAndItself().contains(mp)) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag)
                            candidates.add(parent);
                    }
                seed = parent;
            }
        }

        return candidates;
    }

    private void postProcess(Pair<List<Tree>, List<Tree>> complementPair, List<Pair<Tree, Tree>> pairlist) {
        List<Tree> srcComplement = complementPair.first;
        List<Tree> dstComplement = complementPair.second;
        for (Tree srcSubTree : srcComplement) {
            Tree dstSelected = findSimilarCriteria(srcSubTree, dstComplement);
            if (dstSelected == null) continue;
            dstComplement.remove(dstSelected);
            pairlist.add(new Pair<>(srcSubTree, dstSelected));
        }
    }

    private Tree findSimilarCriteria(Tree srcSubTree, List<Tree> dstComplement) {
        String searchingType = srcSubTree.getType().name;
        List<Tree> candidates = new ArrayList<>();
        for (Tree dstSubTree : dstComplement) {
            if (dstSubTree.getType().name.equals(searchingType))
                candidates.add(dstSubTree);
        }
        if (candidates.isEmpty()) return null;
        return findTheBest(srcSubTree, candidates);
    }

    private Tree findTheBest(Tree srcSubTree, List<Tree> candidates) {
        int serachingHeight = srcSubTree.getMetrics().depth;
        if (candidates.size() == 1) return candidates.get(0);
        for (Tree dstSubTree : candidates) {
            if (serachingHeight == dstSubTree.getMetrics().depth)
                return dstSubTree;
        }
        return null;
        //TODO
    }
}


