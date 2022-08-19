package ASTDiff;
import matchers.Mapping;
import matchers.MultiMappingStore;
import org.refactoringminer.api.*;
import view.webdiff.WebDiff;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        String url;
        String umlOperationBodyMapperCommitURL =  "https://github.com/tsantalis/RefactoringMiner/commit/fbd80e76c68558ba58b62311aa1c34fb38baf53a";
        String myCommit =  "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        url = myCommit;

//        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromURL(url).diff();
//        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalDirectories("D:\\TestCases\\v1" , "D:\\TestCases\\v2").diff();
//        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalFiles("D:\\TestCases\\v1\\addSharp.java" , "D:\\TestCases\\v2\\addSharp.java").diff();
        String f1 = "D:\\TestCases\\v1\\addSharp.java";
        String f2 = "D:\\TestCases\\Commit\\addSharpO.java";

        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalFiles(f1,f2).diff();
        MultiMappingStore mappings = projectASTDiff.getAstDiffMap().values().iterator().next().getMappings();

        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}
