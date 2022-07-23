package ASTDiff;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.apache.log4j.BasicConfigurator;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import view.webdiff.WebDiff;
//import view.webdiff.WebDiff;

import javax.sound.midi.Soundbank;
import java.io.File;
import java.io.IOException;
import java.util.List;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
//        String path1 = "D:\\TestCases\\v1";
//        String path2 = "D:\\TestCases\\v2";
//        String path1 = "/Users/Pouria/Desktop/DebugCases/v1";
//        String path2 = "/Users/Pouria/Desktop/DebugCases/v2";
//        String path1 = "/Users/Pouria/Desktop/DebugCases/distributed/v2";
//        String path2 = "/Users/Pouria/Desktop/DebugCases/distributed/v1";

//        long start = System.currentTimeMillis();
//        UMLModel model1 = new UMLModelASTReader(new File(path1)).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File(path2)).getUmlModel();
//        UMLModelDiff modelDiff = model1.diff(model2);
//        long refminer_execution_end = System.currentTimeMillis();
//        System.out.println("RefactoringMiner executed in " + (refminer_execution_end - start) / 1000 + "s");
//        ProjectASTDiffer projectASTDiffer = new ProjectASTDiffer(modelDiff);
//        projectASTDiffer.diff();
//        long astdiffer_execution_end = System.currentTimeMillis();
//        System.out.println("ProjectASTDiffer executed in " + (astdiffer_execution_end - refminer_execution_end) / 1000 + "s");
//
//        String path1 = "D:\\TestCases\\temp\\v1";
//        String path2 = "D:\\TestCases\\temp\\v2";
//        long start = System.currentTimeMillis();
//        UMLModel model1 = new UMLModelASTReader(new File(path1)).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File(path2)).getUmlModel();
//        UMLModelDiff modelDiff = model1.diff(model2);
//        long refminer_execution_end = System.currentTimeMillis();
//        System.out.println("RefactoringMiner executed in " + (refminer_execution_end - start) / 1000 + "s");
//        ProjectASTDiffer projectASTDiffer = new ProjectASTDiffer(modelDiff);
//        projectASTDiffer.diff();
//        long astdiffer_execution_end = System.currentTimeMillis();
//        System.out.println("ProjectASTDiffer executed in " + (astdiffer_execution_end - refminer_execution_end) / 1000 + "s");
////
//        WebDiff webDiff = new WebDiff(projectASTDiffer);
//        webDiff.run();
        String repo = "https://github.com/pouryafard75/TestCases.git";
        String commitId = "0ae8f723a59722694e394300656128f9136ef466";
        ProjectASTDiff projectASTDiff = new ProjectASTDiffer(repo, commitId).diff();
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();

        System.out.println();

    }
}

