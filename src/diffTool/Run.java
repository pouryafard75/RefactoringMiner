package diffTool;
import diffTool.diff.ProjectASTDiff;
import diffTool.diff.ProjectASTDiffer;
import org.refactoringminer.api.*;
import diffTool.webdiff.WebDiff;
import java.io.IOException;


public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {

        ProjectASTDiff projectASTDiff;
        //Commit-level execution by url:

        String url;

        String myCommit =  "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        String reqCommit =  "https://github.com/phishman3579/java-algorithms-implementation/commit/ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9";
//        String pedramReqCommit =  "https://github.com/rstudio/rstudio/commit/9a581e07cb6381d70f3fd9bb2055e810e2a682a9";
//        String pedramReqCommit2 =  "https://github.com/jfinal/jfinal/commit/881baed894540031bd55e402933bcad28b74ca88";
        String splitConditionExample1 =  "https://github.com/kiegroup/drools/commit/1bf2875e9d73e2d1cd3b58200d5300485f890ff5#diff-1e1bf3853477703f78c734cfb42a7cb2a2a0e55e5ac89265a9a9e73982d5438bL231-L233";
        String splitConditionExample2 =  "https://github.com/nutzam/nutz/commit/6599c748ef35d38085703cf3bd41b9b5b6af5f32#diff-d47d95ef90d7f70d88b06a02d8505b58a81c7c1174c2494e56c9b09d090bc6edL307";
        String splitConditionExample3 =  "https://github.com/graphhopper/graphhopper/commit/7f80425b6a0af9bdfef12c8a873676e39e0a04a6#diff-619f379c7f10d178fba87944b27d543e3f36a9d34803fbdd5ab964a8c9f60a86L943";
        String splitConditionExample4 =  "https://github.com/BuildCraft/BuildCraft/commit/6abc40ed4850d74ee6c155f5a28f8b34881a0284#diff-0850b8a596d522bb87b55e5a3799a56612654c4c2b7d86a8157bf2d6bb8e5e0cL353-L356";
        String splitConditionExample5 =  "https://github.com/JetBrains/intellij-community/commit/1b70adbfd49e00194c4c1170ef65e8114d7a2e46#diff-618af48f6b6713ae4367692615f2309a93d3ac39eb4453167d89ed33d0f8e35aL160";

        String working_commit = "https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938";
        String replicationCommit = "https://github.com/apache/cassandra/commit/ec52e77ecde749e7c5a483b26cbd8041f2a5a33c";

        String tempCommit = "https://github.com/pouryafard75/TestCases/commit/2a7cd0e864723e699070951b079ac33e47f01b8f";
        String pedReqCommit = "https://github.com/thundernest/k-9/commit/23c49d834d3859fc76a604da32d1789d2e863303";
        String pedReqCommit2 = "https://github.com/Netflix/eureka/commit/5103ace802b2819438318dd53b5b07512aae0d25";

        String newCommit = "https://github.com/tsantalis/RefactoringMiner/commit/77ba11175b7d3a3297be5352a512e48e2526569d";


        String sprintBootCommit = "https://github.com/spring-projects/spring-boot/commit/becced5f0b7bac8200df7a5706b568687b517b90";

        String newCase=  "https://github.com/checkstyle/checkstyle/commit/c2d3932843e70e4bb5df7161800aca248f9af778";
        String blackSq = "https://github.com/checkstyle/checkstyle/commit/1bd30555eac039d0486dffb3fc43b7a34117909b#diff-53107268c5f517054756dc73dca09db673c59ff812b31e0267a770407a723139R166";
        String pedTempReq = "https://github.com/phishman3579/java-algorithms-implementation/commit/f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29";
        String timeConsuming = "https://github.com/tsantalis/RefactoringMiner/commit/40cf1c8b96cadaa561587696932fa9c43e515252";
        String error = "https://github.com/pouryafard75/RM-ASTDiff/commit/6095e8477aeb633c5c647776cdeb22f7cdc5031b";
        String ped = "https://github.com/apache/hive/commit/e2dd54ab180b577b08cf6b0e69310ac81fc99fd3";
        String labeled = "https://github.com/apache/commons-lang/commit/fab64bbdc726cf06c5993b7b8f50557882086002?diff=split#diff-fbb057ae399c40020a2b6eab378c80b3f5e813dd5c62ba8ec1509035dc0ce7a6L2235";
        String emptyAnn = "https://github.com/apache/commons-lang/commit/fab64bbdc726cf06c5993b7b8f50557882086002?diff=split#diff-b387dedc3b4e0961c695055141002c1fd38408b66a30feec504e7625aca2b79fR86-R89";
        String pipeLine = "https://github.com/pouryafard75/TestCases/commit/55552e82e8a3bf7c231414e3996f18658580bb0d";
        url =   pipeLine;
        projectASTDiff = ProjectASTDiffer.fromURL(url,false).diff();
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
        String file1,file2;
//        String file1 = "C:\\Docs\\TestCases\\bug\\v1.java";
//        String file2 = "C:\\Docs\\TestCases\\bug\\v2.java";
//          String file1 = "D:\\TestCases\\case\\v1.java";
//          String file2 = "D:\\TestCases\\case\\v2.java";
//        String file1 = "/Users/pourya/IdeaProjects/TestCases/v1/DistributedCacheStream.java";
//        String file2 = "/Users/pourya/IdeaProjects/TestCases/v2/DistributedCacheStream.java";
        file1 = "D:\\TestCases\\case\\v1.java";
        file2 = "D:\\TestCases\\case\\v2.java";
//
//        file1 = "D:\\TestCases\\test\\test1.java";
//        file2 = "D:\\TestCases\\test\\test2.java";
//        projectASTDiff = ProjectASTDiffer.fromLocalFiles(file1,file2).diff();

        //-----------------------------------------------------------------------------------------------------------------\\

        //Mappings:
        /*MultiMappingStore mappings = projectASTDiff.getAstDiffMap().values().iterator().next().getMappings();*/

        //-----------------------------------------------------------------------------------------------------------------\\

        //Visualization

        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
    }
}

