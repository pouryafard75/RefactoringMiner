
package actions;

import matchers.MappingStore;

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
    EditScript computeActions(MappingStore mappings);
}
