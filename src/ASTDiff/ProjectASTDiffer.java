package ASTDiff;

import actions.ASTDiff;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import jdt.CommentVisitor;
import matchers.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import tree.Tree;
import tree.TreeContext;
import tree.TreeUtils;
import utils.Pair;


import javax.servlet.ServletOutputStream;
import java.io.File;
import java.lang.reflect.Array;
import java.sql.Ref;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static tree.TreeUtils.findChildByTypeAndLabel;

public class ProjectASTDiffer
{
    private static final boolean _POST_PROCESS = true;
    private static final boolean _TREE_MATCHING = true;
    private Map<String, ASTDiff> astDiffMap = new HashMap<>();
    private UMLModelDiff umlModelDiff;

    private String srcPath;
    private String dstPath;

    private static final int MIN_ACCEPTABLE_HIGHT = 0;

    public String getSrcPath(){
        return srcPath;
    }
    public String getDstPath(){
        return dstPath;
    }

    public ProjectASTDiffer(UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
        this.umlModelDiff = umlModelDiff;
        umlModelDiff.getRefactorings();
        this.srcPath = this.umlModelDiff.getParentModel().rootFolder.getAbsolutePath();
        this.dstPath = this.umlModelDiff.getChildModel().rootFolder.getAbsolutePath();
    }

    public void diff() throws RefactoringMinerTimedOutException {
        this.commonClasses();
        computeAllEditScripts();
    }
    public ASTDiff getASTDiffbyFileName(String filename)
    {
        return this.astDiffMap.get(filename);
    }

    private void commonClasses() throws RefactoringMinerTimedOutException {
        List<UMLClassDiff> commons = this.umlModelDiff.getCommonClassDiffList();
        for (UMLClassDiff classdiff : commons) {
            ASTDiff classASTDiff = process(classdiff, findTreeContexts(classdiff));
            String fullPath = getSrcPath() + File.separator + classdiff.getOriginalClass().getSourceFile();

            if (this.astDiffMap.containsKey(fullPath))
            {
                this.astDiffMap.get(fullPath).mappings.mergeMappings(classASTDiff.mappings);
            }
            else
                this.astDiffMap.put(fullPath, classASTDiff);
        }
    }

    public void computeAllEditScripts() {

        for (Map.Entry<String,ASTDiff> entry : this.astDiffMap.entrySet())
        {
            entry.getValue().computeEditScript(
                    this.umlModelDiff.getParentModel().getTreeContextMap(),
                    this.umlModelDiff.getChildModel().getTreeContextMap()
            );
        }
    }

    private ASTDiff process(UMLClassDiff classdiff, Pair<TreeContext, TreeContext> treeContextPair) throws RefactoringMinerTimedOutException {
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
        processModelDiffRefactorings(srcTree,dstTree,classdiff,umlModelDiff.getRefactorings(),mappingStore);
        addAndProcessComments(treeContextPair.first, treeContextPair.second,mappingStore);
        return new ASTDiff(treeContextPair.first, treeContextPair.second, mappingStore);
    }

