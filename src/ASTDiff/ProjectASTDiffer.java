package ASTDiff;

import actions.ASTDiff;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import jdt.CommentVisitor;
import matchers.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import tree.Tree;
import tree.TreeContext;
import tree.TreeUtils;
import utils.Pair;


import java.io.File;
import java.util.*;
import java.util.function.Function;

import static tree.TreeUtils.findChildByTypeAndLabel;

public class ProjectASTDiffer
{
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
            entry.getValue().computeEditScript();
        }
    }

    private ASTDiff process(UMLClassDiff classdiff, Pair<TreeContext, TreeContext> treeContextPair) throws RefactoringMinerTimedOutException {
        Tree srcTree = treeContextPair.first.getRoot();
        Tree dstTree = treeContextPair.second.getRoot();
        MappingStore mappingStore = new MappingStore(srcTree,dstTree);
        mappingStore.addMapping(srcTree,dstTree);
        List<Refactoring> ret = classdiff.getRefactorings();


        List<Tree> test = new ArrayList<>();
        mappingStore.addListOfMapping(processRefactorings(srcTree,dstTree,classdiff));

        mappingStore.addPairRecursively(processPackageDeclaration(srcTree,dstTree,classdiff));
        mappingStore.addListOfMappingRecursively(processImports(srcTree,dstTree,classdiff.getImportDiffList()));

        Tree srcClassTree = findByLocationInfo(srcTree,classdiff.getOriginalClass().getLocationInfo(),"TypeDeclaration");
        Tree dstClassTree = findByLocationInfo(dstTree,classdiff.getNextClass().getLocationInfo(),"TypeDeclaration");
        mappingStore.addListOfMapping(classDeclarationMapping(srcClassTree,dstClassTree,classdiff));
        mappingStore.addPairRecursively(processSuperClass(srcTree,dstTree,classdiff));
        mappingStore.addListOfMappingRecursively(processClassImplemetedInterfaces(srcTree,dstTree,classdiff));
        mappingStore.addListOfMapping(processClassAttributes(srcTree,dstTree,classdiff));
        mappingStore.addListOfMapping(processClassJavaDocs(srcTree,dstTree,classdiff));


        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : classdiff.getAnnotationListDiff().getCommonAnnotations()) {
            Tree srcClassAnnotationTree = findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
        }




        ArrayList<UMLOperationBodyMapper> operationBodyMapperList = new ArrayList<>(classdiff.getOperationBodyMapperList());





        for(UMLOperationBodyMapper umlOperationBodyMapper : operationBodyMapperList)
        {

            System.out.println("OperationName " + umlOperationBodyMapper);
            Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();
            mappingStore.addListOfMapping(processOperationDiff(srcTree,dstTree,umlOperationBodyMapper));



            for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: umlOperationBodyMapper.getMatchedVariables()) {
                VariableDeclaration leftPair = matchedPair.getLeft();
                VariableDeclaration rightPair = matchedPair.getRight();
                Tree leftTree = findByLocationInfo(srcTree,leftPair.getLocationInfo());
                Tree rightTree = findByLocationInfo(dstTree,rightPair.getLocationInfo());
                if (leftTree.getParent().getType().name.equals("MethodDeclaration") &&
                    rightTree.getParent().getType().name.equals("MethodDeclaration"))
                    mappingStore.addMappingRecursively(leftTree,rightTree);
            }

