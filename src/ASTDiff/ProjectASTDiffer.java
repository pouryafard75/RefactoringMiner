package ASTDiff;

import actions.ASTDiff;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import jdt.CommentVisitor;
import matchers.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import tree.Tree;
import tree.TreeContext;
import tree.TreeUtils;
import utils.Pair;


import java.util.*;

import static tree.TreeUtils.findChildByTypeAndLabel;

public class ProjectASTDiffer
{
    private boolean _CHECK_COMMENTS = false;
    private ProjectASTDiff projectASTDiff;

    public ProjectASTDiffer(String repo, String commitId)
    {
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        projectASTDiff = new ProjectASTDiff();
        this.projectASTDiff.setProjectData(miner.getProjectData(repo,commitId));

    }
    public ProjectASTDiffer(String url)
    {
        this(getRepo(url),getCommit(url));
    }

    private static String getRepo(String url) {
        int index = nthIndexOf(url,'/',5);
        return url.substring(0,index) + ".git";
    }

    private static String getCommit(String url) {
        int index = nthIndexOf(url,'/',6);
        return url.substring(index+1);
    }
    public static int nthIndexOf(String text, char needle, int n)
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) == needle)
            {
                n--;
                if (n == 0)
                {
                    return i;
                }
            }
        }
        return -1;
    }


    public ProjectASTDiff diff() throws RefactoringMinerTimedOutException {
        makeASTDiff(this.projectASTDiff.getProjectData().getUmlModelDiff().getCommonClassDiffList());
        makeASTDiff(this.projectASTDiff.getProjectData().getUmlModelDiff().getClassRenameDiffList());
//        makeASTDiff(this.projectASTDiff.getProjectData().getUmlModelDiff().getClassMoveDiffList());

        computeAllEditScripts();
        return projectASTDiff;
    }

    public void computeAllEditScripts() {

        for (Map.Entry<DiffInfo,ASTDiff> entry : projectASTDiff.getAstDiffMap().entrySet())
        {
            entry.getValue().computeEditScript(
                    this.projectASTDiff.getProjectData().getUmlModelDiff().getParentModel().getTreeContextMap(),
                    this.projectASTDiff.getProjectData().getUmlModelDiff().getChildModel().getTreeContextMap()
            );
        }
    }
    private void makeASTDiff(List<? extends UMLClassBaseDiff> umlClassBaseDiffList) throws RefactoringMinerTimedOutException {
        for (UMLClassBaseDiff classdiff : umlClassBaseDiffList) {
            ASTDiff classASTDiff = process(classdiff, findTreeContexts(classdiff));
            DiffInfo diffInfo = new DiffInfo(
                    classdiff.getOriginalClass().getLocationInfo().getFilePath(),
                    classdiff.getNextClass().getLocationInfo().getFilePath()
            );
            projectASTDiff.addASTDiff(diffInfo,classASTDiff);
        }
    }
    private ASTDiff process(UMLClassBaseDiff classdiff, Pair<TreeContext, TreeContext> treeContextPair) throws RefactoringMinerTimedOutException {
        TreeContext srcTreeContext = treeContextPair.first;
        TreeContext dstTreeContext = treeContextPair.second;
        Tree srcTree = srcTreeContext.getRoot();
        Tree dstTree = dstTreeContext.getRoot();

        MultiMappingStore mappingStore = new MultiMappingStore(srcTreeContext,dstTreeContext);
        mappingStore.addMapping(srcTree,dstTree);
        processRefactorings(srcTree,dstTree,classdiff.getRefactorings(),mappingStore);
        processPackageDeclaration(srcTree,dstTree,classdiff,mappingStore);
        processImports(srcTree,dstTree,classdiff.getImportDiffList(),mappingStore);
        processClassDeclarationMapping(srcTree,dstTree,classdiff,mappingStore);
        processAllMethods(srcTree,dstTree,classdiff.getOperationBodyMapperList(),mappingStore);
        processModelDiffRefactorings(srcTree,dstTree,classdiff,this.projectASTDiff.getProjectData().getUmlModelDiff().getRefactorings(),mappingStore);
        if (_CHECK_COMMENTS) addAndProcessComments(treeContextPair.first, treeContextPair.second,mappingStore);
        return new ASTDiff(treeContextPair.first, treeContextPair.second, mappingStore);
    }

    private void processModelDiffRefactorings(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, List<Refactoring> refactorings, MultiMappingStore mappingStore) {
            UMLModelDiff umlModelDiff = this.projectASTDiff.getProjectData().getUmlModelDiff();
            for(Refactoring refactoring : refactorings)
            {
                List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).toList();
                List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).toList();
                if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()))
                {
                    if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION))
                    {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                        String otherFileName = pushDownOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
                        processMethod(otherFileTree,dstTree,pushDownOperationRefactoring.getBodyMapper(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION))
                    {
                        PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                        String otherFileName = pullUpOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
                        processMethod(otherFileTree,dstTree,pullUpOperationRefactoring.getBodyMapper(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE))
                    {
                        PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) refactoring;
                        String otherFileName = pushDownAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
                        processFeildDeclration(otherFileTree,dstTree,pushDownAttributeRefactoring.getOriginalAttribute(),pushDownAttributeRefactoring.getMovedAttribute(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE))
                    {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
                        String otherFileName = pullUpAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
                        processFeildDeclration(otherFileTree,dstTree,pullUpAttributeRefactoring.getOriginalAttribute(),pullUpAttributeRefactoring.getMovedAttribute(),mappingStore);
                    }

                }
                else if (beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName()))
                {
                    if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION))
                    {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                        String otherFileName = pushDownOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
                        processMethod(srcTree,otherFileTree,pushDownOperationRefactoring.getBodyMapper(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION))
                    {
                        PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                        String otherFileName = pullUpOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
                        processMethod(srcTree,otherFileTree,pullUpOperationRefactoring.getBodyMapper(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE))
                    {
                        PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) refactoring;
                        String otherFileName = pushDownAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
                        processFeildDeclration(srcTree,otherFileTree,pushDownAttributeRefactoring.getOriginalAttribute(),pushDownAttributeRefactoring.getMovedAttribute(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE))
                    {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
                        String otherFileName = pullUpAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
                        processFeildDeclration(srcTree,otherFileTree,pullUpAttributeRefactoring.getOriginalAttribute(),pullUpAttributeRefactoring.getMovedAttribute(),mappingStore);
                    }

                }
            }
    }

    private void addAndProcessComments(TreeContext firstTC, TreeContext secondTC, MultiMappingStore mappingStore) {
        Pair<List<Tree>, List<Tree>> addedCommentsPair = addComments(firstTC,secondTC);
        processComments(addedCommentsPair,mappingStore);
    }

    private void processAllMethods(Tree srcTree, Tree dstTree, List<UMLOperationBodyMapper> operationBodyMapperList, MultiMappingStore mappingStore) {
        for(UMLOperationBodyMapper umlOperationBodyMapper : new ArrayList<>(operationBodyMapperList))
            processMethod(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
    }
    private void processMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, MultiMappingStore mappingStore)
    {
        processOperationDiff(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
        processMethodParameters(srcTree,dstTree,umlOperationBodyMapper.getMatchedVariables(),mappingStore);
        processMethodJavaDoc(srcTree, dstTree, umlOperationBodyMapper.getOperation1().getJavadoc(),umlOperationBodyMapper.getOperation2().getJavadoc(),mappingStore);
        Tree srcOperationNode =Tree.findByLocationInfo(srcTree,umlOperationBodyMapper.getOperation1().getLocationInfo());
        Tree dstOperationNode =Tree.findByLocationInfo(dstTree,umlOperationBodyMapper.getOperation2().getLocationInfo());
        mappingStore.addMapping(srcOperationNode,dstOperationNode);
        processMethodSignature(srcOperationNode,dstOperationNode,mappingStore);
        fromRefMiner(srcTree,dstTree,umlOperationBodyMapper.getMappings(),mappingStore);
    }

    private void processMethodParameters(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, MultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
            VariableDeclaration leftPair = matchedPair.getLeft();
            VariableDeclaration rightPair = matchedPair.getRight();
            Tree leftTree  =Tree.findByLocationInfo(srcTree,leftPair.getLocationInfo());
            Tree rightTree =Tree.findByLocationInfo(dstTree,rightPair.getLocationInfo());
            if (leftTree.getParent().getType().name.equals("MethodDeclaration") &&
                    rightTree.getParent().getType().name.equals("MethodDeclaration"))
                if (rightTree.isIsomorphicTo(leftTree))
                    mappingStore.addMappingRecursively(leftTree,rightTree);
        }
    }

    private void fromRefMiner(Tree srcTree, Tree dstTree, Set<AbstractCodeMapping> mappingSet, MultiMappingStore mappingStore) {
        ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(mappingSet);
        for (AbstractCodeMapping abstractCodeMapping : mappings)
        {
            if (abstractCodeMapping instanceof LeafMapping)
                processLeafMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
            else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
                processCompositeMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
        }
    }

    private void processCompositeMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
        Tree srcStatementNode =Tree.findByLocationInfo(srcTree,compositeStatementObjectMapping.getFragment1().getLocationInfo());
        Tree dstStatementNode =Tree.findByLocationInfo(dstTree,compositeStatementObjectMapping.getFragment2().getLocationInfo());
        if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
        {
            mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
        }
        else {
            mappingStore.addMapping(srcStatementNode,dstStatementNode);
            if ( (srcStatementNode.getType().name.equals("TryStatement") && dstStatementNode.getType().name.equals("TryStatement")) ||
                    (srcStatementNode.getType().name.equals("CatchClause") && dstStatementNode.getType().name.equals("CatchClause")))
                matchBlocks(srcStatementNode,dstStatementNode,mappingStore);
            else if (!srcStatementNode.getType().name.equals("Block") && !dstStatementNode.getType().name.equals("Block")) {
                new CompositeMatcher().match(srcStatementNode, dstStatementNode, abstractCodeMapping , mappingStore);
            }
        }
    }

    private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
        Tree srcStatementNode =Tree.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo());
        Tree dstStatementNode =Tree.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo());
        if (leafMapping.getFragment2().toString().equals(leafMapping.getFragment1().toString())) {
            mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
        }
        else {
            if (srcStatementNode.isIsoStructuralTo(dstStatementNode))
            {
                mappingStore.addMappingRecursively(srcStatementNode,dstStatementNode);
            }
            else {
                if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
                    mappingStore.addMapping(srcStatementNode, dstStatementNode);
                new LeafMatcher().match(srcStatementNode,dstStatementNode,abstractCodeMapping,mappingStore);
            }
        }
    }

    private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, MultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
            Tree srcClassAnnotationTree =Tree.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree =Tree.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
        }
    }

    private void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, MultiMappingStore mappingStore) {
        String searchingType = "Block";
        Pair<Tree, Tree> matched = matchBasedOnType(srcStatementNode,dstStatementNode, searchingType);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
    }
    private void processComments(Pair<List<Tree>, List<Tree>> addedCommentsPair, MultiMappingStore mappingStore) {
        List<Tree> srcComments = addedCommentsPair.first;
        List<Tree> dstComments = addedCommentsPair.second;
        Map<Tree,List<Tree>> candidates = new HashMap<>();
        for (Tree srcComment : srcComments)
            {
                List<Tree> candidateList = new ArrayList<>();
                for (Tree dstComment : dstComments)
                {
                    if (srcComment.getMetrics().hash == dstComment.getMetrics().hash)
                        candidateList.add(dstComment);
                }
                if (!candidateList.isEmpty())
                    candidates.put(srcComment,candidateList);
            }
        for (Map.Entry<Tree,List<Tree>> entry : candidates.entrySet())
        {
            Tree srcTree = entry.getKey();
            List<Tree> matches = entry.getValue();
            if (matches.size() == 1) {
                mappingStore.addMappingRecursively(srcTree, matches.get(0));
            }
            else
            {
                //TODO: ignore so far
            }
        }
    }

    private Pair<List<Tree> , List<Tree>> addComments(TreeContext first, TreeContext second) {
        CommentVisitor firstCommentVisior = new CommentVisitor(first);
        firstCommentVisior.addCommentToProperSubtree();
        CommentVisitor secondCommentVisitor = new CommentVisitor(second);
        secondCommentVisitor.addCommentToProperSubtree();
        return new Pair<>(firstCommentVisior.getComments(),secondCommentVisitor.getComments());
    }

    private void processClassJavaDocs(Tree srcTree, Tree dstTree, UMLClassBaseDiff classdiff, MultiMappingStore mappingStore) {
        UMLJavadoc javadoc1 = classdiff.getOriginalClass().getJavadoc();
        UMLJavadoc javadoc2 = classdiff.getNextClass().getJavadoc();
        if (javadoc1 != null && javadoc2 != null) {
            Tree srcJavaDocNode =Tree.findByLocationInfo(srcTree, javadoc1.getLocationInfo());
            Tree dstJavaDocNode =Tree.findByLocationInfo(dstTree, javadoc2.getLocationInfo());
            mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
        }
    }

    private List<Pair<Tree, Tree>> processMethodJavaDoc(Tree srcTree, Tree dstTree, UMLJavadoc javadoc1, UMLJavadoc javadoc2, MultiMappingStore mappingStore) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        if (javadoc1 != null && javadoc2 != null)
            if (javadoc1.equalText(javadoc2))
            {
                Tree srcJavaDocNode =Tree.findByLocationInfo(srcTree,javadoc1.getLocationInfo());
                Tree dstJavaDocNode =Tree.findByLocationInfo(dstTree,javadoc2.getLocationInfo());
                mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
            }
        return pairlist;
    }

    private List<Pair<Tree, Tree>> processCommentsInsideMethod(Tree srcTree, Tree dstTree ,UMLOperation operation1, UMLOperation operation2) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        List<UMLComment> operation1Comments = operation1.getComments();
        List<UMLComment> operation2Comments = operation1.getComments();
        for(UMLComment umlComment1 : operation1Comments)
        {
            for(UMLComment umlComment2 : operation2Comments)
            {
                if (umlComment1.getText().equals(umlComment2.getText()))
                {
                    pairlist.add(new Pair<>(Tree.findByLocationInfo(srcTree,umlComment1.getLocationInfo()),Tree.findByLocationInfo(dstTree,umlComment2.getLocationInfo())));
                }
            }
        }
        return pairlist;
    }

    private void processOperationDiff(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, MultiMappingStore mappingStore) {
        UMLOperationDiff umlOperationDiff = umlOperationBodyMapper.getOperationSignatureDiff().isPresent() ? umlOperationBodyMapper.getOperationSignatureDiff().get() : null;
        if (umlOperationDiff == null) return;
        UMLTypeParameterListDiff umlTypeParameterListDiff = umlOperationDiff.getTypeParameterListDiff();
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : umlTypeParameterListDiff.getCommonTypeParameters()) {
            Tree srcTypeParam =Tree.findByLocationInfo(srcTree, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam =Tree.findByLocationInfo(dstTree, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations())
        {
            Tree srcClassAnnotationTree =Tree.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree =Tree.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
        }
        Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
        if (commonExceptionTypes != null)
        {
            for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
                Tree srcExceptionNode =Tree.findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo());
                Tree dstExceptionNode =Tree.findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo());
                mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
            }
        }
        if (umlOperationDiff.getRemovedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
            LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
            LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
            Tree srcNode =Tree.findByLocationInfo(srcTree, srcLocationInfo);
            Tree dstNode =Tree.findByLocationInfo(dstTree, dstLocationInfo);
            if (srcNode.isIsoStructuralTo(dstNode))
                mappingStore.addMappingRecursively(srcNode,dstNode);
        }
    }
    private void processRefactorings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, MultiMappingStore mappingStore) throws RefactoringMinerTimedOutException {
        for (Refactoring refactoring : refactoringList)
        {
            if (refactoring instanceof ExtractOperationRefactoring) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) (refactoring);
                UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                fromRefMiner(srcTree,dstTree,bodyMapper.getMappings(),mappingStore);
            }
            if (refactoring instanceof InlineOperationRefactoring) {
                InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) (refactoring);
                UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                fromRefMiner(srcTree,dstTree,bodyMapper.getMappings(),mappingStore);
            }
            else if (refactoring instanceof RenameAttributeRefactoring) {
                RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) (refactoring);

                Tree srcAttrTree =Tree.findByLocationInfo(srcTree,renameAttributeRefactoring.getOriginalAttribute().getLocationInfo());
                Tree dstAttrTree =Tree.findByLocationInfo(dstTree,renameAttributeRefactoring.getRenamedAttribute().getLocationInfo());
                mappingStore.addMappingRecursively(srcAttrTree.getParent(),dstAttrTree.getParent());
            }
            else if (refactoring instanceof ExtractVariableRefactoring) {
                ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
            }
            else if (refactoring instanceof MergeVariableRefactoring)
            {
                MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring)(refactoring);
                Set<VariableDeclaration> mergedVariables = mergeVariableRefactoring.getMergedVariables();
                VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
                Tree dstVariableType =Tree.findByLocationInfo(dstTree,newVariable.getType().getLocationInfo());
                Tree dstVariableDeclaration =Tree.findByLocationInfo(dstTree,newVariable.getLocationInfo());
                List<Tree> dstChildrenList = dstVariableDeclaration.getChildren().stream().toList();
                Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
                for (VariableDeclaration variableDeclaration : mergedVariables)
                {
                    Tree srcVariableDeclaration =Tree.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
                    Tree srcVariableType =Tree.findByLocationInfo(srcTree,variableDeclaration.getType().getLocationInfo());
                    List<Tree> srcChildrenList = srcVariableDeclaration.getChildren().stream().toList();
                    Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
//                    mappingStore.addMapping(srcVariableDeclaration,dstVariableDeclaration);
                    mappingStore.addMapping(srcVariableType,dstVariableType.getChild(0));
                    mappingStore.addMapping(srcVarName,dstVarName);
                }
            }
        }
    }

    private void processClassImplemetedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classdiff, MultiMappingStore mappingStore) {
        List<UMLType> srcImplementedInterfaces = classdiff.getOriginalClass().getImplementedInterfaces();
        List<UMLType> dstImplementedInterfaces = classdiff.getNextClass().getImplementedInterfaces();
        List<UMLType> removedOnes = classdiff.getRemovedImplementedInterfaces();
        for (UMLType srcUmltype : srcImplementedInterfaces) {
            if (!removedOnes.contains(srcUmltype))
            {
                Tree srcInterfaceTree =Tree.findByLocationInfo(srcTree,srcUmltype.getLocationInfo());
                for (UMLType dstUmlType : dstImplementedInterfaces) {
                    if (dstUmlType.getClassType().equals(srcUmltype.getClassType()))
                    {
                        Tree dstInterfaceTree =Tree.findByLocationInfo(dstTree,dstUmlType.getLocationInfo());
                        mappingStore.addMappingRecursively(srcInterfaceTree, dstInterfaceTree);
                        break;
                    }
                }

            }

        }
    }
    private void processClassAttributes(Tree srcTree, Tree dstTree, UMLClassBaseDiff classdiff, MultiMappingStore mappingStore) {
        List<Pair<UMLAttribute, UMLAttribute>> pairs = findMatchedAttributesPair(classdiff);
        for (Pair<UMLAttribute,UMLAttribute> matchedpair : pairs) {
            processFeildDeclration(srcTree,dstTree,matchedpair.first,matchedpair.second,mappingStore);
        }
    }

    private List<Pair<UMLAttribute, UMLAttribute>> findMatchedAttributesPair(UMLClassBaseDiff classdiff) {
        List<Pair<UMLAttribute,UMLAttribute>> pairs = new ArrayList<>();
        List<UMLAttribute> srcAttributes = classdiff.getOriginalClass().getAttributes();
        List<UMLAttribute> dstAttributes = classdiff.getNextClass().getAttributes();
        List<UMLAttribute> removedOnes = classdiff.getRemovedAttributes();
        for (UMLAttribute srcUmltype : srcAttributes) {
            if (!removedOnes.contains(srcUmltype))
            {
                for (UMLAttribute dstUmlType : dstAttributes) {
                    if (dstUmlType.getName().equals(srcUmltype.getName()))
                    {
                        pairs.add(new Pair<>(srcUmltype,dstUmlType));
                        break;
                    }
                }
            }
        }
        return pairs;
    }

    private void processFeildDeclration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, MultiMappingStore mappingStore)
    {
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("VariableDeclarationFragment");

        Tree srcFeildDelcration =Tree.findByLocationInfo(srcTree,srcUMLAttribute.getLocationInfo()).getParent(); //TODO
        Tree dstFeildDeclration =Tree.findByLocationInfo(dstTree,dstUMLAttribute.getLocationInfo()).getParent(); //TODO
        if (srcFeildDelcration.getMetrics().hash == dstFeildDeclration.getMetrics().hash)
        {
            mappingStore.addMappingRecursively(srcFeildDelcration,dstFeildDeclration);
            return;
        }
        mappingStore.addMapping(srcFeildDelcration,dstFeildDeclration);
        for (String type : searchingTypes) {
            Pair<Tree, Tree> matched = matchBasedOnType(srcFeildDelcration,dstFeildDeclration, type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        String serachingType = "SimpleType";
        Pair<Tree, Tree> result = matchBasedOnType(srcFeildDelcration, dstFeildDeclration, serachingType);
        if (result != null)
            mappingStore.addMappingRecursively(result.first,result.second);
        serachingType = "VariableDeclarationFragment";
        Pair<Tree, Tree> VariableDeclarationFragmentPair = matchBasedOnType(srcFeildDelcration, dstFeildDeclration, serachingType);
        if (VariableDeclarationFragmentPair != null)
        {
            Pair<Tree, Tree> simpleNamePair = matchBasedOnType(VariableDeclarationFragmentPair.first, VariableDeclarationFragmentPair.second, "SimpleName");
            mappingStore.addMapping(simpleNamePair.first,simpleNamePair.second);
            if (srcUMLAttribute.getVariableDeclaration().getInitializer() != null &&
                    dstUMLAttribute.getVariableDeclaration().getInitializer() != null)
            {
                if (VariableDeclarationFragmentPair.first.getChild(1).getType().equals(VariableDeclarationFragmentPair.second.getChild(1).getType())) {
                    mappingStore.addMapping(VariableDeclarationFragmentPair.first.getChild(1), VariableDeclarationFragmentPair.second.getChild(1));
                    //TODO
                }
            }
            if (result != null) {
                mappingStore.addMapping(result.first,result.second);
            }
        }
    }
    private void processSuperClass(Tree srcTree, Tree dstTree, UMLClassBaseDiff classdiff, MultiMappingStore mappingStore) {
        UMLType srcParentUML = classdiff.getOldSuperclass();
        UMLType dstParentUML = classdiff.getNewSuperclass();
        if (srcParentUML != null && dstParentUML != null) {
            Tree srcParentClassTree =Tree.findByLocationInfo(srcTree, srcParentUML.getLocationInfo());
            Tree dstParentClassTree =Tree.findByLocationInfo(dstTree, dstParentUML.getLocationInfo());
            if (srcParentClassTree.isIsomorphicTo(dstParentClassTree))
                mappingStore.addMappingRecursively(srcParentClassTree,dstParentClassTree);
        }
    }

    private void processPackageDeclaration(Tree srcTree, Tree dstTree, UMLClassBaseDiff classdiff, MultiMappingStore mappingStore) {
        //TODO: In current implementation, I assumed that these two package-statements are matched since they both belong to the same class
        //TODO : Question: Can single file have multiple package declaration? if yes, I have to use list of pairs
        Tree srcPackageDeclaration = findPackageDeclaration(srcTree);
        Tree dstPackageDeclaration = findPackageDeclaration(dstTree);
        if (srcPackageDeclaration != null && dstPackageDeclaration != null)
            mappingStore.addMappingRecursively(srcPackageDeclaration,dstPackageDeclaration);
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

    private void processImports(Tree srcTree, Tree dstTree, UMLImportListDiff importDiffList, MultiMappingStore mappingStore) {
        if (importDiffList == null) return;
        Set<String> commonImports = importDiffList.getCommonImports();
        if (commonImports.isEmpty())
             return;
        String searchingType = "ImportDeclaration";
        List<Tree> srcChildren = srcTree.getChildren();
        List<Tree> dstChildren = dstTree.getChildren();
        
        for(String label : commonImports){
            Tree srcImportStatement = findImportByTypeAndLabel(srcChildren,searchingType,label);
            Tree dstImportStatement = findImportByTypeAndLabel(dstChildren,searchingType,label);
            if (srcImportStatement != null && dstImportStatement != null)
                mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
        }
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


    private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, MultiMappingStore mappingStore) {
        //TODO: static and ... are also considered as Modifier for method declration
        //TODO: (cont.) probably, I have to change ASTVisitor to modify those types.
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("Block");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
    }

    private Pair<Tree, Tree> matchBasedOnType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
        Tree srcModifier = TreeUtils.findChildByType(srcOperationNode,searchingType);
        Tree dstModifier = TreeUtils.findChildByType(dstOperationNode,searchingType);
        if (srcModifier != null && dstModifier != null)
            return new Pair<>(srcModifier, dstModifier);
        return null;
    }

    private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classdiff, MultiMappingStore mappingStore) {
        String AST_type = "TypeDeclaration";
        if (classdiff.getOriginalClass().isEnum()) AST_type = "EnumDeclaration";
        Tree srcTypeDeclartion = Tree.findByLocationInfo(srcTree,classdiff.getOriginalClass().getLocationInfo(),AST_type);
        Tree dstTypeDeclartion = Tree.findByLocationInfo(dstTree,classdiff.getNextClass().getLocationInfo(),AST_type);
        mappingStore.addMapping(srcTypeDeclartion,dstTypeDeclartion);
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("TYPE_DECLARATION_KIND");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcTypeDeclartion,dstTypeDeclartion,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        if (classdiff.getOriginalClass().isStatic() && classdiff.getNextClass().isStatic())
            matchModifier(srcTypeDeclartion,dstTypeDeclartion,"static",mappingStore);
        if (classdiff.getOriginalClass().isFinal() && classdiff.getNextClass().isFinal())
            matchModifier(srcTypeDeclartion,dstTypeDeclartion,"final",mappingStore);
        if (classdiff.getOriginalClass().isAbstract() && classdiff.getNextClass().isAbstract())
            matchModifier(srcTypeDeclartion,dstTypeDeclartion,"abstract",mappingStore);

        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classdiff.getTypeParameterDiffList().getCommonTypeParameters()) {
            Tree srcTypeParam = Tree.findByLocationInfo(srcTypeDeclartion, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = Tree.findByLocationInfo(dstTypeDeclartion, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        processSuperClass(srcTypeDeclartion,dstTypeDeclartion,classdiff,mappingStore);
        processClassImplemetedInterfaces(srcTypeDeclartion,dstTypeDeclartion,classdiff,mappingStore);
        processClassAttributes(srcTree,dstTree,classdiff,mappingStore);
        processClassJavaDocs(srcTypeDeclartion,dstTypeDeclartion,classdiff,mappingStore);
        processClassAnnotations(srcTypeDeclartion,dstTypeDeclartion,classdiff.getAnnotationListDiff(),mappingStore);

    }

    private void matchModifier(Tree srcTypeDeclartion, Tree dstTypeDeclartion, String modifier, MultiMappingStore mappingStore) {
        String type = "Modifier";
        Tree srcTree = findChildByTypeAndLabel(srcTypeDeclartion,type,modifier);
        Tree dstTree = findChildByTypeAndLabel(dstTypeDeclartion,type,modifier);
        if (srcTree != null && dstTree != null){
            mappingStore.addMapping(srcTree,dstTree);
        }
    }

    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassBaseDiff classDiff) {
        return new Pair<>
                (this.projectASTDiff.getProjectData().getUmlModelDiff().getParentModel().getTreeContextMap()
                        .get(classDiff.getOriginalClass().getSourceFile()),
                 this.projectASTDiff.getProjectData().getUmlModelDiff().getChildModel().getTreeContextMap().
                         get(classDiff.getNextClass().getSourceFile()));
    }
}





