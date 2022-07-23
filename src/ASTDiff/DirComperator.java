package ASTDiff;
import org.refactoringminer.rm1.ProjectData;
import utils.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirComperator {
    private final Map<String, String> fileContentsBeforeMap;
    private final Map<String, String> fileContentsAfterMap;

    private Set<String> removedFilesName;
    private Set<String> addedFilesName;
    private Set<String> modifiedFilesName;

    public Set<String> getRemovedFilesName() {
        return removedFilesName;
    }

    public Set<String> getAddedFilesName() {
        return addedFilesName;
    }

    public Set<String> getModifiedFilesName() {
        return modifiedFilesName;
    }

    public Pair<String,String> getFileContentsPair(String id)
    {
        return new Pair<>(
                fileContentsBeforeMap.get(id),
                fileContentsAfterMap.get(id)
        );
    }

    public DirComperator(ProjectData projectData)
    {
        this.fileContentsAfterMap = projectData.getFileContentsCurrent();
        this.fileContentsBeforeMap = projectData.getFileContentsBefore();
        compare();
    }

    private void compare() {
        Set<String> beforeFiles = fileContentsBeforeMap.keySet();
        Set<String> afterFiles = fileContentsAfterMap.keySet();

        removedFilesName = new HashSet<>(beforeFiles);
        addedFilesName= new HashSet<>(afterFiles);

        removedFilesName.removeAll(afterFiles);
        addedFilesName.removeAll(beforeFiles);
        Set<String> commonFiles = new HashSet<>(beforeFiles);
        commonFiles.retainAll(afterFiles);
        modifiedFilesName = new HashSet<>();

        //TODO :
        for (String commonFile : commonFiles)
            if (!fileContentsBeforeMap.get(commonFile).equals(fileContentsAfterMap.get(commonFile)))
                modifiedFilesName.add(commonFile);
    }
}
