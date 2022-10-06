
package ASTDiff.actions;

import ASTDiff.matchers.MappingStore;
import ASTDiff.matchers.MultiMappingStore;
import ASTDiff.tree.TreeContext;

import java.util.Map;

/**
 * Interface for script generators that compute edit scripts from mappings.
 *
 * @see MappingStore
 * @see EditScript
 */
public interface EditScriptGenerator {
    /**
     * Compute and return the edit script for the provided mappings.
     */
    EditScript computeActions(MultiMappingStore mappings, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap);
}
