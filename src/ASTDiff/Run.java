package ASTDiff;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.apache.log4j.BasicConfigurator;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import view.webdiff.WebDiff;
//import view.webdiff.WebDiff;

import java.io.File;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        BasicConfigurator.configure();

        String path1 = "D:\\TestCases\\Requested\\Pullup\\v2";
        String path2 = "D:\\TestCases\\Requested\\Pullup\\v1";
        UMLModel model1 = new UMLModelASTReader(new File(path1)).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File(path2)).getUmlModel();


        UMLModelDiff modelDiff = model1.diff(model2);
        ProjectASTDiffer projectASTDiffer = new ProjectASTDiffer(modelDiff);
        projectASTDiffer.diff();

//
        WebDiff webDiff = new WebDiff(projectASTDiffer);
        webDiff.run();

    }
}

