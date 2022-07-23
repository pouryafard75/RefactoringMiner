package ASTDiff;


import actions.ASTDiff;
import org.eclipse.jdt.core.dom.AST;
import org.refactoringminer.rm1.ProjectData;

import java.util.HashMap;
import java.util.Map;
public class ProjectASTDiff {

    private final Map<String, ASTDiff> astDiffMap;
    private ProjectData projectData;
    ProjectASTDiff()
    {
        astDiffMap = new HashMap<>();
    }

    public Map<String, ASTDiff> getAstDiffMap() {
        return astDiffMap;
    }

    public ASTDiff astDiffByName(String filename)
    {
        return this.astDiffMap.get(filename);
    }
    public boolean isASTDiffAvailable(String path)
    {
        return this.astDiffMap.containsKey(path);
    }
    public void addASTDiff(String path, ASTDiff astDiff)
    {
        this.astDiffMap.put(path, astDiff);
    }

    public ProjectData getProjectData() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }
}
