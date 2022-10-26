package diffTool.tests;


import diffTool.diff.ProjectASTDiff;
import diffTool.diff.ProjectASTDiffer;
import diffTool.webdiff.WebDiff;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.util.List;

public class ReplaceLoopWithPipelineTest {
    private static String[] urls;
    @BeforeClass
    public static void init()
    {
        urls = new String[]{
                "https://github.com/checkstyle/checkstyle/commit/de022d2434e58dd633fd50a7f9bb50565a8767b5", //0
                "https://github.com/junit-team/junit5/commit/6b575f2ee5f02288a774ff0a85ce3a3e3cb6946f" //1
        };
    }
    @Test
    public void test1() throws RefactoringMinerTimedOutException {
        String url = urls[1];
        ProjectASTDiff diff = ProjectASTDiffer.fromURL(url).diff();
        new WebDiff(diff).run();
        while(true) { }
    }
    @Test
    public void test2()
    {
        System.out.println("test2");
    }
}
