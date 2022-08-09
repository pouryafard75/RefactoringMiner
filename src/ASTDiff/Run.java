package ASTDiff;
import org.refactoringminer.api.*;
import view.webdiff.WebDiff;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        String url;
        String umlOperationBodyMapperCommitURL =  "https://github.com/tsantalis/RefactoringMiner/commit/fbd80e76c68558ba58b62311aa1c34fb38baf53a";
        String myCommit =  "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";

        url = myCommit;
        ProjectASTDiff projectASTDiff = new ProjectASTDiffer(url).diff();
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}

