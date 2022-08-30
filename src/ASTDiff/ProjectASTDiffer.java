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
import org.refactoringminer.rm1.ProjectData;
import tree.Tree;
import tree.TreeContext;
import tree.TreeUtils;
import utils.Pair;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static tree.TreeUtils.findChildByTypeAndLabel;

public class ProjectASTDiffer
{
    private final boolean _CHECK_COMMENTS = false;
    private ProjectASTDiff projectASTDiff;

    public ProjectASTDiff getProjectASTDiff() {
        return projectASTDiff;
    }

    private static ProjectASTDiffer fromRepoCommit(String repo, String commitId)
    {
        return new ProjectASTDiffer(new GitHistoryRefactoringMinerImpl().getProjectData(repo,commitId));
    }
    public static ProjectASTDiffer fromLocalRepo(String dirPath, String commitId)
    {
        return new ProjectASTDiffer(new GitHistoryRefactoringMinerImpl().calcProjectData(dirPath, commitId));
    }
    public static ProjectASTDiffer fromLocalDirectories(String dir1path, String dir2path) throws IOException, RefactoringMinerTimedOutException
    {
        return new ProjectASTDiffer(UMLModelASTReader.makeProjectData(dir1path,dir2path));
    }
    public static ProjectASTDiffer fromLocalFiles(String file1, String file2) throws IOException, RefactoringMinerTimedOutException {
        //TODO: RefactoringMiner doesnt support pair of files at the moment.
//        return null;
        return new ProjectASTDiffer(UMLModelASTReader.makeProjectData_fromFiles(file1,file2));
    }
    public static ProjectASTDiffer fromURL(String url)
    {
        return ProjectASTDiffer.fromRepoCommit(getRepo(url),getCommit(url));
    }
    private ProjectASTDiffer(ProjectData projectData){
        this.projectASTDiff = new ProjectASTDiff(projectData);
    }

    private static String getRepo(String url) {
        int index = nthIndexOf(url,'/',5);
        return url.substring(0,index) + ".git";
    }

