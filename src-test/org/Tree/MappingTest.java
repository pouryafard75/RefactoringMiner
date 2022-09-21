package org.Tree;

import ASTDiff.ProjectASTDiff;
import ASTDiff.ProjectASTDiffer;
import com.google.common.io.CharStreams;
import matchers.Mapping;
import matchers.MultiMappingStore;
import org.junit.Test;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class MappingTest {

    private final static Logger logger = LoggerFactory.getLogger(MappingTest.class);
    @Test
    public void v() throws IOException, RefactoringMinerTimedOutException {

        String childFilePath = "res/Trees/case1/parent.java";
        String parentFilePath = "res/Trees/case1/child.java";
        String mappingFilePath = "res/Trees/case1/mappings.json";
        String mappingFilePath2 = "Trees/case1/mappings.json";
        InputStream resourceAsStream = MappingTest.class.getClassLoader().getResourceAsStream(mappingFilePath2);
        assert resourceAsStream != null;
        String temp = new BufferedReader(new InputStreamReader(resourceAsStream)).lines().collect(Collectors.joining("\n"));
        ProjectASTDiff projectASTDiff = ProjectASTDiffer.fromLocalFiles(parentFilePath, childFilePath).diff();
        MultiMappingStore mappings = projectASTDiff.getAstDiffMap().values().iterator().next().getMappings();
        String actual = mappings.exportString();
//        mappings.exportToFile(new File(mappingFilePath));
        String expected = Files.readString(Path.of(mappingFilePath));
        assertEquals("Mapping vs SavedJson",expected,actual);
        System.out.println("");

    }
}
