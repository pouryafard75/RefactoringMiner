package ASTDiff.matchers;

import java.util.List;

import ASTDiff.tree.Tree;

/**
 * Match the nodes using a bottom-up approach. It browses the nodes of the source
 * and destination trees using a post-order traversal, testing if two
 * selected nodes might be mapped. The two nodes are mapped if they are mappable
 * and have a similarity greater than SIM_THRESHOLD. Whenever two trees
 * are mapped, an optimal TED algorithm is applied to look for possibly forgotten
 * nodes.
 */
public class GreedyBottomUpMatcher extends AbstractBottomUpMatcher {
    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        for (Tree t : src.postOrder()) {
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(mappings, t, dst);
                break;
            }
            else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
                List<Tree> candidates = getDstCandidates(mappings, t);
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
                    lastChanceMatch(mappings, t, best);
                    mappings.addMapping(t, best);
                }
            }
        }
        return mappings;
    }
}
