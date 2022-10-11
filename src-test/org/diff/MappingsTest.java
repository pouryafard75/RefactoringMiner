//package org.diff;
//
//import diffTool.diff.ProjectASTDiff;
//import diffTool.diff.ProjectASTDiffer;
//import diffTool.webdiff.WebDiff;
//import org.junit.Test;
//import org.refactoringminer.api.RefactoringMinerTimedOutException;
//
//public class MappingsTest {
//    static String url;
//    @Test
//    public void a() throws RefactoringMinerTimedOutException {
//        url = "https://github.com/tsantalis/RefactoringMiner/commit/77ba11175b7d3a3297be5352a512e48e2526569d";
//        ProjectASTDiff diff = ProjectASTDiffer.fromURL(url).diff();
//        new WebDiff(diff).run();
//        System.out.println();
//    }
//}
