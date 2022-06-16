package jdt;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLComment;
import tree.Tree;
import tree.TreeContext;
import tree.TypeSet;

import java.util.ArrayList;
import java.util.List;

public class CommentVisitor {
    private List<UMLComment> umlCommentList;
    private TreeContext treeContext;
    private List<Tree> comments = new ArrayList<>();

    public List<Tree> getComments() {
        return comments;
    }

    private boolean added = false;
    public CommentVisitor(TreeContext treeContext)
    {
        this.treeContext = treeContext;
        this.umlCommentList = treeContext.getUmlCommentList();
    }

    public void addCommentToProperSubtree()
    {
        if (added) return;
        added = true;
        Tree root = treeContext.getRoot();
        for (UMLComment umlComment : umlCommentList)
        {
            int commentStartOffset = umlComment.getLocationInfo().getStartOffset();
            int commentEndffset = umlComment.getLocationInfo().getEndOffset();
            Tree target = findProperSubTree(root,commentStartOffset,commentEndffset);
            Tree commentSubTree = makeCommentSubTree(umlComment);
            target.properInsertChild(commentSubTree);
            comments.add(commentSubTree);
    }

}

    private Tree makeCommentSubTree(UMLComment umlComment) {
            tree.Type proper_type = null;
            if(umlComment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.LINE_COMMENT)) {
                proper_type = TypeSet.type("LineComment");
            }
            else if (umlComment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK_COMMENT)) {
                proper_type = TypeSet.type("BlockComment");
            }
            else
            {
                throw new RuntimeException("Error here");
            }
            tree.Type commentType = TypeSet.type("Comment");
            int commentStartOffset = umlComment.getLocationInfo().getStartOffset();
            int commentLenght = umlComment.getLocationInfo().getLength();
            Tree commentTree = treeContext.createTree(commentType,"");
            commentTree.setPos(commentStartOffset);
            commentTree.setLength(commentLenght);
            Tree leaf = treeContext.createTree(proper_type,umlComment.getText());
            leaf.setPos(commentStartOffset);
            leaf.setLength(commentLenght);
            commentTree.addChild(leaf);
            return commentTree;
    }

    private Tree findProperSubTree(Tree root, int commentStartOffset, int commentEndffset) {
        Tree res = root.getProperTreeBetweenPositions(commentStartOffset,commentEndffset);
        if (res == null) res = root;
        return res;
    }
}