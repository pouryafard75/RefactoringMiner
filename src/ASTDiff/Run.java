package ASTDiff;
import matchers.Mapping;
import matchers.MultiMappingStore;
import org.refactoringminer.api.*;
import view.webdiff.WebDiff;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {

        ProjectASTDiff projectASTDiff;
        //Commit-level execution by url:

        String url;
        String myCommit =  "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        String reqCommit =  "https://github.com/phishman3579/java-algorithms-implementation/commit/ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9";
//        String pedReqCommit =  "https://github.com/rstudio/rstudio/commit/9a581e07cb6381d70f3fd9bb2055e810e2a682a9";
//        String pedReqCommit2 =  "https://github.com/jfinal/jfinal/commit/881baed894540031bd55e402933bcad28b74ca88";
        url = myCommit;
//        projectASTDiff = ProjectASTDiffer.fromURL(url).diff();
//
        //-----------------------------------------------------------------------------------------------------------------\\

        //Local-repo execution:

        /*String localDir = "D:\\TestCases\\";
        String commitID = "e47272d6e1390b6366f577b84c58eae50f8f0a69";
        projectASTDiff = ProjectASTDiffer.fromLocalRepo(localDir,commitID).diff();*/

        //-----------------------------------------------------------------------------------------------------------------\\



        //-----------------------------------------------------------------------------------------------------------------\\

        //Directory-level execution:

        /*String dir1 = "D:\\TestCases\\v1";
        String dir2 = "D:\\TestCases\\v2";
        projectASTDiff = ProjectASTDiffer.fromLocalDirectories(dir1, dir2 ).diff();*/

        //-----------------------------------------------------------------------------------------------------------------\\

        //File-level execution:

//        String file1 = "C:\\Docs\\TestCases\\bug\\v1.java";
//        String file2 = "C:\\Docs\\TestCases\\bug\\v2.java";

        String file1 = "C:\\Docs\\TestCases\\sout\\parent.java";
        String file2 = "C:\\Docs\\TestCases\\sout\\child.java";
        projectASTDiff = ProjectASTDiffer.fromLocalFiles(file1,file2).diff();

        //-----------------------------------------------------------------------------------------------------------------\\

        //Mappings:
        /*MultiMappingStore mappings = projectASTDiff.getAstDiffMap().values().iterator().next().getMappings();*/

        //-----------------------------------------------------------------------------------------------------------------\\

        //Visualization

        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}

