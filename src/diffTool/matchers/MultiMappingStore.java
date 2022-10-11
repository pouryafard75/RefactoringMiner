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

package diffTool.matchers;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diffTool.tree.FakeTree;
import diffTool.tree.Tree;
import diffTool.tree.TreeContext;
import diffTool.utils.Pair;

public class MultiMappingStore implements Iterable<Mapping> {
    private Map<Tree, Set<Tree>> srcToDsts_all;
    private Map<Tree, Set<Tree>> dstToSrcs_all;


    private final TreeContext srcTC;
    private final TreeContext dstTC;

    public MultiMappingStore(TreeContext srcTC, TreeContext dstTC) {
        this.srcTC = srcTC;
        this.dstTC = dstTC;
        srcToDsts_all = new LinkedHashMap<>();
        dstToSrcs_all = new LinkedHashMap<>();
    }
    public boolean isDstMultiMapped(Tree dstTree){
        if (!dstToSrcs_all.containsKey(dstTree))
            return false;
        if (dstToSrcs_all.get(dstTree).size() > 1)
            return true;
        Tree mappedSrc = dstToSrcs_all.get(dstTree).iterator().next();
        if (!srcToDsts_all.containsKey(mappedSrc))
            return false;
        return srcToDsts_all.get(mappedSrc).size() > 1;
    }
    public boolean isSrcMultiMapped(Tree srcTree){
        if (!srcToDsts_all.containsKey(srcTree))
            return false;
        if (srcToDsts_all.get(srcTree).size() > 1)
            return true;
        Tree mappedSrc = srcToDsts_all.get(srcTree).iterator().next();
        if (!dstToSrcs_all.containsKey(mappedSrc))
            return false;
        return dstToSrcs_all.get(mappedSrc).size() > 1;
    }
    private Map<Tree,Tree> getSrcToDstMono() {
        Map<Tree,Tree> monos = new HashMap<>();
        for (Tree _src : srcToDsts_all.keySet())
        {
            if (srcToDsts_all.get(_src).size() > 1)
                continue;
            Tree _dst = srcToDsts_all.get(_src).iterator().next();
            if (dstToSrcs_all.get(_dst).size() > 1)
                continue;
            monos.put(_src,_dst);
        }
        return monos;
    }
    private Map<Tree,Tree> getDstToSrcMono() {
        Map<Tree,Tree> monos = new HashMap<>();
        for (Tree _dst : dstToSrcs_all.keySet())
        {
            if (dstToSrcs_all.get(_dst).size() > 1)
                continue;
            Tree _src = dstToSrcs_all.get(_dst).iterator().next();
            if (srcToDsts_all.get(_src).size() > 1)
                continue;
            monos.put(_dst,_src);
        }
        return monos;
    }

    public void mergeMappings(MultiMappingStore addon)
    {
        if (addon == null) return;
        for (Mapping m : addon.getMappings())
        {
            this.addMapping(m.first,m.second);
        }
    }

    public Set<Mapping> getMappings() {
        Set<Mapping> mappings = new HashSet<>();
        for (Tree src : srcToDsts_all.keySet())
            for (Tree dst: srcToDsts_all.get(src))
                mappings.add(new Mapping(src, dst));
        return mappings;
    }
    public MappingStore getMonoMappingStore(){
        MappingStore monoStore = new MappingStore(this.srcTC,this.dstTC);
        for (Map.Entry<Tree,Tree> entry : getSrcToDstMono().entrySet())
            monoStore.addMapping(entry.getKey(),entry.getValue());
        return monoStore;
    }

