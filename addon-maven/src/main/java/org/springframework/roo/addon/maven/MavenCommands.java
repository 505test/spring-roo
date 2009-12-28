package org.springframework.roo.addon.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

@ScopeDevelopmentShell
public class MavenCommands implements CommandMarker {
	
	private MavenOperations mavenOperations;
	protected final Logger logger = Logger.getLogger(getClass().getName());

	public MavenCommands(StaticFieldConverter staticFieldConverter, MavenOperations mavenOperations) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(mavenOperations, "Maven operations required");
		staticFieldConverter.add(Template.class);
		this.mavenOperations = mavenOperations;
	}

	@CliAvailabilityIndicator("project")
	public boolean isCreateProjectAvailable() {
		return mavenOperations.isCreateProjectAvailable();
	}
	
	@CliCommand(value="project", help="Creates a new project")
	public void createProject(
			@CliOption(key={"", "topLevelPackage"}, mandatory=true, help="The uppermost package name") JavaPackage topLevelPackage,
			@CliOption(key="projectName", mandatory=false, help="The name of the project (last segment of package name used as default)") String projectName,
			@CliOption(key="java", mandatory=false, help="Forces a particular major version of Java to be used (will be auto-detected if unspecified; specify 5 or 6 or 7 only)") Integer majorJavaVersion,
			@CliOption(key="template", mandatory=false, specifiedDefaultValue="STANDARD_PROJECT", unspecifiedDefaultValue="STANDARD_PROJECT", help="The type of project to create (defaults to STANDARD_PROJECT)") Template template) {
		mavenOperations.createProject(template, topLevelPackage, projectName, majorJavaVersion);
	}
	
	@CliAvailabilityIndicator({"dependency add", "dependency remove"})
	public boolean isDependencyModificationAllowed() {
		return mavenOperations.isDependencyModificationAllowed();
	}

	@CliCommand(value="dependency add", help="Adds a new dependency to the Maven project object model (POM)")
	public void addDependency(@CliOption(key="groupId", mandatory=true, help="The group ID of the dependency") JavaPackage groupId, 
			@CliOption(key="artifactId", mandatory=true, help="The artifact ID of the dependency") JavaSymbolName artifactId,
			@CliOption(key="version", mandatory=true, help="The version of the dependency") String version) {
		mavenOperations.addDependency(groupId, artifactId, version);
	}
	
	@CliCommand(value="dependency remove", help="Removes an existing dependency from the Maven project object model (POM)")
	public void removeDependency(@CliOption(key="groupId", mandatory=true, help="The group ID of the dependency") JavaPackage groupId, 
			@CliOption(key="artifactId", mandatory=true, help="The artifact ID of the dependency") JavaSymbolName artifactId, 
			@CliOption(key="version", mandatory=true, help="The version of the dependency") String version) {
		mavenOperations.removeDependency(groupId, artifactId, version);
	}
	
	@CliAvailabilityIndicator({"perform package", "perform eclipse", "perform tests", "perform clean", "perform assembly", "perform command"})
	public boolean isPerformCommandAllowed() {
		return mavenOperations.isPerformCommandAllowed();
	}

	@CliCommand(value={"perform package"}, help="Packages the application using Maven, but does not execute any tests")
	public void runPackage() throws IOException {
		mvn("-Dmaven.test.skip=true package");
	}

	@CliCommand(value={"perform eclipse"}, help="Sets up Eclipse configuration via Maven")
	public void runEclipse() throws IOException {
		mvn("eclipse:clean eclipse:eclipse");
	}

	@CliCommand(value={"perform tests"}, help="Executes the tests via Maven")
	public void runTest() throws IOException {
		mvn("test");
	}

	@CliCommand(value={"perform assembly"}, help="Executes the assembly goal via Maven")
	public void runAssembly() throws IOException {
		mvn("assembly:assembly");
	}

	@CliCommand(value={"perform clean"}, help="Executes a full clean (including Eclipse files) via Maven")
	public void runClean() throws IOException {
		mvn("clean");
	}
	
	@CliCommand(value={"perform command"}, help="Executes a user-specified Maven command")
	public void mvn(@CliOption(key="mavenCommand", mandatory=true, help="User-specified Maven command (eg test:test)") String extra) throws IOException {
		File root = new File(mavenOperations.getProjectRoot());
		Assert.isTrue(root.isDirectory() && root.exists(), "Project root does not currently exist as a directory ('" + root.getCanonicalPath() + "')");
		BufferedReader input = null;
		try {
			String cmd = null;
			if (File.separatorChar == '\\') {
				cmd = "mvn.bat " + extra;
			} else {
				cmd = "mvn " + extra;
			}
			Process p = Runtime.getRuntime().exec(cmd, null, root);
		    input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String line;
		    while ((line = input.readLine()) != null) {
		    	if (line.startsWith("[ERROR]")) {
		    		logger.severe(line);
		    	} else if (line.startsWith("[WARNING]")) {
		    		logger.warning(line);
		    	} else {
		    		logger.info(line);
		    	}
		    }
	    } catch (IOException ioe) {
	    	if (ioe.getMessage().contains("No such file or directory") || // for *nix/Mac
	    		ioe.getMessage().contains("CreateProcess error=2"))       // for Windows
	    	{
	    		logger.severe("Could not locate Maven executable; please ensure mvn command is in your path");
	    		return;
	    	}
	    	throw ioe;
	    } finally {
	    	if (input != null) {
	    		input.close();
	    	}
	    }
	}

}
