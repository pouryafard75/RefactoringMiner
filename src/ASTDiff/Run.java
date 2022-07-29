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
        url =  "https://github.com/pouryafard75/TestCases/commit/794576dfbd289c2ac361882ff22de3ee4aa1a36c";
        ProjectASTDiff projectASTDiff = new ProjectASTDiffer(url).diff();
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();

        System.out.println();

    }
}

