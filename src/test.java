import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class test {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\IdeaProjects\\TestCases\\NewCases\\RenameMethod\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\IdeaProjects\\TestCases\\NewCases\\RenameMethod\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();

        ASTDiffer astDiffer = new ASTDiffer(modelDiff);
        ASTDiff astDiff = astDiffer.diff();


        System.out.println("end");
    }
}

class ASTDiffer
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
        CompilationUnit firstcomp = comps.first;
        CompilationUnit secondcomp = comps.second;
        List<UMLOperationBodyMapper> umlOperationBodyMapperList = classdiff.getOperationBodyMapperList();
        UMLOperationBodyMapper temp = umlOperationBodyMapperList.get(1);
        LocationInfo locationInChild =  temp.getContainer1().getLocationInfo();
        LocationInfo locationInParent  =  temp.getContainer2().getLocationInfo();
        ASTNode firstAST = NodeFinder.perform(firstcomp,locationInParent.getStartOffset(),locationInParent.getLength());
        ASTNode secondAST = NodeFinder.perform(secondcomp,locationInChild.getStartOffset(),locationInChild.getLength());
        System.out.println("sa");

//        for (UMLOperationBodyMapper umloperationBodyMapper : obml)
//            System.out.println("ola");
////            new NodeFinder(first,0,0);
        }

    private Pair<CompilationUnit, CompilationUnit> findComps(UMLClassDiff classDiff) {
        //TODO: find corresponding comps
        Pair<CompilationUnit, CompilationUnit> p = new Pair<>();
        p.setPair(this.umlModelDiff.getChildModel().getCompilationUnitMap().values().iterator().next(),
                    this.umlModelDiff.getParentModel().getCompilationUnitMap().values().iterator().next());
        return p;
    }
}


class ASTDiff
{
    List<ASTNode> unms6tcchef1;
    List<ASTNode> matcherdf;
    List<ASTNode> unms6tcchef2;

}

class Pair<F,S>
{
    F first;
    S second;
    public void setPair(F f, S s)
    {
        this.first = f;
        this.second = s;
    }
    Pair(){}
    Pair(F f, S s)
    {
        this.first = f;
        this.second = s;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}
