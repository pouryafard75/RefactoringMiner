package diffTool.tests;


import diffTool.diff.ProjectASTDiff;
import diffTool.diff.ProjectASTDiffer;
import diffTool.webdiff.WebDiff;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import junit.runner.Version;
import java.util.List;

public class ReplaceLoopWithPipelineTest {
    private static String[] urls;
    @BeforeClass
    public static void init()
    {
        urls = new String[]{
                "https://github.com/checkstyle/checkstyle/commit/de022d2434e58dd633fd50a7f9bb50565a8767b5", //0
                "https://github.com/junit-team/junit5/commit/6b575f2ee5f02288a774ff0a85ce3a3e3cb6946f", //1
                "https://github.com/pouryafard75/TestCases/commit/7ad7095e47abc37587342bbe873bd1f177b87acb", // 2
                "https://github.com/pouryafard75/TestCases/commit/a7288d93ff44f045a97568af62135b8ca7a5d0e3" // 3
        };
    }

    @Test
    public void test1() throws RefactoringMinerTimedOutException {
        String url = urls[0];
        ProjectASTDiff diff = ProjectASTDiffer.fromURL(url).diff();
        new WebDiff(diff).run();
        System.out.println("");
        while(true) { }
    }
    @Test
    public void test2() throws RefactoringMinerTimedOutException {
        String url = "https://github.com/pouryafard75/TestCases/commit/9bfec59746de9bf6bc783aab5c68aa5ab8bbdd6d";
        ProjectASTDiff diff = ProjectASTDiffer.fromURL(url).diff();
        new WebDiff(diff).run();
        System.out.println("");
        while(true) { }
    }
}