    public Map<Tree,Set<Tree>> dstToSrcMultis(){
        Map<Tree,Set<Tree>> multis = new HashMap<>();
        for (Tree _dst : dstToSrcs_all.keySet())
        {
            if (dstToSrcs_all.get(_dst).size() > 1 && !(_dst instanceof FakeTree))
                multis.put(_dst,dstToSrcs_all.get(_dst));
            else
            {
                Tree mappedSrc = dstToSrcs_all.get(_dst).iterator().next();
                if (srcToDsts_all.get(mappedSrc).size() > 1  && !(_dst instanceof FakeTree))
                    multis.put(_dst,dstToSrcs_all.get(_dst));
            }
        }
        return multis;
    }
    public Map<Tree,Set<Tree>> srcToDstMultis(){
        Map<Tree,Set<Tree>> multis = new HashMap<>();
        for (Tree _src : srcToDsts_all.keySet())
        {
            if (srcToDsts_all.get(_src).size() > 1)
                multis.put(_src,srcToDsts_all.get(_src));
            else
            {
                Tree mappedSrc = srcToDsts_all.get(_src).iterator().next();
                if (dstToSrcs_all.get(mappedSrc).size() > 1)
                    multis.put(_src,srcToDsts_all.get(_src));
            }
        }
        return multis;
    }

    public void addMapping(Tree src, Tree dst) {
        if (!srcToDsts_all.containsKey(src))
            srcToDsts_all.put(src, new HashSet<>());
        srcToDsts_all.get(src).add(dst);
        if (!dstToSrcs_all.containsKey(dst))
            dstToSrcs_all.put(dst, new HashSet<>());
        dstToSrcs_all.get(dst).add(src);
    }

    public void addListOfMapping(List<Pair<Tree,Tree>> pairList)
    {
        if (pairList == null) return;
        for (Pair<Tree,Tree> pair : pairList) {
            addMapping(pair.first,pair.second);
        }
    }

    public void removeMapping(Tree src, Tree dst) {
        srcToDsts_all.get(src).remove(dst);
        srcToDsts_all.get(dst).remove(src);
    }

    public int size() {
        return getMappings().size();
    }

    public Set<Tree> getDstForSrc(Tree src) {
        return srcToDsts_all.get(src);
    }

    public Set<Tree> getSrcForDst(Tree dst) {
        return dstToSrcs_all.get(dst);
    }

    private Set<Tree> allMappedSrcs() {
        return srcToDsts_all.keySet();
    }

    private Set<Tree> allMappedDsts() {
        return dstToSrcs_all.keySet();
    }

    public boolean hasSrc(Tree src) {
        return srcToDsts_all.containsKey(src);
    }

    public boolean hasDst(Tree dst) {
        return dstToSrcs_all.containsKey(dst);
    }

    public boolean has(Tree src, Tree dst) {
        return srcToDsts_all.get(src).contains(dst);
    }

    public boolean isSrcUnique(Tree src) {
        return getDstForSrc(src).size() == 1;
    }

    public boolean isDstUnique(Tree dst) {
        return getSrcForDst(dst).size() == 1;
    }

    public boolean isSrcMapped(Tree src) {
        return srcToDsts_all.containsKey(src);
    }

    public boolean isDstMapped(Tree dst) {
        return dstToSrcs_all.containsKey(dst);
    }

    public boolean areSrcsUnmapped(Collection<Tree> srcs, Tree dst) {
        for (Tree src : srcs)
            if (isSrcMapped(src)) {
                Set<Tree> dstForSrc = this.getDstForSrc(src);
                for (Tree dstMapped : dstForSrc) {
                    if (dst.getDescendantsAndItself().contains(dstMapped))
                        return false;
                }
            }
        return true;
    }

