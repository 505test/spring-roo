package org.springframework.roo.addon.web.mvc.controller.scaffold;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAYS;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.COLLECTION;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.Jsr303JavaType.VALID;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.BINDING_RESULT;
import static org.springframework.roo.model.SpringJavaType.CONVERSION_SERVICE;
import static org.springframework.roo.model.SpringJavaType.LOCALE_CONTEXT_HOLDER;
import static org.springframework.roo.model.SpringJavaType.MODEL;
import static org.springframework.roo.model.SpringJavaType.MODEL_ATTRIBUTE;
import static org.springframework.roo.model.SpringJavaType.PATH_VARIABLE;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.URI_UTILS;
import static org.springframework.roo.model.SpringJavaType.WEB_UTILS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooWebScaffold}.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.0
 */
public class WebScaffoldMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = WebScaffoldMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaSymbolName CS_FIELD = new JavaSymbolName("conversionService");

	// Fields
	private boolean compositePk;
	private JavaType formBackingType;
	private JavaTypeMetadataDetails javaTypeMetadataHolder;
	private Map<JavaSymbolName, DateTimeFormatDetails> dateTypes;
	private Map<JavaType, JavaTypeMetadataDetails> specialDomainTypes;
	private String controllerPath;
	private String entityName;
	private WebScaffoldAnnotationValues annotationValues;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalType
	 * @param annotationValues
	 * @param specialDomainTypes
	 * @param dependentTypes
	 * @param dateTypes
	 * @param crudAdditions
	 */
	public WebScaffoldMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalType, final WebScaffoldAnnotationValues annotationValues, final SortedMap<JavaType, JavaTypeMetadataDetails> specialDomainTypes, final List<JavaTypeMetadataDetails> dependentTypes, final Map<JavaSymbolName, DateTimeFormatDetails> dateTypes, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions) {
		super(identifier, aspectName, governorPhysicalType);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' is invalid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(specialDomainTypes, "Special domain types map required");
		Assert.notNull(dependentTypes, "Dependent types list required");

		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.controllerPath = annotationValues.getPath();
		this.dateTypes = dateTypes;
		this.formBackingType = annotationValues.getFormBackingObject();	// must happen before it's used below
		this.entityName = JavaSymbolName.getReservedWordSafeName(formBackingType).getSymbolName();
		this.javaTypeMetadataHolder = specialDomainTypes.get(formBackingType);
		this.specialDomainTypes = specialDomainTypes;

		Assert.notNull(javaTypeMetadataHolder, "Metadata holder required for form backing type: " + formBackingType);

		if (javaTypeMetadataHolder.getPersistenceDetails() != null && !javaTypeMetadataHolder.getPersistenceDetails().getRooIdentifierFields().isEmpty()) {
			this.compositePk = true;
			builder.addField(getField(CS_FIELD, CONVERSION_SERVICE));
			builder.addConstructor(getConstructor());
		}

		final MemberTypeAdditions persistMethod = crudAdditions.get(PERSIST_METHOD);
		if (annotationValues.isCreate() && persistMethod != null) {
			builder.addMethod(getCreateMethod(persistMethod));
			builder.addMethod(getCreateFormMethod(dependentTypes));
			persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		}

		// "list" method
		final MemberTypeAdditions countAllMethod = crudAdditions.get(COUNT_ALL_METHOD);
		final MemberTypeAdditions findMethod = crudAdditions.get(FIND_METHOD);
		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		final MemberTypeAdditions findEntriesMethod = crudAdditions.get(FIND_ENTRIES_METHOD);

		if (findMethod != null) {
			builder.addMethod(getShowMethod(findMethod));
			findMethod.copyAdditionsTo(builder, governorTypeDetails);
		}

		if (countAllMethod != null && findAllMethod != null && findEntriesMethod != null) {
			builder.addMethod(getListMethod(findAllMethod, countAllMethod, findEntriesMethod));
			countAllMethod.copyAdditionsTo(builder, governorTypeDetails);
			findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
			findEntriesMethod.copyAdditionsTo(builder, governorTypeDetails);
		}

		// "update" method
		final MemberTypeAdditions updateMethod = crudAdditions.get(MERGE_METHOD);
		if (annotationValues.isUpdate() && updateMethod != null && findMethod != null) {
			builder.addMethod(getUpdateMethod(updateMethod));
			builder.addMethod(getUpdateFormMethod(findMethod));
			updateMethod.copyAdditionsTo(builder, governorTypeDetails);
		}

		final MemberTypeAdditions deleteMethod = crudAdditions.get(REMOVE_METHOD);
		if (annotationValues.isDelete() && deleteMethod != null && findMethod != null) {
			builder.addMethod(getDeleteMethod(deleteMethod, findMethod));
			deleteMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		if (!specialDomainTypes.isEmpty()) {
			for (final MethodMetadata method : getPopulateMethods()) {
				builder.addMethod(method);
			}
		}
		if (!dateTypes.isEmpty()) {
			builder.addMethod(getDateTimeFormatHelperMethod());
		}
		if (annotationValues.isCreate() || annotationValues.isUpdate()) {
			builder.addMethod(getEncodeUrlPathSegmentMethod());
		}

		this.itdTypeDetails = builder.build();
	}

	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	private ConstructorMetadata getConstructor() {
		final ConstructorMetadata constructor = governorTypeDetails.getDeclaredConstructor(Arrays.asList(CONVERSION_SERVICE));
		if (constructor != null) {
			return constructor;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + CS_FIELD + " = " + CS_FIELD + ";");

		final ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
		final AnnotationMetadataBuilder autowired = new AnnotationMetadataBuilder(AUTOWIRED);
		constructorBuilder.addAnnotation(autowired.build());
		constructorBuilder.addParameterType(AnnotatedJavaType.convertFromJavaType(CONVERSION_SERVICE));
		constructorBuilder.addParameterName(CS_FIELD);
		constructorBuilder.setModifier(Modifier.PUBLIC);
		constructorBuilder.setBodyBuilder(bodyBuilder);
		return constructorBuilder.build();
	}

	private MethodMetadata getDeleteMethod(final MemberTypeAdditions deleteMethodAdditions, final MemberTypeAdditions findMethod) {
		final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null) {
			return null;
		}
		final JavaSymbolName methodName = new JavaSymbolName("delete");
		if (methodExists(methodName)) {
			return null;
		}

		final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "id"));
		final AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(PATH_VARIABLE, attributes);

		final List<AnnotationAttributeValue<?>> firstResultAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		firstResultAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "page"));
		firstResultAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		final AnnotationMetadataBuilder firstResultAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM, firstResultAttributes);

		final List<AnnotationAttributeValue<?>> maxResultsAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		maxResultsAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "size"));
		maxResultsAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		final AnnotationMetadataBuilder maxResultAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM, maxResultsAttributes);

		final List<AnnotatedJavaType> parameterTypes = Arrays.asList(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierType(), pathVariableAnnotation.build()), new AnnotatedJavaType(new JavaType(Integer.class.getName()), firstResultAnnotation.build()), new AnnotatedJavaType(new JavaType(Integer.class.getName()), maxResultAnnotation.build()), new AnnotatedJavaType(MODEL));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("id"), new JavaSymbolName("page"), new JavaSymbolName("size"), new JavaSymbolName("uiModel"));

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{id}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("DELETE"))));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);

		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		final String formBackingTypeName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(formBackingTypeName + " " + entityName + " = " + findMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine(deleteMethodAdditions.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("uiModel.asMap().clear();");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"page\", (page == null) ? \"1\" : page.toString());");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"size\", (size == null) ? \"10\" : size.toString());");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "\";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	/**
	 * Returns the metadata for the "list" method that this ITD introduces into
	 * the controller.
	 *
	 * @param findAllAdditions
	 * @param countAllAdditions
	 * @param findEntriesAdditions
	 * @return <code>null</code> if no such method is to be introduced
	 */
	private MethodMetadata getListMethod(final MemberTypeAdditions findAllAdditions, final MemberTypeAdditions countAllAdditions, final MemberTypeAdditions findEntriesAdditions) {
		final JavaSymbolName methodName = new JavaSymbolName("list");
		if (methodExists(methodName)) {
			return null;
		}

		final List<AnnotationAttributeValue<?>> firstResultAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		firstResultAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "page"));
		firstResultAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		final AnnotationMetadataBuilder firstResultAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM, firstResultAttributes);

		final List<AnnotationAttributeValue<?>> maxResultsAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		maxResultsAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "size"));
		maxResultsAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		final AnnotationMetadataBuilder maxResultAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM, maxResultsAttributes);

		final List<AnnotatedJavaType> parameterTypes = Arrays.asList(new AnnotatedJavaType(INT_OBJECT, firstResultAnnotation.build()), new AnnotatedJavaType(INT_OBJECT, maxResultAnnotation.build()), new AnnotatedJavaType(MODEL));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("page"), new JavaSymbolName("size"), new JavaSymbolName("uiModel"));

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("GET"))));
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes));

		final String plural = javaTypeMetadataHolder.getPlural().toLowerCase();

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (page != null || size != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("int sizeNo = size == null ? 10 : size.intValue();");
		bodyBuilder.appendFormalLine("final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + plural + "\", " + findEntriesAdditions.getMethodCall() + ");");
		bodyBuilder.appendFormalLine("float nrOfPages = (float) " + countAllAdditions.getMethodCall() + " / sizeNo;");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"maxPages\", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + plural + "\", " + findAllAdditions.getMethodCall() + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getShowMethod(final MemberTypeAdditions findMethod) {
		final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null) {
			return null;
		}

		final JavaSymbolName methodName = new JavaSymbolName("show");
		if (methodExists(methodName)) {
			return null;
		}

		final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "id"));
		final AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(PATH_VARIABLE, attributes);

		final List<AnnotatedJavaType> parameterTypes = Arrays.asList(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierType(), pathVariableAnnotation.build()), new AnnotatedJavaType(MODEL));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("id"), new JavaSymbolName("uiModel"));

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{id}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("GET"))));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName.toLowerCase() + "\", " + findMethod.getMethodCall() + ");");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"itemId\", " + (compositePk ? "conversionService.convert(" : "") + "id" + (compositePk ? ", String.class)" : "") + ");");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/show\";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateMethod(final MemberTypeAdditions persistMethod) {
		final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getIdentifierAccessorMethod() == null) {
			return null;
		}

		final JavaSymbolName methodName = new JavaSymbolName("create");
		if (methodExists(methodName)) {
			return null;
		}

		final AnnotationMetadataBuilder validAnnotation = new AnnotationMetadataBuilder(VALID);

		final List<AnnotatedJavaType> parameterTypes = Arrays.asList(new AnnotatedJavaType(formBackingType, validAnnotation.build()), new AnnotatedJavaType(BINDING_RESULT), new AnnotatedJavaType(MODEL), new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest")));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(entityName), new JavaSymbolName("bindingResult"), new JavaSymbolName("uiModel"), new JavaSymbolName("httpServletRequest"));

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("POST"))));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (bindingResult.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("uiModel.asMap().clear();");
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + encodeUrlPathSegment(" + (compositePk ? "conversionService.convert(" : "") + entityName + "." + javaTypePersistenceMetadataHolder.getIdentifierAccessorMethod().getMethodName() + "()" + (compositePk ? ", String.class)" : ".toString()") + ", httpServletRequest);");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateFormMethod(final List<JavaTypeMetadataDetails> dependentTypes) {
		final JavaSymbolName methodName = new JavaSymbolName("createForm");
		if (methodExists(methodName)) {
			return null;
		}

		final List<JavaType> parameterTypes = Arrays.asList(MODEL);
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("uiModel"));

		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("GET"))));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		annotations.add(requestMapping);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", new " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "());");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		boolean listAdded = false;
		for (final JavaTypeMetadataDetails dependentType: dependentTypes) {
			if (dependentType.getPersistenceDetails().getCountMethod() == null) {
				continue;
			}
			if (!listAdded) {
				final JavaType stringArrayType = new JavaType(STRING.getFullyQualifiedTypeName(), 1, DataType.TYPE, null, null);

				final JavaType listType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(stringArrayType));
				final String listShort = listType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

				final JavaType arrayListType = new JavaType(ARRAY_LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(stringArrayType));
				final String arrayListShort = arrayListType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

				bodyBuilder.appendFormalLine(listShort + " dependencies = new " + arrayListShort + "();");
				listAdded = true;
			}
			bodyBuilder.appendFormalLine("if (" + dependentType.getPersistenceDetails().getCountMethod().getMethodCall() + " == 0) {");
			bodyBuilder.indent();
			// Adding string array which has the fieldName at position 0 and the path at position 1
			bodyBuilder.appendFormalLine("dependencies.add(new String[] { \"" + dependentType.getJavaType().getSimpleTypeName().toLowerCase() + "\", \"" + dependentType.getPlural().toLowerCase() + "\" });");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}
		if (listAdded) {
			bodyBuilder.appendFormalLine("uiModel.addAttribute(\"dependencies\", dependencies);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateMethod(final MemberTypeAdditions updateMethod) {
		final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getMergeMethod() == null) {
			return null;
		}
		final JavaSymbolName methodName = new JavaSymbolName("update");
		if (methodExists(methodName)) {
			return null;
		}

		final AnnotationMetadataBuilder validAnnotation = new AnnotationMetadataBuilder(VALID);

		final List<AnnotatedJavaType> parameterTypes = Arrays.asList(new AnnotatedJavaType(formBackingType, validAnnotation.build()), new AnnotatedJavaType(BINDING_RESULT), new AnnotatedJavaType(MODEL), new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest")));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(entityName), new JavaSymbolName("bindingResult"), new JavaSymbolName("uiModel"), new JavaSymbolName("httpServletRequest"));

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("PUT"))));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (bindingResult.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("uiModel.asMap().clear();");
		bodyBuilder.appendFormalLine(updateMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + encodeUrlPathSegment(" + (compositePk ? "conversionService.convert(" : "") + entityName + "." +  javaTypePersistenceMetadataHolder.getIdentifierAccessorMethod().getMethodName() + "()" + (compositePk ? ", String.class)" : ".toString()") + ", httpServletRequest);");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateFormMethod(final MemberTypeAdditions findMethod) {
		final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null) {
			return null;
		}
		final JavaSymbolName methodName = new JavaSymbolName("updateForm");
		if (methodExists(methodName)) {
			return null;
		}

		final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "id"));
		final AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(PATH_VARIABLE, attributes);

		final List<AnnotatedJavaType> parameterTypes = Arrays.asList(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierType(), pathVariableAnnotation.build()), new AnnotatedJavaType(MODEL));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("id"), new JavaSymbolName("uiModel"));

		final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{id}"));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("GET"))));
		final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", " + findMethod.getMethodCall() + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private List<MethodMetadata> getPopulateMethods() {
		final List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		if (!annotationValues.isPopulateMethods()) {
			return methods;
		}
		for (final JavaType type : specialDomainTypes.keySet()) {
			final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			final JavaTypeMetadataDetails javaTypeMd = specialDomainTypes.get(type);
			final JavaTypePersistenceMetadataDetails javaTypePersistenceMd = javaTypeMd.getPersistenceDetails();
			if (javaTypePersistenceMd != null && javaTypePersistenceMd.getFindAllMethod() != null) {
				bodyBuilder.appendFormalLine("return " + javaTypePersistenceMd.getFindAllMethod().getMethodCall() + ";");
				javaTypePersistenceMd.getFindMethod().copyAdditionsTo(builder, governorTypeDetails);
			} else if (javaTypeMd.isEnumType()) {
				bodyBuilder.appendFormalLine("return " + ARRAYS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".asList(" + type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".class.getEnumConstants());");
			} else {
				continue;
			}

			final JavaSymbolName methodName = new JavaSymbolName("populate" + javaTypeMd.getPlural());
			if (methodExists(methodName)) {
				continue;
			}

			final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypeMd.getPlural().toLowerCase()));
			annotations.add(new AnnotationMetadataBuilder(MODEL_ATTRIBUTE, attributes));

			final JavaType returnType = new JavaType(COLLECTION.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(type));

			final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			methods.add(methodBuilder.build());
		}
		return methods;
	}

	private MethodMetadata getEncodeUrlPathSegmentMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("encodeUrlPathSegment");
		if (methodExists(methodName)) {
			return null;
		}

		final List<JavaType> parameterTypes = Arrays.asList(JavaType.STRING, new JavaType("javax.servlet.http.HttpServletRequest"));
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("pathSegment"), new JavaSymbolName("httpServletRequest"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String enc = httpServletRequest.getCharacterEncoding();");
		bodyBuilder.appendFormalLine("if (enc == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("enc = " + WEB_UTILS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".DEFAULT_CHARACTER_ENCODING;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("pathSegment = " + URI_UTILS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".encodePathSegment(pathSegment, enc);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (" + new JavaType("java.io.UnsupportedEncodingException").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " uee) {}");
		bodyBuilder.appendFormalLine("return pathSegment;");

		return new MethodMetadataBuilder(getId(), 0, methodName, JavaType.STRING, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder).build();
	}

	private MethodMetadata getDateTimeFormatHelperMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("addDateTimeFormatPatterns");
		if (methodExists(methodName)) {
			return null;
		}

		final List<JavaType> parameterTypes = Arrays.asList(MODEL);
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("uiModel"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		for (final Entry<JavaSymbolName, DateTimeFormatDetails> javaSymbolNameDateTimeFormatDetailsEntry : dateTypes.entrySet()) {
			String pattern;
			if (javaSymbolNameDateTimeFormatDetailsEntry.getValue().pattern != null) {
				pattern = "\"" + javaSymbolNameDateTimeFormatDetailsEntry.getValue().pattern + "\"";
			} else {
				final JavaType dateTimeFormat = new JavaType("org.joda.time.format.DateTimeFormat");
				final String dateTimeFormatSimple = dateTimeFormat.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				final String localeContextHolderSimple = LOCALE_CONTEXT_HOLDER.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				pattern = dateTimeFormatSimple + ".patternForStyle(\"" + javaSymbolNameDateTimeFormatDetailsEntry.getValue().style + "\", " + localeContextHolderSimple + ".getLocale())";
			}
			bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "_" + javaSymbolNameDateTimeFormatDetailsEntry.getKey().getSymbolName().toLowerCase() + "_date_format\", " + pattern + ");");
		}

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), 0, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private boolean methodExists(final JavaSymbolName methodName) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName) != null;
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

	public static String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
