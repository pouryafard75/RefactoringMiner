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

//        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\IdeaProjects\\TestCases\\NewCases\\RenameMethod\\v1")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\IdeaProjects\\TestCases\\NewCases\\RenameMethod\\v2")).getUmlModel();UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\IdeaProjects\\TestCases\\NewCases\\RenameMethod\\v1")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("D:\\TestCases\\MultiMapping\\v1")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("D:\\TestCases\\MultiMapping\\v2")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("D:\\TestCases\\v1")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("D:\\TestCases\\v2")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("D:\\DebugCases\\working\\v1")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("D:\\DebugCases\\working\\v2")).getUmlModel();
//          UMLModel model1 = new UMLModelASTReader(new File("D:\\DebugCases\\cases\\v1")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("D:\\DebugCases\\cases\\v2")).getUmlModel();
//          UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\Desktop\\TestCases\\Requested\\PostProcess\\v1")).getUmlModel();
//          UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\Desktop\\TestCases\\Requested\\PostProcess\\v2")).getUmlModel();
//          UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\Desktop\\TestCases\\Requested\\DistributedCacheStream\\v1")).getUmlModel();
//          UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\Desktop\\TestCases\\Requested\\DistributedCacheStream\\v2")).getUmlModel();
          UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\OneDrive\\Desktop\\TestCases\\Requested\\Test\\v1")).getUmlModel();
          UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pouria\\OneDrive\\Desktop\\TestCases\\Requested\\Test\\v2")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("D:\\DebugCases\\sa\\cases\\v1")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("D:\\DebugCases\\sa\\cases\\v2")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("/Users/Pouria/Desktop/SimpleCase/v1/")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("/Users/Pouria/Desktop/DebugCases/Examples/cases/v1/")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("/Users/Pouria/Desktop/DebugCases/Examples/cases/v2/")).getUmlModel();
//        UMLModel model1 = new UMLModelASTReader(new File("/Users/Pouria/Desktop/DebugCases/Examples/working/v1/")).getUmlModel();
//        UMLModel model2 = new UMLModelASTReader(new File("/Users/Pouria/Desktop/DebugCases/Examples/working/v2/")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        ProjectASTDiffer projectASTDiffer = new ProjectASTDiffer(modelDiff);
        projectASTDiffer.diff();

//
        WebDiff webDiff = new WebDiff(projectASTDiffer);
        webDiff.run();


    }
}

