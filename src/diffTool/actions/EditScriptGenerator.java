
package diffTool.actions;

import diffTool.matchers.MappingStore;
import diffTool.matchers.MultiMappingStore;
import diffTool.tree.TreeContext;

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
