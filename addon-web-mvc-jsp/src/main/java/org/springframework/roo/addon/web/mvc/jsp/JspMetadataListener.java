package org.springframework.roo.addon.web.mvc.jsp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.beaninfo.BeanInfoUtils;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.IdentifierMetadata;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.controller.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Listens for {@link WebScaffoldMetadata} and produces JSPs when requested by that metadata.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class JspMetadataListener implements MetadataProvider, MetadataNotificationListener {
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MenuOperations menuOperations;
	@Reference private JspOperations jspOperations;
	@Reference private PathResolver pathResolver;
	@Reference private TilesOperations tilesOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private TypeLocationService typeLocationService;

	private Map<JavaType, String> pluralCache = new HashMap<JavaType, String>();

	private BeanInfoMetadata beanInfoMetadata; // caution: concurrent access not supported

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		// Work out the MIDs of the other metadata we depend on
		// NB: The JavaType and Path are to the corresponding web scaffold controller class

		JavaType javaType = JspMetadata.getJavaType(metadataIdentificationString);
		Path path = JspMetadata.getPath(metadataIdentificationString);
		String webScaffoldMetadataKey = WebScaffoldMetadata.createIdentifier(javaType, path);
		WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataKey);

		if (webScaffoldMetadata == null || !webScaffoldMetadata.isValid()) {
			// Can't get the corresponding scaffold, so we certainly don't need to manage any JSPs at this time
			return null;
		}
		
		// Shouldn't be needed, as we get notified for every change to web scaffold metadata anyway
		// metadataDependencyRegistry.registerDependency(webScaffoldMetadataKey, metadataIdentificationString);

		// We need to lookup the metadata for the entity we are creating
		String beanInfoMetadataKey = webScaffoldMetadata.getIdentifierForBeanInfoMetadata();
		String entityMetadataKey = webScaffoldMetadata.getIdentifierForEntityMetadata();

		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);

		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			// Can't get hold of the entity we are needing to build JSPs for
			return null;
		}

		this.beanInfoMetadata = beanInfoMetadata;

		// Install web artifacts only if Spring MVC config is missing
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views"))) {
			jspOperations.installCommonViewArtefacts();
		}

		String finderMetadataKey = FinderMetadata.createIdentifier(EntityMetadata.getJavaType(entityMetadataKey), path);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetadataKey);

		JspMetadata md = new JspMetadata(metadataIdentificationString, beanInfoMetadata, webScaffoldMetadata);

		installImage("images/show.png");
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			installImage("images/update.png");
		}
		if (webScaffoldMetadata.getAnnotationValues().isDelete()) {
			installImage("images/delete.png");
		}

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");

		List<FieldMetadata> elegibleFields = getElegibleFields();

		JspViewManager viewManager = new JspViewManager(metadataService, elegibleFields, beanInfoMetadata, entityMetadata, webScaffoldMetadata.getAnnotationValues(), typeLocationService);

		String controllerPath = webScaffoldMetadata.getAnnotationValues().getPath();

		if (controllerPath.startsWith("/")) {
			controllerPath = controllerPath.substring(1);
		}

		// Make the holding directory for this controller
		String destinationDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/" + controllerPath);
		if (!fileManager.exists(destinationDirectory)) {
			fileManager.createDirectory(destinationDirectory);
		} else {
			File file = new File(destinationDirectory);
			Assert.isTrue(file.isDirectory(), destinationDirectory + " is a file, when a directory was expected");
		}

		// By now we have a directory to put the JSPs inside
		String listPath1 = destinationDirectory + "/list.jspx";
		writeToDiskIfNecessary(listPath1, viewManager.getListDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "list", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/list.jspx");

		String showPath = destinationDirectory + "/show.jspx";
		writeToDiskIfNecessary(showPath, viewManager.getShowDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "show", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/show.jspx");

		JavaSymbolName categoryName = new JavaSymbolName(beanInfoMetadata.getJavaBean().getSimpleTypeName());

		Map<String, String> properties = new HashMap<String, String>();
		properties.put("menu_category_" + categoryName.getSymbolName().toLowerCase() + "_label", categoryName.getReadableSymbolName());
		
		if (webScaffoldMetadata.getAnnotationValues().isCreate()) {
			String listPath = destinationDirectory + "/create.jspx";
			writeToDiskIfNecessary(listPath, viewManager.getCreateDocument());
			JavaSymbolName menuItemId = new JavaSymbolName("new");
			// add 'create new' menu item
			menuOperations.addMenuItem(categoryName, menuItemId, "global_menu_new", "/" + controllerPath + "?form", MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", new JavaSymbolName(beanInfoMetadata.getJavaBean().getSimpleTypeName()).getReadableSymbolName());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "create", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/create.jspx");
		} else {
			menuOperations.cleanUpMenuItem(categoryName, new JavaSymbolName("new"), MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			tilesOperations.removeViewDefinition(controllerPath + "/" + "create", controllerPath);
		}
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			String listPath = destinationDirectory + "/update.jspx";
			writeToDiskIfNecessary(listPath, viewManager.getUpdateDocument());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "update", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/update.jspx");
		} else {
			tilesOperations.removeViewDefinition(controllerPath + "/" + "update", controllerPath);
		}
		// setup labels for i18n support
		String resourceId = XmlUtils.convertId("label." + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName().toLowerCase());
		properties.put(resourceId, new JavaSymbolName(beanInfoMetadata.getJavaBean().getSimpleTypeName()).getReadableSymbolName());

		String pluralResourceId = XmlUtils.convertId(resourceId + ".plural");
		properties.put(pluralResourceId, new JavaSymbolName(getPlural(beanInfoMetadata.getJavaBean())).getReadableSymbolName());

		for (MethodMetadata method : beanInfoMetadata.getPublicAccessors(false)) {
			JavaSymbolName fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(fieldName);
			String fieldResourceId = XmlUtils.convertId(resourceId + "." + fieldName.getSymbolName().toLowerCase());
			if (field != null && isRooIdentifier(field.getFieldType())) {
				IdentifierMetadata im = (IdentifierMetadata) metadataService.get(IdentifierMetadata.createIdentifier(field.getFieldType(), Path.SRC_MAIN_JAVA));
				if (im != null) {
					for (FieldMetadata f : im.getFields()) {
						String sb = f.getFieldName().getReadableSymbolName();
						properties.put(XmlUtils.convertId(resourceId + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "." + f.getFieldName().getSymbolName().toLowerCase()), (sb == null || sb.length() == 0) ? fieldName.getSymbolName() : sb);
					}
				}
			} else if (!fieldName.equals(entityMetadata.getIdentifierField().getFieldName()) || !fieldName.equals(entityMetadata.getVersionField().getFieldName())) {
				String sb = fieldName.getReadableSymbolName();
				properties.put(fieldResourceId, (sb == null || sb.length() == 0) ? fieldName.getSymbolName() : sb);
			}
		}

		if (entityMetadata.getFindAllMethod() != null) {
			// Add 'list all' menu item
			JavaSymbolName menuItemId = new JavaSymbolName("list");
			menuOperations.addMenuItem(categoryName, menuItemId, "global_menu_list", "/" + controllerPath + "?page=1&size=${empty param.size ? 10 : param.size}", MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", new JavaSymbolName(getPlural(beanInfoMetadata.getJavaBean())).getReadableSymbolName());
		} else {
			menuOperations.cleanUpMenuItem(categoryName, new JavaSymbolName("list"), MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
		}
		
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(beanInfoMetadata.getJavaBean(), Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine plural for type " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName());

		List<String> allowedMenuItems = new ArrayList<String>();
		if (webScaffoldMetadata.getAnnotationValues().isExposeFinders() && finderMetadata != null) {
			for (MethodMetadata methodMetadata : finderMetadata.getAllDynamicFinders()) {
				String finderName = methodMetadata.getMethodName().getSymbolName();
				String listPath = destinationDirectory + "/" + finderName + ".jspx";
				// finders only get scaffolded if the finder name is not too long (see ROO-1027)
				if (listPath.length() > 244) {
					continue;
				}
				
				writeToDiskIfNecessary(listPath, viewManager.getFinderDocument(methodMetadata));
				JavaSymbolName finderLabel = new JavaSymbolName(finderName.replace("find" + getPlural(beanInfoMetadata.getJavaBean()) + "By", ""));
				// Add 'Find by' menu item
				menuOperations.addMenuItem(categoryName, finderLabel, "global_menu_find", "/" + controllerPath + "?find=" + finderName.replace("find" + getPlural(beanInfoMetadata.getJavaBean()), "") + "&form", MenuOperations.FINDER_MENU_ITEM_PREFIX);
				properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + finderLabel.getSymbolName().toLowerCase() + "_label", finderLabel.getReadableSymbolName());
				allowedMenuItems.add(MenuOperations.FINDER_MENU_ITEM_PREFIX + categoryName.getSymbolName().toLowerCase() + "_" + finderLabel.getSymbolName().toLowerCase());
				for (JavaSymbolName paramName : methodMetadata.getParameterNames()) {
					properties.put(XmlUtils.convertId(resourceId + "." + paramName.getSymbolName().toLowerCase()), paramName.getReadableSymbolName());
				}
				tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + finderName, TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/" + finderName + ".jspx");
			}
		}
		
		propFileOperations.addProperties(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", properties, true, false);

		// clean up links to finders which are removed by now
		menuOperations.cleanUpFinderMenuItems(categoryName, allowedMenuItems);

		return md;
	}

	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(String jspFilename, Document proposed) {
		Document original = null;

		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(jspFilename)) {
			try {
				original = XmlUtils.getDocumentBuilder().parse(fileManager.getInputStream(jspFilename));
			} catch (Exception e) {
				new IllegalStateException("Could not parse file: " + jspFilename);
			}
			Assert.notNull(original, "Unable to parse " + jspFilename);
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				mutableFile = fileManager.updateFile(jspFilename);
			}
		} else {
			original = proposed;
			mutableFile = fileManager.createFile(jspFilename);
			Assert.notNull(mutableFile, "Could not create JSP file '" + jspFilename + "'");
		}

		try {
			if (mutableFile != null) {
				// Build a string representation of the JSP
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, original);
				String jspContent = byteArrayOutputStream.toString();
				byteArrayOutputStream.close();
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
		// A file existed, but it contained the same content, so we return false
		return false;
	}

	private List<FieldMetadata> getElegibleFields() {
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		for (MethodMetadata method : beanInfoMetadata.getPublicAccessors(false)) {
			JavaSymbolName propertyName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);

			if (field != null && hasMutator(field)) {

				// Never include id field (it shouldn't normally have a mutator anyway, but the user might have added one)
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Id")) != null) {
					continue;
				}
				// Never include version field (it shouldn't normally have a mutator anyway, but the user might have added one)
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Version")) != null) {
					continue;
				}
				fields.add(field);
			}
		}
		return fields;
	}

	private boolean hasMutator(FieldMetadata fieldMetadata) {
		for (MethodMetadata mutator : beanInfoMetadata.getPublicMutators()) {
			if (fieldMetadata.equals(beanInfoMetadata.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(mutator)))) return true;
		}
		return false;
	}

	private boolean isRooIdentifier(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			return false;
		}
		ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (cid == null) {
			return false;
		}
		return null != MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), new JavaType(RooIdentifier.class.getName()));
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(WebScaffoldMetadata.getMetadataIdentiferType())), "Expected class-level notifications only for web scaffold metadata (not '" + upstreamDependency + "')");

			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType javaType = WebScaffoldMetadata.getJavaType(upstreamDependency);
			Path path = WebScaffoldMetadata.getPath(upstreamDependency);
			downstreamDependency = JspMetadata.createIdentifier(javaType, path);

			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");

		metadataService.get(downstreamDependency, true);
	}

	public String getProvidesType() {
		return JspMetadata.getMetadataIdentiferType();
	}

	private void installImage(String imagePath) {
		String imageFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, imagePath);
		if (!fileManager.exists(imageFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), imagePath), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, imagePath)).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}

	private String getPlural(JavaType type) {
		if (pluralCache.get(type) != null) {
			return pluralCache.get(type);
		}
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine the plural for the '" + type.getFullyQualifiedTypeName() + "' type");
		if (!pluralMetadata.getPlural().equals(type.getSimpleTypeName())) {
			pluralCache.put(type, pluralMetadata.getPlural());
			return pluralMetadata.getPlural();
		}
		pluralCache.put(type, pluralMetadata.getPlural() + "Items");
		return pluralMetadata.getPlural() + "Items";
	}
}