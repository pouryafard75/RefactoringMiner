package actions.model;

import tree.Tree;

import java.util.*;

public class MultiMoveActionGenerator{
    private Map<Set<Tree>, Set<Tree>> fullMap;
    private Map<Tree,List<MultiMove>> actionMapSrc = new HashMap<>();
    private Map<Tree,List<MultiMove>> actionMapDst = new HashMap<>();

    ArrayList<Action> actions;
    private static int counter = 0;

    public MultiMoveActionGenerator()
    {
        fullMap = new LinkedHashMap<>();
        actions = new ArrayList<>();
    }
    public ArrayList<Action> generate()
    {
//        return actions;
        return simplify(actions);
    }

    private ArrayList<Action> simplify(ArrayList<Action> actions) {
        for (Tree t : actionMapSrc.keySet()) {
            if (actionMapSrc.containsKey(t.getParent())
                    && actionMapSrc.keySet().containsAll(t.getParent().getDescendants()))
                removeActionsForThisTreeFromSrc(t);
            else {
                if (t.getChildren().size() > 0 && actionMapSrc.keySet().containsAll(t.getDescendants())) {

                }
            }
        }
        for (Tree t : actionMapDst.keySet()) {
            if (actionMapDst.containsKey(t.getParent())
                    && actionMapDst.keySet().containsAll(t.getParent().getDescendants()))
                removeActionsForThisTreeFromDst(t);
            else {
                if (t.getChildren().size() > 0 && actionMapDst.keySet().containsAll(t.getDescendants())) {
                }
            }
        }
        return actions;
    }
    private void removeActionsForThisTreeFromSrc(Tree t){
        List<MultiMove> mappedSrc = actionMapSrc.get(t);
        for(MultiMove action : mappedSrc) {
            if (!action.isUpdated())
                actions.remove(action);

        }
    }
    private void removeActionsForThisTreeFromDst(Tree t){
        List<MultiMove> mappedDst = actionMapDst.get(t);
        for(MultiMove action : mappedDst)
            if (!action.isUpdated())
                actions.remove(action);
    }


    public void addMapping(Set<Tree> srcTrees, Set<Tree> dstTrees) {
        this.fullMap.put(srcTrees,dstTrees);
        for (Tree srcTree : srcTrees)
        {
            for (Tree dstTree : dstTrees)
            {
                boolean updated = false;
                if (srcTree.isLeaf() && dstTree.isLeaf())
                    updated = (srcTree.getMetrics().hash != dstTree.getMetrics().hash);
                MultiMove action = new MultiMove(srcTree,dstTree,-1, counter + 1,updated);
                if (!actions.contains(action)) {
                    actions.add(action);
                    if (!actionMapSrc.containsKey(srcTree))
                        actionMapSrc.put(srcTree, new ArrayList<>());
                    if (!actionMapDst.containsKey(dstTree))
                        actionMapDst.put(dstTree, new ArrayList<>());

                    actionMapSrc.get(srcTree).add(action);
                    actionMapDst.get(dstTree).add(action);
                }
            }
        }
        counter += 1;
    }
}