    private static String getCommit(String url) {
        int index = nthIndexOf(url,'/',6);
        return url.substring(index+1);
    }
    private static int nthIndexOf(String text, char needle, int n)
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
        long diff_execution_started = System.currentTimeMillis();
        makeASTDiff(this.projectASTDiff.getProjectData().getUmlModelDiff().getCommonClassDiffList());
        makeASTDiff(this.projectASTDiff.getProjectData().getUmlModelDiff().getClassRenameDiffList());
//        makeASTDiff(this.projectASTDiff.getProjectData().getUmlModelDiff().getClassMoveDiffList());
        long diff_execution_finished =  System.currentTimeMillis();
        System.out.println("Diff execution: " + (diff_execution_finished - diff_execution_started)/ 1000 + " seconds");
        computeAllEditScripts();
        return projectASTDiff;
    }

    private void computeAllEditScripts() {
        long editScript_start = System.currentTimeMillis();
        for (Map.Entry<DiffInfo,ASTDiff> entry : projectASTDiff.getAstDiffMap().entrySet())
        {
            entry.getValue().computeEditScript(
                    this.projectASTDiff.getProjectData().getUmlModelDiff().getParentModel().getTreeContextMap(),
                    this.projectASTDiff.getProjectData().getUmlModelDiff().getChildModel().getTreeContextMap()
            );
        }
        long editScript_end = System.currentTimeMillis();
        System.out.println("EditScript execution: " + (editScript_end - editScript_start)/ 1000 + " seconds");
    }
    private void makeASTDiff(List<? extends UMLClassBaseDiff> umlClassBaseDiffList) throws RefactoringMinerTimedOutException {
        for (UMLClassBaseDiff classDiff : umlClassBaseDiffList) {
            ASTDiff classASTDiff = process(classDiff, findTreeContexts(classDiff));
            DiffInfo diffInfo = new DiffInfo(
                    classDiff.getOriginalClass().getLocationInfo().getFilePath(),
                    classDiff.getNextClass().getLocationInfo().getFilePath()
            );
            projectASTDiff.addASTDiff(diffInfo,classASTDiff);
        }
    }

    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassBaseDiff classDiff) {
        return new Pair<>
                (this.projectASTDiff.getProjectData().getUmlModelDiff().getParentModel().getTreeContextMap()
                        .get(classDiff.getOriginalClass().getSourceFile()),
                        this.projectASTDiff.getProjectData().getUmlModelDiff().getChildModel().getTreeContextMap().
                                get(classDiff.getNextClass().getSourceFile()));
    }
    private ASTDiff process(UMLClassBaseDiff classDiff, Pair<TreeContext, TreeContext> treeContextPair) throws RefactoringMinerTimedOutException {
        TreeContext srcTreeContext = treeContextPair.first;
        TreeContext dstTreeContext = treeContextPair.second;
        Tree srcTree = srcTreeContext.getRoot();
        Tree dstTree = dstTreeContext.getRoot();
        MultiMappingStore mappingStore = new MultiMappingStore(srcTreeContext,dstTreeContext);
        mappingStore.addMapping(srcTree,dstTree);
        processRefactorings(srcTree,dstTree,classDiff.getRefactorings(),mappingStore);
        processPackageDeclaration(srcTree,dstTree,classDiff,mappingStore);
        processImports(srcTree,dstTree,classDiff.getImportDiffList(),mappingStore);
        processClassDeclarationMapping(srcTree,dstTree,classDiff,mappingStore);
        processAllMethods(srcTree,dstTree,classDiff.getOperationBodyMapperList(),mappingStore);
        processModelDiffRefactorings(srcTree,dstTree,classDiff,this.projectASTDiff.getProjectData().getUmlModelDiff().getRefactorings(),mappingStore);
        if (_CHECK_COMMENTS) addAndProcessComments(treeContextPair.first, treeContextPair.second,mappingStore);
        return new ASTDiff(treeContextPair.first, treeContextPair.second, mappingStore);
    }

    private void processModelDiffRefactorings(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, List<Refactoring> refactorings, MultiMappingStore mappingStore) {
            UMLModelDiff umlModelDiff = this.projectASTDiff.getProjectData().getUmlModelDiff();
            for(Refactoring refactoring : refactorings)
            {
                List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
                List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
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
                        processFieldDeclaration(otherFileTree,dstTree,pushDownAttributeRefactoring.getOriginalAttribute(),pushDownAttributeRefactoring.getMovedAttribute(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE))
                    {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
                        String otherFileName = pullUpAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
                        processFieldDeclaration(otherFileTree,dstTree,pullUpAttributeRefactoring.getOriginalAttribute(),pullUpAttributeRefactoring.getMovedAttribute(),mappingStore);
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
                        processFieldDeclaration(srcTree,otherFileTree,pushDownAttributeRefactoring.getOriginalAttribute(),pushDownAttributeRefactoring.getMovedAttribute(),mappingStore);
                    }
                    else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE))
                    {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
                        String otherFileName = pullUpAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
                        Tree otherFileTree = umlModelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
                        processFieldDeclaration(srcTree,otherFileTree,pullUpAttributeRefactoring.getOriginalAttribute(),pullUpAttributeRefactoring.getMovedAttribute(),mappingStore);
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
        if (umlOperationBodyMapper.getOperation1() != null & umlOperationBodyMapper.getOperation2() != null) {
            processMethodJavaDoc(srcTree, dstTree, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), mappingStore);
            Tree srcOperationNode = Tree.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo());
            Tree dstOperationNode = Tree.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo());
            mappingStore.addMapping(srcOperationNode, dstOperationNode);
            processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
            fromRefMiner(srcTree, dstTree, umlOperationBodyMapper.getMappings(), mappingStore);
        }
        else {
            Tree srcOperationNode = Tree.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
            Tree dstOperationNode = Tree.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
            mappingStore.addMapping(srcOperationNode, dstOperationNode);
            processMethodSignature(srcOperationNode, dstOperationNode,umlOperationBodyMapper,  mappingStore);
            fromRefMiner(srcTree, dstTree, umlOperationBodyMapper.getMappings(), mappingStore);
        }
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
        // TODO: 8/2/2022 Need to rethink regarding the logic asap, with this logic , we might see a huge drop in performance

