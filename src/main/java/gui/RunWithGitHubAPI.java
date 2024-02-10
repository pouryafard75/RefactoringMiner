package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;


/* Created by pourya on 2022-12-26 9:30 p.m. */
public class RunWithGitHubAPI {
    public static final String REFACTORING_MINER_PATH = "/Users/pourya/IdeaProjects/RM-ASTDiff/";
    public static final String ORACLE_DIR = REFACTORING_MINER_PATH + "/src/test/resources/oracle/commits/";
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {

    String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";

    String repo = URLHelper.getRepo(url);
    String commit = URLHelper.getCommit(url);

    ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
    new WebDiff(projectASTDiff).run();

    }
}
