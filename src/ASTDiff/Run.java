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
        String url = null;
        url = "https://github.com/pouryafard75/TestCases/commit/e6095071d214cd73c05a65ac2d122da54d00db7b";
        url = "https://github.com/pouryafard75/TestCases/commit/3eedc184815d0618239abe00bb88816d71da3313";
        url = "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        ProjectASTDiff projectASTDiff = new ProjectASTDiffer(url).diff();
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();

        System.out.println();

    }
}