    /**
     * Return whether or not all the given destination nodes are unmapped.
     */
    public boolean areDstsUnmapped(Collection<Tree> dsts, Tree src) {
        for (Tree dst : dsts)
            if (isDstMapped(dst)) {
                Set<Tree> srcForDst = this.getSrcForDst(dst);
                for (Tree srcMapped : srcForDst) {
                    if (src.getDescendantsAndItself().contains(srcMapped))
                        return false;
                }
            }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Tree t : srcToDsts_all.keySet()) {
            String l = srcToDsts_all.get(t).stream().map(Object::toString).collect(Collectors.joining(", "));
            b.append(String.format("%s -> %s", t.toString(), l)).append('\n');
        }
        return b.toString();
    }

    @Override
    public Iterator<Mapping> iterator() {
        return getMappings().iterator();
    }

    public void addMappingRecursively(Tree src, Tree dst) {
        addMapping(src, dst);
        if (src.getChildren() != null)
            for (int i = 0; i < src.getChildren().size(); i++)
                addMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    public void add(MappingStore match) {
        for (Mapping mapping : match) {
            addMapping(mapping.first,mapping.second);
        }
    }
    public void addWithMaps(MappingStore match,Map<Tree,Tree> srcCopy, Map<Tree,Tree> dstCopy) {
        for (Mapping mapping : match) {
            Tree realSrc = srcCopy.get(mapping.first);
            Tree realDst = dstCopy.get(mapping.second);
            if (realSrc != null && realDst != null)
                this.addMapping(realSrc,realDst);
        }
    }

    public List<MappingExportModel> exportModelList() {
        List<MappingExportModel> exportList = new ArrayList<>();
        for (Mapping mapping : new ArrayList<>(getMappings())) {
            MappingExportModel mappingExportModel = new MappingExportModel(
                    mapping.first.getType().name,
                    mapping.first.getLabel(),
                    mapping.first.getPos(),
                    mapping.first.getEndPos(),
                    mapping.first.hashCode(),
                    mapping.second.getType().name,
                    mapping.first.getLabel(),
                    mapping.first.getPos(),
                    mapping.first.getEndPos(),
                    mapping.second.hashCode()
            );
            exportList.add(mappingExportModel);
        }
        exportList.sort(
                (MappingExportModel m1, MappingExportModel m2) ->
                        Integer.compare(m1.getFirstHash() * m1.getSecondHash(), m2.firstHash * m2.getSecondHash())
                        );
        return exportList;
    }

    public String exportString() throws JsonProcessingException {
        List<MappingExportModel> mappingExportModels = this.exportModelList();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappingExportModels);
    }
    public void exportToFile(File outputFile) throws IOException {
        List<MappingExportModel> mappingExportModels = this.exportModelList();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, mappingExportModels);
    }

    public static class MappingExportModel implements Serializable {

        String firstType,secondType,firstLabel,secondLabel;
        int firstPos,secondPos,firstEndPos,secondEndPos;
        @JsonIgnore
        int firstHash,secondHash;

        public MappingExportModel(String firstType, String firstLabel,int firstPos,int firstEndPos, int firstHash,String secondType, String secondLabel, int secondPos, int secondEndPos, int secondHash) {
            this.firstType = firstType;
            this.secondType = secondType;
            this.firstLabel = firstLabel;
            this.secondLabel = secondLabel;
            this.firstPos = firstPos;
            this.secondPos = secondPos;
            this.firstEndPos = firstEndPos;
            this.secondEndPos = secondEndPos;
            this.firstHash = firstHash;
            this.secondHash = secondHash;
        }

        public int getFirstHash() {
            return firstHash;
        }

        public int getSecondHash() {
            return secondHash;
        }

        public void setFirstHash(int firstHash) {
            this.firstHash = firstHash;
        }

        public void setSecondHash(int secondHash) {
            this.secondHash = secondHash;
        }

        public String getFirstType() {
            return firstType;
        }

        public void setFirstType(String firstType) {
            this.firstType = firstType;
        }

        public String getSecondType() {
            return secondType;
        }

        public void setSecondType(String secondType) {
            this.secondType = secondType;
        }

        public String getFirstLabel() {
            return firstLabel;
        }

        public void setFirstLabel(String firstLabel) {
            this.firstLabel = firstLabel;
        }

        public String getSecondLabel() {
            return secondLabel;
        }

        public void setSecondLabel(String secondLabel) {
            this.secondLabel = secondLabel;
        }

        public int getFirstPos() {
            return firstPos;
        }

        public void setFirstPos(int firstPos) {
            this.firstPos = firstPos;
        }

        public int getSecondPos() {
            return secondPos;
        }

        public void setSecondPos(int secondPos) {
            this.secondPos = secondPos;
        }

        public int getFirstEndPos() {
            return firstEndPos;
        }

        public void setFirstEndPos(int firstEndPos) {
            this.firstEndPos = firstEndPos;
        }

        public int getSecondEndPos() {
            return secondEndPos;
        }

        public void setSecondEndPos(int secondEndPos) {
            this.secondEndPos = secondEndPos;
        }
    }
}
