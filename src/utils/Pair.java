package utils;

/**
 * A class to store immutable pairs of objects.
 * @param <T1> the type of the first object.
 * @param <T2> the type of the second object.
 */
public class Pair<T1, T2> {
    /**
     * The first object.
     */
    public final T1 first;

    /**
     * The second object.
     */
    public final T2 second;

    /**
     * Instantiate a pair between the given left and right objects.
     */
    public Pair(T1 a, T2 b) {
        this.first = a;
        this.second = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;
        return first.equals(pair.first) && second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 33 * result + second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s", first.toString(), second.toString());
    }
}
