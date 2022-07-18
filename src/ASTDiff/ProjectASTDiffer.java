package ASTDiff;

import actions.ASTDiff;
import actions.EditScript;
import actions.SimplifiedChawatheScriptGenerator;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import matchers.MappingStore;
import tree.Tree;
import tree.TreeContext;
import utils.Pair;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private ASTDiff process(UMLClassDiff classdiff, Pair<TreeContext, TreeContext> tcs) {
        TreeContext parentTreeCTX = tcs.first;
        TreeContext childTreeCTX = tcs.second;
        List<UMLOperationBodyMapper> umlOperationBodyMapperList = classdiff.getOperationBodyMapperList();
        UMLOperationBodyMapper temp = umlOperationBodyMapperList.get(0);
        LocationInfo locationInParent =  temp.getContainer1().getLocationInfo();
        LocationInfo locationInChild  =  temp.getContainer2().getLocationInfo();
        Tree childNode = childTreeCTX.getRoot().getTreeBetweenPositions(locationInChild.getStartOffset(), locationInChild.getEndOffset());
        Tree parentNode = parentTreeCTX.getRoot().getTreeBetweenPositions(locationInParent.getStartOffset(), locationInParent.getEndOffset());

        Tree test = childTreeCTX.getRoot().getTreeBetweenPositions(90,93);
//        Tree test = childTreeCTX.getRoot().getTreeBetweenPositions(94,

        MappingStore m = new MappingStore(parentTreeCTX.getRoot(),childTreeCTX.getRoot());
        m.addMapping(parentNode,childNode);
        m.addMapping(parentTreeCTX.getRoot(),childTreeCTX.getRoot());
        EditScript es = new SimplifiedChawatheScriptGenerator().computeActions(m);

        return new ASTDiff(parentTreeCTX,childTreeCTX,m,es);
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





