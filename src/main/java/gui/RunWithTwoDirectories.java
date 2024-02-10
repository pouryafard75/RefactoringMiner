package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RunWithTwoDirectories {
    public static void main(String[] args) throws IOException {
        final String projectRoot = System.getProperty("user.dir");
        String folder1 = projectRoot + "/tmp/v1/";
        String folder2 = projectRoot + "/tmp/v2/";
        folder1 = "/Users/pourya/IdeaProjects/RM-ASTDiff//src/test/resources/oracle/commits/defects4j/before/Jsoup/3";
        folder2 = "/Users/pourya/IdeaProjects/RM-ASTDiff//src/test/resources/oracle/commits/defects4j/after/Jsoup/3";


        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtDirectories(Path.of(folder1), Path.of(folder2));
//        MappingExportModel.exportToFile(
//                new File("old.json"),
//                projectASTDiff.getDiffSet().iterator().next().getAllMappings());

        new WebDiff(projectASTDiff).run();
    }
}
