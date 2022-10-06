package ASTDiff;


import ASTDiff.Diff.ProjectASTDiff;
import ASTDiff.Diff.ProjectASTDiffer;
import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import ASTDiff.webdiff.WebDiff;

import java.io.IOException;


public class CmdRunner {
        private static final Logger logger = LogManager.getLogger(CmdRunner.class);
        private static final String appName = "RM ASTDiff Tool";
        private static final String header = "Enter Repo Url with -u option or pair of directories with -src,-dst";
        private static final String repo_url = "https://github.com/pouryafard75/RM-ASTDiff/issues";
        private static final String footer = String.format("Please report issues to : %s", repo_url);


        public static void main(String[] args) {
            CommandLineParser parser = new DefaultParser();
            Options options = createOptions();


            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);

                if (line.hasOption("help")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp(appName, header , options , footer , true);
                } else if (line.hasOption("u")) {
                    String inputUrl = line.getOptionValue("u");
                    try {
                        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromURL(inputUrl).diff();
                        WebDiff webDiff = new WebDiff(projectASTDiff);
                        webDiff.run();
                    } catch (RefactoringMinerTimedOutException e) {
                        e.printStackTrace();
                    }
                }
                else if (line.hasOption("src") && line.hasOption("dst"))
                {
                    String srcDir = line.getOptionValue("src");
                    String dstDir = line.getOptionValue("dst");
                    try {
                        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalDirectories(srcDir,dstDir).diff();
                        WebDiff webDiff = new WebDiff(projectASTDiff);
                        webDiff.run();
                    } catch (RefactoringMinerTimedOutException | IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (line.hasOption("src") && !line.hasOption("dst"))
                    throw new RuntimeException("Enter dstDir with -dst option");
                else if (!line.hasOption("src") && line.hasOption("dst"))
                    throw new RuntimeException("Enter srcDir with -src option");
                else {
                    System.out.println("repo or src/dst pair missing!");
                }
            }
            catch( ParseException exp ) {
                System.out.println( "Unexpected exception:" + exp.getMessage() );
            }
        }
    private static Options createOptions() {
        Options options = new Options();
        options.addOption(Option.builder("u").hasArg().longOpt("url").desc("the url path to the repository").build());
        options.addOption(Option.builder("r").hasArg().longOpt("repo").desc("the path to the repository").build());
        options.addOption(Option.builder("src").hasArg().longOpt("srcfile").desc("(if no repo option) the path to the source file").build());
        options.addOption(Option.builder("dst").hasArg().longOpt("dstfile").desc("(if no repo option) the path to the destination file").build());
        options.addOption(Option.builder("help").desc("print help message").build());
        return options;
    }
}