    private void processModelDiffRefactorings(Tree srcTree, Tree dstTree, UMLClassDiff classDiff, List<Refactoring> refactorings, MultiMappingStore mappingStore) {

            for(Refactoring refactoring : refactorings)
            {
                System.out.println("");
                List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).toList();
                List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).toList();
                if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()))
                {

                    if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION))
                    {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                        String otherFileName = pushDownOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
                        Tree otherFileTree = this.umlModelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
                        processMethod(otherFileTree,dstTree,pushDownOperationRefactoring.getBodyMapper(),mappingStore);
                    }
                }
                else if (beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName()))
                {
                    if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION))
                    {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                        String otherFileName = pushDownOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
                        Tree otherFileTree = this.umlModelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
                        processMethod(srcTree,otherFileTree,pushDownOperationRefactoring.getBodyMapper(),mappingStore);
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
        Tree srcOperationNode = findByLocationInfo(srcTree,umlOperationBodyMapper.getOperation1().getLocationInfo());
        Tree dstOperationNode = findByLocationInfo(dstTree,umlOperationBodyMapper.getOperation2().getLocationInfo());
        mappingStore.addMapping(srcOperationNode,dstOperationNode);
        processMethodSignature(srcOperationNode,dstOperationNode,mappingStore);
        fromRefMiner(srcTree,dstTree,umlOperationBodyMapper.getMappings(),mappingStore);
    }

    private void processMethodParameters(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, MultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
            VariableDeclaration leftPair = matchedPair.getLeft();
            VariableDeclaration rightPair = matchedPair.getRight();
            Tree leftTree  = findByLocationInfo(srcTree,leftPair.getLocationInfo());
            Tree rightTree = findByLocationInfo(dstTree,rightPair.getLocationInfo());
            if (leftTree.getParent().getType().name.equals("MethodDeclaration") &&
                    rightTree.getParent().getType().name.equals("MethodDeclaration"))
                mappingStore.addMappingRecursively(leftTree,rightTree);
        }
    }

    private void fromRefMiner(Tree srcTree, Tree dstTree, Set<AbstractCodeMapping> mappingSet, MultiMappingStore mappingStore) {
        ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(mappingSet);
        for (AbstractCodeMapping abstractCodeMapping : mappings)
        {
            if (abstractCodeMapping instanceof LeafMapping) {
                processLeafMapping(srcTree,dstTree,(LeafMapping)abstractCodeMapping,mappingStore);
            }
            else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
            {
                processCompositeMapping(srcTree,dstTree,(CompositeStatementObjectMapping)abstractCodeMapping,mappingStore);
            }
        }
    }

    private void processCompositeMapping(Tree srcTree, Tree dstTree, CompositeStatementObjectMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        Tree srcStatementNode = findByLocationInfo(srcTree,abstractCodeMapping.getFragment1().getLocationInfo());
        Tree dstStatementNode = findByLocationInfo(dstTree,abstractCodeMapping.getFragment2().getLocationInfo());
        if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
        {
            mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
        }
        else {
            mappingStore.addMapping(srcStatementNode,dstStatementNode);
            if ( (srcStatementNode.getType().name.equals("TryStatement") && dstStatementNode.getType().name.equals("TryStatement")) ||
                    (srcStatementNode.getType().name.equals("CatchClause") && dstStatementNode.getType().name.equals("CatchClause")))
                matchBlocks(srcStatementNode,dstStatementNode,mappingStore);
            CompositeStatementObject frag1Comp = (CompositeStatementObject) (abstractCodeMapping.getFragment1());
            CompositeStatementObject frag2Comp = (CompositeStatementObject) (abstractCodeMapping.getFragment2());

            List<AbstractExpression> frag1ExprList = new ArrayList<>(frag1Comp.getExpressions());
            List<AbstractExpression> frag2ExprList = new ArrayList<>(frag2Comp.getExpressions());

            if (srcStatementNode.getType().name.equals("ForStatement")
                &&
                dstStatementNode.getType().name.equals("ForStatement"))
            {

            }
            ListIterator<AbstractExpression> iter1 = frag1ExprList.listIterator();
            ListIterator<AbstractExpression> iter2 = frag2ExprList.listIterator();

            while (iter1.hasNext())
            {
                AbstractExpression frag1Expr = iter1.next();
                while (iter2.hasNext())
                {
                    AbstractExpression frag2Expr = iter2.next();
                    if (frag1Expr.getExpression().equals(frag2Expr.getExpression()))
                    {
                        Tree frag1Node = findByLocationInfo(srcTree,frag1Expr.getLocationInfo());
                        Tree frag2Node = findByLocationInfo(dstTree,frag2Expr.getLocationInfo());
                        if ((frag1Node.getParent().getType().name.equals("IfStatement") &&
                                frag2Node.getParent().getType().name.equals("IfStatement")))
                            mappingStore.addMapping(frag1Node.getParent(),frag2Node.getParent());
                        mappingStore.addMappingRecursively(frag1Node,frag2Node);
                        iter1.remove();
                        iter2.remove();
                        break;
                    }
                }
            }

        }
    }

    private void processLeafMapping(Tree srcTree, Tree dstTree, LeafMapping abstractCodeMapping, MultiMappingStore mappingStore) {
        Tree srcStatementNode = findByLocationInfo(srcTree,abstractCodeMapping.getFragment1().getLocationInfo());
        Tree dstStatementNode = findByLocationInfo(dstTree,abstractCodeMapping.getFragment2().getLocationInfo());
        if (abstractCodeMapping.getFragment2().toString().equals(abstractCodeMapping.getFragment1().toString())) {
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
                mappingStore.addListOfMapping(match(srcStatementNode, dstStatementNode,mappingStore));
            }
        }
    }

    private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, MultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
            Tree srcClassAnnotationTree = findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
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

    private void processClassJavaDocs(Tree srcTree, Tree dstTree, UMLClassDiff classdiff, MultiMappingStore mappingStore) {
        UMLJavadoc javadoc1 = classdiff.getOriginalClass().getJavadoc();
        UMLJavadoc javadoc2 = classdiff.getNextClass().getJavadoc();
        if (javadoc1 != null && javadoc2 != null) {
            Tree srcJavaDocNode = findByLocationInfo(srcTree, javadoc1.getLocationInfo());
            Tree dstJavaDocNode = findByLocationInfo(dstTree, javadoc2.getLocationInfo());
            mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
        }
    }

    private List<Pair<Tree, Tree>> processMethodJavaDoc(Tree srcTree, Tree dstTree, UMLJavadoc javadoc1, UMLJavadoc javadoc2, MultiMappingStore mappingStore) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        if (javadoc1 != null && javadoc2 != null)
            if (javadoc1.equalText(javadoc2))
            {
                Tree srcJavaDocNode = findByLocationInfo(srcTree,javadoc1.getLocationInfo());
                Tree dstJavaDocNode = findByLocationInfo(dstTree,javadoc2.getLocationInfo());
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
                    pairlist.add(new Pair<>(findByLocationInfo(srcTree,umlComment1.getLocationInfo()),findByLocationInfo(dstTree,umlComment2.getLocationInfo())));
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
            Tree srcTypeParam = findByLocationInfo(srcTree, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = findByLocationInfo(dstTree, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations())
        {
            Tree srcClassAnnotationTree = findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
        }
        Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
        if (commonExceptionTypes != null)
        {
            for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
                Tree srcExceptionNode = findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo());
                Tree dstExceptionNode = findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo());
                mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
            }
        }
        if (!umlOperationDiff.isReturnTypeChanged()) {
            if (umlOperationDiff.getAddedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
                LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
                LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
                Tree srcNode = findByLocationInfo(srcTree, srcLocationInfo);
                Tree dstNode = findByLocationInfo(dstTree, dstLocationInfo);
                mappingStore.addMappingRecursively(srcNode,dstNode);
            }
        }
    }

    public List<Pair<Tree, Tree>> match(Tree src, Tree dst,MultiMappingStore mappingStore) {
        System.out.println("abcd");
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        if (!_TREE_MATCHING) return pairlist;
        Function<Tree, Integer> HEIGHT_PRIORITY_CALCULATOR = (Tree t) -> t.getMetrics().height;

        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);

        while (PriorityTreeQueue.synchronize(srcTrees, dstTrees)) {
            var localHashMappings = new HashBasedMapper();
            localHashMappings.addSrcs(srcTrees.pop());
            localHashMappings.addDsts(dstTrees.pop());


            localHashMappings.unique().forEach(
//                    (pair) -> MultiMappingStore.recursivePairings(pair.first.stream().findAny().get(), pair.second.stream().findAny().get(), pairlist));
                    (pair) -> mappingStore.addMappingRecursively(pair.first.stream().findAny().get(), pair.second.stream().findAny().get()));

            localHashMappings.unmapped().forEach((pair) -> {
                pair.first.forEach(tree -> srcTrees.open(tree));
                pair.second.forEach(tree -> dstTrees.open(tree));
            });
        }
        System.out.println("");
        if (_POST_PROCESS) {
            greedyMatcher(src,dst,mappingStore);
        }
        return pairlist;
    }

    private void postProcess(Pair<List<Tree>, List<Tree>> complementPair, List<Pair<Tree, Tree>> pairlist) {
        List<Tree> srcComplement = complementPair.first;
        List<Tree> dstComplement = complementPair.second;
        for(Tree srcSubTree : srcComplement)
        {
            Tree dstSelected = findSimilarCriteria(srcSubTree,dstComplement);
            if (dstSelected == null) continue;
            dstComplement.remove(dstSelected);
            pairlist.add(new Pair<>(srcSubTree,dstSelected));
        }
    }

    private Tree findSimilarCriteria(Tree srcSubTree, List<Tree> dstComplement) {
        String searchingType = srcSubTree.getType().name;
        List<Tree> candidates = new ArrayList<>();
        for (Tree dstSubTree : dstComplement)
        {
            if (dstSubTree.getType().name.equals(searchingType))
                candidates.add(dstSubTree);
        }
        if (candidates.isEmpty()) return null;
        return findTheBest(srcSubTree,candidates);
    }

    private Tree findTheBest(Tree srcSubTree, List<Tree> candidates) {
        int serachingHeight = srcSubTree.getMetrics().depth;
        if (candidates.size() == 1) return candidates.get(0);
        for (Tree dstSubTree : candidates)
        {
            if (serachingHeight == dstSubTree.getMetrics().depth)
                return dstSubTree;
        }
        return null;
        //TODO
    }

    private Pair<List<Tree>, List<Tree>> calcComplements(Tree src, Tree dst, List<Pair<Tree, Tree>> pairlist) {
        List<Tree> srcMatchedList = pairlist.stream().map(pair -> pair.first).collect(Collectors.toList());
        List<Tree> dstMatchedList = pairlist.stream().map(pair -> pair.second).collect(Collectors.toList());

        List<Tree> srcComplement = src.getCustomDescendants("Block");
        srcComplement.removeAll(srcMatchedList);

        List<Tree> dstComplement = dst.getCustomDescendants("Block");
        dstComplement.removeAll(dstMatchedList);
        return new Pair<>(srcComplement,dstComplement);
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

                Tree srcAttrTree = findByLocationInfo(srcTree,renameAttributeRefactoring.getOriginalAttribute().getLocationInfo());
                Tree dstAttrTree = findByLocationInfo(dstTree,renameAttributeRefactoring.getRenamedAttribute().getLocationInfo());
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
                Tree dstVariableType = findByLocationInfo(dstTree,newVariable.getType().getLocationInfo());
                Tree dstVariableDeclaration = findByLocationInfo(dstTree,newVariable.getLocationInfo());
                List<Tree> dstChildrenList = dstVariableDeclaration.getChildren().stream().toList();
                Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
                for (VariableDeclaration variableDeclaration : mergedVariables)
                {
                    Tree srcVariableDeclaration = findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
                    Tree srcVariableType = findByLocationInfo(srcTree,variableDeclaration.getType().getLocationInfo());
                    List<Tree> srcChildrenList = srcVariableDeclaration.getChildren().stream().toList();
                    Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
//                    mappingStore.addMapping(srcVariableDeclaration,dstVariableDeclaration);
                    mappingStore.addMapping(srcVariableType,dstVariableType.getChild(0));
                    mappingStore.addMapping(srcVarName,dstVarName);

                }
            }
        }
    }

    private void processClassImplemetedInterfaces(Tree srcTree, Tree dstTree, UMLClassDiff classdiff, MultiMappingStore mappingStore) {
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
                        mappingStore.addMappingRecursively(srcInterfaceTree, dstInterfaceTree);
                        break;
                    }
                }

            }

        }
    }

    private void processClassAttributes(Tree srcTree, Tree dstTree, UMLClassDiff classdiff, MultiMappingStore mappingStore) {
        List<Pair<Pair<UMLAttribute, UMLAttribute>, Pair<Tree, Tree>>> matchedAttributes = findMatchedAttributesTree(srcTree, dstTree, classdiff);


        for (Pair<Pair<UMLAttribute, UMLAttribute>, Pair<Tree, Tree>> matchedpair : matchedAttributes) {
            Pair<UMLAttribute, UMLAttribute> umlAttrPair = matchedpair.first;
            if (matchedpair.second.first.getMetrics().hash == matchedpair.second.second.getMetrics().hash)
            {
                System.out.println(umlAttrPair.first);
                System.out.println(umlAttrPair.second);
                mappingStore.addMappingRecursively(matchedpair.second.first, matchedpair.second.second);
            }
            else {
                processFeildDeclration(matchedpair,mappingStore);
            }
        }
    }
    private void processFeildDeclration(Pair<Pair<UMLAttribute, UMLAttribute>, Pair<Tree, Tree>> matchedpair, MultiMappingStore mappingStore)
    {
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("VariableDeclarationFragment");

        Tree srcFeildDelcration = matchedpair.second.first;
        Tree dstFeildDeclration = matchedpair.second.second;
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
            if (matchedpair.first.first.getVariableDeclaration().getInitializer() != null &&
            matchedpair.first.second.getVariableDeclaration().getInitializer() != null)
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
    private List<Pair<Pair<UMLAttribute,UMLAttribute>, Pair<Tree, Tree>>> findMatchedAttributesTree(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        List<Pair<Pair<UMLAttribute,UMLAttribute>, Pair<Tree, Tree>>> pairlist = new ArrayList<>();

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
                        pairlist.add(
                                new Pair<>
                                        (
                                        new Pair<>(srcUmltype,dstUmlType),
                                        new Pair<>(srcFindedTree.getParent(), dstFindedTree.getParent())
                                        )
                        );
                        break;
                    }
                }
            }
        }
        return pairlist;
    }


    private void processSuperClass(Tree srcTree, Tree dstTree, UMLClassDiff classdiff, MultiMappingStore mappingStore) {
        UMLType srcParentUML = classdiff.getOldSuperclass();
        UMLType dstParentUML = classdiff.getNewSuperclass();
        if (srcParentUML != null && dstParentUML != null) {
            Tree srcParentClassTree = findByLocationInfo(srcTree, srcParentUML.getLocationInfo());
            Tree dstParentClassTree = findByLocationInfo(dstTree, dstParentUML.getLocationInfo());
            mappingStore.addMappingRecursively(srcParentClassTree,dstParentClassTree);
        }
    }

    private void processPackageDeclaration(Tree srcTree, Tree dstTree, UMLClassDiff classdiff, MultiMappingStore mappingStore) {
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

    private static Tree findByLocationInfo(Tree tree, LocationInfo locationInfo){
        int startoffset = locationInfo.getStartOffset();
        int endoffset = locationInfo.getEndOffset();
        Tree result = tree.getTreeBetweenPositions(startoffset, endoffset);
        return result;
    }
    private static Tree findByLocationInfo(Tree tree, LocationInfo locationInfo, String type){
        int startoffset = locationInfo.getStartOffset();
        int endoffset = locationInfo.getEndOffset();
        Tree result = tree.getTreeBetweenPositions(startoffset, endoffset,type);
        return result;
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

    private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassDiff classdiff, MultiMappingStore mappingStore) {
        Tree srcTypeDeclartion = findByLocationInfo(srcTree,classdiff.getOriginalClass().getLocationInfo(),"TypeDeclaration");
        Tree dstTypeDeclartion = findByLocationInfo(dstTree,classdiff.getNextClass().getLocationInfo(),"TypeDeclaration");
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
            Tree srcTypeParam = findByLocationInfo(srcTypeDeclartion, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = findByLocationInfo(dstTypeDeclartion, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        processSuperClass(srcTypeDeclartion,dstTypeDeclartion,classdiff,mappingStore);
        processClassImplemetedInterfaces(srcTypeDeclartion,dstTypeDeclartion,classdiff,mappingStore);
        processClassAttributes(srcTypeDeclartion,dstTypeDeclartion,classdiff,mappingStore);
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

    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassDiff classDiff) {
        //TODO: find corresponding comps

//        String filename = rootpath + File.separator + classDiff.getOriginalClass().getSourceFile();
        String filename = classDiff.getOriginalClass().getSourceFile();
        return new Pair<>
                (this.umlModelDiff.getParentModel().getTreeContextMap().get(filename),
                 this.umlModelDiff.getChildModel().getTreeContextMap().get(filename));
    }
    public MultiMappingStore greedyMatcher(Tree src, Tree dst, MultiMappingStore mappings) {
        double simThreshold = 0.5;
//        if (true) return mappings;
        for (Tree t : src.postOrder()) {
            boolean _flag = true;
            if (mappings.isSrcMapped(t)) {
                ArrayList<Tree> temp = new ArrayList<>(mappings.getDstForSrc(t));
                for (Tree tree : temp) {
                    if (dst.getDescendantsAndItself().contains(tree)) {
                        _flag = false;
                        break;
                    }
                }
            }
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                break;
            }
            else if (!(mappings.isSrcMapped(t) || t.isLeaf()) || (!t.isLeaf() && _flag)) {
                List<Tree> candidates = getDstCandidates(mappings, t,dst);
                Tree best = null;
                double max = -1D;
                for (Tree cand : candidates) {
                    double sim = SimilarityMetrics.diceSimilarity(t, cand, mappings);
                    if (sim > max && sim >= simThreshold) {
                        max = sim;
                        best = cand;
                    }
                }

                if (best != null) {
                    mappings.addMapping(t, best);
                }
            }
        }
        return mappings;
    }
    protected List<Tree> getDstCandidates(MultiMappingStore mappings, Tree src, Tree checkingInsideDst) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            if (mappings.isSrcMapped(c)) {
                for (Tree t : mappings.getDstForSrc(c)) {
                    if (checkingInsideDst.getDescendantsAndItself().contains(t))
                        seeds.add(t);
                }
            }
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (Tree seed : seeds) {
            while (seed.getParent() != null) {
                Tree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !parent.isRoot())
                    if (!(mappings.isDstMapped(parent)))
                        candidates.add(parent);
                    else
                    {
                        List<Tree> mappedParent = mappings.getSrcForDst(parent).stream().toList();
                        boolean flag = true;
                        for(Tree mp : mappedParent)
                        {
                            if (checkingInsideDst.getDescendantsAndItself().contains(mp))
                            {
                                flag = false;
                                break;
                            }
                        }
                        if (flag)
                            candidates.add(parent);
                    }
                seed = parent;
            }
        }

        return candidates;
    }
}





