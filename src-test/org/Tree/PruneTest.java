package org.Tree;

import jdt.JdtTreeGenerator;
import org.junit.BeforeClass;
import org.junit.Test;
import tree.Tree;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PruneTest {

    private static Tree methodBlock;

    private static final String filePath = "res/Trees/pruneTest.java";

    @BeforeClass
    public static void init() throws IOException {
        Tree root = new JdtTreeGenerator().generateFrom().charset("UTF-8").file(filePath).getRoot();
        methodBlock = root.getChild(0).getChild(2).getChild(2);
        System.out.println("Initialization Completed");
    }

    @Test
    public void anonymousClass(){
        Tree testTree = methodBlock.getChild(0);
        Map<Tree,Tree> cpyMap = new HashMap<>();
        Tree res = testTree.deepCopyWithMapPruning(cpyMap);
        assertEquals("Number of objects", 9,cpyMap.size());
    }

    @Test
    public void lambdaWithBlock(){
        Tree testTree = methodBlock.getChild(1);
        Map<Tree,Tree> cpyMap = new HashMap<>();
        Tree res = testTree.deepCopyWithMapPruning(cpyMap);
        assertEquals("Number of objects", 9,cpyMap.size());
    }

    @Test
    public void lambdaWithoutBlock(){
        Tree testTree = methodBlock.getChild(2);
        Map<Tree,Tree> cpyMap = new HashMap<>();
        Tree res = testTree.deepCopyWithMapPruning(cpyMap);
        assertEquals("Number of objects", 15,cpyMap.size());
    }

}
