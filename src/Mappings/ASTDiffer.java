package Mappings;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;

import java.util.ArrayList;
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
            process(classdiff,findComps(classdiff));

        }
    }

    private void process(UMLClassDiff classdiff, Pair<CompilationUnit, CompilationUnit> comps) {
        CompilationUnit firstcomp = comps.getFirst();
        CompilationUnit secondcomp = comps.getSecond();
//        List<UMLOperationBodyMapper> umlOperationBodyMapperList = classdiff.getOperationBodyMapperList();
//        UMLOperationBodyMapper temp = umlOperationBodyMapperList.get(1);
//        LocationInfo locationInChild =  temp.getContainer1().getLocationInfo();
//        LocationInfo locationInParent  =  temp.getContainer2().getLocationInfo();
//        ASTNode firstAST = NodeFinder.perform(firstcomp,locationInParent.getStartOffset(),locationInParent.getLength());
//        ASTNode secondAST = NodeFinder.perform(secondcomp,locationInChild.getStartOffset(),locationInChild.getLength());
        BaseTree myBase = new BaseTree(firstcomp);



    }

    private Pair<CompilationUnit, CompilationUnit> findComps(UMLClassDiff classDiff) {
        //TODO: find corresponding comps
        Pair<CompilationUnit, CompilationUnit> p = new Pair<>();
        p.setPair(this.umlModelDiff.getChildModel().getCompilationUnitMap().values().iterator().next(),
                this.umlModelDiff.getParentModel().getCompilationUnitMap().values().iterator().next());
        Type a  = new Type("a");
        new BaseTree(a);
        return p;
    }
}





