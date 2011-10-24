package org.springframework.roo.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link MavenOperations}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class MavenOperationsImpl extends AbstractProjectOperations implements MavenOperations {

	// Constants
	private static final Dependency JAXB_API = new Dependency("javax.xml.bind", "jaxb-api", "2.1");
	private static final Dependency JSR250_API = new Dependency("javax.annotation", "jsr250-api", "1.0");
	private static final Logger LOGGER = HandlerUtils.getLogger(MavenOperationsImpl.class);
	private static final String GAV_SEPARATOR = ":";
	
	// Fields
	@Reference private ApplicationContextOperations applicationContextOperations;
	@Reference private ProcessManager processManager;

	public boolean isCreateProjectAvailable() {
		return !isProjectAvailable(getFocusedModuleName());
	}

	public boolean isCreateModuleAvailable() {
		return true;
	}
	
	public String getProjectRoot() {
		return pathResolver.getRoot(Path.ROOT.contextualize(pomManagementService.getFocusedModuleName()));
	}
	
	public void createProject(final JavaPackage topLevelPackage, final String projectName, final Integer majorJavaVersion, final String parentPom) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		createMavenPom(topLevelPackage, projectName, majorJavaVersion, parentPom, "");

		fileManager.scan();

		// Set up the Spring application context configuration file
		applicationContextOperations.createMiddleTierApplicationContext(topLevelPackage, "");

		// Set up the logging configuration file
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "packaging/log4j.properties-template"), fileManager.createFile(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties")).getOutputStream());
		} catch (final IOException e) {
			LOGGER.warning("Unable to install log4j logging configuration");
		}
	}

	public void createModule(final JavaPackage topLevelPackage, final String projectName, final Integer majorJavaVersion, final String parentPom, final String moduleName) {
		Assert.isTrue(isCreateModuleAvailable(), "Project creation is unavailable at this time");
		String pomPath = createMavenPom(topLevelPackage, projectName, majorJavaVersion, parentPom, moduleName);

		//fileManager.scan();

		// Set up the Spring application context configuration file
		applicationContextOperations.createMiddleTierApplicationContext(topLevelPackage, moduleName);

		// Set up the logging configuration file
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "packaging/log4j.properties-template"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES.contextualize(moduleName), "log4j.properties")).getOutputStream());
		} catch (final IOException e) {
			LOGGER.warning("Unable to install log4j logging configuration");
		}

		String focusedPomPath = pomManagementService.getFocusedModule().getPath();
		final Document pomDocument = XmlUtils.readXml(fileManager.getInputStream(focusedPomPath));
		Element root = pomDocument.getDocumentElement();

		Element module = XmlUtils.findFirstElement("/project/modules", root);
		if (module == null) {
			module = pomDocument.createElement("modules");
			Element repositories = XmlUtils.findFirstElement("/project/repositories", root);
			root.insertBefore(module, repositories);
		}
		boolean alreadyPresent = false;
		for (Element element : XmlUtils.findElements("module", module)) {
			if (element.getTextContent().trim().equals(moduleName)) {
				alreadyPresent = true;
				break;
			}
		}
		if (!alreadyPresent) {
			Element packaging = XmlUtils.findFirstElement("/project/packaging", root);
			if (packaging != null) {
				packaging.setTextContent("pom");
			}
			module.appendChild(XmlUtils.createTextElement(pomDocument, "module", moduleName));
			final String addMessage = getDescriptionOfChange(ADDED, Collections.singleton(moduleName), "module", "modules");
			fileManager.createOrUpdateTextFileIfRequired(getFocusedModule().getPath(), XmlUtils.nodeToString(pomDocument), addMessage, false);
		}

		setModule(pomManagementService.getPomFromPath(pomPath));
	}

	/**
	 * Creates the Maven POM for a new user project
	 * 
	 * @param topLevelPackage the top-level Java package (required)
	 * @param nullableProjectName the project name provided by the user (can be blank)
	 * @param majorJavaVersion the major Java version as entered by the user (can be <code>null</code> to auto-detect from the developer's machine)
	 * @param parentPom the Maven coordinates of the parent POM, in the form G:A:V (can be blank)
	 * @param moduleName
	 * @return
	 */
	private String createMavenPom(final JavaPackage topLevelPackage, String nullableProjectName, final Integer majorJavaVersion, final String parentPom, final String moduleName) {
		Assert.notNull(topLevelPackage, "Top level package required");
		
		// Read the POM template from this addon's classpath resources
		final Document pom = XmlUtils.readXml(TemplateUtils.getTemplate(getClass(), "packaging/jar-pom-template.xml"));
		final Element root = pom.getDocumentElement();

		if (!StringUtils.hasText(nullableProjectName) && StringUtils.hasText(moduleName)) {
			nullableProjectName = moduleName;
		}

		// Set the name
		final String projectName = StringUtils.hasText(nullableProjectName) ? nullableProjectName : topLevelPackage.getLastElement();
		XmlUtils.findRequiredElement("/project/name", root).setTextContent(projectName);
		
		// Set the coordinates of the project and its parent, if any
		setGroupIds(topLevelPackage.getFullyQualifiedPackageName(), parentPom, root);
		
		// Project artifactId
		XmlUtils.findRequiredElement("/project/artifactId", root).setTextContent(projectName);

		DomUtils.createChildIfNotExists("packaging", root, pom).setTextContent("jar");
		
		// Update the target Java version
		final String javaVersion = getJavaVersion(majorJavaVersion);
		final List<Element> versionElements = XmlUtils.findElements("//*[.='JAVA_VERSION']", root);
		for (final Element versionElement : versionElements) {
			versionElement.setTextContent(javaVersion);
		}

		String pomPath = pathResolver.getIdentifier(Path.ROOT.contextualize(moduleName), "pom.xml");

		// Write the new POM to disk
		fileManager.createOrUpdateTextFileIfRequired(pomPath, XmlUtils.nodeToString(pom), true);

		// Java 5 needs the javax.annotation library (it's included in Java 6 and above), and the jaxb-api for Hibernate
		if ("1.5".equals(javaVersion)) {
			addDependencies(moduleName, Arrays.asList(JSR250_API, JAXB_API));
		}

		return pomPath;
	}

	/**
	 * Sets the Maven groupIds of the parent and/or project as necessary
	 * 
	 * @param projectGroupId the project's groupId (required)
	 * @param parentPom the Maven coordinates of the parent POM, in the form G:A:V (can be blank)
	 * @param root the root element of the POM document
	 */
	private void setGroupIds(final String projectGroupId, final String parentPom, final Element root) {
		final Element projectGroupIdElement = XmlUtils.findRequiredElement("/project/groupId", root);
		if (StringUtils.hasText(parentPom)) {
			final String[] parentPomCoordinates = StringUtils.delimitedListToStringArray(parentPom, GAV_SEPARATOR);
			Assert.isTrue(parentPomCoordinates.length == 3, "Expected three coordinates for parent POM, but found " + parentPomCoordinates.length + ": " + Arrays.toString(parentPomCoordinates) + "; did you use the '" + GAV_SEPARATOR + "' separator?");
			final String parentGroupId = parentPomCoordinates[0];
			
			// Parent and project groupId
			XmlUtils.findRequiredElement("/project/parent/groupId", root).setTextContent(parentGroupId);
			if (projectGroupId.equals(parentGroupId)) {
				// Maven best practice is to inherit the groupId from the parent
				root.removeChild(projectGroupIdElement);
				DomUtils.removeTextNodes(root);
			} else {
				// Project has its own groupId => needs to be declared
				projectGroupIdElement.setTextContent(projectGroupId);
			}
			
			// Parent artifactId
			XmlUtils.findRequiredElement("/project/parent/artifactId", root).setTextContent(parentPomCoordinates[1]);
			
			// Parent version
			XmlUtils.findRequiredElement("/project/parent/version", root).setTextContent(parentPomCoordinates[2]);
		} else {
			// No parent POM was specified; remove the templated parent element
			root.removeChild(XmlUtils.findRequiredElement("/project/parent", root));
			DomUtils.removeTextNodes(root);
			projectGroupIdElement.setTextContent(projectGroupId);
		}
	}
	
	/**
	 * Returns the project's target Java version in POM format
	 * 
	 * @param majorJavaVersion the major version provided by the user; can be
	 * <code>null</code> to auto-detect it
	 * @return a non-blank string
	 */
	private String getJavaVersion(final Integer majorJavaVersion) {
		if (majorJavaVersion != null && majorJavaVersion >= 5 && majorJavaVersion <= 7) {
			return String.valueOf(majorJavaVersion);
		}
		
		// No valid version given; detect the major Java version to use
		final String ver = System.getProperty("java.version");
		if (ver.contains("1.7.")) {
			return "1.7";
		}
		if (ver.contains("1.6.")) {
			return "1.6";
		}
		// To be running Roo they must be on Java 5 or above
		return "1.5";
	}

	public void executeMvnCommand(final String extra) throws IOException {
		final File root = new File(getProjectRoot());
		Assert.isTrue(root.isDirectory() && root.exists(), "Project root does not currently exist as a directory ('" + root.getCanonicalPath() + "')");

		final String cmd = (File.separatorChar == '\\' ? "mvn.bat " : "mvn ") + extra;
		final Process p = Runtime.getRuntime().exec(cmd, null, root);

		// Ensure separate threads are used for logging, as per ROO-652
		final LoggingInputStream input = new LoggingInputStream(p.getInputStream(), processManager);
		final LoggingInputStream errors = new LoggingInputStream(p.getErrorStream(), processManager);

		p.getOutputStream().close(); // Close OutputStream to avoid blocking by Maven commands that expect input, as per ROO-2034
		input.start();
		errors.start();

		try {
			if (p.waitFor() != 0) {
				LOGGER.warning("The command '" + cmd + "' did not complete successfully");
			}
		} catch (final InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static class LoggingInputStream extends Thread {

		// Fields
		private final BufferedReader reader;
		private final ProcessManager processManager;

		/**
		 * Constructor
		 *
		 * @param inputStream
		 * @param processManager
		 */
		public LoggingInputStream(final InputStream inputStream, final ProcessManager processManager) {
			this.reader = new BufferedReader(new InputStreamReader(inputStream));
			this.processManager = processManager;
		}

		@Override
		public void run() {
			ActiveProcessManager.setActiveProcessManager(processManager);
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("[ERROR]")) {
						LOGGER.severe(line);
					} else if (line.startsWith("[WARNING]")) {
						LOGGER.warning(line);
					} else {
						LOGGER.info(line);
					}
				}
			} catch (final IOException e) {
				if (e.getMessage().contains("No such file or directory") || // For *nix/Mac
					e.getMessage().contains("CreateProcess error=2")) { // For Windows
					LOGGER.severe("Could not locate Maven executable; please ensure mvn command is in your path");
				}
			} finally {
				IOUtils.closeQuietly(reader);
				ActiveProcessManager.clearActiveProcessManager();
			}
		}
	}



	//NEW

	public void createProject(final JavaPackage topLevelPackage, final String projectName, final Integer majorJavaVersion, final GAV parentPom, final PackagingProvider packagingType) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		final String javaVersion = getJavaVersion(majorJavaVersion);
		packagingType.createArtifacts(topLevelPackage, projectName, javaVersion, parentPom);
	}

	public void createModule(final JavaPackage topLevelPackage, final String name, final GAV parent, final PackagingProvider packagingType) {
		Assert.isTrue(isCreateModuleAvailable(), "Cannot create modules at this time");
		final String moduleName = StringUtils.defaultIfEmpty(name, topLevelPackage.getLastElement());
		final GAV module = new GAV(topLevelPackage.getFullyQualifiedPackageName(), moduleName, parent.getVersion());
		// TODO create or update "modules" element of parent module's POM
		// Create the new module's directory, named by its artifactId (Maven standard practice)
		fileManager.createDirectory(moduleName);
		// Focus the new module so that artifacts created below go to the correct path(s)
		focus(module);
		packagingType.createArtifacts(topLevelPackage, name, "${java.version}", parent);
	}

	public void focus(final GAV module) {
		Assert.notNull(module, "Specify the module to focus on");
		throw new UnsupportedOperationException("Module focussing not implemented yet");	// TODO by JTT for ROO-120
	}
}