//            mappingStore.addListOfMapping(processCommentsInsideMethod(srcTree, dstTree, umlOperationBodyMapper.getOperation1(),umlOperationBodyMapper.getOperation2()));
            mappingStore.addListOfMapping(processMethodJavaDoc(srcTree, dstTree, umlOperationBodyMapper.getOperation1().getJavadoc(),umlOperationBodyMapper.getOperation2().getJavadoc()));


            Tree srcOperationNode = srcTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation1().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation1().getLocationInfo().getEndOffset());
            Tree dstOperationNode = dstTree.getTreeBetweenPositions(umlOperationBodyMapper.getOperation2().getLocationInfo().getStartOffset(),umlOperationBodyMapper.getOperation2().getLocationInfo().getEndOffset());
            mappingStore.addMapping(srcOperationNode,dstOperationNode);
            mappingStore.addListOfMapping(processMethodSignature(srcOperationNode,dstOperationNode));
            ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(umlOperationBodyMapper.getMappings());

            for (AbstractCodeMapping abstractCodeMapping : mappings)
            {
                Tree srcStatementNode = findByLocationInfo(srcTree,abstractCodeMapping.getFragment1().getLocationInfo());
                Tree dstStatementNode = findByLocationInfo(dstTree,abstractCodeMapping.getFragment2().getLocationInfo());

                if (abstractCodeMapping instanceof LeafMapping) {
                    if (abstractCodeMapping.getFragment2().toString().equals(abstractCodeMapping.getFragment1().toString())) {
                        mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
                    }
                    else {
                        if (srcStatementNode.isIsoStructuralTo(dstStatementNode))
                        {
                            mappingStore.addMappingRecursively(srcStatementNode,dstStatementNode);
                        }
                        else {
                            mappingStore.addMapping(srcStatementNode, dstStatementNode);
                            mappingStore.addListOfMapping(match(srcStatementNode, dstStatementNode));
                        }
                    }
                }
                else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
                {
                    if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
                    {
                        mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
                    }
                    else {
                        mappingStore.addMapping(srcStatementNode,dstStatementNode);
                        CompositeStatementObject frag1Comp = (CompositeStatementObject) (abstractCodeMapping.getFragment1());
                        CompositeStatementObject frag2Comp = (CompositeStatementObject) (abstractCodeMapping.getFragment2());
                        List<AbstractExpression> frag1ExprList = frag1Comp.getExpressions();
                        List<AbstractExpression> frag2ExprList = frag2Comp.getExpressions();
                        for (AbstractExpression frag1Expr : frag1ExprList) {
                            for (AbstractExpression frag2Expr : frag2ExprList)
                            {
                                if (frag1Expr.getExpression().equals(frag2Expr.getExpression()))
                                {
                                    Tree frag1Node = findByLocationInfo(srcTree,frag1Expr.getLocationInfo());
                                    Tree frag2Node = findByLocationInfo(dstTree,frag2Expr.getLocationInfo());
                                    if (frag1Node.getParent().getType().name.equals("IfStatement") &&
                                        frag2Node.getParent().getType().name.equals("IfStatement"))
                                        mappingStore.addMapping(frag1Node.getParent(),frag2Node.getParent());
                                    mappingStore.addMappingRecursively(frag1Node,frag2Node);
                                    break;
                                }
                            }
                        }

                    }
                }
            }

        }

        Pair<List<Tree>, List<Tree>> addedCommentsPair = addComments(treeContextPair.first, treeContextPair.second);
        matchComments(addedCommentsPair,mappingStore);


        return new ASTDiff(treeContextPair.first, treeContextPair.second, mappingStore);
    }

    private void matchComments(Pair<List<Tree>, List<Tree>> addedCommentsPair, MappingStore mappingStore) {
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

    private List<Pair<Tree, Tree>> processClassJavaDocs(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        UMLJavadoc javadoc1 = classdiff.getOriginalClass().getJavadoc();
        UMLJavadoc javadoc2 = classdiff.getNextClass().getJavadoc();
        if (javadoc1 != null && javadoc2 != null) {
            Tree srcJavaDocNode = findByLocationInfo(srcTree, javadoc1.getLocationInfo());
            Tree dstJavaDocNode = findByLocationInfo(dstTree, javadoc2.getLocationInfo());
            pairlist.addAll(MappingStore.recursivePairings(srcJavaDocNode, dstJavaDocNode, null));
        }
        return pairlist;
    }

    private List<Pair<Tree, Tree>> processMethodJavaDoc(Tree srcTree, Tree dstTree, UMLJavadoc javadoc1, UMLJavadoc javadoc2) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        if (javadoc1 != null && javadoc2 != null)
            if (javadoc1.equalText(javadoc2))
            {
                Tree srcJavaDocNode = findByLocationInfo(srcTree,javadoc1.getLocationInfo());
                Tree dstJavaDocNode = findByLocationInfo(dstTree,javadoc2.getLocationInfo());
                pairlist.addAll(MappingStore.recursivePairings(srcJavaDocNode,dstJavaDocNode,null));
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

    private static List<Pair<Tree, Tree>> processOperationDiff(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper) {
        List<Pair<Tree, Tree>> pairlist = new ArrayList<>();
        UMLOperationDiff umlOperationDiff = umlOperationBodyMapper.getOperationSignatureDiff().isPresent() ? umlOperationBodyMapper.getOperationSignatureDiff().get() : null;
        if (umlOperationDiff == null) return pairlist;

        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations())
        {
            Tree srcClassAnnotationTree = findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            pairlist.addAll(MappingStore.recursivePairings(srcClassAnnotationTree,dstClassAnnotationTree,null));
        }
        Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
        if (commonExceptionTypes != null)
        {
            for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
                Tree srcExceptionNode = findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo());
                Tree dstExceptionNode = findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo());
                pairlist.addAll(MappingStore.recursivePairings(srcExceptionNode, dstExceptionNode, null));
            }
        }
        if (!umlOperationDiff.isReturnTypeChanged()) {
            if (umlOperationDiff.getAddedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
                LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
                LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
                Tree srcNode = findByLocationInfo(srcTree, srcLocationInfo);
                Tree dstNode = findByLocationInfo(dstTree, dstLocationInfo);
                pairlist.addAll(MappingStore.recursivePairings(srcNode, dstNode, null));
            }
        }
