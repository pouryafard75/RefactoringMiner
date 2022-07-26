package ASTDiff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import matchers.*;
import tree.Tree;
import utils.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class BasicTreeMatcher implements TreeMatcher {

    private static final int MIN_ACCEPTABLE_HIGHT = 0;

    @Override
    public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        basicMatcher(src, dst, mappingStore);
    }

    public void basicMatcher(Tree src, Tree dst, MultiMappingStore mappingStore) {
        Function<Tree, Integer> HEIGHT_PRIORITY_CALCULATOR = (Tree t) -> t.getMetrics().height;
        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);
        while (PriorityTreeQueue.synchronize(srcTrees, dstTrees)) {
            var localHashMappings = new HashBasedMapper();
            localHashMappings.addSrcs(srcTrees.pop());
            localHashMappings.addDsts(dstTrees.pop());
            localHashMappings.unique().forEach(
                    (pair) -> mappingStore.addMappingRecursively(pair.first.stream().findAny().get(), pair.second.stream().findAny().get()));
            localHashMappings.unmapped().forEach((pair) -> {
                pair.first.forEach(tree -> srcTrees.open(tree));
                pair.second.forEach(tree -> dstTrees.open(tree));
            });
        }
        greedyMatcher(src, dst, mappingStore);
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
                    mappings.addMapping(t, best);
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
                        List<Tree> mappedParent = mappings.getSrcForDst(parent).stream().toList();
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


