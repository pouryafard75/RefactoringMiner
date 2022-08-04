package ASTDiff;
import org.refactoringminer.api.*;
import view.webdiff.WebDiff;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        String url;

        url =  "https://github.com/tsantalis/RefactoringMiner/commit/fbd80e76c68558ba58b62311aa1c34fb38baf53a";
        url = "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        url = "https://github.com/apache/cassandra/commit/ec52e77ecde749e7c5a483b26cbd8041f2a5a33c";
        url = "https://github.com/pouryafard75/TestCases/commit/79200217acd30ba626d27de59618d86b0a2e178f";
        url = "https://github.com/robovm/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07";
        url = "https://github.com/pouryafard75/TestCases/commit/970a0346232e5ee5c54637cc0f03cb74c668d8b4";
        url = "https://github.com/pouryafard75/TestCases/commit/5486652ce6eacf52f97335754d56d72f3d479bdf";
        url = "https://github.com/pouryafard75/TestCases/commit/cbe6f88cf0ad33127c857d1d90d6bc32bde28893";
        url = "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        ProjectASTDiff projectASTDiff = new ProjectASTDiffer(url).diff();
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}

