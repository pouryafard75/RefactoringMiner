package matchers;

import tree.Tree;

import java.util.HashSet;
import java.util.Set;

public class SimilarityMetrics {
    private SimilarityMetrics() {}

    public static double chawatheSimilarity(Tree src, Tree dst, MappingStore mappings) {
        int max = Math.max(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfMappedDescendants(src, dst, mappings) / (double) max;
    }

    public static double overlapSimilarity(Tree src, Tree dst, MappingStore mappings) {
        int min = Math.min(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfMappedDescendants(src, dst, mappings) / (double) min;
    }

    public static double diceSimilarity(Tree src, Tree dst, MappingStore mappings) {
        return diceCoefficient(numberOfMappedDescendants(src, dst, mappings),
                src.getDescendants().size(), dst.getDescendants().size());
    }
    public static double diceSimilarity(Tree src, Tree dst, MultiMappingStore mappings) {
        return diceCoefficient(numberOfMappedDescendants(src, dst, mappings),
                src.getDescendants().size(), dst.getDescendants().size());
    }

    public static double jaccardSimilarity(Tree src, Tree dst, MappingStore mappings) {
        return jaccardIndex(numberOfMappedDescendants(src, dst, mappings),
                src.getDescendants().size(), dst.getDescendants().size());
    }

    public static double diceCoefficient(int commonElementsNb, int leftElementsNb, int rightElementsNb) {
        return 2D * commonElementsNb / (leftElementsNb + rightElementsNb);
    }

    public static double jaccardIndex(int commonElementsNb, int leftElementsNb, int rightElementsNb) {
        double denominator = (leftElementsNb + rightElementsNb - commonElementsNb);
        double res = commonElementsNb / denominator;
        return res;
    }

    private static int numberOfMappedDescendants(Tree src, Tree dst, MappingStore mappings) {
        Set<Tree> dstDescendants = new HashSet<>(dst.getDescendants());
        int mappedDescendants = 0;

        for (var srcDescendant : src.getDescendants()) {
            if (mappings.isSrcMapped(srcDescendant)) {
                var dstForSrcDescendant = mappings.getDstForSrc(srcDescendant);
                if (dstDescendants.contains(dstForSrcDescendant))
                    mappedDescendants++;
            }
        }

        return mappedDescendants;
    }
    private static int numberOfMappedDescendants(Tree src, Tree dst, MultiMappingStore mappings) {
        Set<Tree> dstDescendants = new HashSet<>(dst.getDescendants());
        int mappedDescendants = 0;

        for (var srcDescendant : src.getDescendants()) {
            if (mappings.isSrcMapped(srcDescendant)) {
                var dstForSrcDescendantList = mappings.getDstForSrc(srcDescendant);
                Tree dstDescendantsTree;
                if (dstForSrcDescendantList.size() > 1)
                {
                    throw new RuntimeException();
                }
                dstDescendantsTree = mappings.getDstForSrc(srcDescendant).stream().toList().get(0);
                if (dstDescendants.contains(dstDescendantsTree))
                    mappedDescendants++;
            }
        }

        return mappedDescendants;
    }
}
