package org.springframework.roo.addon.web.mvc.jsp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18nSupport;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperationsImpl;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.osgi.UrlFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Jeremy Grelle
 * @since 1.0
 * 
 */
@Component
@Service
public class JspOperationsImpl implements JspOperations {
	
	private Logger log = Logger.getLogger(JspOperationsImpl.class.getName());
	
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ClasspathOperations classpathOperations;
	@Reference private WebMvcOperations webMvcOperations;
	@Reference private PathResolver pathResolver;
	@Reference private MenuOperations menuOperations;
	@Reference private TilesOperations tilesOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private I18nSupport i18nSupport;
	
	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
	}

	public boolean isProjectAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	public boolean isInstallLanguageCommandAvailable() {
		return fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/footer.jspx"));
	}
	
	public void installCommonViewArtefacts() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");

		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.notNull(projectMetadata, "Unable to obtain path resolver");

		// Install tiles config
		updateConfiguration();

		// Install styles
		copyDirectoryContents("images/*.*", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/images"));

		// Install styles
		copyDirectoryContents("styles/*.css", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/styles"));
		copyDirectoryContents("styles/*.properties", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/classes"));

		// Install layout
		copyDirectoryContents("tiles/default.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/"));
		copyDirectoryContents("tiles/layouts.xml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/"));
		copyDirectoryContents("tiles/header.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"));
		copyDirectoryContents("tiles/footer.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"));
		copyDirectoryContents("tiles/views.xml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"));

		// Install common view files
		copyDirectoryContents("*.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"));

		// Install tags
		copyDirectoryContents("tags/form/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/form"));
		copyDirectoryContents("tags/form/fields/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/form/fields"));
		copyDirectoryContents("tags/menu/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu"));
		copyDirectoryContents("tags/util/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/util"));
		
		// Install default language 'en'
		installI18n(i18nSupport.getLanguage(Locale.ENGLISH));

		String i18nDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties");
		if (!fileManager.exists(i18nDirectory)) {
			try {
				fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties"));
				propFileOperations.changeProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", "application_name", projectMetadata.getProjectName().substring(0, 1).toUpperCase() + projectMetadata.getProjectName().substring(1), true);
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}
	
	public void installView(String path, String viewName, String title, String category) {
		installView(path, viewName, title, category, null, true);
	}
	
	public void installView(String path, String viewName, String title, String category, Document document) {
		installView(path, viewName, title, category, document, true);
	}
	
	private void installView(String path, String viewName, String title, String category, Document document, boolean registerStaticController) {
		Assert.hasText(path, "Path required");
		Assert.hasText(viewName, "View name required");
		Assert.hasText(title, "Title required");
		path = cleanPath(path);
		viewName = cleanViewName(viewName);
		String lcViewName = viewName.toLowerCase();
		if (document != null) {
			XmlUtils.writeXml(fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views" + path + "/" + lcViewName + ".jspx")).getOutputStream(), document);
		} else {
			try {
				Document doc = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), "index-template.jspx"));
				XmlUtils.findRequiredElement("/div/message", doc.getDocumentElement()).setAttribute("code", "label" + path.replace("/", "_") + "_" + lcViewName);
				XmlUtils.findRequiredElement("/div/page", doc.getDocumentElement()).setAttribute("id", path.replace("/", "_") + "_" + lcViewName);
				XmlUtils.writeXml(fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views" + path + "/" + lcViewName + ".jspx")).getOutputStream(), doc);
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for controller class.", e);
			}
		}
		installView(new JavaSymbolName(viewName), path, title, category, registerStaticController);
	}

	/**
	 * Creates a new Spring MVC static view.
	 * 
	 * @param path the static view to create in (required, ie '/foo/blah')
	 * @param viewName the mapping this view should adopt (required, ie 'index')
	 */
	private void installView(JavaSymbolName viewName, String folderName, String title, String category, boolean registerStaticController) {
		//probe if common we artifacts exist, and install them if needed
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/default.jspx"))) {
			webMvcOperations.installAllWebMvcArtifacts();
			installCommonViewArtefacts();
		}
		String lcViewName = viewName.getSymbolName().toLowerCase();
		propFileOperations.changeProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", "label" + folderName.replace("/", "_") + "_" + lcViewName, title, true);
		menuOperations.addMenuItem(new JavaSymbolName(category), new JavaSymbolName(folderName.replace("/", "_") + lcViewName + "_id"), title, "global_generic", folderName + "/" + lcViewName, null);
		tilesOperations.addViewDefinition(folderName, folderName + "/" + lcViewName, TilesOperationsImpl.DEFAULT_TEMPLATE, "/WEB-INF/views" + folderName + "/" + lcViewName + ".jspx");
		
		String mvcConfig = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webmvc-config.xml");
		
		if (registerStaticController && fileManager.exists(mvcConfig)) {
			MutableFile mvcConfigFile = fileManager.updateFile(mvcConfig);
			Document doc;
			try {
				doc = XmlUtils.getDocumentBuilder().parse(mvcConfigFile.getInputStream());
			} catch (Exception e) {
				throw new IllegalStateException("Could not parse " + mvcConfig, e);
			}
			 
			if (null == XmlUtils.findFirstElement("/beans/view-controller[@path='" + folderName + "/" + lcViewName + "']", doc.getDocumentElement())) {
				Element sibling = XmlUtils.findFirstElement("/beans/view-controller", doc.getDocumentElement());
				Element view = new XmlElementBuilder("mvc:view-controller", doc).addAttribute("path", folderName + "/" + lcViewName).build();
				if (sibling != null) {
					sibling.getParentNode().insertBefore(view, sibling);
				} else {
					doc.getDocumentElement().appendChild(view);
				}
				XmlUtils.writeXml(mvcConfigFile.getOutputStream(), doc);
			}
		}
	}

	/**
	 * Creates a new Spring MVC controller.
	 * 
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**". You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 * 
	 * @param controller the controller class to create (required)
	 * @param preferredMapping the mapping this controller should adopt (optional; if unspecified it will be based on the controller name)
	 */
	public void createManualController(JavaType controller, String preferredMapping) {
		Assert.notNull(controller, "Controller Java Type required");

		String resourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);
		String folderName = null;

		// Create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		if (preferredMapping == null || preferredMapping.length() == 0) {
			String typeName = controller.getSimpleTypeName();
			int dropFrom = typeName.lastIndexOf("Controller");
			if (dropFrom > -1) {
				typeName = typeName.substring(0, dropFrom);
			}
			folderName = typeName.toLowerCase();
			preferredMapping = "/" + folderName + "/**";
		}
		if (!preferredMapping.startsWith("/")) {
			folderName = preferredMapping;
			preferredMapping = "/" + preferredMapping;
		} else {
			folderName = preferredMapping.substring(1);
		}
		if (!preferredMapping.endsWith("/**")) {
			preferredMapping = preferredMapping + "/**";
		} else {
			folderName = folderName.replace("/**", "");
		}
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), preferredMapping));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		// Create annotation @Controller
		List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		AnnotationMetadata controllerAnnotation = new DefaultAnnotationMetadata(new JavaType("org.springframework.stereotype.Controller"), controllerAttributes);

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, pathResolver.getPath(resourceIdentifier));
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
		annotations.add(controllerAnnotation);

		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();

		List<AnnotationMetadata> getMethodAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> getMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		getMethodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), getMethodAttributes));
		List<AnnotatedJavaType> getParamTypes = new ArrayList<AnnotatedJavaType>();
		getParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), null));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse"), null));
		List<JavaSymbolName> getParamNames = new ArrayList<JavaSymbolName>();
		getParamNames.add(new JavaSymbolName("modelMap"));
		getParamNames.add(new JavaSymbolName("request"));
		getParamNames.add(new JavaSymbolName("response"));
		MethodMetadata getMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("get"), JavaType.VOID_PRIMITIVE, getParamTypes, getParamNames, getMethodAnnotations, null, null);
		methods.add(getMethod);

		List<AnnotationMetadata> postMethodAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> postMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		postMethodAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		postMethodAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "{id}"));
		postMethodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), postMethodAttributes));
		List<AnnotatedJavaType> postParamTypes = new ArrayList<AnnotatedJavaType>();
		List<AnnotationMetadata> idParamAnnotations = new ArrayList<AnnotationMetadata>();
		idParamAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.PathVariable"), new ArrayList<AnnotationAttributeValue<?>>()));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Long"), idParamAnnotations));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), null));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse"), null));
		List<JavaSymbolName> postParamNames = new ArrayList<JavaSymbolName>();
		postParamNames.add(new JavaSymbolName("id"));
		postParamNames.add(new JavaSymbolName("modelMap"));
		postParamNames.add(new JavaSymbolName("request"));
		postParamNames.add(new JavaSymbolName("response"));
		MethodMetadata postMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("post"), JavaType.VOID_PRIMITIVE, postParamTypes, postParamNames, postMethodAnnotations, null, null);
		methods.add(postMethod);

		List<AnnotationMetadata> indexMethodAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> indexMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		indexMethodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), indexMethodAttributes));
		List<AnnotatedJavaType> indexParamTypes = new ArrayList<AnnotatedJavaType>();
		MethodMetadata indexMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("index"), JavaType.STRING_OBJECT, indexParamTypes, postParamNames, indexMethodAnnotations, null, "return \"" + folderName + "/index\";");
		methods.add(indexMethod);

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, controller, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, annotations, null);

		classpathOperations.generateClassFile(details);

		installView(folderName, "/index", new JavaSymbolName(controller.getSimpleTypeName()).getReadableSymbolName() + " View", "Controller", null, false);
	}

	/**
	 * Adds Tiles Maven dependencies and updates the MVC config to include Tiles view support
	 * 
	 */
	private void updateConfiguration() {
		// Add tiles dependencies to pom
		Element configuration = XmlUtils.getConfiguration(getClass(), "tiles/configuration.xml");

		List<Element> springDependencies = XmlUtils.findElements("/configuration/tiles/dependencies/dependency", configuration);
		for (Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}

		// Add config to MVC app context
		String mvcConfig = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		MutableFile mutableMvcConfigFile = fileManager.updateFile(mvcConfig);
		Document mvcConfigDocument;
		try {
			mvcConfigDocument = XmlUtils.getDocumentBuilder().parse(mutableMvcConfigFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open Spring MVC config file '" + mvcConfig + "'", ex);
		}

		Element beans = mvcConfigDocument.getDocumentElement();

		if (null != XmlUtils.findFirstElement("/beans/bean[@id='tilesViewResolver']", beans) || null != XmlUtils.findFirstElement("/beans/bean[@id='tilesConfigurer']", beans)) {
			return; // Tiles is already configured, nothing to do
		}

		InputStream configTemplateInputStream = TemplateUtils.getTemplate(getClass(), "tiles/tiles-mvc-config-template.xml");
		Assert.notNull(configTemplateInputStream, "Could not acquire dependencies.xml file");
		Document configDoc;
		try {
			configDoc = XmlUtils.getDocumentBuilder().parse(configTemplateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element configElement = configDoc.getDocumentElement();
		List<Element> tilesConfig = XmlUtils.findElements("/config/bean", configElement);

		for (Element bean : tilesConfig) {
			Node importedBean = mvcConfigDocument.importNode(bean, true);
			beans.appendChild(importedBean);
		}

		XmlUtils.writeXml(mutableMvcConfigFile.getOutputStream(), mvcConfigDocument);
		try {
			configTemplateInputStream.close();
		} catch (IOException ignore) {}
	}

	/**
	 * This method will copy the contents of a directory to another if the resource does not already exist in the target directory
	 * 
	 * @param sourceAntPath directory
	 * @param target directory
	 */
	private void copyDirectoryContents(String sourceAntPath, String targetDirectory) {
		Assert.hasText(sourceAntPath, "Source path required");
		Assert.hasText(targetDirectory, "Target directory required");

		if (!targetDirectory.endsWith("/")) {
			targetDirectory += "/";
		}

		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		Set<URL> urls = UrlFindingUtils.findMatchingClasspathResources(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");
		for (URL url : urls) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
			if (!fileManager.exists(targetDirectory + fileName)) {
				try {
					FileCopyUtils.copy(url.openStream(), fileManager.createFile(targetDirectory + fileName).getOutputStream());
				} catch (IOException e) {
					new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
				}
			}
		}
	}

	public void installI18n(I18n i18n) {
		Assert.notNull(i18n, "Language choice required");
		 
		if (i18n.getLocale() == null) {
			log.warning("could not parse language choice");
			return;
		}
		
		String targetDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "");
		
		//install message bundle
		String messageBundle = targetDirectory + "/WEB-INF/i18n/messages_" + i18n.getLocale() + ".properties";
		//special case for english locale (default)
		if (i18n.getLocale().equals(Locale.ENGLISH)) {
			messageBundle  = targetDirectory + "/WEB-INF/i18n/messages.properties";
		}
		if (!fileManager.exists(messageBundle)) {
			try {
				FileCopyUtils.copy(i18n.getMessageBundle(), fileManager.createFile(messageBundle).getOutputStream());
			} catch (IOException e) {
				new IllegalStateException("Encountered an error during copying of message bundle MVC JSP addon.", e);
			}
		}
		
		//install flag
		String flagGraphic = targetDirectory + "/images/" + i18n.getLocale() + ".png";
		if (!fileManager.exists(flagGraphic)) {
			try {
				FileCopyUtils.copy(i18n.getFlagGraphic(), fileManager.createFile(flagGraphic).getOutputStream());
			} catch (IOException e) {
				new IllegalStateException("Encountered an error during copying of flag graphic for MVC JSP addon.", e);
			}
		}
		
		//setup language definition in languages.jspx
		String footerFileLocation = targetDirectory + "/WEB-INF/views/footer.jspx";
		MutableFile footerFile = null;

		Document footer = null;
		try {
			if (fileManager.exists(footerFileLocation)) {
				footerFile = fileManager.updateFile(footerFileLocation);
				footer = XmlUtils.getDocumentBuilder().parse(footerFile.getInputStream());
			} else {
				new IllegalStateException("Could not aquire the footer.jspx file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		if (null == XmlUtils.findFirstElement("//span[@id='language']/language[@locale='" + i18n.getLocale().toString() + "']", footer.getDocumentElement())) {
			Element span = XmlUtils.findRequiredElement("//span[@id='language']", footer.getDocumentElement());
			span.appendChild(new XmlElementBuilder("util:language", footer).addAttribute("locale", i18n.getLocale().toString()).addAttribute("label", i18n.getLanguage()).build());
			XmlUtils.writeXml(footerFile.getOutputStream(), footer);
		}
	}
	
	private String cleanPath(String path) {
		if (path.equals("/")) {
			return "";
		}
		if (!path.startsWith("/")) {
			path = "/".concat(path);
		} 
		if (path.contains(".")) {
			path = path.substring(0, path.indexOf(".") - 1);
		}
		return path.toLowerCase();
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
