package ASTDiff;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jdt.core.dom.CompilationUnit;
import tree.Tree;
import tree.TreeContext;
import utils.Pair;

import java.util.List;

public class ASTDiffer
{
    private UMLModelDiff umlModelDiff;
    public ASTDiffer(UMLModelDiff umlModelDiff)
    {
        this.umlModelDiff = umlModelDiff;

    }

    public ASTDiff diff()
    {
        this.commonClasses();
        return null;
    }

    private void commonClasses() {
        List<UMLClassDiff> commons = this.umlModelDiff.getCommonClassDiffList();
        for (UMLClassDiff classdiff : commons) {
            process(classdiff,findTreeContexts(classdiff));

        }
    }

    private void process(UMLClassDiff classdiff, Pair<TreeContext, TreeContext> tcs) {
        TreeContext parentTreeCTX = tcs.first;
        TreeContext childTreeCTX = tcs.second;
        List<UMLOperationBodyMapper> umlOperationBodyMapperList = classdiff.getOperationBodyMapperList();
        UMLOperationBodyMapper temp = umlOperationBodyMapperList.get(0);
        LocationInfo locationInParent =  temp.getContainer1().getLocationInfo();
        LocationInfo locationInChild  =  temp.getContainer2().getLocationInfo();
        Tree childNode = childTreeCTX.getRoot().getTreeBetweenPositions(locationInChild.getStartOffset(), locationInChild.getEndOffset());
        Tree parentNode = parentTreeCTX.getRoot().getTreeBetweenPositions(locationInParent.getStartOffset(), locationInParent.getEndOffset());



        System.out.println("Here");
//        ASTNode firstAST = NodeFinder.perform(firstcomp,locationInParent.getStartOffset(),locationInParent.getLength());
//        ASTNode secondAST = NodeFinder.perform(secondcomp,locationInChild.getStartOffset(),locationInChild.getLength());



    }

    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassDiff classDiff) {
        //TODO: find corresponding comps
        String filename = "example.java";
        return new Pair<TreeContext,TreeContext>
                (this.umlModelDiff.getParentModel().getCompilationUnitMap().get(filename),
                 this.umlModelDiff.getChildModel().getCompilationUnitMap().get(filename));

    }
}





