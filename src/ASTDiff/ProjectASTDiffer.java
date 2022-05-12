package ASTDiff;

import actions.ASTDiff;
import actions.EditScript;
import actions.SimplifiedChawatheScriptGenerator;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
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
            ASTDiff classASTDiff = process(classdiff, findTreeContexts(classdiff));
            String fullPath = getSrcPath() + File.separator + classdiff.getOriginalClass().getSourceFile();
            this.astDiffMap.put(fullPath, classASTDiff);
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

        for(UMLOperationBodyMapper umlOperationBodyMapper : classdiff.getOperationBodyMapperList())
        {
            VariableDeclarationContainer container1 = umlOperationBodyMapper.getContainer1();
            VariableDeclarationContainer container2 = umlOperationBodyMapper.getContainer2();
            List<UMLType> commonParamsTypes = container1.commonParameterTypes(container2);
            List<UMLType> commonParamsTypes2 = container2.commonParameterTypes(container1);

//            for (UMLType umlType : commonParamsTypes) {
//                umlType.get
//
//            }
            Tree srcOperationNode = srcTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation1().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation1().getLocationInfo().getEndOffset());
            Tree dstOperationNode = dstTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation2().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation2().getLocationInfo().getEndOffset());
            mappingStore.addMapping(srcOperationNode,dstOperationNode);
            mappingStore.addListOfMapping(processMethodSignature(srcOperationNode,dstOperationNode));
//            mappingStore.addListOfMapping(firstlevelChildren(srcOperationNode,dstOperationNode));
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
                else
                {
                    Set<Replacement> replacements = abstractCodeMapping.getReplacements();
                    if (replacements.size() == 1)
                    {
                        Replacement replacement = replacements.iterator().next();
                        if (replacement.getType() == Replacement.ReplacementType.NUMBER_LITERAL)
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
            }

        }
        return new ASTDiff(treeContextPair.first, treeContextPair.second,mappingStore,new SimplifiedChawatheScriptGenerator().computeActions(mappingStore));
    }

    private List<Pair<Tree, Tree>> processMethodSignature(Tree srcOperationNode, Tree dstOperationNode) {
        List<Pair<Tree,Tree>> pairList = new ArrayList<>();
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("Modifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("Block");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
            if (matched != null)
                pairList.add(matched);
        }
        return pairList;
    }

    private List<Pair<Tree, Tree>> processClassDesc(Tree srcOperationNode, Tree dstOperationNode) {
        List<Pair<Tree,Tree>> pairList = new ArrayList<>();
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("Modifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("TYPE_DECLARATION_KIND");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
            if (matched != null)
                pairList.add(matched);
        }
        return pairList;
    }


    private Pair<Tree, Tree> matchBasedOnType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
        Tree srcModifier = TreeUtils.findChildByType(srcOperationNode,searchingType);
        Tree dstModifier = TreeUtils.findChildByType(dstOperationNode,searchingType);
        if (srcModifier != null && dstModifier != null)
            return new Pair<>(srcModifier, dstModifier);
        return null;
    }

    private List<Pair<Tree, Tree>> classDeclarationMapping(Tree srcTypeDeclartion, Tree dstTypeDeclartion) {
        List<Pair<Tree, Tree>> pairlist = processClassDesc(srcTypeDeclartion, dstTypeDeclartion);
        pairlist.add(new Pair<>(srcTypeDeclartion,dstTypeDeclartion));
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





