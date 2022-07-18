
package actions;

import actions.model.Action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Edit scripts are sequence of tree edit actions between two ASTs.
 * Edit scripts are iterable.
 * @see Action
 */
public class EditScript implements Iterable<Action> {
    private List<Action> actions;

    /**
     * Instantiate a new edit script.
     */
    public EditScript() {
        actions = new ArrayList<>();
    }

    @Override
    public Iterator<Action> iterator() {
        return actions.iterator();
    }

    /**
     * Add an action in the script.
     */
    public void add(Action action) {
        actions.add(action);
    }

    /**
     * Add an action in the script at the provided index.
     */
    public void add(int index, Action action) {
        actions.add(index, action);
    }

    /**
     * Return the at the given index.
     */
    public Action get(int index) {
        return actions.get(index);
    }

    /**
     * Return the number of actions.
     */
    public int size() {
        return actions.size();
    }

    /**
     * Remove the provided action from the script.
     */
    public boolean remove(Action action) {
        return actions.remove(action);
    }

    /**
     * Remove the action at the provided index from the script.
     */
    public Action remove(int index) {
        return actions.remove(index);
    }

    /**
     * Convert the edit script as a list of actions.
     */
    public List<Action> asList() {
        return actions;
    }

    /**
     * Return the index of the last occurence of the action in the script.
     */
    public int lastIndexOf(Action action) {
        return actions.lastIndexOf(action);
    }
}
