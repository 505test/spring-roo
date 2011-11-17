package org.springframework.roo.addon.jsf.managedbean;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.APPLICATION;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.APPLICATION_SCOPED;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.DATE_TIME_CONVERTER;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.DISPLAY_CREATE_DIALOG;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.DISPLAY_LIST;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.DOUBLE_RANGE_VALIDATOR;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.EL_CONTEXT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.ENUM_CONVERTER;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.EXPRESSION_FACTORY;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.FACES_MESSAGE;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.HTML_OUTPUT_TEXT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.HTML_PANEL_GRID;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.LENGTH_VALIDATOR;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.LONG_RANGE_VALIDATOR;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.MANAGED_BEAN;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_AUTO_COMPLETE;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_CALENDAR;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_CLOSE_EVENT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_COMMAND_BUTTON;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_DEFAULT_STREAMED_CONTENT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_FILE_DOWNLOAD_ACTION_LISTENER;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_FILE_UPLOAD;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_FILE_UPLOAD_EVENT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_INPUT_TEXT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_INPUT_TEXTAREA;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_MESSAGE;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_REQUEST_CONTEXT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_SELECT_BOOLEAN_CHECKBOX;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_SELECT_MANY_MENU;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_SPINNER;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.PRIMEFACES_STREAMED_CONTENT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.REGEX_VALIDATOR;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.UI_COMPONENT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.UI_SELECT_ITEM;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.UI_SELECT_ITEMS;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JavaType.VOID_PRIMITIVE;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.BYTE_ARRAY_INPUT_STREAM;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.POST_CONSTRUCT;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.Jsr303JavaType.PATTERN;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.jsf.model.UploadedFileContentType;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.NumberUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfManagedBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	static final String APPLICATION_TYPE_KEY = "applicationTypeKey";
	static final String APPLICATION_TYPE_FIELDS_KEY = "applicationTypeFieldsKey";
	static final String ENUMERATED_KEY = "enumeratedKey";
	static final String CRUD_ADDITIONS_KEY = "crudAdditionsKey";
	static final String LIST_VIEW_FIELD_KEY = "listViewFieldKey";
	static final String PARAMETER_TYPE_KEY = "parameterTypeKey";
	static final String PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY = "parameterTypeManagedBeanNameKey";
	static final String PARAMETER_TYPE_PLURAL_KEY = "parameterTypePluralKey";

	private static final String PROVIDES_TYPE_STRING = JsfManagedBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaSymbolName NAME = new JavaSymbolName("name");
	private static final JavaSymbolName CREATE_DIALOG_VISIBLE = new JavaSymbolName("createDialogVisible");
	private static final JavaSymbolName DATA_VISIBLE = new JavaSymbolName("dataVisible");
	private static final JavaSymbolName COLUMNS = new JavaSymbolName("columns");
	private static final String HTML_PANEL_GRID_ID = "htmlPanelGrid";

	// Fields
	private Set<FieldMetadata> locatedFields;
	private JavaType entity;
	private String beanName;
	private String plural;
	private JavaSymbolName entityName;
	private final List<FieldMetadata> builderFields = new ArrayList<FieldMetadata>();
	private final List<MethodMetadata> builderMethods = new ArrayList<MethodMetadata>();

	private enum Action {
		CREATE, EDIT, VIEW;
	};

	public JsfManagedBeanMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JsfManagedBeanAnnotationValues annotationValues, final String plural, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions, final Set<FieldMetadata> locatedFields, final MethodMetadata identifierAccessor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' is invalid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.isTrue(StringUtils.hasText(plural), "Plural required");
		Assert.notNull(crudAdditions, "Crud additions map required");
		Assert.notNull(locatedFields, "Located fields required");

		if (!isValid()) {
			return;
		}

		entity = annotationValues.getEntity();

		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		final MemberTypeAdditions mergeMethod = crudAdditions.get(MERGE_METHOD);
		final MemberTypeAdditions persistMethod = crudAdditions.get(PERSIST_METHOD);
		final MemberTypeAdditions removeMethod = crudAdditions.get(REMOVE_METHOD);
		if (identifierAccessor == null || findAllMethod == null || mergeMethod == null || persistMethod == null || removeMethod == null || entity == null) {
			valid = false;
			return;
		}

		this.locatedFields = locatedFields;
		beanName = annotationValues.getBeanName();
		this.plural = plural;
		entityName = JavaSymbolName.getReservedWordSafeName(entity);

		final JavaSymbolName allEntitiesFieldName = new JavaSymbolName("all" + plural);
		final JavaType entityListType = getListType(entity);

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation(annotationValues.getBeanName()));

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getScopeAnnotation());

		// Add builderFields
		builderFields.add(getField(PRIVATE, NAME, STRING, "\"" + plural + "\""));
		builderFields.add(getField(entityName, entity));
		builderFields.add(getField(allEntitiesFieldName, entityListType));
		builderFields.add(getField(PRIVATE, DATA_VISIBLE, BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));
		builderFields.add(getField(COLUMNS, getListType(STRING)));
		builderFields.add(getPanelGridField(Action.CREATE));
		builderFields.add(getPanelGridField(Action.EDIT));
		builderFields.add(getPanelGridField(Action.VIEW));
		builderFields.add(getField(PRIVATE, CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));

		// Add builderMethods
		builderMethods.add(getInitMethod(identifierAccessor));
		builderMethods.add(getAccessorMethod(NAME, STRING));
		builderMethods.add(getAccessorMethod(COLUMNS, getListType(STRING)));
		builderMethods.add(getAccessorMethod(allEntitiesFieldName, entityListType));
		builderMethods.add(getMutatorMethod(allEntitiesFieldName, entityListType));
		builderMethods.add(getFindAllEntitiesMethod(allEntitiesFieldName, findAllMethod));
		builderMethods.add(getAccessorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
		builderMethods.add(getMutatorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
		builderMethods.add(getPanelGridAccessorMethod(Action.CREATE));
		builderMethods.add(getPanelGridMutatorMethod(Action.CREATE));
		builderMethods.add(getPanelGridAccessorMethod(Action.EDIT));
		builderMethods.add(getPanelGridMutatorMethod(Action.EDIT));
		builderMethods.add(getPanelGridAccessorMethod(Action.VIEW));
		builderMethods.add(getPanelGridMutatorMethod(Action.VIEW));
		builderMethods.add(getPopulatePanelMethod(Action.CREATE));
		builderMethods.add(getPopulatePanelMethod(Action.EDIT));
		builderMethods.add(getPopulatePanelMethod(Action.VIEW));

		builderMethods.add(getEntityAccessorMethod());
		builderMethods.add(getMutatorMethod(entityName, entity));

		addOtherFieldsAndMethods();

		builderMethods.add(getOnEditMethod());
		builderMethods.add(getAccessorMethod(CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE));
		builderMethods.add(getMutatorMethod(CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE));
		builderMethods.add(getDisplayListMethod());
		builderMethods.add(getDisplayCreateDialogMethod());
		builderMethods.add(getPersistMethod(mergeMethod, persistMethod, identifierAccessor));
		builderMethods.add(getDeleteMethod(removeMethod));
		builderMethods.add(getResetMethod());
		builderMethods.add(getHandleDialogCloseMethod());

		// Add builderFields first to builder followed by builderMethods
		for (FieldMetadata field : builderFields) {
			builder.addField(field);
		}
		for (MethodMetadata method : builderMethods) {
			builder.addMethod(method);
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private void addOtherFieldsAndMethods() {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		for (final FieldMetadata field : locatedFields) {
			final CustomData customData = field.getCustomData();

			if (customData.keySet().contains(APPLICATION_TYPE_KEY)) {
				builderMethods.add(getAutoCompleteApplicationTypeMethod(field));
			} else if (customData.keySet().contains(ENUMERATED_KEY)) {
				builderMethods.add(getAutoCompleteEnumMethod(field));
			} else if (field.getCustomData().keySet().contains(PARAMETER_TYPE_KEY)) {
				final String fieldName = field.getFieldName().getSymbolName();
				final JavaType parameterType = (JavaType) field.getCustomData().get(PARAMETER_TYPE_KEY);
				final JavaSymbolName selectedFieldName = new JavaSymbolName(getSelectedFieldName(fieldName));
				final JavaType listType = getListType(parameterType);

				builderFields.add(getField(selectedFieldName, listType));
				builderMethods.add(getAccessorMethod(selectedFieldName, listType));

				imports.addImport(HASH_SET);

				final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
				bodyBuilder.appendFormalLine("if (" + selectedFieldName.getSymbolName() + " != null) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(entityName.getSymbolName() + ".set" + StringUtils.capitalize(fieldName) + "(new HashSet<" + parameterType.getSimpleTypeName() + ">(" + selectedFieldName + "));");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
				bodyBuilder.appendFormalLine("this." + selectedFieldName.getSymbolName() + " = " + selectedFieldName.getSymbolName() + ";");
				builderMethods.add(getMutatorMethod(selectedFieldName, listType, bodyBuilder));
			} else if (field.getAnnotation(ROO_UPLOADED_FILE) != null) {
				imports.addImport(PRIMEFACES_STREAMED_CONTENT);
				imports.addImport(PRIMEFACES_DEFAULT_STREAMED_CONTENT);
				imports.addImport(BYTE_ARRAY_INPUT_STREAM);

				final String fieldName = field.getFieldName().getSymbolName();
				final JavaSymbolName streamedContentFieldName = new JavaSymbolName(fieldName + "StreamedContent");

				builderMethods.add(getFileUploadListenerMethod(field));

				final AnnotationMetadata annotation = field.getAnnotation(ROO_UPLOADED_FILE);
				final String contentType = (String) annotation.getAttribute("contentType").getValue();
				final String fileExtension = StringUtils.toLowerCase(UploadedFileContentType.getFileExtension(contentType).name());

				final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
				bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + " != null && " + entityName.getSymbolName() + ".get" + StringUtils.capitalize(fieldName) + "() != null) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine("return new DefaultStreamedContent(new ByteArrayInputStream(" + entityName.getSymbolName() + ".get" + StringUtils.capitalize(fieldName) + "()), \"" + contentType + "\", \"" + fieldName + "." + fileExtension + "\");");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
				bodyBuilder.appendFormalLine("return new DefaultStreamedContent(new ByteArrayInputStream(\"\".getBytes()));");
				builderMethods.add(getAccessorMethod(streamedContentFieldName, PRIMEFACES_STREAMED_CONTENT, bodyBuilder));
			}
		}
	}

	private AnnotationMetadata getManagedBeanAnnotation(final String beanName) {
		AnnotationMetadata annotation = getTypeAnnotation(MANAGED_BEAN);
		if (annotation == null) {
			return null;
		}
		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(annotation);
		annotationBuilder.addStringAttribute("name", beanName);
		return annotationBuilder.build();
	}

	private AnnotationMetadata getScopeAnnotation() {
		if (hasScopeAnnotation()) {
			return null;
		}
		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(SESSION_SCOPED);
		return annotationBuilder.build();
	}

	private boolean hasScopeAnnotation() {
		return (governorTypeDetails.getAnnotation(SESSION_SCOPED) != null || governorTypeDetails.getAnnotation(VIEW_SCOPED) != null || governorTypeDetails.getAnnotation(REQUEST_SCOPED) != null || governorTypeDetails.getAnnotation(APPLICATION_SCOPED) != null);
	}

	private JavaType getListType(final JavaType parameterType) {
		return new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(parameterType));
	}

	private FieldMetadata getPanelGridField(final Action panelType) {
		return getField(new JavaSymbolName(StringUtils.toLowerCase(panelType.name()) + "PanelGrid"), HTML_PANEL_GRID);
	}

	// Methods

	private MethodMetadata getInitMethod(MethodMetadata identifierAccessor) {
		final JavaSymbolName methodName = new JavaSymbolName("init");
		if (governorHasMethod(methodName)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(ARRAY_LIST);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("columns = new ArrayList<String>();");
		for (final FieldMetadata field : locatedFields) {
			if (field.getCustomData().keySet().contains(LIST_VIEW_FIELD_KEY)) {
				bodyBuilder.appendFormalLine("columns.add(\"" + field.getFieldName().getSymbolName() + "\");");
			}
		}

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(POST_CONSTRUCT));
		return methodBuilder.build();
	}

	private MethodMetadata getEntityAccessorMethod() {
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + entityName.getSymbolName() + ";");
		return getAccessorMethod(entityName, entity, bodyBuilder);
	}

	private MethodMetadata getOnEditMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("onEdit");
		if (governorHasMethod(methodName)) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		for (final FieldMetadata field : locatedFields) {
			final CustomData customData = field.getCustomData();
			if (!customData.keySet().contains(PARAMETER_TYPE_KEY)) {
				continue;
			}

			final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			imports.addImport(ARRAY_LIST);

			final String fieldName = field.getFieldName().getSymbolName();
			final JavaType parameterType = (JavaType) customData.get(PARAMETER_TYPE_KEY);
			final String parameterTypeManagedBeanName = (String) customData.get(PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY);
			final String parameterTypePlural = (String) customData.get(PARAMETER_TYPE_PLURAL_KEY);

			bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + " != null && " + entityName.getSymbolName() + ".get" + (StringUtils.hasText(parameterTypeManagedBeanName) ? parameterTypePlural : StringUtils.capitalize(fieldName)) + "() != null) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(getSelectedFieldName(fieldName) + " = new ArrayList<" + parameterType.getSimpleTypeName() + ">(" + entityName.getSymbolName() + ".get" + StringUtils.capitalize(fieldName) + "());");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}
		bodyBuilder.appendFormalLine("return null;");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder).build();
	}

	private MethodMetadata getFindAllEntitiesMethod(final JavaSymbolName allEntitiesFieldName, final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("findAll" + plural);
		if (governorHasMethod(methodName)) {
			return null;
		}

		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(allEntitiesFieldName.getSymbolName() + " = " + findAllMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine(DATA_VISIBLE + " = !" + allEntitiesFieldName.getSymbolName() + ".isEmpty();");
		bodyBuilder.appendFormalLine("return null;");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder).build();
	}

	private MethodMetadata getPanelGridAccessorMethod(final Action action) {
		final String fieldName = StringUtils.toLowerCase(action.name()) + "PanelGrid";
		final JavaSymbolName methodName = BeanInfoUtils.getAccessorMethodName(new JavaSymbolName(fieldName), HTML_PANEL_GRID);
		if (governorHasMethod(methodName)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
		bodyBuilder.indent();
		switch (action) {
			case CREATE:
				bodyBuilder.appendFormalLine(fieldName + " = populateCreatePanel();");
				break;
			case EDIT:
				bodyBuilder.appendFormalLine(fieldName + " = populateEditPanel();");
				break;
			default:
				bodyBuilder.appendFormalLine(fieldName + " = populateViewPanel();");
				break;
		}
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + fieldName + ";");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder).build();
	}

	private MethodMetadata getPanelGridMutatorMethod(final Action action) {
		return getMutatorMethod(new JavaSymbolName(StringUtils.toLowerCase(action.name()) + "PanelGrid"), HTML_PANEL_GRID);
	}

	private MethodMetadata getPopulatePanelMethod(final Action action) {
		JavaSymbolName methodName;
		String suffix1;
		String suffix2;
		switch (action) {
			case CREATE:
				suffix1 = "CreateOutput";
				suffix2 = "CreateInput";
				methodName = new JavaSymbolName("populateCreatePanel");
				break;
			case EDIT:
				suffix1 = "EditOutput";
				suffix2 = "EditInput";
				methodName = new JavaSymbolName("populateEditPanel");
				break;
			default:
				suffix1 = "Label";
				suffix2 = "Value";
				methodName = new JavaSymbolName("populateViewPanel");
				break;
		}

		if (governorHasMethod(methodName)) {
			return null;
		}

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId());
		methodBuilder.setModifier(PUBLIC);
		methodBuilder.setMethodName(methodName);
		methodBuilder.setReturnType(HTML_PANEL_GRID);
		methodBuilder.setParameterTypes(new ArrayList<AnnotatedJavaType>());
		methodBuilder.setParameterNames(new ArrayList<JavaSymbolName>());

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_CONTEXT);
		imports.addImport(APPLICATION);
		imports.addImport(HTML_PANEL_GRID);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("Application application = facesContext.getApplication();");

		if (locatedFields.isEmpty()) {
			bodyBuilder.appendFormalLine("return " + getComponentCreation("HtmlPanelGrid"));
			methodBuilder.setBodyBuilder(bodyBuilder);
			return methodBuilder.build();
		}

		imports.addImport(EL_CONTEXT);
		imports.addImport(EXPRESSION_FACTORY);
		imports.addImport(HTML_OUTPUT_TEXT);

		bodyBuilder.appendFormalLine("ExpressionFactory expressionFactory = application.getExpressionFactory();");
		bodyBuilder.appendFormalLine("ELContext elContext = facesContext.getELContext();");
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine("HtmlPanelGrid " + HTML_PANEL_GRID_ID + " = " + getComponentCreation("HtmlPanelGrid"));
		bodyBuilder.appendFormalLine("");

		for (final FieldMetadata field : locatedFields) {
			final CustomData customData = field.getCustomData();
			final JavaType fieldType = field.getFieldType();
			final String simpleTypeName = fieldType.getSimpleTypeName();
			final String fieldName = field.getFieldName().getSymbolName();
			final String fieldLabelId = fieldName + suffix1;

			final BigDecimal minValue = NumberUtils.max(getMinOrMax(field, MIN), getMinOrMax(field, DECIMAL_MIN));
			final BigDecimal maxValue = NumberUtils.min(getMinOrMax(field, MAX), getMinOrMax(field, DECIMAL_MAX));
			final Integer sizeMinValue = getSizeMinOrMax(field, "min");
			final BigDecimal sizeMaxValue = NumberUtils.min(getSizeMinOrMax(field, "max"), getColumnLength(field));
			final boolean required = action != Action.VIEW && (!isNullable(field) || minValue != null || maxValue != null || sizeMinValue != null || sizeMaxValue != null);
			final boolean isTextarea = (sizeMinValue != null && sizeMinValue.intValue() > 30) || (sizeMaxValue != null && sizeMaxValue.intValue() > 30) || customData.keySet().contains(CustomDataKeys.LOB_FIELD);

			// Field label
			bodyBuilder.appendFormalLine("HtmlOutputText " + fieldLabelId + " = " + getComponentCreation("HtmlOutputText"));
			bodyBuilder.appendFormalLine(fieldLabelId + ".setId(\"" + fieldLabelId + "\");");
			bodyBuilder.appendFormalLine(fieldLabelId + ".setValue(\"" + field.getFieldName().getReadableSymbolName() + ": " + (required ? "* " : "  ") + "\");");
			bodyBuilder.appendFormalLine(getAddToPanelText(fieldLabelId));
			bodyBuilder.appendFormalLine("");

			// Field value
			final String fieldValueId = fieldName + suffix2;
			final String converterName = fieldValueId + "Converter";
			final String htmlOutputTextStr = "HtmlOutputText " + fieldValueId + " = " + getComponentCreation("HtmlOutputText");
			final String inputTextStr = "InputText " + fieldValueId + " = " + getComponentCreation("InputText");
			final String componentIdStr = fieldValueId + ".setId(\"" + fieldValueId + "\");";
			final String requiredStr = fieldValueId + ".setRequired(" + required + ");";

			if (field.getAnnotation(ROO_UPLOADED_FILE) != null) {
				AnnotationMetadata annotation = field.getAnnotation(ROO_UPLOADED_FILE);
				final String contentType = (String) annotation.getAttribute("contentType").getValue();
				final String allowedType = UploadedFileContentType.getFileExtension(contentType).name();
				if (action == Action.VIEW) {
					imports.addImport(UI_COMPONENT);
					imports.addImport(PRIMEFACES_FILE_DOWNLOAD_ACTION_LISTENER);
					imports.addImport(PRIMEFACES_COMMAND_BUTTON);
					imports.addImport(PRIMEFACES_STREAMED_CONTENT);

					// bodyBuilder.appendFormalLine("CommandButton " + fieldValueId + " = " + getComponentCreation("CommandButton"));
					// bodyBuilder.appendFormalLine(fieldValueId + ".addActionListener(new FileDownloadActionListener(expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." +
					// fieldName + "StreamedContent}\", StreamedContent.class), null));");
					// bodyBuilder.appendFormalLine(fieldValueId + ".setValue(\"Download\");");
					// bodyBuilder.appendFormalLine(fieldValueId + ".setAjax(false);");

					// TODO Make following code work as currently the view panel is not refreshed and the download field is always seen as null
					bodyBuilder.appendFormalLine("UIComponent " + fieldValueId + ";");
					bodyBuilder.appendFormalLine("if (" + entityName + " != null && " + entityName + ".get" + StringUtils.capitalize(fieldName) + "() != null && " + entityName + ".get" + StringUtils.capitalize(fieldName) + "().length > 0) {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(fieldValueId + " = " + getComponentCreation("CommandButton"));
					bodyBuilder.appendFormalLine("((CommandButton) " + fieldValueId + ").addActionListener(new FileDownloadActionListener(expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + fieldName + "StreamedContent}\", StreamedContent.class), null));");
					bodyBuilder.appendFormalLine("((CommandButton) " + fieldValueId + ").setValue(\"Download\");");
					bodyBuilder.appendFormalLine("((CommandButton) " + fieldValueId + ").setAjax(false);");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("} else {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(fieldValueId + " = " + getComponentCreation("HtmlOutputText"));
					bodyBuilder.appendFormalLine("((HtmlOutputText) " + fieldValueId + ").setValue(\"\");");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
				} else {
					imports.addImport(PRIMEFACES_FILE_UPLOAD);
					imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);

					bodyBuilder.appendFormalLine("FileUpload " + fieldValueId + " = " + getComponentCreation("FileUpload"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(fieldValueId + ".setFileUploadListener(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + "." + getFileUploadMethodName(fieldName) + "}\", void.class, new Class[] { FileUploadEvent.class }));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setMode(\"advanced\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setAllowTypes(\"/(\\\\.|\\\\/)(" + getAllowTypeRegex(allowedType) + ")$/\");");

					final AnnotationAttributeValue<?> autoUploadAttr = annotation.getAttribute("autoUpload");
					if (autoUploadAttr != null && (Boolean) autoUploadAttr.getValue()) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setAuto(true);");
					}
				}
			} else if (fieldType.equals(BOOLEAN_OBJECT) || fieldType.equals(BOOLEAN_PRIMITIVE)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName));
				} else {
					imports.addImport(PRIMEFACES_SELECT_BOOLEAN_CHECKBOX);
					bodyBuilder.appendFormalLine("SelectBooleanCheckbox " + fieldValueId + " = " + getComponentCreation("SelectBooleanCheckbox"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (customData.keySet().contains(ENUMERATED_KEY)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName));
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);

					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreation("AutoComplete"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(getSetCompleteMethod(fieldValueId, fieldName));
					bodyBuilder.appendFormalLine(fieldValueId + ".setDropdown(true);");
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (JdkJavaType.isDateField(fieldType)) {
				if (action == Action.VIEW) {
					imports.addImport(DATE_TIME_CONVERTER);

					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine("DateTimeConverter " + converterName + " = (DateTimeConverter) application.createConverter(DateTimeConverter.CONVERTER_ID);");
					// TODO Get working: bodyBuilder.appendFormalLine(converterName + ".setPattern(((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern());");
					bodyBuilder.appendFormalLine(converterName + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(" + converterName + ");");
				} else {
					imports.addImport(PRIMEFACES_CALENDAR);
					imports.addImport(DATE);
					// imports.addImport(DATE_FORMAT);
					// imports.addImport(SIMPLE_DATE_FORMAT);

					bodyBuilder.appendFormalLine("Calendar " + fieldValueId + " = " + getComponentCreation("Calendar"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, "Date"));
					bodyBuilder.appendFormalLine(fieldValueId + ".setNavigator(true);");
					bodyBuilder.appendFormalLine(fieldValueId + ".setEffect(\"slideDown\");");
					// TODO Get working: bodyBuilder.appendFormalLine(fieldValueId + ".setPattern(((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern());");
					bodyBuilder.appendFormalLine(fieldValueId + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(requiredStr);
					if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PAST) != null) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setMaxdate(new Date());");
					}
					if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), FUTURE) != null) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setMindate(new Date());");
					}
				}
			} else if (JdkJavaType.isIntegerType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					imports.addImport(PRIMEFACES_SPINNER);
					if (fieldType.equals(JdkJavaType.BIG_INTEGER)) {
						imports.addImport(fieldType);
					}

					bodyBuilder.appendFormalLine("Spinner " + fieldValueId + " = " + getComponentCreation("Spinner"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);
					if (minValue != null || maxValue != null) {
						if (minValue != null) {
							bodyBuilder.appendFormalLine(fieldValueId + ".setMin(" + minValue.doubleValue() + ");");
						}
						if (maxValue != null) {
							bodyBuilder.appendFormalLine(fieldValueId + ".setMax(" + maxValue.doubleValue() + ");");
						}
						bodyBuilder.append(getLongRangeValdatorString(fieldValueId, minValue, maxValue));
					}
					bodyBuilder.appendFormalLine("");
				}
			} else if (JdkJavaType.isDecimalType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					if (fieldType.equals(JdkJavaType.BIG_DECIMAL)) {
						imports.addImport(fieldType);
					}

					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);
					if (minValue != null || maxValue != null) {
						bodyBuilder.append(getDoubleRangeValdatorString(fieldValueId, minValue, maxValue));
					}
				}
			} else if (fieldType.equals(STRING)) {
				if (isTextarea) {
					imports.addImport(PRIMEFACES_INPUT_TEXTAREA);
					bodyBuilder.appendFormalLine("InputTextarea " + fieldValueId + " = " + getComponentCreation("InputTextarea"));
					bodyBuilder.appendFormalLine(fieldValueId + ".setMaxHeight(100);");
				} else {
					if (action == Action.VIEW) {
						bodyBuilder.appendFormalLine(htmlOutputTextStr);
					} else {
						imports.addImport(PRIMEFACES_INPUT_TEXT);
						bodyBuilder.appendFormalLine(inputTextStr);
					}
				}

				bodyBuilder.appendFormalLine(componentIdStr);
				bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName));
				if (action == Action.VIEW) {
					if (isTextarea) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setReadonly(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setDisabled(true);");
					}
				} else {
					if (sizeMinValue != null || sizeMaxValue != null) {
						bodyBuilder.append(getLengthValdatorString(fieldValueId, sizeMinValue, sizeMaxValue));
						bodyBuilder.appendFormalLine(requiredStr);
					}
					setRegexPatternValidationString(field, fieldValueId, bodyBuilder);
				}
			} else if (customData.keySet().contains(PARAMETER_TYPE_KEY)) {
				final JavaType parameterType = (JavaType) customData.get(PARAMETER_TYPE_KEY);
				final String parameterTypeSimpleTypeName = parameterType.getSimpleTypeName();
				final String parameterTypeFieldName = StringUtils.uncapitalize(parameterTypeSimpleTypeName);
				final String parameterTypeManagedBeanName = (String) customData.get(PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY);
				final String parameterTypePlural = (String) customData.get(PARAMETER_TYPE_PLURAL_KEY);

				if (StringUtils.hasText(parameterTypeManagedBeanName)) {
					if (customData.keySet().contains(ONE_TO_MANY_FIELD) || customData.keySet().contains(MANY_TO_MANY_FIELD) && isInverseSideOfRelationship(field, ONE_TO_MANY, MANY_TO_MANY)) {
						bodyBuilder.appendFormalLine(htmlOutputTextStr);
						bodyBuilder.appendFormalLine(componentIdStr);
						bodyBuilder.appendFormalLine(fieldValueId + ".setValue(\"This relationship is managed from the " + parameterTypeSimpleTypeName + " side\");");
					} else {
						final JavaType converterType = new JavaType(destination.getPackage().getFullyQualifiedPackageName() + ".converter." + parameterTypeSimpleTypeName + "Converter");
						imports.addImport(PRIMEFACES_SELECT_MANY_MENU);
						imports.addImport(UI_SELECT_ITEMS);
						imports.addImport(fieldType);
						imports.addImport(converterType);

						bodyBuilder.appendFormalLine("SelectManyMenu " + fieldValueId + " = " + getComponentCreation("SelectManyMenu"));
						bodyBuilder.appendFormalLine(componentIdStr);
						bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + getSelectedFieldName(fieldName) + "}\", List.class));");
						bodyBuilder.appendFormalLine("UISelectItems " + fieldValueId + "Items = (UISelectItems) application.createComponent(UISelectItems.COMPONENT_TYPE);");
						if (action == Action.VIEW) {
							bodyBuilder.appendFormalLine(fieldValueId + ".setReadonly(true);");
							bodyBuilder.appendFormalLine(fieldValueId + ".setDisabled(true);");
							bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + entityName.getSymbolName() + "." + fieldName + "}\", " + simpleTypeName + ".class));");
						} else {
							bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + parameterTypeManagedBeanName + ".all" + StringUtils.capitalize(parameterTypePlural) + "}\", List.class));");
							bodyBuilder.appendFormalLine(requiredStr);
						}
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \"" + parameterTypeFieldName + "\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{" + parameterTypeFieldName + "}\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + parameterTypeFieldName + "}\", " + parameterTypeSimpleTypeName + ".class));");
						bodyBuilder.appendFormalLine(getAddChildToComponent(fieldValueId, fieldValueId + "Items"));
					}
				} else {
					// Parameter type is an enum
					bodyBuilder.appendFormalLine("SelectManyMenu " + fieldValueId + " = " + getComponentCreation("SelectManyMenu"));
					bodyBuilder.appendFormalLine(componentIdStr);
					if (action == Action.VIEW) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setReadonly(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setDisabled(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + getSelectedFieldName(fieldName) + "}\", List.class));");
						bodyBuilder.appendFormalLine("UISelectItems " + fieldValueId + "Items = (UISelectItems) application.createComponent(UISelectItems.COMPONENT_TYPE);");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + entityName.getSymbolName() + "." + fieldName + "}\", " + simpleTypeName + ".class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \"" + parameterTypeFieldName + "\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{" + parameterTypeFieldName + "}\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + parameterTypeFieldName + "}\", " + parameterTypeSimpleTypeName + ".class));");
						bodyBuilder.appendFormalLine(getAddChildToComponent(fieldValueId, fieldValueId + "Items"));
					} else {
						imports.addImport(UI_SELECT_ITEM);
						imports.addImport(ENUM_CONVERTER);

						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + getSelectedFieldName(fieldName) + "}\", List.class));");
						bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new EnumConverter(" + parameterTypeSimpleTypeName + ".class));");
						bodyBuilder.appendFormalLine(requiredStr);
						bodyBuilder.appendFormalLine("UISelectItem " + fieldValueId + "Item;");
						bodyBuilder.appendFormalLine("for (" + parameterTypeSimpleTypeName + " " + StringUtils.uncapitalize(parameterTypeSimpleTypeName) + " : " + parameterTypeSimpleTypeName + ".values()) {");
						bodyBuilder.indent();
						bodyBuilder.appendFormalLine(fieldValueId + "Item = (UISelectItem) application.createComponent(UISelectItem.COMPONENT_TYPE);");
						bodyBuilder.appendFormalLine(fieldValueId + "Item.setItemLabel(" + StringUtils.uncapitalize(parameterTypeSimpleTypeName) + ".name());");
						bodyBuilder.appendFormalLine(fieldValueId + "Item.setItemValue(" + StringUtils.uncapitalize(parameterTypeSimpleTypeName) + ");");
						bodyBuilder.appendFormalLine(getAddChildToComponent(fieldValueId, fieldValueId + "Item"));
						bodyBuilder.indentRemove();
						bodyBuilder.appendFormalLine("}");
					}
				}
			} else if (customData.keySet().contains(APPLICATION_TYPE_KEY)) {
				if (customData.keySet().contains(ONE_TO_ONE_FIELD) && isInverseSideOfRelationship(field, ONE_TO_ONE)) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(fieldValueId + ".setValue(\"This relationship is managed from the " + simpleTypeName + " side\");");
				} else {
					JavaType converterType = new JavaType(destination.getPackage().getFullyQualifiedPackageName() + ".converter." + simpleTypeName + "Converter");
					imports.addImport(converterType);
					if (action == Action.VIEW) {
						bodyBuilder.appendFormalLine(htmlOutputTextStr);
						bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
						bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
					} else {
						imports.addImport(PRIMEFACES_AUTO_COMPLETE);
						imports.addImport(fieldType);

						bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreation("AutoComplete"));
						bodyBuilder.appendFormalLine(componentIdStr);
						bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
						bodyBuilder.appendFormalLine(getSetCompleteMethod(fieldValueId, fieldName));
						bodyBuilder.appendFormalLine(fieldValueId + ".setDropdown(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \"" + fieldName + "\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"" + getAutoCcompleteItemLabelValue(field, fieldName) + "\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + fieldName + "}\", " + simpleTypeName + ".class));");
						bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
						bodyBuilder.appendFormalLine(requiredStr);
					}
				}
			} else {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);

					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			}

			if (action != Action.VIEW) {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
				// Add message for input field
				imports.addImport(PRIMEFACES_MESSAGE);

				bodyBuilder.appendFormalLine("");
				bodyBuilder.appendFormalLine("Message " + fieldValueId + "Message = " + getComponentCreation("Message"));
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setId(\"" + fieldValueId + "Message\");");
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setFor(\"" + fieldValueId + "\");");
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setDisplay(\"icon\");");
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId + "Message"));
			} else {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
			}

			bodyBuilder.appendFormalLine("");
		}
		bodyBuilder.appendFormalLine("return " + HTML_PANEL_GRID_ID + ";");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder).build();
	}

	private String getAutoCcompleteItemLabelValue(FieldMetadata field, final String fieldName) {
		StringBuilder sb = new StringBuilder();
		@SuppressWarnings("unchecked")
		final List<FieldMetadata> applicationTypeFields = (List<FieldMetadata>) field.getCustomData().get(APPLICATION_TYPE_FIELDS_KEY);
		for (FieldMetadata applicationTypeField : applicationTypeFields) {
			sb.append("#{").append(fieldName).append(".").append(applicationTypeField.getFieldName().getSymbolName()).append("} ");
		}
		return sb.length() > 0 ? sb.toString().trim() : fieldName;
	}

	private void setRegexPatternValidationString(final FieldMetadata field, final String fieldValueId, final InvocableMemberBodyBuilder bodyBuilder) {
		final AnnotationMetadata patternAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PATTERN);
		if (patternAnnotation != null) {
			final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			imports.addImport(REGEX_VALIDATOR);

			AnnotationAttributeValue<?> regexpAttr = patternAnnotation.getAttribute(new JavaSymbolName("regexp"));
			bodyBuilder.appendFormalLine("RegexValidator " + fieldValueId + "RegexValidator = new RegexValidator();");
			bodyBuilder.appendFormalLine(fieldValueId + "RegexValidator.setPattern(\"" + regexpAttr.getValue() + "\");");
			bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "RegexValidator);");
		}
	}

	private String getAllowTypeRegex(final String allowedType) {
		StringBuilder builder = new StringBuilder();
		char[] value = allowedType.toCharArray();
		for (int i = 0; i < value.length; i++) {
			builder.append("[").append(Character.toLowerCase(value[i])).append(Character.toUpperCase(value[i])).append("]");
		}
		if (allowedType.equals(UploadedFileContentType.JPG.name())) {
			builder.append("|[jJ][pP][eE][gG]");
		}
		return builder.toString();
	}

	private String getSelectedFieldName(final String fieldName) {
		return "selected" + StringUtils.capitalize(fieldName);
	}

	private String getAddToPanelText(final String componentId) {
		return getAddChildToComponent(HTML_PANEL_GRID_ID, componentId);
	}

	private String getAddChildToComponent(final String componentId, final String childComponentId) {
		return componentId + ".getChildren().add(" + childComponentId + ");";
	}

	private boolean isNullable(final FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) == null;
	}

	private BigDecimal getMinOrMax(final FieldMetadata field, final JavaType annotationType) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), annotationType);
		if (annotation != null && annotation.getAttribute(new JavaSymbolName("value")) != null) {
			return new BigDecimal(String.valueOf(annotation.getAttribute(new JavaSymbolName("value")).getValue()));
		}
		return null;
	}

	private Integer getSizeMinOrMax(final FieldMetadata field, final String attrName) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
		if (annotation != null && annotation.getAttribute(new JavaSymbolName(attrName)) != null) {
			return (Integer) annotation.getAttribute(new JavaSymbolName(attrName)).getValue();
		}
		return null;
	}

	private Integer getColumnLength(final FieldMetadata field) {
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
		if (values != null && values.containsKey("length")) {
			return (Integer) values.get("length");
		}
		return null;
	}

	public String getLongRangeValdatorString(final String fieldValueId, final BigDecimal minValue, final BigDecimal maxValue) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LONG_RANGE_VALIDATOR);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("LongRangeValidator " + fieldValueId + "Validator = new LongRangeValidator();");
		if (minValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum(" + minValue.longValue() + ");");
		}
		if (maxValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum(" + maxValue.longValue() + ");");
		}
		bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "Validator);");
		return bodyBuilder.getOutput();
	}

	public String getDoubleRangeValdatorString(final String fieldValueId, final BigDecimal minValue, final BigDecimal maxValue) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(DOUBLE_RANGE_VALIDATOR);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("DoubleRangeValidator " + fieldValueId + "Validator = new DoubleRangeValidator();");
		if (minValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum(" + minValue.doubleValue() + ");");
		}
		if (maxValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum(" + maxValue.doubleValue() + ");");
		}
		bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "Validator);");
		return bodyBuilder.getOutput();
	}

	public String getLengthValdatorString(final String fieldValueId, final Number minValue, final Number maxValue) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LENGTH_VALIDATOR);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("LengthValidator " + fieldValueId + "Validator = new LengthValidator();");
		if (minValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum(" + minValue.intValue() + ");");
		}
		if (maxValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum(" + maxValue.intValue() + ");");
		}
		bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "Validator);");
		return bodyBuilder.getOutput();
	}

	private MethodMetadata getFileUploadListenerMethod(final FieldMetadata field) {
		final String fieldName = field.getFieldName().getSymbolName();
		final JavaSymbolName methodName = getFileUploadMethodName(fieldName);
		final JavaType parameterType = PRIMEFACES_FILE_UPLOAD_EVENT;
		if (governorHasMethod(methodName, parameterType)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_CONTEXT);
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName + ".set" + StringUtils.capitalize(fieldName) + "(event.getFile().getContents());");
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(\"Successful\", event.getFile().getFileName() + \" is uploaded.\");");
		bodyBuilder.appendFormalLine("facesContext.addMessage(null, facesMessage);");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder).build();
	}

	private MethodMetadata getAutoCompleteEnumMethod(final FieldMetadata autoCompleteField) {
		final JavaSymbolName methodName = new JavaSymbolName("complete" + StringUtils.capitalize(autoCompleteField.getFieldName().getSymbolName()));
		final JavaType parameterType = STRING;
		if (governorHasMethod(methodName, parameterType)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LIST);
		imports.addImport(ARRAY_LIST);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("query"));
		final JavaType returnType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(autoCompleteField.getFieldType()));

		final String simpleTypeName = autoCompleteField.getFieldType().getSimpleTypeName();
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("List<" + simpleTypeName + "> suggestions = new ArrayList<" + simpleTypeName + ">();");
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + StringUtils.uncapitalize(simpleTypeName) + " : " + simpleTypeName + ".values()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(simpleTypeName) + ".name().toLowerCase().startsWith(query.toLowerCase())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("suggestions.add(" + StringUtils.uncapitalize(simpleTypeName) + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return suggestions;");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder).build();
	}

	private MethodMetadata getAutoCompleteApplicationTypeMethod(final FieldMetadata field) {
		final JavaSymbolName methodName = new JavaSymbolName("complete" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
		final JavaType parameterType = STRING;
		if (governorHasMethod(methodName, parameterType)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LIST);
		imports.addImport(ARRAY_LIST);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("query"));

		@SuppressWarnings("unchecked")
		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = (Map<MethodMetadataCustomDataKey, MemberTypeAdditions>) field.getCustomData().get(CRUD_ADDITIONS_KEY);
		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
		final String simpleTypeName = field.getFieldType().getSimpleTypeName();

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("List<" + simpleTypeName + "> suggestions = new ArrayList<" + simpleTypeName + ">();");
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + StringUtils.uncapitalize(simpleTypeName) + " : " + findAllMethod.getMethodCall() + ") {");
		bodyBuilder.indent();

		StringBuilder sb = new StringBuilder();
		@SuppressWarnings("unchecked")
		final List<FieldMetadata> applicationTypeFields = (List<FieldMetadata>) field.getCustomData().get(APPLICATION_TYPE_FIELDS_KEY);
		for (int i = 0; i < applicationTypeFields.size(); i++) {
			JavaSymbolName accessorMethodName = BeanInfoUtils.getAccessorMethodName(applicationTypeFields.get(i));
			if (i > 0) {
				sb.append(" + ").append(" \" \" ").append(" + ");
			}
			sb.append(StringUtils.uncapitalize(simpleTypeName)).append(".").append(accessorMethodName).append("()");
		}
		bodyBuilder.appendFormalLine("String " + StringUtils.uncapitalize(simpleTypeName) + "Str = String.valueOf(" + sb.toString().trim() + ");");

		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(simpleTypeName) + "Str.toLowerCase().startsWith(query.toLowerCase())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("suggestions.add(" + StringUtils.uncapitalize(simpleTypeName) + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return suggestions;");

		final JavaType returnType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(field.getFieldType()));

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder).build();
	}

	private MethodMetadata getDisplayCreateDialogMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_CREATE_DIALOG);
		if (governorHasMethod(methodName)) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = true;");
		bodyBuilder.appendFormalLine("return \"" + entityName.getSymbolName() + "\";");
		return getMethod(PUBLIC, methodName, STRING, null, null, bodyBuilder);
	}

	private MethodMetadata getDisplayListMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_LIST);
		if (governorHasMethod(methodName)) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		bodyBuilder.appendFormalLine("findAll" + plural + "();");
		bodyBuilder.appendFormalLine("return \"" + entityName.getSymbolName() + "\";");
		return getMethod(PUBLIC, methodName, STRING, null, null, bodyBuilder);
	}

	private MethodMetadata getPersistMethod(final MemberTypeAdditions mergeMethod, final MemberTypeAdditions persistMethod, final MethodMetadata identifierAccessor) {
		final JavaSymbolName methodName = new JavaSymbolName("persist");
		if (governorHasMethod(methodName)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_REQUEST_CONTEXT);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String message = \"\";");
		bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + "." + identifierAccessor.getMethodName().getSymbolName() + "() != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(mergeMethod.getMethodCall() + ";");
		mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("message = \"Successfully updated\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("message = \"Successfully created\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("RequestContext context = RequestContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("context.execute(\"createDialog.hide()\");");
		bodyBuilder.appendFormalLine("context.execute(\"editDialog.hide()\");");
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(message);");
		bodyBuilder.appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder).build();
	}

	private MethodMetadata getDeleteMethod(final MemberTypeAdditions removeMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("delete");
		if (governorHasMethod(methodName)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(FACES_CONTEXT);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
		removeMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(\"Successfully deleted\");");
		bodyBuilder.appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder).build();
	}

	private MethodMetadata getResetMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("reset");
		if (governorHasMethod(methodName)) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = null;");
		for (final FieldMetadata field : locatedFields) {
			final CustomData customData = field.getCustomData();
			if (!customData.keySet().contains(PARAMETER_TYPE_KEY)) {
				continue;
			}

			final String parameterTypeManagedBeanName = (String) customData.get(PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY);
			final String parameterTypePlural = (String) customData.get(PARAMETER_TYPE_PLURAL_KEY);
			final JavaSymbolName fieldName = new JavaSymbolName(getSelectedFieldName(StringUtils.hasText(parameterTypeManagedBeanName) ? parameterTypePlural : field.getFieldName().getSymbolName()));
			bodyBuilder.appendFormalLine(fieldName.getSymbolName() + " = null;");
		}
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		return getMethod(PUBLIC, methodName, VOID_PRIMITIVE, null, null, bodyBuilder);
	}

	private MethodMetadata getHandleDialogCloseMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("handleDialogClose");
		final JavaType parameterType = PRIMEFACES_CLOSE_EVENT;
		if (governorHasMethod(methodName, parameterType)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_CLOSE_EVENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("reset();");

		return new MethodMetadataBuilder(getId(), PUBLIC, methodName, VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder).build();
	}

	private JavaSymbolName getFileUploadMethodName(final String fieldName) {
		return new JavaSymbolName("handleFileUploadFor" + StringUtils.capitalize(fieldName));
	}

	private String getComponentCreation(final String componentName) {
		return new StringBuilder().append("(").append(componentName).append(") application.createComponent(").append(componentName).append(".COMPONENT_TYPE);").toString();
	}

	private String getSetValueExpression(final String inputFieldVar, final String fieldName, final String className) {
		return inputFieldVar + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + entityName.getSymbolName() + "." + fieldName + "}\", " + className + ".class));";
	}

	private String getSetValueExpression(final String fieldValueId, final String fieldName) {
		return getSetValueExpression(fieldValueId, fieldName, "String");
	}

	private String getSetCompleteMethod(final String fieldValueId, final String fieldName) {
		return fieldValueId + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + ".complete" + StringUtils.capitalize(fieldName) + "}\", List.class, new Class[] { String.class }));";
	}

	private boolean isInverseSideOfRelationship(final FieldMetadata field, final JavaType... annotationTypes) {
		for (JavaType annotationType : annotationTypes) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), annotationType);
			if (annotation != null && annotation.getAttribute(new JavaSymbolName("mappedBy")) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static LogicalPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
