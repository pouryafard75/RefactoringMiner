
package actions;

import tree.Tree;

import java.util.HashSet;

import java.util.Set;

import spark.Spark;
public abstract class AbstractITreeClassifier implements TreeClassifier {
    protected final ASTDiff diff;

    protected final Set<Tree> srcUpdTrees = new HashSet<>();

    protected final Set<Tree> dstUpdTrees = new HashSet<>();

    protected final Set<Tree> srcMvTrees = new HashSet<>();

    protected final Set<Tree> dstMvTrees = new HashSet<>();

    protected final Set<Tree> srcDelTrees = new HashSet<>();

    protected final Set<Tree> dstAddTrees = new HashSet<>();

    public AbstractITreeClassifier(ASTDiff diff) {
        this.diff = diff;
        classify();
    }

    protected abstract void classify();

    public Set<Tree> getUpdatedSrcs() {
        return srcUpdTrees;
    }

    public Set<Tree> getUpdatedDsts() {
        return dstUpdTrees;
    }

    public Set<Tree> getMovedSrcs() {
        return srcMvTrees;
    }

    public Set<Tree> getMovedDsts() {
        return dstMvTrees;
    }

    public Set<Tree> getDeletedSrcs() {
        return srcDelTrees;
    }

    public Set<Tree> getInsertedDsts() {
        return dstAddTrees;
    }
}
