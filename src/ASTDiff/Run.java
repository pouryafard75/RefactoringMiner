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
        url =  "https://github.com/tsantalis/RefactoringMiner/commit/56c23f776b5d67b80fa7e608379f57cd9805aae4";
//        url =  "https://github.com/belaban/JGroups/commit/f1533756133dec84ce8218202585ac85904da7c9";
//        url =  "https://github.com/tsantalis/RefactoringMiner/commit/a3160cbac6ec8a0ce1664b07010bbc617f18ad6f";
        url =  "https://github.com/tsantalis/RefactoringMiner/commit/fbd80e76c68558ba58b62311aa1c34fb38baf53a";
        url = "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        ProjectASTDiff projectASTDiff = new ProjectASTDiffer(url).diff();
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}

