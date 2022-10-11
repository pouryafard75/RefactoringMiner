package diffTool.matchers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import diffTool.tree.Tree;
//import com.google.common.collect.Sets;

public abstract class AbstractBottomUpMatcher implements Matcher {
    private static final int DEFAULT_SIZE_THRESHOLD = 1000;
    private static final double DEFAULT_SIM_THRESHOLD = 0.5;

    protected int sizeThreshold = DEFAULT_SIZE_THRESHOLD;
    protected double simThreshold = DEFAULT_SIM_THRESHOLD;

    public AbstractBottomUpMatcher() {
    }

    protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            if (mappings.isSrcMapped(c))
                seeds.add(mappings.getDstForSrc(c));
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (Tree seed : seeds) {
            while (seed.getParent() != null) {
                Tree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !(mappings.isDstMapped(parent) || parent.isRoot()))
                    candidates.add(parent);
                seed = parent;
            }
        }

        return candidates;
    }

    protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        if (src.getMetrics().size < sizeThreshold || dst.getMetrics().size < sizeThreshold) {
            Matcher m = new ZsMatcher();
            MappingStore zsMappings = m.match(src, dst, new MappingStore(src, dst));
            for (Mapping candidate : zsMappings) {
                Tree srcCand = candidate.first;
                Tree dstCand = candidate.second;
                if (mappings.isMappingAllowed(srcCand, dstCand))
                    mappings.addMapping(srcCand, dstCand);
            }
        }
    }

    public int getSizeThreshold() {
        return sizeThreshold;
    }

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public double getSimThreshold() {
        return simThreshold;
    }

    public void setSimThreshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

}