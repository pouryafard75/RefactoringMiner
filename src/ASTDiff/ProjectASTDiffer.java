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


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectASTDiffer
{
    public Map<String, ASTDiff> astDiffMap = new HashMap<>();
    private UMLModelDiff umlModelDiff;
    String rootpath;

    public ProjectASTDiffer(UMLModelDiff umlModelDiff)
    {
        this.umlModelDiff = umlModelDiff;
        this.rootpath = this.umlModelDiff.getParentModel().rootFolder.getAbsolutePath();

    }

    public ASTDiff diff()
    {
        this.commonClasses();
        return null;
    }

    private void commonClasses() {
        List<UMLClassDiff> commons = this.umlModelDiff.getCommonClassDiffList();

        for (UMLClassDiff classdiff : commons) {
            this.astDiffMap.put(rootpath + classdiff.getOriginalClassName(), process(classdiff,findTreeContexts(classdiff)));
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
        MappingStore m = new MappingStore(parentTreeCTX.getRoot(),childTreeCTX.getRoot());
        m.addMapping(parentNode,childNode);
        m.addMapping(parentTreeCTX.getRoot(),childTreeCTX.getRoot());
        EditScript es = new SimplifiedChawatheScriptGenerator().computeActions(m);

        return new ASTDiff(parentTreeCTX,childTreeCTX,m,es);
    }


    private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassDiff classDiff) {
        //TODO: find corresponding comps
        String filename = "example.java";
        return new Pair<TreeContext,TreeContext>
                (this.umlModelDiff.getParentModel().getCompilationUnitMap().get(filename),
                 this.umlModelDiff.getChildModel().getCompilationUnitMap().get(filename));
    }
}





