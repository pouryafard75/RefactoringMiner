package actions.model;

import tree.Tree;

import java.util.*;

public class MultiMoveActionGenerator{
    private Map<Set<Tree>, Set<Tree>> fullMap;
    private Map<Tree,List<Action>> actionMapSrc = new HashMap<>();
    private Map<Tree,List<Action>> actionMapDst = new HashMap<>();

    ArrayList<Action> actions;
    private static int counter = 0;

    public MultiMoveActionGenerator()
    {
        fullMap = new LinkedHashMap<>();
        actions = new ArrayList<>();
    }
    public ArrayList<Action> generate()
    {
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
        List<Action> mappedDst = actionMapSrc.get(t);
        for(Action action : mappedDst)
            actions.remove(action);
    }
    private void removeActionsForThisTreeFromDst(Tree t){
        List<Action> mappedDst = actionMapDst.get(t);
        for(Action action : mappedDst)
            actions.remove(action);
    }


    public void addMapping(Set<Tree> srcTrees, Set<Tree> dstTrees) {
        this.fullMap.put(srcTrees,dstTrees);
        for (Tree srcTree : srcTrees)
        {
            for (Tree dstTree : dstTrees)
            {
                Action action = new MultiMove(srcTree,dstTree,-1, counter + 1);
                actions.add(action);
                if (!actionMapSrc.containsKey(srcTree))
                    actionMapSrc.put(srcTree,new ArrayList<>());
                if (!actionMapDst.containsKey(dstTree))
                    actionMapDst.put(dstTree,new ArrayList<>());

                actionMapSrc.get(srcTree).add(action);
                actionMapDst.get(dstTree).add(action);

            }
        }
        counter += 1;
    }
}

