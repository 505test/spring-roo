package org.springframework.roo.addon.jsf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link JsfOperations}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class JsfOperationsImpl extends AbstractOperations implements JsfOperations {
	private static final String PRIMEFACES_XPATH = "/configuration/jsf-libraries/jsf-library[@id = 'PRIMEFACES']";
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	@Reference private Shell shell;

	public boolean isSetupAvailable() {
		return projectOperations.isProjectAvailable() && !hasWebXml() && !hasFacesConfig();
	}

	public boolean isScaffoldAvailable() {
		return hasWebXml() && hasFacesConfig();
	}

	public void setup(JsfImplementation jsfImplementation) {
		if (jsfImplementation == null) {
			jsfImplementation = JsfImplementation.ORACLE_MOJARRA;
		}

		changeJsfImplementation(jsfImplementation);
		copyWebXml();
		copyFacesConfig();
		copyDirectoryContents("images/*.*", projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/images"), false);
		copyDirectoryContents("css/*.css", projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/css"), false);
		copyDirectoryContents("css/skin/*.*", projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/css/skin"), false);
		copyDirectoryContents("css/skin/images/*.*", projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/css/skin/images"), false);
		copyDirectoryContents("templates/*.xhtml", projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/templates"), false);

		fileManager.scan();
	}

	public void changeJsfImplementation(JsfImplementation jsfImplementation) {
		updateConfiguration(jsfImplementation);
	}

	public void generateAll(JavaPackage destinationPackage) {
		Assert.notNull(destinationPackage, "Destination package required");
		
		// Create JSF managed bean for each entity
		generateManagedBeans(destinationPackage);
	}

	private boolean hasWebXml() {
		return fileManager.exists(getWebXmlFile());
	}
	
	private String getWebXmlFile() {
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml");
	}

	private void copyWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		if (hasWebXml()) {
			return;
		}

		Document document = getDocumentTemplate("web-template.xml");
		String projectName = projectOperations.getProjectMetadata().getProjectName();
		WebXmlUtils.setDisplayName(projectName, document, null);
		WebXmlUtils.setDescription("Roo generated " + projectName + " application", document, null);
		
		fileManager.createOrUpdateTextFileIfRequired(getWebXmlFile(), XmlUtils.nodeToString(document), false);
	}

	private void copyFacesConfig() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		if (hasFacesConfig()) {
			return;
		}
		
		Document document = getDocumentTemplate("faces-config-template.xml");
		
		fileManager.createOrUpdateTextFileIfRequired(getFacesConfigFile(), XmlUtils.nodeToString(document), false);
	}

	private boolean hasFacesConfig() {
		return fileManager.exists(getFacesConfigFile());
	}

	private String getFacesConfigFile() {
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/faces-config.xml");
	}

	private void updateConfiguration(JsfImplementation jsfImplementation) {
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Remove unnecessary artifacts not specific to current JSF implementation
		cleanup(configuration, jsfImplementation);

		// Update pom.xml with JSF/Primefaces dependencies and repositories
		updateDependencies(configuration, jsfImplementation);
		updateRepositories(configuration, jsfImplementation);
	}

	private void cleanup(Element configuration, JsfImplementation jsfImplementation) {
		List<JsfImplementation> jsfImplementations = new ArrayList<JsfImplementation>();
		for (JsfImplementation implementation : JsfImplementation.values()) {
			if (implementation != jsfImplementation) {
				jsfImplementations.add(implementation);
			}
		}
		
		String implementationXPath = getImplementationXPath(jsfImplementations);
		projectOperations.removeDependencies(getDependencies(implementationXPath, configuration));
	}
	
	private List<Dependency> getDependencies(String xPathExpression, Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (Element dependencyElement : XmlUtils.findElements(xPathExpression + "/dependencies/dependency", configuration)) {
			dependencies.add(new Dependency(dependencyElement));
		}
		return dependencies;
	}

	private void updateDependencies(Element configuration, JsfImplementation jsfImplementation) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> jsfImplementationDependencies = XmlUtils.findElements(getImplementationXPath(jsfImplementation) +"/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfImplementationDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		List<Element> jsfLibraryDependencies = XmlUtils.findElements(PRIMEFACES_XPATH + "/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfLibraryDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		projectOperations.addDependencies(dependencies);
	}

	private void updateRepositories(Element configuration, JsfImplementation jsfImplementation) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> jsfRepositories = XmlUtils.findElements(getImplementationXPath(jsfImplementation) +"/repositories/repository", configuration);
		for (Element repositoryElement : jsfRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		List<Element> jsfLibraryRepositories = XmlUtils.findElements(PRIMEFACES_XPATH + "/repositories/repository", configuration);
		for (Element repositoryElement : jsfLibraryRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		projectOperations.addRepositories(repositories);
	}

	private void generateManagedBeans(JavaPackage destinationPackage) {
		Set<ClassOrInterfaceTypeDetails> cids = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooEntity.class.getName()));
		for (ClassOrInterfaceTypeDetails cid : cids) {
			if (Modifier.isAbstract(cid.getModifier())) {
				continue;
			}
			
			JavaType entity = cid.getName();
			Path path = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());
			EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(entity, path));
			if (entityMetadata == null || (!entityMetadata.isValid())) {
				continue;
			}
			
			// Check to see if this entity metadata has a JSF metadata listening to it
			String downstreamJsfMetadataId = JsfManagedBeanMetadata.createIdentifier(entity, path);
			if (dependencyRegistry.getDownstream(entityMetadata.getId()).contains(downstreamJsfMetadataId)) {
				// There is already a JSF managed bean for this entity
				continue;
			}
			
			// To get here, there is no listening managed bean, so add one
			JavaType managedBean = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Bean");
			createManagedBean(managedBean, entity);
		}
	}

	public void createManagedBean(JavaType managedBean, JavaType entity) {
		if (fileManager.exists(typeLocationService.getPhysicalLocationCanonicalPath(managedBean, Path.SRC_MAIN_JAVA))) {
			// Type exists already - nothing to do
			return; 
		}
		
		// Create type annotation for new managed bean
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(managedBean, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, managedBean, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(RooJsfManagedBean.class.getName()), attributes));

		typeManagementService.generateClassFile(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + managedBean.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
		shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
	}
	
	private String getImplementationXPath(List<JsfImplementation> jsfImplementations) {
		StringBuilder builder = new StringBuilder("/configuration/jsf-implementations/jsf-implementation[");
		for (int i = 0, n = jsfImplementations.size(); i < n; i++) {
			builder.append("@id = '");
			builder.append(jsfImplementations.get(i).name());
			builder.append("'");
			if (i < n - 1) {
				builder.append(" or ");
			}
		}
		builder.append("]");
		return builder.toString();
	}
	
	private String getImplementationXPath(JsfImplementation jsfImplementation) {
		return "/configuration/jsf-implementations/jsf-implementation[@id = '" + jsfImplementation.name() + "']";
	}
}