//        umlOperationDiff.getAddedOperation().getTypeParameters()
        return pairlist;
    }

    public List<Pair<Tree, Tree>> match(Tree src, Tree dst) {

        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        Function<Tree, Integer> HEIGHT_PRIORITY_CALCULATOR = (Tree t) -> t.getMetrics().height;

        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, MIN_ACCEPTABLE_HIGHT, HEIGHT_PRIORITY_CALCULATOR);

        while (PriorityTreeQueue.synchronize(srcTrees, dstTrees)) {
            var localHashMappings = new HashBasedMapper();
            localHashMappings.addSrcs(srcTrees.pop());
            localHashMappings.addDsts(dstTrees.pop());


            localHashMappings.unique().forEach(
                    (pair) -> MappingStore.recursivePairings(pair.first.stream().findAny().get(), pair.second.stream().findAny().get(), pairlist));

            localHashMappings.unmapped().forEach((pair) -> {
                pair.first.forEach(tree -> srcTrees.open(tree));
                pair.second.forEach(tree -> dstTrees.open(tree));
            });
        }
        return pairlist;
    }

    private List<Pair<Tree, Tree>> processRefactorings(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) throws RefactoringMinerTimedOutException {
        List<Pair<Tree, Tree>> pairlist = new ArrayList<>();
        for (Refactoring refactoring : classdiff.getRefactorings())
        {
            if (refactoring instanceof ExtractOperationRefactoring) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) (refactoring);
                UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                for (AbstractCodeMapping abstractCodeMapping : bodyMapper.getMappings())
                {
                    Tree srcStatementNode = findByLocationInfo(srcTree,abstractCodeMapping.getFragment1().getLocationInfo());
                    Tree dstStatementNode = findByLocationInfo(dstTree,abstractCodeMapping.getFragment2().getLocationInfo());
                    if (abstractCodeMapping instanceof LeafMapping) {
                        if (abstractCodeMapping.getFragment2().toString().equals(abstractCodeMapping.getFragment1().toString())) {
                            pairlist.addAll(MappingStore.recursivePairings(srcStatementNode, dstStatementNode, null));
                        }
                        else {
                            if (srcStatementNode.isIsoStructuralTo(dstStatementNode)) {
                                pairlist.addAll(MappingStore.recursivePairings(srcStatementNode, dstStatementNode, null));
                            }
                            else {
                                pairlist.add(new Pair<>(srcStatementNode, dstStatementNode));
                                pairlist.addAll(match(srcStatementNode, dstStatementNode));
                            }
                        }
                    }
                    else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
                    {
                        if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
                        {
                            pairlist.addAll(MappingStore.recursivePairings(srcStatementNode, dstStatementNode, null));
                        }
                        else
                        {
                            pairlist.add(new Pair<>(srcStatementNode,dstStatementNode));
                            CompositeStatementObject frag1Comp = (CompositeStatementObject) (abstractCodeMapping.getFragment1());
                            CompositeStatementObject frag2Comp = (CompositeStatementObject) (abstractCodeMapping.getFragment2());
                            List<AbstractExpression> frag1ExprList = frag1Comp.getExpressions();
                            List<AbstractExpression> frag2ExprList = frag2Comp.getExpressions();
                            for (AbstractExpression frag1Expr : frag1ExprList) {
                                for (AbstractExpression frag2Expr : frag2ExprList)
                                {
                                    if (frag1Expr.getExpression().equals(frag2Expr.getExpression()))
                                    {
                                        Tree frag1Node = findByLocationInfo(srcTree,frag1Expr.getLocationInfo());
                                        Tree frag2Node = findByLocationInfo(dstTree,frag2Expr.getLocationInfo());
                                        if (frag1Node.getParent().getType().name.equals("IfStatement") &&
                                                frag2Node.getParent().getType().name.equals("IfStatement"))
                                            pairlist.add(new Pair<>(frag1Node.getParent(),frag2Node.getParent()));
                                        pairlist.addAll(MappingStore.recursivePairings(frag1Node,frag2Node,null));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (refactoring instanceof RenameAttributeRefactoring) {
                RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) (refactoring);

                Tree srcAttrTree = findByLocationInfo(srcTree,renameAttributeRefactoring.getOriginalAttribute().getLocationInfo());
                Tree dstAttrTree = findByLocationInfo(dstTree,renameAttributeRefactoring.getRenamedAttribute().getLocationInfo());
                pairlist.addAll(MappingStore.recursivePairings(srcAttrTree.getParent(),dstAttrTree.getParent(),null));
            }
            else if (refactoring instanceof ExtractVariableRefactoring) {
                ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
//                System.out.println("hi");
            }
        }
        return pairlist;
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
        List<Pair<Pair<UMLAttribute, UMLAttribute>, Pair<Tree, Tree>>> matchedAttributes = findMatchedAttributesTree(srcTree, dstTree, classdiff);


        for (Pair<Pair<UMLAttribute, UMLAttribute>, Pair<Tree, Tree>> matchedpair : matchedAttributes) {
            Pair<UMLAttribute, UMLAttribute> umlAttrPair = matchedpair.first;
            if (matchedpair.second.first.getMetrics().hash == matchedpair.second.second.getMetrics().hash)
            {
                System.out.println(umlAttrPair.first);
                System.out.println(umlAttrPair.second);
                pairList.addAll(MappingStore.recursivePairings(matchedpair.second.first, matchedpair.second.second, null));
            }
            else {
                pairList.addAll(processFeildDeclration(matchedpair));
            }
//            pairList.add(new Pair<>(matchedpair.first,matchedpair.second));

        }
        return pairList;
    }
    private List<Pair<Tree,Tree>> processFeildDeclration(Pair<Pair<UMLAttribute, UMLAttribute>, Pair<Tree, Tree>> matchedpair)
    {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("VariableDeclarationFragment");

        Tree srcFeildDelcration = matchedpair.second.first;
        Tree dstFeildDeclration = matchedpair.second.second;
        pairlist.add(new Pair<>(srcFeildDelcration,dstFeildDeclration));
        for (String type : searchingTypes) {
            Pair<Tree, Tree> matched = matchBasedOnType(srcFeildDelcration,dstFeildDeclration, type);
            if (matched != null)
                pairlist.add(matched);
        }
        String serachingType = "SimpleType";
        Pair<Tree, Tree> result = matchBasedOnType(srcFeildDelcration, dstFeildDeclration, serachingType);
        if (result != null)
            pairlist.addAll(MappingStore.recursivePairings(result.first,result.second,null));
        serachingType = "VariableDeclarationFragment";
        Pair<Tree, Tree> VariableDeclarationFragmentPair = matchBasedOnType(srcFeildDelcration, dstFeildDeclration, serachingType);
        if (VariableDeclarationFragmentPair != null)
        {
            Pair<Tree, Tree> SimpleNamePair = matchBasedOnType(VariableDeclarationFragmentPair.first, VariableDeclarationFragmentPair.second, "SimpleName");
            pairlist.add(new Pair<>(SimpleNamePair.first,SimpleNamePair.second));
            if (matchedpair.first.first.getVariableDeclaration().getInitializer() != null &&
            matchedpair.first.second.getVariableDeclaration().getInitializer() != null)
            {
                if (VariableDeclarationFragmentPair.first.getChild(1).getType().equals(VariableDeclarationFragmentPair.second.getChild(1).getType()))
                    pairlist.add(new Pair<>(VariableDeclarationFragmentPair.first.getChild(1), VariableDeclarationFragmentPair.second.getChild(1)));
            }
            if (result != null) {
                pairlist.add(new Pair<>(result.first, result.second));
            }
        }
        return pairlist;
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


    private Pair<Tree, Tree> processSuperClass(Tree srcTree, Tree dstTree, UMLClassDiff classdiff) {
        UMLType srcParentUML = classdiff.getOldSuperclass();
        UMLType dstParentUML = classdiff.getNewSuperclass();
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

    private List<Pair<Tree, Tree>> processImports(Tree srcTree, Tree dstTree, UMLImportListDiff importDiffList) {
        List<Pair<Tree,Tree>> matchedTrees = new ArrayList<>();
        if (importDiffList == null) return matchedTrees;
        Set<String> commonImports = importDiffList.getCommonImports();
        if (commonImports.isEmpty())
             return matchedTrees;
        String searchingType = "ImportDeclaration";
        List<Tree> srcChildren = srcTree.getChildren();
        List<Tree> dstChildren = dstTree.getChildren();
        
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
    private List<Pair<Tree, Tree>> processMethodSignature(Tree srcOperationNode, Tree dstOperationNode) {
        //TODO: static and ... are also considered as Modifier for method declration
        //TODO: (cont.) probably, I have to change ASTVisitor to modify those types.
        List<Pair<Tree,Tree>> pairList = new ArrayList<>();
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("Modifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("SimpleName");
//        searchingTypes.add("SimpleType");
        // TODO: Above line was added to check the Exceptions, probably not the right way to handle this.
//        searchingTypes.add("SimpleType");
        searchingTypes.add("PrimitiveType");
        searchingTypes.add("Block");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
            if (matched != null)
                pairList.add(matched);
        }
//        pairList.addAll(processReturnType(srcOperationNode,dstOperationNode));
        return pairList;
    }

    private List<Pair<Tree, Tree>> processReturnType(Tree srcOperationNode, Tree dstOperationNode) {
        List<Pair<Tree, Tree>> pairlist = new ArrayList<>();
        String serachingType = "SimpleType";
        Pair<Tree, Tree> result = matchBasedOnType(srcOperationNode, dstOperationNode, serachingType);
        if (result != null)
            pairlist.addAll(MappingStore.recursivePairings(result.first,result.second,null));

        return pairlist;
    }


    private Pair<Tree, Tree> matchBasedOnType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
        Tree srcModifier = TreeUtils.findChildByType(srcOperationNode,searchingType);
        Tree dstModifier = TreeUtils.findChildByType(dstOperationNode,searchingType);
        if (srcModifier != null && dstModifier != null)
            return new Pair<>(srcModifier, dstModifier);
        return null;
    }

    private List<Pair<Tree, Tree>> classDeclarationMapping(Tree srcTypeDeclartion, Tree dstTypeDeclartion, UMLClassDiff classdiff) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        pairlist.add(new Pair<>(srcTypeDeclartion,dstTypeDeclartion));
        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add("AccessModifier");
        searchingTypes.add("SimpleName");
        searchingTypes.add("TYPE_DECLARATION_KIND");
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = matchBasedOnType(srcTypeDeclartion,dstTypeDeclartion,type);
            if (matched != null)
                pairlist.add(matched);
        }
        if (classdiff.getOriginalClass().isStatic() && classdiff.getNextClass().isStatic())
            pairlist.addAll(matchModifier(srcTypeDeclartion,dstTypeDeclartion,"static"));
        if (classdiff.getOriginalClass().isFinal() && classdiff.getNextClass().isFinal())
            pairlist.addAll(matchModifier(srcTypeDeclartion,dstTypeDeclartion,"final"));
        if (classdiff.getOriginalClass().isAbstract() && classdiff.getNextClass().isAbstract())
            pairlist.addAll(matchModifier(srcTypeDeclartion,dstTypeDeclartion,"abstract"));

        return pairlist;
    }

    private List<Pair<Tree, Tree>> matchModifier(Tree srcTypeDeclartion, Tree dstTypeDeclartion, String modifier) {
        List<Pair<Tree,Tree>> pairlist = new ArrayList<>();
        String type = "Modifier";
        Tree srcTree = findChildByTypeAndLabel(srcTypeDeclartion,type,modifier);
        Tree dstTree = findChildByTypeAndLabel(dstTypeDeclartion,type,modifier);
        if (srcTree != null && dstTree != null){
            pairlist.add(new Pair<>(srcTree,dstTree));
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


    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassDiff classDiff) {
        //TODO: find corresponding comps

//        String filename = rootpath + File.separator + classDiff.getOriginalClass().getSourceFile();
        String filename = classDiff.getOriginalClass().getSourceFile();
        return new Pair<>
                (this.umlModelDiff.getParentModel().getTreeContextMap().get(filename),
                 this.umlModelDiff.getChildModel().getTreeContextMap().get(filename));
    }
}





