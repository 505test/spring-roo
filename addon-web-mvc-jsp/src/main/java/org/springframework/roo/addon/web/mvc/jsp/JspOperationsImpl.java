package org.springframework.roo.addon.web.mvc.jsp;

import static org.springframework.roo.model.SpringJavaType.CONTROLLER;
import static org.springframework.roo.model.SpringJavaType.MODEL_MAP;
import static org.springframework.roo.model.SpringJavaType.PATH_VARIABLE;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_MAIN_WEBAPP;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.backup.BackupOperations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18nSupport;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides operations to create various view layer resources.
 *
 * @author Stefan Schmidt
 * @author Jeremy Grelle
 * @since 1.0
 */
@Component
@Service
public class JspOperationsImpl extends AbstractOperations implements JspOperations {

	/**
	 * Returns the folder name and mapping value for the given preferred maaping
	 *
	 * @param preferredMapping (can be blank)
	 * @param controller the associated controller type (required)
	 * @return a non-<code>null</code> pair
	 */
	static Pair<String, String> getFolderAndMapping(final String preferredMapping, final JavaType controller) {
		if (StringUtils.hasText(preferredMapping)) {
			String folderName = StringUtils.removePrefix(preferredMapping, "/");
			folderName = StringUtils.removeSuffix(folderName, "**");
			folderName = StringUtils.removeSuffix(folderName, "/");

			String mapping = StringUtils.prefix(preferredMapping, "/");
			mapping = StringUtils.removeSuffix(mapping, "/");
			mapping = StringUtils.suffix(mapping, "/**");

			return new Pair<String, String>(folderName, mapping);
		}

		// Use sensible defaults
		final String typeNameLower = StringUtils.removeSuffix(controller.getSimpleTypeName(), "Controller").toLowerCase();
		return new Pair<String, String>(typeNameLower, "/" + typeNameLower + "/**");
	}

	// Fields
	@Reference private BackupOperations backupOperations;
	@Reference private I18nSupport i18nSupport;
	@Reference private MenuOperations menuOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private TilesOperations tilesOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	@Reference private UaaRegistrationService uaaRegistrationService;
	@Reference private WebMvcOperations webMvcOperations;

