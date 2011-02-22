package org.springframework.roo.project.maven;

import java.io.IOException;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.ProjectOperations;

/**
 * Provides Maven project operations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface MavenOperations extends ProjectOperations {

	boolean isCreateProjectAvailable();

	String getProjectRoot();

	void createProject(JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion);

	void executeMvnCommand(String extra) throws IOException;
}