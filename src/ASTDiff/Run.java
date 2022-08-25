package ASTDiff;
import matchers.Mapping;
import matchers.MultiMappingStore;
import org.refactoringminer.api.*;
import view.webdiff.WebDiff;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {


        //Commit-level execution by url:

        String url;
//        String myCommit =  "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        String myCommit =  "https://github.com/phishman3579/java-algorithms-implementation/commit/ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9";
        url = myCommit;
        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromURL(url).diff();

        //-----------------------------------------------------------------------------------------------------------------\\

        //Directory-level execution:

        /*String dir1 = "D:\\TestCases\\v1";
        String dir2 = "D:\\TestCases\\v2";
        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalDirectories(dir1, dir2 ).diff();*/

        //-----------------------------------------------------------------------------------------------------------------\\

        //File-level execution:

        /*String file1 = "D:\\TestCases\\v1\\addSharp.java";
        String file2 = "D:\\TestCases\\v2\\addSharp.java";
        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalFiles(file1,file2).diff();*/

        //-----------------------------------------------------------------------------------------------------------------\\

        //Mappings:
        /*MultiMappingStore mappings = projectASTDiff.getAstDiffMap().values().iterator().next().getMappings();*/

        //-----------------------------------------------------------------------------------------------------------------\\

        //Visualization

        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}

