package ASTDiff;

import actions.ASTDiff;
import actions.EditScript;
import actions.SimplifiedChawatheScriptGenerator;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import matchers.Mapping;
import matchers.MappingStore;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
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

    public ProjectASTDiffer(UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
        this.umlModelDiff = umlModelDiff;
        umlModelDiff.getRefactorings();
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

        mappingStore.addPairRecursively(processPackageDeclaration(srcTree,dstTree,classdiff));
        mappingStore.addListOfMappingRecursively(processImports(srcTree,dstTree,classdiff.getImportDiffList().getCommonImports()));

        Tree srcClassTree = findByLocationInfo(srcTree,classdiff.getOriginalClass().getLocationInfo(),"TypeDeclaration");
        Tree dstClassTree = findByLocationInfo(dstTree,classdiff.getNextClass().getLocationInfo(),"TypeDeclaration");
        mappingStore.addListOfMapping(classDeclarationMapping(srcClassTree,dstClassTree));
        mappingStore.addPairRecursively(processSuperClass(srcTree,dstTree,classdiff));
        mappingStore.addListOfMappingRecursively(processClassImplemetedInterfaces(srcTree,dstTree,classdiff));
        mappingStore.addListOfMapping(processClassAttributes(srcTree,dstTree,classdiff));


        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : classdiff.getAnnotationListDiff().getCommonAnnotations()) {
            Tree srcClassAnnotationTree = findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
        }



        List<UMLOperationBodyMapper> operationBodyMapperList = classdiff.getOperationBodyMapperList();

        for(UMLOperationBodyMapper umlOperationBodyMapper : operationBodyMapperList)
        {
            Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();
            for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: umlOperationBodyMapper.getMatchedVariables()) {
                VariableDeclaration leftPair = matchedPair.getLeft();
                VariableDeclaration rightPair = matchedPair.getRight();
                Tree leftTree = findByLocationInfo(srcTree,leftPair.getLocationInfo());
                Tree rightTree = findByLocationInfo(dstTree,rightPair.getLocationInfo());
                mappingStore.addMappingRecursively(leftTree,rightTree);
            }
            Tree srcOperationNode = srcTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation1().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation1().getLocationInfo().getEndOffset());
            Tree dstOperationNode = dstTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation2().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation2().getLocationInfo().getEndOffset());
            mappingStore.addMapping(srcOperationNode,dstOperationNode);
            mappingStore.addListOfMapping(processMethodSignature(srcOperationNode,dstOperationNode));
            Set<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();

            if (umlOperationBodyMapper.getOperation1().getName().equals("getCreationToken"))
                System.out.println("ola");

            for (AbstractCodeMapping abstractCodeMapping : mappings)
            {
                Tree srcStatementNode = findByLocationInfo(srcTree,abstractCodeMapping.getFragment1().getLocationInfo());
                Tree dstStatementNode = findByLocationInfo(dstTree,abstractCodeMapping.getFragment2().getLocationInfo());
                if (abstractCodeMapping.getReplacements().isEmpty())
                {
                    mappingStore.addMappingRecursively(srcStatementNode,dstStatementNode);
                }
                else
                {
                    Set<Replacement> replacements = abstractCodeMapping.getReplacements();
                    if (replacements.size() == 1)
                    {
                        Replacement replacement = replacements.iterator().next();
                        if (replacement.getType() == Replacement.ReplacementType.NUMBER_LITERAL ||
                            replacement.getType() == Replacement.ReplacementType.STRING_LITERAL)
                        {
                            mappingStore.addMappingRecursively(srcStatementNode,dstStatementNode);
                        }

                    }
                }
            }

        }
        return new ASTDiff(treeContextPair.first, treeContextPair.second,mappingStore,new SimplifiedChawatheScriptGenerator().computeActions(mappingStore));
    }

    private List<Pair<Tree, Tree>> processClassImplemetedInterfaces(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();

        List<UMLType> srcImplementedInterfaces = classdiff.getOriginalClass().getImplementedInterfaces();
        List<UMLType> dstImplementedInterfaces = classdiff.getNextClass().getImplementedInterfaces();

        List<UMLType> removedOnes = classdiff.getRemovedImplementedInterfaces();
        for (UMLType srcUmltype : srcImplementedInterfaces) {
            if (!removedOnes.contains(srcUmltype))
            {
                Tree srcInterfaceTree = findByLocationInfo(srcTree,srcUmltype.getLocationInfo());
                for (UMLType dstUmlType : dstImplementedInterfaces) {
                    if (dstUmlType.getClassType().equals(srcUmltype.getClassType()))
                    {
                        Tree dstInterfaceTree = findByLocationInfo(dstTree,dstUmlType.getLocationInfo());
                        pairlist.add(new Pair<>(srcInterfaceTree, dstInterfaceTree));
                        break;
                    }
                }

            }

        }
        return pairlist;
    }

    private List<Pair<Tree, Tree>> processClassAttributes(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        List<Pair<Tree,Tree>> pairList = new ArrayList<>();
        List<Pair<Tree, Tree>> matchedAttributes = findMatchedAttributesTree(srcTree, dstTree, classdiff);

        for (Pair<Tree,Tree> matchedpair : matchedAttributes) {
            pairList.add(new Pair<>(matchedpair.first,matchedpair.second));
            pairList.addAll(processFeildDeclration(matchedpair.first,matchedpair.second));
        }
        return pairList;
    }
    private List<Pair<Tree,Tree>> processFeildDeclration(Tree srcFeildDelcration, Tree dstFeildDeclration)
    {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();

        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("SimpleType");
        for (String type : searchingTypes) {
            Pair<Tree, Tree> matched = matchBasedOnType(srcFeildDelcration,dstFeildDeclration, type);
            if (matched != null)
                pairlist.add(matched);
        }
        return pairlist;
    }
    private List<Pair<Tree, Tree>> findMatchedAttributesTree(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();

        List<UMLAttribute> srcAttributes = classdiff.getOriginalClass().getAttributes();
        List<UMLAttribute> dstAttributes = classdiff.getNextClass().getAttributes();

        List<UMLAttribute> removedOnes = classdiff.getRemovedAttributes();
        for (UMLAttribute srcUmltype : srcAttributes) {
            if (!removedOnes.contains(srcUmltype))
            {
                Tree srcFindedTree = findByLocationInfo(srcTree,srcUmltype.getLocationInfo());
                for (UMLAttribute dstUmlType : dstAttributes) {
                    if (dstUmlType.getName().equals(srcUmltype.getName()))
                    {
                        Tree dstFindedTree = findByLocationInfo(dstTree,dstUmlType.getLocationInfo());
                        pairlist.add(new Pair<>(srcFindedTree.getParent(), dstFindedTree.getParent()));
                        break;
                    }
                }

            }

        }
        return pairlist;
    }


    private Pair<Tree, Tree> processSuperClass(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        UMLType srcParentUML = classdiff.getOriginalClass().getSuperclass();
        UMLType dstParentUML = classdiff.getNextClass().getSuperclass();
        if (srcParentUML != null && dstParentUML != null) {
            Tree srcParentClassTree = findByLocationInfo(srcTree, srcParentUML.getLocationInfo());
            Tree dstParentClassTree = findByLocationInfo(dstTree, dstParentUML.getLocationInfo());
            return new Pair<>(srcParentClassTree,dstParentClassTree);
        }
        return null;
    }

    private Pair<Tree, Tree> processPackageDeclaration(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        //TODO: In current implementation, I assumed that these two package-statements are matched since they both belong to the same class
        //TODO : Question: Can single file have multiple package declaration? if yes, I have to use list of pairs
        Tree srcPackageDeclaration = findPackageDeclaration(srcTree);
        Tree dstPackageDeclaration = findPackageDeclaration(dstTree);
        if (srcPackageDeclaration != null && dstPackageDeclaration != null)
            return new Pair<Tree,Tree>(srcPackageDeclaration,dstPackageDeclaration);
        return null;
    }

    private Tree findPackageDeclaration(Tree inputTree) {
        String searchingType = "PackageDeclaration";
        if (!inputTree.getChildren().isEmpty())
        {
            List<Tree> children = inputTree.getChildren();
            for(Tree child: children)
            {
                if (child.getType().name.equals(searchingType))
                    return child;
            }
        }
        return null;
    }

    private List<Pair<Tree, Tree>> processImports(Tree srcTree, Tree dstTree, Set<String> commonImports) {
        if (commonImports.isEmpty())
             return null;
        String searchingType = "ImportDeclaration";
        List<Tree> srcChildren = srcTree.getChildren();
        List<Tree> dstChildren = dstTree.getChildren();
        List<Pair<Tree,Tree>> matchedTrees = new ArrayList<>();
        for(String label : commonImports){
            Tree srcImportStatement = findImportByTypeAndLabel(srcChildren,searchingType,label);
            Tree dstImportStatement = findImportByTypeAndLabel(dstChildren,searchingType,label);
            if (srcImportStatement != null && dstImportStatement != null)
                matchedTrees.add(new Pair<>(srcImportStatement,dstImportStatement));
        }
        return matchedTrees;
    }

    private Tree findImportByTypeAndLabel(List<Tree> inputTree, String searchingType, String label) {
        for (Tree srcStatement: inputTree) {
            if (srcStatement.getType().name.equals(searchingType)) {
                if (srcStatement.getChild(0).getLabel().equals(label)) //TODO getChild 0 will cause a lot of problem
                    return srcStatement;
            }
        }
        return null;
    }

    private Tree findByLocationInfo(Tree tree, LocationInfo locationInfo){
        int startoffset = locationInfo.getStartOffset();
        int endoffset = locationInfo.getEndOffset();
        Tree result = tree.getTreeBetweenPositions(startoffset, endoffset);
        return result;
    }
    private Tree findByLocationInfo(Tree tree, LocationInfo locationInfo, String type){
        int startoffset = locationInfo.getStartOffset();
        int endoffset = locationInfo.getEndOffset();
        Tree result = tree.getTreeBetweenPositions(startoffset, endoffset,type);
        return result;
    }
    private List<Pair<Tree, Tree>> processMethodSignature(Tree srcOperationNode, Tree dstOperationNode) {
        //TODO: static and ... are also considered as Modifier for method declration
        //TODO: (cont.) probably, I have to change ASTVisitor to modify those types.
        List<Pair<Tree,Tree>> pairList = new ArrayList<>();
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("SimpleName");
//        searchingTypes.add("SimpleType");
        // TODO: Above line was added to check the Exceptions, probably not the right way to handle this.
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
        searchingTypes.add("AccessModifier");
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





