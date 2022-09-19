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
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package tree;

import io.TreeIoUtils;
import tree.Tree;
import utils.Pair;
import tree.TreeContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TreeLoader {

    private TreeLoader() {}

    public static TreeContext load(String name) {
        try {
            File initialFile = new File(name);
            InputStream resourceAsStream = new FileInputStream(initialFile);
//            InputStream resourceAsStream = TreeLoader.class.getResourceAsStream(name);
            return TreeIoUtils.fromXml().generateFrom().stream(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to load test resource: %s", name), e);
        }
    }
}
