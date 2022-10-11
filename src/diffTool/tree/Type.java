

package diffTool.tree;

/**
 * Class representing the types of AST nodes. The types should be unmutable and having
 * a unique reference, that is ensured via the TypeSet class which is responsible for
 * the instantiations of types.
 * There is one unique type (the empty type) that indicates that a given AST element
 * does not have a type.
 *
 * @see TypeSet
 */
public final class Type {

    /**
     * The type name (immutable).
     */
    public final String name;

    /**
     * The empty type.
     */
    public static final Type NO_TYPE = TypeSet.type("");

    private Type(String value) {
        name = value;
    }

    /**
     * Indicates whether or not the current type is the empty type.
     */
    public boolean isEmpty() {
        return this == NO_TYPE;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    static class TypeFactory {
        protected TypeFactory() {}

        protected Type makeType(String name) {
            return new Type(name);
        }
    }
}

