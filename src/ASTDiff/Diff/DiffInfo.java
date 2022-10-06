package ASTDiff.Diff;

import ASTDiff.utils.Pair;

public class DiffInfo extends Pair<String,String> {

    /**
     * Instantiate a pair between the given left and right objects.
     *
     * @param a
     * @param b
     */
    public DiffInfo(String a, String b) {
        super(a, b);
    }
    public String getInfo()
    {
        if (first.equals(second))
            return first;
        else
            return first + " --> " + second;
    }
}
