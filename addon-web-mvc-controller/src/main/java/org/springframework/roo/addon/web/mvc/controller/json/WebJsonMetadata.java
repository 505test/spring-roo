package org.springframework.roo.addon.web.mvc.controller.json;

import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;
import static org.springframework.roo.model.SpringJavaType.HTTP_HEADERS;
import static org.springframework.roo.model.SpringJavaType.HTTP_STATUS;
import static org.springframework.roo.model.SpringJavaType.PATH_VARIABLE;
import static org.springframework.roo.model.SpringJavaType.REQUEST_BODY;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_BODY;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_ENTITY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for Json functionality provided through {@link RooWebScaffold}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public class WebJsonMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final JavaType RESPONSE_ENTITY_STRING = new JavaType(RESPONSE_ENTITY.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING));
	private static final String PROVIDES_TYPE_STRING = WebJsonMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private JavaType jsonEnabledType;
	private JsonMetadata jsonMetadata;
	private String entityName;
	private WebJsonAnnotationValues annotationValues;
	private boolean servicesInjected;

	public WebJsonMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final WebJsonAnnotationValues annotationValues, final Map<String, MemberTypeAdditions> persistenceAdditions, final FieldMetadata identifierField, final String plural, final Set<FinderMetadataDetails> finderDetails, JsonMetadata jsonMetadata, boolean servicesInjected) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(persistenceAdditions, "Persistence additions required");
		Assert.notNull(finderDetails, "Array of dynamic finder methods cannot be null");
		Assert.notNull(jsonMetadata, "Json metadata required");
		if (!isValid()) {
			return;
		}
		this.annotationValues = annotationValues;
		this.jsonEnabledType = annotationValues.getJsonObject();
		this.entityName = JavaSymbolName.getReservedWordSafeName(jsonEnabledType).getSymbolName();
		this.servicesInjected = servicesInjected;

		this.jsonMetadata = jsonMetadata;
		
		MemberTypeAdditions findMethod = persistenceAdditions.get(PersistenceCustomDataKeys.FIND_METHOD.name());
		builder.addMethod(getJsonShowMethod(identifierField, findMethod));
		
		MemberTypeAdditions findAllMethod = persistenceAdditions.get(PersistenceCustomDataKeys.FIND_ALL_METHOD.name());
		builder.addMethod(getJsonListMethod(findAllMethod));

		MemberTypeAdditions persistMethod = persistenceAdditions.get(PersistenceCustomDataKeys.PERSIST_METHOD.name());
		builder.addMethod(getJsonCreateMethod(persistMethod));
		builder.addMethod(getCreateFromJsonArrayMethod(persistMethod));
		
		MemberTypeAdditions mergeMethod = persistenceAdditions.get(PersistenceCustomDataKeys.MERGE_METHOD.name());
		builder.addMethod(getJsonUpdateMethod(mergeMethod));
		builder.addMethod(getUpdateFromJsonArrayMethod(mergeMethod));

		MemberTypeAdditions removeMethod = persistenceAdditions.get(PersistenceCustomDataKeys.REMOVE_METHOD.name());
			builder.addMethod(getJsonDeleteMethod(removeMethod, identifierField, findMethod));

		if (annotationValues.isExposeFinders() && !finderDetails.isEmpty()) {
			for (FinderMetadataDetails finder : finderDetails) {
				builder.addMethod(getFinderJsonMethod(finder, plural));
			}
		}
		
		itdTypeDetails = builder.build();
	}

	private MethodMetadata getJsonShowMethod(final FieldMetadata identifierField, final MemberTypeAdditions findMethod) {
		if (!StringUtils.hasText(annotationValues.getShowJsonMethod()) || identifierField == null || findMethod == null){ 
			return null;
		}
		
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getShowJsonMethod());
		if (methodExists(methodName)) {
			return null;
		}
		JavaSymbolName toJsonMethodName = jsonMetadata.getToJsonMethodName();
		
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), identifierField.getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(PATH_VARIABLE, attributes);
		
		final List<AnnotatedJavaType> paramTypes = Arrays.asList(new AnnotatedJavaType(identifierField.getFieldType(), pathVariableAnnotation.build()));

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(identifierField.getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + identifierField.getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));

		String beanShortName = getShortName(jsonEnabledType);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + findMethod.getMethodCall() + ";");
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		String responseEntityShortName = getShortName(RESPONSE_ENTITY);
		String httpStatusShortName = getShortName(HTTP_STATUS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text; charset=utf-8\");");
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(headers, " + httpStatusShortName + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + beanShortName.toLowerCase() + "." + toJsonMethodName.getSymbolName() + "(), headers, " +  httpStatusShortName + ".OK);");

		if (!servicesInjected) {
			findMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getJsonCreateMethod(final MemberTypeAdditions persistMethod) {
		if (!StringUtils.hasText(annotationValues.getCreateFromJsonMethod()) || persistMethod == null){ 
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getCreateFromJsonMethod());
		if (methodExists(methodName)) {
			return null;
		}
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(REQUEST_BODY);
		final List<AnnotatedJavaType> paramTypes = Arrays.asList(new AnnotatedJavaType(JavaType.STRING, requestBodyAnnotation.build()));

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("POST"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		String formBackingTypeName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(formBackingTypeName + " " + JavaSymbolName.getReservedWordSafeName(jsonEnabledType) + " = " + formBackingTypeName + "." + fromJsonMethodName.getSymbolName() + "(json);");
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		if (!servicesInjected) {
			persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateFromJsonArrayMethod(MemberTypeAdditions persistMethod) {
		if (!StringUtils.hasText(annotationValues.getCreateFromJsonArrayMethod()) || persistMethod == null){ 
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getCreateFromJsonArrayMethod());
		if (methodExists(methodName)) {
			return null;
		}
		JavaSymbolName fromJsonArrayMethodName = jsonMetadata.getFromJsonArrayMethodName();
		
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(REQUEST_BODY);
		final List<AnnotatedJavaType> paramTypes = Arrays.asList(new AnnotatedJavaType(JavaType.STRING, requestBodyAnnotation.build()));
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/jsonArray"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("POST"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String beanName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		List<JavaType> params = new ArrayList<JavaType>();
		params.add(jsonEnabledType);
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		if (!servicesInjected) {
			persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getJsonListMethod(MemberTypeAdditions findAllMethod) {
		if (!StringUtils.hasText(annotationValues.getListJsonMethod()) || findAllMethod == null){ 
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getListJsonMethod());
		if (methodExists(methodName)) {
			return null;
		}
		JavaSymbolName toJsonArrayMethodName = jsonMetadata.getToJsonArrayMethodName();

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String entityName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		String responseEntityShortName = getShortName(RESPONSE_ENTITY);
		String httpStatusShortName = getShortName(HTTP_STATUS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text; charset=utf-8\");");
		JavaType list = new JavaType(List.class.getName(), 0, DataType.TYPE, null, Arrays.asList(jsonEnabledType));
		bodyBuilder.appendFormalLine(list.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " result = " + findAllMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + entityName + "." + toJsonArrayMethodName.getSymbolName() + "(result), headers, " +  httpStatusShortName + ".OK);");

		if (!servicesInjected) {
			findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonUpdateMethod(MemberTypeAdditions mergeMethod) {
		if (!StringUtils.hasText(annotationValues.getUpdateFromJsonMethod()) || mergeMethod == null){ 
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getUpdateFromJsonMethod());
		if (methodExists(methodName)) {
			return null;
		}
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(REQUEST_BODY);
		
		final List<AnnotatedJavaType> paramTypes = Arrays.asList(new AnnotatedJavaType(JavaType.STRING, requestBodyAnnotation.build()));
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("PUT"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String beanShortName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String beanSymbolName = JavaSymbolName.getReservedWordSafeName(jsonEnabledType).getSymbolName();
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine(beanShortName + " " + beanSymbolName + " = " + beanShortName + "." + fromJsonMethodName.getSymbolName() + "(json);");
		bodyBuilder.appendFormalLine("if (" + mergeMethod.getMethodCall() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
		if (!servicesInjected) {
			mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateFromJsonArrayMethod(MemberTypeAdditions mergeMethod) {
		if (!StringUtils.hasText(annotationValues.getUpdateFromJsonArrayMethod()) || mergeMethod == null){ 
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getUpdateFromJsonArrayMethod());
		if (methodExists(methodName)) {
			return null;
		}
		JavaSymbolName fromJsonArrayMethodName = jsonMetadata.getFromJsonArrayMethodName();

		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(REQUEST_BODY);
		
		final List<AnnotatedJavaType> paramTypes = Arrays.asList(new AnnotatedJavaType(JavaType.STRING, requestBodyAnnotation.build()));
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/jsonArray"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("PUT"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String beanName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(jsonEnabledType);
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + mergeMethod.getMethodCall() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
		if (!servicesInjected) {
			mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonDeleteMethod(MemberTypeAdditions removeMethod, FieldMetadata identifierField, MemberTypeAdditions findMethod) {
		if (!StringUtils.hasText(annotationValues.getDeleteFromJsonMethod()) || removeMethod == null || identifierField == null || findMethod == null){ 
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getDeleteFromJsonMethod());
		if (methodExists(methodName)) {
			return null;
		}

		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), identifierField.getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(PATH_VARIABLE, attributes);

		final List<AnnotatedJavaType> paramTypes = Arrays.asList(new AnnotatedJavaType(identifierField.getFieldType(), pathVariableAnnotation.build()));

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(identifierField.getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + identifierField.getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("DELETE"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		String beanShortName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String beanShortNameField = StringUtils.uncapitalize(beanShortName);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortNameField + " = " + findMethod.getMethodCall() + ";");
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("if (" + beanShortNameField + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + getShortName(RESPONSE_ENTITY) + "<String>(headers, " + getShortName(HTTP_STATUS) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + HTTP_STATUS.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");

		if (!servicesInjected) {
			removeMethod.copyAdditionsTo(builder, governorTypeDetails);
			findMethod.copyAdditionsTo(builder, governorTypeDetails);
		}
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, RESPONSE_ENTITY_STRING, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getFinderJsonMethod(FinderMetadataDetails finderDetails, String plural) {
		JavaSymbolName finderMethodName = new JavaSymbolName("json" + StringUtils.capitalize(finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName()));
		if (finderDetails == null || jsonMetadata.getToJsonArrayMethodName() == null || methodExists(finderMethodName)) {
			return null;
		}
		

		List<AnnotatedJavaType> annotatedParamTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		StringBuilder methodParams = new StringBuilder();
		
		for (FieldMetadata field: finderDetails.getFinderMethodParamFields()) {
			JavaSymbolName fieldName = field.getFieldName();
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), StringUtils.uncapitalize(fieldName.getSymbolName())));
			if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) || field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				attributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
			}
			AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM, attributes);
			annotations.add(requestParamAnnotation.build());
			if (field.getFieldType().equals(new JavaType(Date.class.getName())) || field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
				AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DATE_TIME_FORMAT);
				if (annotation != null) {
					annotations.add(annotation);
				}
			}
			paramNames.add(fieldName);
			annotatedParamTypes.add(new AnnotatedJavaType(field.getFieldType(), annotations));

			if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				methodParams.append(field.getFieldName() + " == null ? new Boolean(false) : " + field.getFieldName() + ", ");
			} else {
				methodParams.append(field.getFieldName() + ", ");
			}
		}

		if (methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}
		
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(paramNames);

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + plural, "")));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));
		String shortBeanName = jsonEnabledType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String httpHeadersShortName = getShortName(HTTP_HEADERS);
		String responseEntityShortName = getShortName(RESPONSE_ENTITY);
		String httpStatusShortName = getShortName(HTTP_STATUS);
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text; charset=utf-8\");");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + shortBeanName + "." + jsonMetadata.getToJsonArrayMethodName().getSymbolName().toString() + "(" + shortBeanName + "." + finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList()), headers, " +  httpStatusShortName + ".OK);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, RESPONSE_ENTITY_STRING, annotatedParamTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private String getShortName(JavaType type) {
		return type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
	}
	
	private boolean methodExists(JavaSymbolName methodName) {
		for (MethodMetadata method : MemberFindingUtils.getMethods(governorTypeDetails)) {
			if (method.getMethodName().equals(methodName)) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