//        if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
//        {
//            mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
//        }
//        else
        {
            mappingStore.addMapping(srcStatementNode,dstStatementNode);
            if ( (srcStatementNode.getType().name.equals("TryStatement") && dstStatementNode.getType().name.equals("TryStatement")) ||
                    (srcStatementNode.getType().name.equals("CatchClause") && dstStatementNode.getType().name.equals("CatchClause"))) {
                matchBlocks(srcStatementNode, dstStatementNode, mappingStore);
                if  (srcStatementNode.getType().name.equals("CatchClause") && dstStatementNode.getType().name.equals("CatchClause"))
                    new CompositeMatcher().match(srcStatementNode,dstStatementNode,abstractCodeMapping,mappingStore);
            }
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
            if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
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
                //TODO: ignore at the moment
            }
        }
    }

    private Pair<List<Tree> , List<Tree>> addComments(TreeContext first, TreeContext second) {
        CommentVisitor firstCommentVisitor = new CommentVisitor(first);
        firstCommentVisitor.addCommentToProperSubtree();
        CommentVisitor secondCommentVisitor = new CommentVisitor(second);
        secondCommentVisitor.addCommentToProperSubtree();
        return new Pair<>(firstCommentVisitor.getComments(),secondCommentVisitor.getComments());
    }

    private void processClassJavaDocs(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, MultiMappingStore mappingStore) {
        UMLJavadoc javadoc1 = classDiff.getOriginalClass().getJavadoc();
        UMLJavadoc javadoc2 = classDiff.getNextClass().getJavadoc();
        if (javadoc1 != null && javadoc2 != null) {
            Tree srcJavaDocNode =Tree.findByLocationInfo(srcTree, javadoc1.getLocationInfo());
            Tree dstJavaDocNode =Tree.findByLocationInfo(dstTree, javadoc2.getLocationInfo());
            if (javadoc1.equalText(javadoc2))
                mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
        }
    }

    private void processMethodJavaDoc(Tree srcTree, Tree dstTree, UMLJavadoc javadoc1, UMLJavadoc javadoc2, MultiMappingStore mappingStore) {
        if (javadoc1 != null && javadoc2 != null)
            if (javadoc1.equalText(javadoc2))
            {
                Tree srcJavaDocNode =Tree.findByLocationInfo(srcTree,javadoc1.getLocationInfo());
                Tree dstJavaDocNode =Tree.findByLocationInfo(dstTree,javadoc2.getLocationInfo());
                mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
            }
//        return pairList;
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
    private void processRefactorings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, MultiMappingStore mappingStore){
//        if (true) return;
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

                Tree srcAttrTree =Tree.findByLocationInfo(srcTree,renameAttributeRefactoring.getOriginalAttribute().getLocationInfo()).getParent(); //Super Risky
                Tree dstAttrTree =Tree.findByLocationInfo(dstTree,renameAttributeRefactoring.getRenamedAttribute().getLocationInfo()).getParent(); //Super Risky
//                if (dstAttrTree.isIsomorphicTo(srcAttrTree))
//                    mappingStore.addMappingRecursively(srcAttrTree.getParent(),dstAttrTree.getParent());
                processFieldDeclaration(srcAttrTree,dstAttrTree,renameAttributeRefactoring.getOriginalAttribute(),renameAttributeRefactoring.getRenamedAttribute(),mappingStore);
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
                List<Tree> dstChildrenList = dstVariableDeclaration.getChildren();
                Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
                for (VariableDeclaration variableDeclaration : mergedVariables)
                {
                    Tree srcVariableDeclaration =Tree.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
                    Tree srcVariableType =Tree.findByLocationInfo(srcTree,variableDeclaration.getType().getLocationInfo());
                    List<Tree> srcChildrenList = srcVariableDeclaration.getChildren();
                    Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
//                    mappingStore.addMapping(srcVariableDeclaration,dstVariableDeclaration);
                    mappingStore.addMapping(srcVariableType,dstVariableType.getChild(0));
                    mappingStore.addMapping(srcVarName,dstVarName);
                }
            }
        }
    }

    private void processClassImplementedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, MultiMappingStore mappingStore) {
        List<UMLType> srcImplementedInterfaces = classDiff.getOriginalClass().getImplementedInterfaces();
        List<UMLType> dstImplementedInterfaces = classDiff.getNextClass().getImplementedInterfaces();
        List<UMLType> removedOnes = classDiff.getRemovedImplementedInterfaces();
        for (UMLType srcUmlType : srcImplementedInterfaces) {
            if (!removedOnes.contains(srcUmlType))
            {
                Tree srcInterfaceTree =Tree.findByLocationInfo(srcTree,srcUmlType.getLocationInfo());
                for (UMLType dstUmlType : dstImplementedInterfaces) {
                    if (dstUmlType.getClassType().equals(srcUmlType.getClassType()))
                    {
                        Tree dstInterfaceTree =Tree.findByLocationInfo(dstTree,dstUmlType.getLocationInfo());
                        mappingStore.addMappingRecursively(srcInterfaceTree, dstInterfaceTree);
                        break;
                    }
                }

            }

        }
    }
    private void processClassAttributes(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, MultiMappingStore mappingStore) {
        List<Pair<UMLAttribute, UMLAttribute>> pairs = findMatchedAttributesPair(classDiff);
        for (Pair<UMLAttribute,UMLAttribute> matchedPair : pairs) {
            processFieldDeclaration(srcTree,dstTree,matchedPair.first,matchedPair.second,mappingStore);
        }
    }

    private List<Pair<UMLAttribute, UMLAttribute>> findMatchedAttributesPair(UMLClassBaseDiff classDiff) {
        List<Pair<UMLAttribute,UMLAttribute>> pairs = new ArrayList<>();
        List<UMLAttribute> srcAttributes = classDiff.getOriginalClass().getAttributes();
        List<UMLAttribute> dstAttributes = classDiff.getNextClass().getAttributes();
        List<UMLAttribute> removedOnes = classDiff.getRemovedAttributes();
        for (UMLAttribute srcUmlType : srcAttributes) {
            if (!removedOnes.contains(srcUmlType))
            {
                for (UMLAttribute dstUmlType : dstAttributes) {
                    if (dstUmlType.getName().equals(srcUmlType.getName()))
                    {
                        pairs.add(new Pair<>(srcUmlType,dstUmlType));
                        break;
                    }
                }
            }
        }
        return pairs;
    }

    private void processFieldDeclaration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, MultiMappingStore mappingStore)
    {
        Tree srcFieldDeclaration =Tree.findByLocationInfo(srcTree,srcUMLAttribute.getLocationInfo()).getParent(); //TODO
        Tree dstFieldDeclaration =Tree.findByLocationInfo(dstTree,dstUMLAttribute.getLocationInfo()).getParent(); //TODO
        if (srcFieldDeclaration.getMetrics().hash == dstFieldDeclaration.getMetrics().hash ||
            srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
        {
            // TODO: 8/3/2022 isoStructural can't be a good idea here, i.e anonymous class
            mappingStore.addMappingRecursively(srcFieldDeclaration,dstFieldDeclaration);
            return;
        }
        mappingStore.addMapping(srcFieldDeclaration,dstFieldDeclaration);
        matchFieldAllModifiers(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute,dstUMLAttribute,mappingStore);
        Tree srcType = Tree.findByLocationInfo(srcTree,srcUMLAttribute.getType().getLocationInfo());
        Tree dstType = Tree.findByLocationInfo(dstTree,dstUMLAttribute.getType().getLocationInfo());
        if (srcType.isIsoStructuralTo(dstType)) mappingStore.addMappingRecursively(srcType,dstType);
        Tree srcVarDeclaration = Tree.findByLocationInfo(srcTree,srcUMLAttribute.getVariableDeclaration().getLocationInfo());
        Tree dstVarDeclaration = Tree.findByLocationInfo(dstTree,dstUMLAttribute.getVariableDeclaration().getLocationInfo());
        mappingStore.addMapping(srcVarDeclaration,dstVarDeclaration);
        new LeafMatcher().match(srcVarDeclaration,dstVarDeclaration,null,mappingStore);
        processAttributeJavaDoc(srcTree,dstTree,srcUMLAttribute.getJavadoc(),dstUMLAttribute.getJavadoc(),mappingStore);
        mappingStore.addMapping(srcVarDeclaration.getChild(0),dstVarDeclaration.getChild(0));

    }

    private void processAttributeJavaDoc(Tree srcTree,Tree dstTree, UMLJavadoc srcJavaDoc, UMLJavadoc dstJavaDoc, MultiMappingStore mappingStore) {
        if (srcJavaDoc == null || dstJavaDoc == null) return;
        Tree srcJavaDocTree = Tree.findByLocationInfo(srcTree,srcJavaDoc.getLocationInfo());
        Tree dstJavaDocTree = Tree.findByLocationInfo(dstTree,dstJavaDoc.getLocationInfo());
        if (srcJavaDocTree != null && dstJavaDocTree != null)
            if (srcJavaDocTree.isIsoStructuralTo(dstJavaDocTree))
                mappingStore.addMappingRecursively(srcJavaDocTree,dstJavaDocTree);
    }

    private void matchFieldAllModifiers(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute, MultiMappingStore mappingStore) {
        Pair<Tree, Tree> attributeAccessModifierPair = findAttributeAccessModifierPair(srcFieldDeclaration, dstFieldDeclaration, srcUMLAttribute, dstUMLAttribute);
        if (attributeAccessModifierPair.first != null && attributeAccessModifierPair.second != null)
            mappingStore.addMapping(attributeAccessModifierPair.first, attributeAccessModifierPair.second);
        if (srcUMLAttribute.isFinal() && dstUMLAttribute.isFinal())
            matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"final",mappingStore);
        if (srcUMLAttribute.isFinal() && dstUMLAttribute.isVolatile())
            matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"volatile",mappingStore);
        if (srcUMLAttribute.isFinal() && dstUMLAttribute.isStatic())
            matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"static",mappingStore);
        if (srcUMLAttribute.isFinal() && dstUMLAttribute.isTransient())
            matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"transient",mappingStore);
    }

    private void matchModifierForFeild(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String modifier, MultiMappingStore mappingStore) {
        Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, modifier);
        Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, modifier);
        if (srcModifierTree != null && dstModifierTree != null)
            mappingStore.addMapping(srcModifierTree,dstModifierTree);

    }

    private Pair<Tree, Tree> findAttributeAccessModifierPair(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute) {
        Tree srcAccessModifier = findAttributeAccessModifier(srcFieldDeclaration);
        Tree dstAccessModifier = findAttributeAccessModifier(dstFieldDeclaration);
        return new Pair<>(srcAccessModifier,dstAccessModifier);
    }

    private Tree findAttributeAccessModifier(Tree anyFieldDeclaration) {
        if (anyFieldDeclaration.getChildren().size() > 0)
        {
            for (Tree child : anyFieldDeclaration.getChildren())
            {
                if (child.getType().name.equals("AccessModifier"))
                    return child;
                if (child.getType().name.equals("VariableDeclarationFragment"))
                    break;
            }
        }
        return null;
    }
    private Tree findAttributeModifierByLabel(Tree anyFieldDeclaration,String label) {
        if (anyFieldDeclaration.getChildren().size() > 0)
        {
            for (Tree child : anyFieldDeclaration.getChildren())
            {
                if (child.getLabel().equals(label))
                    return child;
                if (child.getType().name.equals("VariableDeclarationFragment"))
                    break;
            }
        }
        return null;
    }
    private Tree findAttributeTreeByType(Tree anyFieldDeclaration,String type) {
        if (anyFieldDeclaration.getChildren().size() > 0)
        {
            for (Tree child : anyFieldDeclaration.getChildren())
            {
                if (child.getType().name.equals(type))
                    return child;
                if (child.getType().name.equals("VariableDeclarationFragment"))
                    break;
            }
        }
        return null;
    }

    private void processSuperClass(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, MultiMappingStore mappingStore) {
        UMLType srcParentUML = classDiff.getOldSuperclass();
        UMLType dstParentUML = classDiff.getNewSuperclass();
        if (srcParentUML != null && dstParentUML != null) {
            Tree srcParentClassTree =Tree.findByLocationInfo(srcTree, srcParentUML.getLocationInfo());
            Tree dstParentClassTree =Tree.findByLocationInfo(dstTree, dstParentUML.getLocationInfo());
            if (srcParentClassTree.isIsomorphicTo(dstParentClassTree))
                mappingStore.addMappingRecursively(srcParentClassTree,dstParentClassTree);
        }
    }

    private void processPackageDeclaration(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, MultiMappingStore mappingStore) {
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


    private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, UMLOperationBodyMapper umlOperationBodyMapper, MultiMappingStore mappingStore) {

        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("Block");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        if (umlOperationBodyMapper.getOperation1().isStatic() && umlOperationBodyMapper.getOperation2().isStatic())
                matchModifier(srcOperationNode,dstOperationNode,"static",mappingStore);
        if (umlOperationBodyMapper.getOperation1().isFinal() && umlOperationBodyMapper.getOperation2().isFinal())
            matchModifier(srcOperationNode,dstOperationNode,"final",mappingStore);
        if (umlOperationBodyMapper.getOperation1().isAbstract() && umlOperationBodyMapper.getOperation2().isAbstract())
            matchModifier(srcOperationNode,dstOperationNode,"abstract",mappingStore);
    }

    private Pair<Tree, Tree> matchBasedOnType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
        Tree srcModifier = TreeUtils.findChildByType(srcOperationNode,searchingType);
        Tree dstModifier = TreeUtils.findChildByType(dstOperationNode,searchingType);
        if (srcModifier != null && dstModifier != null)
            return new Pair<>(srcModifier, dstModifier);
        return null;
    }

    private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, MultiMappingStore mappingStore) {
        String AST_type = "TypeDeclaration";
        if (classDiff.getOriginalClass().isEnum()) AST_type = "EnumDeclaration";
        Tree srcTypeDeclaration = Tree.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),AST_type);
        Tree dstTypeDeclaration = Tree.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),AST_type);
        mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("TYPE_DECLARATION_KIND");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcTypeDeclaration,dstTypeDeclaration,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
            matchModifier(srcTypeDeclaration,dstTypeDeclaration,"static",mappingStore);
        if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
            matchModifier(srcTypeDeclaration,dstTypeDeclaration,"final",mappingStore);
        if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
            matchModifier(srcTypeDeclaration,dstTypeDeclaration,"abstract",mappingStore);

        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
            Tree srcTypeParam = Tree.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = Tree.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        processSuperClass(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassAttributes(srcTree,dstTree,classDiff,mappingStore);
        processClassJavaDocs(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);

    }

    private void matchModifier(Tree srcTypeDeclaration, Tree dstTypeDeclaration, String modifier, MultiMappingStore mappingStore) {
        // TODO: 8/3/2022 Search should be limited to the method signature before the method name
        String type = "Modifier";
        Tree srcTree = findChildByTypeAndLabel(srcTypeDeclaration,type,modifier);
        Tree dstTree = findChildByTypeAndLabel(dstTypeDeclaration,type,modifier);
        if (srcTree != null && dstTree != null){
            mappingStore.addMapping(srcTree,dstTree);
        }
    }

}





