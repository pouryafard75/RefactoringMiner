package diffTool.matchers;

import diffTool.tree.Tree;
import diffTool.utils.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class HashBasedMapper {
    private final Long2ObjectMap<Pair<Set<Tree>, Set<Tree>>> mappings;

    public HashBasedMapper() {
        mappings = new Long2ObjectOpenHashMap<>();
    }

    public void addSrcs(Collection<Tree> srcs) {
        for (Tree t : srcs)
            addSrc(t);
    }

    public void addDsts(Collection<Tree> dsts) {
        for (Tree t : dsts)
            addDst(t);
    }

    public void addSrc(Tree src) {
        mappings.putIfAbsent(src.getMetrics().hash, new Pair<>(new HashSet<>(), new HashSet<>()));
        mappings.get(src.getMetrics().hash).first.add(src);
    }

    public void addDst(Tree dst) {
        mappings.putIfAbsent(dst.getMetrics().hash, new Pair<>(new HashSet<>(), new HashSet<>()));
        mappings.get(dst.getMetrics().hash).second.add(dst);
    }

    public Stream<Pair<Set<Tree>, Set<Tree>>> unique() {
        return mappings.values().stream()
                .filter((value) -> value.first.size() == 1 && value.second.size() == 1);
    }

    public Stream<Pair<Set<Tree>, Set<Tree>>> ambiguous() {
        return mappings.values().stream()
                .filter((value) -> (value.first.size() > 1 && value.second.size() >= 1)
                        || (value.first.size() >= 1 && value.second.size() > 1));
    }

    public Stream<Pair<Set<Tree>, Set<Tree>>> unmapped() {
        return mappings.values().stream()
                .filter((value) -> value.first.size() == 0 || value.second.size() == 0);
    }

    public boolean isSrcMapped(Tree src) {
        return mappings.get(src.getMetrics().hash).second.size() > 0;
    }

    public boolean isDstMapped(Tree dst) {
        return mappings.get(dst.getMetrics().hash).first.size() > 0;
    }
}
