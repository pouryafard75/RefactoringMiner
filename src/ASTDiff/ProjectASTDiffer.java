package ASTDiff;

import actions.ASTDiff;
import actions.EditScript;
import actions.SimplifiedChawatheScriptGenerator;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import matchers.Mapping;
import matchers.MappingStore;
import tree.Tree;
import tree.TreeContext;
import tree.TreeUtils;
import utils.Pair;


import java.io.File;
import java.util.*;

public class ProjectASTDiffer
{
    private Map<String, ASTDiff> astDiffMap = new HashMap<>();
    private UMLModelDiff umlModelDiff;
    private String srcPath;
    private String dstPath;

    public String getSrcPath(){
        return srcPath;
    }
    public String getDstPath(){
        return dstPath;
    }

    public UMLModelDiff getUmlModelDiff() {
        return umlModelDiff;
    }

    public ProjectASTDiffer(UMLModelDiff umlModelDiff)
    {
        this.umlModelDiff = umlModelDiff;
        this.srcPath = this.umlModelDiff.getParentModel().rootFolder.getAbsolutePath();
        this.dstPath = this.umlModelDiff.getChildModel().rootFolder.getAbsolutePath();

    }

    public void diff()
    {
        this.commonClasses();
    }
    public ASTDiff getASTDiffbyFileName(String filename)
    {
        return this.astDiffMap.get(filename);
    }

    private void commonClasses() {
        List<UMLClassDiff> commons = this.umlModelDiff.getCommonClassDiffList();

        for (UMLClassDiff classdiff : commons) {
            this.astDiffMap.put(getSrcPath() + File.separator +  classdiff.getOriginalClass().getSourceFile(), process(classdiff,findTreeContexts(classdiff)));
        }

    }

    private ASTDiff process(UMLClassDiff classdiff, Pair<TreeContext, TreeContext> treeContextPair)
    {


        Tree srcTree = treeContextPair.first.getRoot();
        Tree dstTree = treeContextPair.second.getRoot();

        MappingStore mappingStore = new MappingStore(srcTree,dstTree);
        mappingStore.addMapping(srcTree,dstTree);

        List<Pair<Tree, Tree>> classDecMapping = classDeclarationMapping(
                srcTree.getTreeBetweenPositions
                        (classdiff.getOriginalClass().getLocationInfo().getStartOffset(), classdiff.getOriginalClass().getLocationInfo().getEndOffset()),
                dstTree.getTreeBetweenPositions
                        (classdiff.getNextClass().getLocationInfo().getStartOffset(), classdiff.getNextClass().getLocationInfo().getEndOffset()));

        mappingStore.addListOfMapping(classDecMapping);

        for(UMLOperation umlOperation : classdiff.getAddedOperations())
        {
            Tree addedOperationNode = dstTree.getTreeBetweenPositions(umlOperation.getLocationInfo().getStartOffset(),umlOperation.getLocationInfo().getEndOffset());
            System.out.println("ol");
        }
        for(UMLOperationBodyMapper umlOperationBodyMapper : classdiff.getOperationBodyMapperList())
        {

            Tree srcOperationNode = srcTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation1().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation1().getLocationInfo().getEndOffset());
            Tree dstOperationNode = dstTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation2().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation2().getLocationInfo().getEndOffset());
            mappingStore.addMapping(srcOperationNode,dstOperationNode);
            mappingStore.addListOfMapping(firstlevelChildren(srcOperationNode,dstOperationNode));
            Set<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();
            for (AbstractCodeMapping abstractCodeMapping : mappings)
            {
                if (abstractCodeMapping.getReplacements().isEmpty())
                {
                    int srcStartOffset = abstractCodeMapping.getFragment1().getLocationInfo().getStartOffset();
                    int srcEndOffset = abstractCodeMapping.getFragment1().getLocationInfo().getEndOffset();

                    int dstStartOffset = abstractCodeMapping.getFragment2().getLocationInfo().getStartOffset();
                    int dstEndOffset = abstractCodeMapping.getFragment2().getLocationInfo().getEndOffset();

                    Tree srcStatementNode = srcTree.getTreeBetweenPositions(srcStartOffset,srcEndOffset);
                    Tree dstStatementNode = dstTree.getTreeBetweenPositions(dstStartOffset,dstEndOffset);
                    mappingStore.addMappingRecursively(srcStatementNode,dstStatementNode);
                }
            }

        }
        return new ASTDiff(treeContextPair.first, treeContextPair.second,mappingStore,new SimplifiedChawatheScriptGenerator().computeActions(mappingStore));
    }

    private List<Pair<Tree, Tree>> classDeclarationMapping(Tree srcTypeDeclartion, Tree dstTypeDeclartion) {
        System.out.println("ol");
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        pairlist.add(new Pair<>(srcTypeDeclartion,dstTypeDeclartion));
        for (int i = 0; i < 3; i++) {
            pairlist.add(new Pair<>(srcTypeDeclartion.getChildren().get(i),dstTypeDeclartion.getChildren().get(i)));
        }
        return pairlist;
    }


    private List<Pair<Tree, Tree>> firstlevelChildren(Tree srcOperationNode, Tree dstOperationNode) {
        List<Pair<Tree,Tree>> pairList = new ArrayList<>();
        List<Tree> srcChildren =  srcOperationNode.getChildren();
        List<Tree> dstChildren = dstOperationNode.getChildren();
        for (int i = 0; i < srcChildren.size(); i++) {
            pairList.add(new Pair<>(srcChildren.get(i),dstChildren.get(i)));
        }
        return pairList;
    }

    private List<Pair<Tree, Tree>> matchAllNodesInside(Tree srcStatementNode, Tree dstStatementNode) {
        Iterator<Tree> srcTreeIterator = TreeUtils.breadthFirstIterator(srcStatementNode);
        List<Tree> srcTreeList = new ArrayList<Tree>();
        srcTreeIterator.forEachRemaining(srcTreeList::add);

        Iterator<Tree> dstTreeIterator = TreeUtils.breadthFirstIterator(dstStatementNode);
        List<Tree> dstTreeList = new ArrayList<Tree>();
        dstTreeIterator.forEachRemaining(dstTreeList::add);

        List<Pair<Tree,Tree>> mappinglist = new ArrayList<Pair<Tree, Tree>>();

        for (int i = 0; i < srcTreeList.size(); i++) {
            mappinglist.add(new Pair<Tree,Tree>(srcTreeList.get(i),dstTreeList.get(i)));
        }
            return mappinglist;
    }


    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassDiff classDiff) {
        //TODO: find corresponding comps

//        String filename = rootpath + File.separator + classDiff.getOriginalClass().getSourceFile();
        String filename = classDiff.getOriginalClass().getSourceFile();
        return new Pair<TreeContext,TreeContext>
                (this.umlModelDiff.getParentModel().getTreeContextMap().get(filename),
                 this.umlModelDiff.getChildModel().getTreeContextMap().get(filename));
    }
}





