package org.springframework.roo.project.maven;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.model.Builder;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

public class PomBuilder implements Builder<Pom> {

	private static final String DEFAULT_RELATIVE_PATH = "../pom.xml";
	private static final String DEFAULT_SOURCE_DIRECTORY = "${project.basedir}/src/main/java";
	private static final String DEFAULT_TEST_SOURCE_DIRECTORY = "${project.basedir}/src/test/java";

	private  String groupId;
	private  String artifactId;
	private  String version;
	private  String packaging;
	private final Set<Dependency> dependencies = new HashSet<Dependency>();
	private  ParentBuilder parent;
	private final Set<ModuleBuilder> modules = new LinkedHashSet<ModuleBuilder>();
	private final Set<Property> pomProperties = new HashSet<Property>();

	private String name;
	private String moduleName;

	private final Set<Repository> repositories = new HashSet<Repository>();
	private final Set<Repository> pluginRepositories = new HashSet<Repository>();

	private  String sourceDirectory;
	private  String testSourceDirectory;
	private final Set<Filter> filters = new HashSet<Filter>();
	private final Set<Plugin> buildPlugins = new HashSet<Plugin>();
	private final Set<Resource> resources = new HashSet<Resource>();

	private final String path;

	public static PomBuilder getInstance(Element root, String pomPath, String moduleName) {
		return new PomBuilder(root, pomPath, moduleName);
	}

	public PomBuilder(Element root, String pomPath, String moduleName) {

		this.path = pomPath;

		String groupId = XmlUtils.getTextContent("/project/groupId", root);
		if (groupId == null) {
			// Fall back to a group ID assumed to be the same as any possible <parent> (ROO-1193)
			groupId = XmlUtils.getTextContent("/project/parent/groupId", root);
		}
		Assert.notNull(groupId, "Maven pom.xml must provide a <groupId> for the <project>");
		this.groupId = groupId;

		this.artifactId = XmlUtils.getTextContent("/project/artifactId", root);
		Assert.hasText(artifactId, "Project name could not be determined from POM '" + pomPath + "'");

		this.version = XmlUtils.getTextContent("/project/version", root);
		this.packaging = XmlUtils.getTextContent("/project/packaging", root);
		this.name = XmlUtils.getTextContent("/project/name", root);
		this.moduleName = moduleName;
		this.sourceDirectory = XmlUtils.getTextContent("/project/build/sourceDirectory", root, DEFAULT_SOURCE_DIRECTORY);
		this.testSourceDirectory = XmlUtils.getTextContent("/project/build/testSourceDirectory", root, DEFAULT_TEST_SOURCE_DIRECTORY);

		for (final Element module : XmlUtils.findElements("/project/modules/module", root)) {
			String name = module.getTextContent();
			if (StringUtils.hasText(name)) {
				String modulePath = resolveRelativePath(pomPath, name);
				modules.add(new ModuleBuilder(name, modulePath));
			}
		}

		this.parent = getParent(pomPath, root);

		// Build dependencies list
		for (final Element dependencyElement : XmlUtils.findElements("/project/dependencies/dependency", root)) {
			dependencies.add(new Dependency(dependencyElement));
		}

		// Build plugins list
		for (final Element pluginElement : XmlUtils.findElements("/project/build/plugins/plugin", root)) {
			buildPlugins.add(new Plugin(pluginElement));
		}

		// Build repositories list
		for (final Element repositoryElement : XmlUtils.findElements("/project/repositories/repository", root)) {
			repositories.add(new Repository(repositoryElement));
		}

		// Build plugin repositories list
		for (final Element pluginRepositoryElement : XmlUtils.findElements("/project/pluginRepositories/pluginRepository", root)) {
			pluginRepositories.add(new Repository(pluginRepositoryElement));
		}

		// Build properties list
		for (final Element propertyElement : XmlUtils.findElements("/project/properties/*", root)) {
			pomProperties.add(new Property(propertyElement));
		}

		// Filters list
		for (final Element filterElement : XmlUtils.findElements("/project/build/filters/filter", root)) {
			filters.add(new Filter(filterElement));
		}

		// Resources list
		for (final Element resourceElement : XmlUtils.findElements("/project/build/resources/resource", root)) {
			resources.add(new Resource(resourceElement));
		}
	}

	public Pom build() {
		//TODO: Add checks to verify that all the parameters are available for Pom construction
		Set<Module> builtModules = new LinkedHashSet<Module>();
		Parent parentPom = null;
		if (parent != null)  {
			parentPom = parent.build();
		}
		for (ModuleBuilder moduleBuilder : modules) {
			builtModules.add(moduleBuilder.build());
		}
		return new Pom(groupId, artifactId, version, packaging, dependencies, parentPom, builtModules, pomProperties, name, repositories, pluginRepositories, sourceDirectory, testSourceDirectory, filters, buildPlugins, resources, path, moduleName);
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackaging() {
		return packaging;
	}

	public Set<Dependency> getDependencies() {
		return dependencies;
	}

	public ParentBuilder getParent() {
		return parent;
	}

	public Set<ModuleBuilder> getModules() {
		return modules;
	}

	public Set<Property> getPomProperties() {
		return pomProperties;
	}

	public String getName() {
		return name;
	}

	public Set<Repository> getRepositories() {
		return repositories;
	}

	public Set<Repository> getPluginRepositories() {
		return pluginRepositories;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public String getTestSourceDirectory() {
		return testSourceDirectory;
	}

	public Set<Filter> getFilters() {
		return filters;
	}

	public Set<Plugin> getBuildPlugins() {
		return buildPlugins;
	}

	public Set<Resource> getResources() {
		return resources;
	}

	public String getPath() {
		return path;
	}

	public String getModuleName() {
		return moduleName;
	}

	private ParentBuilder getParent(String pomPath, Element root) {
		Element parentElement = XmlUtils.findFirstElement("/project/parent", root);

		if (parentElement != null) {
			String relativePath = XmlUtils.getTextContent("/relativePath", parentElement, DEFAULT_RELATIVE_PATH);
			String parentPomPath = resolveRelativePath(pomPath, relativePath);
			return new ParentBuilder(parentElement, parentPomPath);
		}

		return null;
	}

	private String resolveRelativePath(String relativeTo, String relativePath) {
		if (relativeTo.endsWith(File.separator)) {
			relativeTo = relativeTo.substring(0, relativeTo.length() - 1);
		}
		while (new File(relativeTo).isFile()) {
			relativeTo = relativeTo.substring(0, relativeTo.lastIndexOf(File.separator));
		}
		String[] relativePathSegments = relativePath.split(FileUtils.getFileSeparatorAsRegex());

		int backCount = 0;
		for (String relativePathSegment : relativePathSegments) {
			if (relativePathSegment.equals("..")) {
				backCount++;
			} else {
				break;
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int i = backCount; i < relativePathSegments.length; i++) {
			sb.append(relativePathSegments[i]);
			sb.append("/");
		}

		while (backCount > 0) {
			relativeTo = relativeTo.substring(0, relativeTo.lastIndexOf(File.separatorChar));
			backCount--;
		}
		String path = relativeTo + File.separator + sb.toString();
		if (new File(path).isDirectory()) {
			path = path + "pom.xml";
		}
		return path;
	}

}
