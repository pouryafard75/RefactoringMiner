package org.refactoringminer.rm1;

import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.Map;
import java.util.Set;

public class ProjectData {

    UMLModelDiff umlModelDiff;
//    Set<String> repositoryDirectoriesBefore;
//    Set<String> repositoryDirectoriesCurrent;
//    Map<String, String> renamedFilesHint;
    Map<String, String> fileContentsBefore;
    Map<String, String> fileContentsCurrent;


    public Map<String, String> getFileContentsBefore() {
        return fileContentsBefore;
    }

    public Map<String, String> getFileContentsCurrent() {
        return fileContentsCurrent;
    }

//    public Map<String, String> getRenamedFilesHint() {  return renamedFilesHint;  }
//    public Set<String> getRepositoryDirectoriesBefore() { return repositoryDirectoriesBefore; }
//    public Set<String> getRepositoryDirectoriesCurrent() { return repositoryDirectoriesCurrent; }

    public UMLModelDiff getUmlModelDiff() {
        return umlModelDiff;
    }

    public void setUmlModelDiff(UMLModelDiff umlModelDiff) {
        this.umlModelDiff = umlModelDiff;
    }

    public void setFileContentsBefore(Map<String, String> fileContentsBefore) {
        this.fileContentsBefore = fileContentsBefore;
    }

    public void setFileContentsCurrent(Map<String, String> fileContentsCurrent) {
        this.fileContentsCurrent = fileContentsCurrent;
    }

//    public void setRenamedFilesHint(Map<String, String> renamedFilesHint) {
//        this.renamedFilesHint = renamedFilesHint;
//    }
//    public void setRepositoryDirectoriesBefore(Set<String> repositoryDirectoriesBefore) { this.repositoryDirectoriesBefore = repositoryDirectoriesBefore;}
//    public void setRepositoryDirectoriesCurrent(Set<String> repositoryDirectoriesCurrent) { this.repositoryDirectoriesCurrent = repositoryDirectoriesCurrent;}


}