	public boolean isControllerAvailable() {
		return fileManager.exists(projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views")) && !fileManager.exists(projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/faces-config.xml"));
	}

	private boolean isProjectAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public boolean isSetupAvailable() {
		return isProjectAvailable() && !isControllerAvailable();
	}

	public boolean isInstallLanguageCommandAvailable() {
		return isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views/footer.jspx"));
	}

	public void installCommonViewArtefacts() {
		Assert.isTrue(isProjectAvailable(), "Project metadata required");

		if (!isControllerAvailable()) {
			webMvcOperations.installAllWebMvcArtifacts();
		}

		final PathResolver pathResolver = projectOperations.getPathResolver();

		// Install tiles config
		updateConfiguration();

		// Install styles
		copyDirectoryContents("images/*.*", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "images"), false);

		// Install styles
		copyDirectoryContents("styles/*.css", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "styles"), false);
		copyDirectoryContents("styles/*.properties", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/classes"), false);

		// Install layout
		copyDirectoryContents("tiles/default.jspx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/layouts/"), false);
		copyDirectoryContents("tiles/layouts.xml", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/layouts/"), false);
		copyDirectoryContents("tiles/header.jspx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views/"), false);
		copyDirectoryContents("tiles/footer.jspx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views/"), false);
		copyDirectoryContents("tiles/views.xml", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views/"), false);

		// Install common view files
		copyDirectoryContents("*.jspx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views/"), false);

		// Install tags
		copyDirectoryContents("tags/form/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/form"), false);
		copyDirectoryContents("tags/form/fields/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/form/fields"), false);
		copyDirectoryContents("tags/menu/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/menu"), false);
		copyDirectoryContents("tags/util/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/util"), false);

		// Install default language 'en'
		installI18n(i18nSupport.getLanguage(Locale.ENGLISH));

		final String i18nDirectory = pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties");
		if (!fileManager.exists(i18nDirectory)) {
			try {
				final String projectName = projectOperations.getProjectMetadata().getProjectName();
				fileManager.createFile(pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties"));
				propFileOperations.addPropertyIfNotExists(SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties", "application_name", projectName.substring(0, 1).toUpperCase() + projectName.substring(1), true);
			} catch (final Exception e) {
				throw new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}

	public void installView(final String path, final String viewName, final String title, final String category) {
		installView(path, viewName, title, category, null, true);
	}

	public void installView(final String path, final String viewName, final String title, final String category, final Document document) {
		installView(path, viewName, title, category, document, true);
	}

	private void installView(final String path, final String viewName, final String title, final String category, Document document, final boolean registerStaticController) {
		Assert.hasText(path, "Path required");
		Assert.hasText(viewName, "View name required");
		Assert.hasText(title, "Title required");
		
		final String cleanedPath = cleanPath(path);
		final String cleanedViewName = cleanViewName(viewName);
		final String lcViewName = cleanedViewName.toLowerCase();
		
		if (document == null) {
			try {
				document = getDocumentTemplate("index-template.jspx");
				XmlUtils.findRequiredElement("/div/message", document.getDocumentElement()).setAttribute("code", "label" + cleanedPath.replace("/", "_").toLowerCase() + "_" + lcViewName);
			} catch (final Exception e) {
				throw new IllegalStateException("Encountered an error during copying of resources for controller class.", e);
			}
		}

		final String viewFile = projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/views" + cleanedPath.toLowerCase() + "/" + lcViewName + ".jspx");
		fileManager.createOrUpdateTextFileIfRequired(viewFile, XmlUtils.nodeToString(document), false);

		installView(new JavaSymbolName(cleanedViewName), cleanedPath, title, category, registerStaticController);
	}

	/**
	 * Creates a new Spring MVC static view.
	 *
	 * @param viewName the bare logical name of the new view (required, e.g. "index")
	 * @param folderName the folder in which to create the view; must be empty or start with a slash
	 * @param category the menu category in which to list the new view (required)
	 * @param registerStaticController whether to register a static controller in the Spring MVC configuration file
	 */
	private void installView(final JavaSymbolName viewName, final String folderName, final String title, final String category, final boolean registerStaticController) {
		// Probe if common web artifacts exist, and install them if needed
		final PathResolver pathResolver = projectOperations.getPathResolver();
		if (!fileManager.exists(pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/layouts/default.jspx"))) {
			installCommonViewArtefacts();
		}
		
		final String lcViewName = viewName.getSymbolName().toLowerCase();

		// Update the application-specific resource bundle (i.e. default translation)
		final String messageCode = "label" + folderName.replace("/", "_").toLowerCase() + "_" + lcViewName;
		propFileOperations.addPropertyIfNotExists(SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties", messageCode, title, true);
		
		// Add the menu item
		final String relativeUrl = folderName + "/" + lcViewName;
		System.out.println("Relative URL = " + relativeUrl);
		menuOperations.addMenuItem(new JavaSymbolName(category), new JavaSymbolName(folderName.replace("/", "_").toLowerCase() + lcViewName + "_id"), title, "global_generic", relativeUrl, null);
		
		// Add the view definition
		tilesOperations.addViewDefinition(folderName.toLowerCase(), relativeUrl, TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views" + folderName.toLowerCase() + "/" + lcViewName + ".jspx");

		if (registerStaticController) {
			// Update the Spring MVC config file
			registerStaticSpringMvcController(relativeUrl);
		}
	}

	/**
	 * Registers a static Spring MVC controller to handle the given relative URL.
	 * 
	 * @param relativeUrl the relative URL to handle (required); a leading slash
	 * will be added if required
	 */
	private void registerStaticSpringMvcController(final String relativeUrl) {
		final String mvcConfig = projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		if (fileManager.exists(mvcConfig)) {
			final Document document = XmlUtils.readXml(fileManager.getInputStream(mvcConfig));
			final String prefixedUrl = StringUtils.prefix(relativeUrl, "/");
			if (XmlUtils.findFirstElement("/beans/view-controller[@path='" + prefixedUrl + "']", document.getDocumentElement()) == null) {
				final Element sibling = XmlUtils.findFirstElement("/beans/view-controller", document.getDocumentElement());
				final Element view = new XmlElementBuilder("mvc:view-controller", document).addAttribute("path", prefixedUrl).build();
				if (sibling != null) {
					sibling.getParentNode().insertBefore(view, sibling);
				} else {
					document.getDocumentElement().appendChild(view);
				}
				fileManager.createOrUpdateTextFileIfRequired(mvcConfig, XmlUtils.nodeToString(document), false);
			}
		}
	}

	public void updateTags(final boolean backup) {
		if (backup) {
			backupOperations.backup();
		}
		final PathResolver pathResolver = projectOperations.getPathResolver();
		// Update tags
		copyDirectoryContents("tags/form/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/form"), true);
		copyDirectoryContents("tags/form/fields/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/form/fields"), true);
		copyDirectoryContents("tags/menu/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/menu"), true);
		copyDirectoryContents("tags/util/*.tagx", pathResolver.getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/tags/util"), true);
	}

	/**
	 * Creates a new Spring MVC controller.
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**". You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 *
	 * @param controller the controller class to create (required)
	 * @param preferredMapping the mapping this controller should adopt (optional; if unspecified it will be based on the controller name)
	 */
	public void createManualController(final JavaType controller, final String preferredMapping) {
		Assert.notNull(controller, "Controller Java Type required");

		// Create annotation @RequestMapping("/myobject/**")
		final Pair<String, String> folderAndMapping = getFolderAndMapping(preferredMapping, controller);
		final String folderName = folderAndMapping.getKey();

		final String resourceIdentifier = typeLocationService.getPhysicalTypeCanonicalPath(controller, SRC_MAIN_JAVA);
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, projectOperations.getPathResolver().getPath(resourceIdentifier));
		final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();

		// Add HTTP get method
		methods.add(getHttpGetMethod(declaredByMetadataId));

		// Add HTTP post method
		methods.add(getHttpPostMethod(declaredByMetadataId));

		// Add index method
		methods.add(getIndexMethod(folderName, declaredByMetadataId));

		// Create Type definition
		final List<AnnotationMetadataBuilder> typeAnnotations = new ArrayList<AnnotationMetadataBuilder>();

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), folderAndMapping.getValue()));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		typeAnnotations.add(requestMapping);

		// Create annotation @Controller
		final List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		final AnnotationMetadataBuilder controllerAnnotation = new AnnotationMetadataBuilder(CONTROLLER, controllerAttributes);
		typeAnnotations.add(controllerAnnotation);

		final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(typeAnnotations);
		typeDetailsBuilder.setDeclaredMethods(methods);
		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());

		installView(folderName, "/index", new JavaSymbolName(controller.getSimpleTypeName()).getReadableSymbolName() + " View", "Controller", null, false);
	}

	private MethodMetadataBuilder getIndexMethod(final String folderName, final String declaredByMetadataId) {
		final List<AnnotationMetadataBuilder> indexMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		indexMethodAnnotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, new ArrayList<AnnotationAttributeValue<?>>()));
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return \"" + folderName + "/index\";");
		final MethodMetadataBuilder indexMethodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("index"), JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		indexMethodBuilder.setAnnotations(indexMethodAnnotations);
		return indexMethodBuilder;
	}

	private MethodMetadataBuilder getHttpPostMethod(final String declaredByMetadataId) {
		final List<AnnotationMetadataBuilder> postMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		final List<AnnotationAttributeValue<?>> postMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		postMethodAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("POST"))));
		postMethodAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "{id}"));
		postMethodAnnotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, postMethodAttributes));

		final List<AnnotatedJavaType> postParamTypes = new ArrayList<AnnotatedJavaType>();
		final AnnotationMetadataBuilder idParamAnnotation = new AnnotationMetadataBuilder(PATH_VARIABLE);
		postParamTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Long"), idParamAnnotation.build()));
		postParamTypes.add(new AnnotatedJavaType(MODEL_MAP));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest")));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse")));

		final List<JavaSymbolName> postParamNames = new ArrayList<JavaSymbolName>();
		postParamNames.add(new JavaSymbolName("id"));
		postParamNames.add(new JavaSymbolName("modelMap"));
		postParamNames.add(new JavaSymbolName("request"));
		postParamNames.add(new JavaSymbolName("response"));

		final MethodMetadataBuilder postMethodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("post"), JavaType.VOID_PRIMITIVE, postParamTypes, postParamNames, new InvocableMemberBodyBuilder());
		postMethodBuilder.setAnnotations(postMethodAnnotations);
		return postMethodBuilder;
	}

	private MethodMetadataBuilder getHttpGetMethod(final String declaredByMetadataId) {
		final List<AnnotationMetadataBuilder> getMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		final List<AnnotationAttributeValue<?>> getMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		getMethodAnnotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, getMethodAttributes));

		final List<AnnotatedJavaType> getParamTypes = new ArrayList<AnnotatedJavaType>();
		getParamTypes.add(new AnnotatedJavaType(MODEL_MAP));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest")));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse")));

		final List<JavaSymbolName> getParamNames = new ArrayList<JavaSymbolName>();
		getParamNames.add(new JavaSymbolName("modelMap"));
		getParamNames.add(new JavaSymbolName("request"));
		getParamNames.add(new JavaSymbolName("response"));

		final MethodMetadataBuilder getMethodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("get"), JavaType.VOID_PRIMITIVE, getParamTypes, getParamNames, new InvocableMemberBodyBuilder());
		getMethodBuilder.setAnnotations(getMethodAnnotations);
		return getMethodBuilder;
	}

	/**
	 * Adds Tiles Maven dependencies and updates the MVC config to include Tiles view support
	 */
	private void updateConfiguration() {
		// Add tiles dependencies to pom
		final Element configuration = XmlUtils.getRootElement(getClass(), "tiles/configuration.xml");

		final List<Dependency> dependencies = new ArrayList<Dependency>();
		final List<Element> springDependencies = XmlUtils.findElements("/configuration/tiles/dependencies/dependency", configuration);
		for (final Element dependencyElement : springDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);

		// Add config to MVC app context
		final String mvcConfig = projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		final Document mvcConfigDocument = XmlUtils.readXml(fileManager.getInputStream(mvcConfig));
		final Element beans = mvcConfigDocument.getDocumentElement();

		if (XmlUtils.findFirstElement("/beans/bean[@id = 'tilesViewResolver']", beans) != null || XmlUtils.findFirstElement("/beans/bean[@id = 'tilesConfigurer']", beans) != null) {
			return; // Tiles is already configured, nothing to do
		}

		final Document configDoc = getDocumentTemplate("tiles/tiles-mvc-config-template.xml");
		final Element configElement = configDoc.getDocumentElement();
		final List<Element> tilesConfig = XmlUtils.findElements("/config/bean", configElement);
		for (final Element bean : tilesConfig) {
			final Node importedBean = mvcConfigDocument.importNode(bean, true);
			beans.appendChild(importedBean);
		}
		fileManager.createOrUpdateTextFileIfRequired(mvcConfig, XmlUtils.nodeToString(mvcConfigDocument), true);
	}

	public void installI18n(final I18n i18n) {
		Assert.notNull(i18n, "Language choice required");

		if (i18n.getLocale() == null) {
			logger.warning("could not parse language choice");
			return;
		}

		final String targetDirectory = projectOperations.getPathResolver().getIdentifier(SRC_MAIN_WEBAPP, "");

		// Install message bundle
		String messageBundle = targetDirectory + "/WEB-INF/i18n/messages_" + i18n.getLocale().getLanguage() /*+ country*/ + ".properties";
		// Special case for english locale (default)
		if (i18n.getLocale().equals(Locale.ENGLISH)) {
			messageBundle = targetDirectory + "/WEB-INF/i18n/messages.properties";
		}
		if (!fileManager.exists(messageBundle)) {
			try {
				FileCopyUtils.copy(i18n.getMessageBundle(), fileManager.createFile(messageBundle).getOutputStream());
			} catch (final IOException e) {
				throw new IllegalStateException("Encountered an error during copying of message bundle MVC JSP addon.", e);
			}
		}

		// Install flag
		final String flagGraphic = targetDirectory + "/images/" + i18n.getLocale().getLanguage() /*+ country*/ + ".png";
		if (!fileManager.exists(flagGraphic)) {
			try {
				FileCopyUtils.copy(i18n.getFlagGraphic(), fileManager.createFile(flagGraphic).getOutputStream());
			} catch (final IOException e) {
				throw new IllegalStateException("Encountered an error during copying of flag graphic for MVC JSP addon.", e);
			}
		}

		// Setup language definition in languages.jspx
		final String footerFileLocation = targetDirectory + "/WEB-INF/views/footer.jspx";
		final Document footer = XmlUtils.readXml(fileManager.getInputStream(footerFileLocation));

		if (XmlUtils.findFirstElement("//span[@id='language']/language[@locale='" + i18n.getLocale().getLanguage() + "']", footer.getDocumentElement()) == null) {
			final Element span = XmlUtils.findRequiredElement("//span[@id='language']", footer.getDocumentElement());
			span.appendChild(new XmlElementBuilder("util:language", footer).addAttribute("locale", i18n.getLocale().getLanguage()).addAttribute("label", i18n.getLanguage()).build());
			fileManager.createOrUpdateTextFileIfRequired(footerFileLocation, XmlUtils.nodeToString(footer), false);
		}

		// Record use of add-on (most languages are implemented via public add-ons)
		final String bundleSymbolicName = BundleFindingUtils.findFirstBundleForTypeName(context.getBundleContext(), i18n.getClass().getName());
		if (bundleSymbolicName != null) {
			uaaRegistrationService.registerBundleSymbolicNameUse(bundleSymbolicName, null);
		}
	}

	private String cleanPath(String path) {
		if ("/".equals(path)) {
			return "";
		}
		path = StringUtils.prefix(path, "/");
		if (path.contains(".")) {
			path = path.substring(0, path.indexOf(".") - 1);
		}
		return path;
	}

	private String cleanViewName(String viewName) {
		if (viewName.startsWith("/")) {
			viewName = viewName.substring(1);
		}
		if (viewName.contains(".")) {
			viewName = viewName.substring(0, viewName.indexOf(".") - 1);
		}
		return viewName;
	}
}
