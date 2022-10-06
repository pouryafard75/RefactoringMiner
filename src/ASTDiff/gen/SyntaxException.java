package ASTDiff.gen;

import java.io.Reader;

/**
 * A class to represent syntax error encountered by tree generators.
 */
public class SyntaxException extends RuntimeException {
    private final TreeGenerator g;
    private final Reader r;

    /**
     * Instantiate a syntax expression encountered by the provided
     * tree generator on the provided reader via the provided cause.
     */
    public SyntaxException(TreeGenerator g, Reader r, Throwable cause) {
        super(cause);
        this.g = g;
        this.r = r;
    }
}
