package Mappings;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import java.io.File;
import java.io.IOException;
import java.util.List;


public class Run {
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

