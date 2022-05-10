/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package actions;
import gr.uom.java.xmi.diff.UMLClassDiff;
import matchers.MappingStore;
import tree.Tree;
import tree.TreeContext;

import java.io.IOException;

/**
 * Class to facilitate the computation of diffs between ASTs.
 */
public class ASTDiff {

//    public String srcPath;
//    public String dstPath;

    public final TreeContext srcTC;

    public final TreeContext dstTC;

    public final MappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    public final EditScript editScript;

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public ASTDiff(TreeContext src, TreeContext dst,
                MappingStore mappings, EditScript editScript) {
//        this.srcPath = srcPath;
//        this.dstPath = dstPath;
        this.srcTC = src;
        this.dstTC = dst;
        this.mappings = mappings;
        this.editScript = editScript;
    }
    /**
     * Compute and return a all node classifier that indicates which node have
     * been added/deleted/updated/moved.
     */
    public TreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    /**
     * Compute and return a root node classifier that indicates which node have
     * been added/deleted/updated/moved. Only the root note is marked when a whole
     * subtree has been subject to a same operation.
     */
    public TreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
