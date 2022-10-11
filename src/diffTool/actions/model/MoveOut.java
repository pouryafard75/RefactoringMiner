package diffTool.actions.model;

import diffTool.tree.Tree;

public class MoveOut extends TreeAddition {
    private String dstFile;

    public MoveOut(Tree node, Tree parent, String dstfile, int pos) {
        super(node, parent, pos);
        this.dstFile = dstfile;
    }

    public String getDstFile() {
        return dstFile;
    }

    @Override
    public String getName() {
        return "M";
    }

    @Override
    public String toString()  {

        return "Moved to File: " + getDstFile();
    }
}
