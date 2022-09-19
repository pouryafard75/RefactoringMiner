package matchers;

import tree.Tree;
public interface Matcher {
    /**
     * Compute and return the mappings between a provided src and dst AST.
     * The mappings are added as a side effect in the provided mapping store.
     * The provided mapping store is return as well to allow chaining.
     */
    MappingStore match(Tree src, Tree dst, MappingStore mappings);

    /**
     * Utility method that compute and return the mappings between a provided
     * src and dst AST in a new mapping store.
     *
     * @see #match(Tree, Tree, MappingStore)
     */
    default MappingStore match(Tree src, Tree dst) {
        return match(src, dst, new MappingStore(src, dst));
    }
}
