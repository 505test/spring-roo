package org.springframework.roo.addon.jsf;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.jsf.JsfJavaType.CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_COMPONENT;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfConverter}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfConverterMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	static final String CONVERTER_FIELD_CUSTOM_DATA_KEY = "converterField";
	private static final String PROVIDES_TYPE_STRING = JsfConverterMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType OBJECT = new JavaType("java.lang.Object");
	
	// Fields
	private JavaType entity;
	private Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param plural
	 * @param crudAdditions the additions this metadata should make in order to
	 * invoke the target entity type's CRUD methods (required)
	 * @param locatedFieldsAndAccessors
	 * @param enumTypes
	 * @param identifierAccessor the entity id's accessor (getter) method (can be <code>null</code>)
	 */
	public JsfConverterMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JsfConverterAnnotationValues annotationValues, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions, final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' is invalid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(crudAdditions, "Crud additions map required");
		Assert.notNull(locatedFieldsAndAccessors, "Located fields and accessors map required");
		
		if (!isValid()) {
			return;
		}
		
		this.entity = annotationValues.getEntity();
		this.locatedFieldsAndAccessors = locatedFieldsAndAccessors;
		
		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		final MemberTypeAdditions mergeMethod = crudAdditions.get(MERGE_METHOD);
		final MemberTypeAdditions persistMethod = crudAdditions.get(PERSIST_METHOD);
		final MemberTypeAdditions removeMethod = crudAdditions.get(REMOVE_METHOD);
		if (findAllMethod == null || mergeMethod == null || persistMethod == null || removeMethod == null) {
			valid = false;
			return;
		}
		final List<MethodMetadata> converterMethods = getConverterMethods();
		if (converterMethods.isEmpty()) {
			valid = false;
			return;
		}

		if (!isConverterInterfaceIntroduced()) {
			final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			imports.addImport(CONVERTER);
			builder.addImplementsType(CONVERTER);
		}
		
		String builderString = getBuilderString(converterMethods);
		builder.addMethod(getGetAsObjectMethod(builderString, findAllMethod));
		builder.addMethod(getGetAsStringMethod(builderString, findAllMethod));

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private boolean isConverterInterfaceIntroduced() {
		return isImplementing(governorTypeDetails, CONVERTER);
	}
	
	private MethodMetadata getGetAsObjectMethod(final String builderString, final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("getAsObject");
		List<JavaType> parameterTypes = Arrays.asList(FACES_CONTEXT, UI_COMPONENT, JavaType.STRING);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(entity);
		imports.addImport(FACES_CONTEXT);
		imports.addImport(UI_COMPONENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("context"), new JavaSymbolName("component"), new JavaSymbolName("value"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		// Create getAsObject method
		final List<JavaType> getAsObjectParameterTypes = new ArrayList<JavaType>(parameterTypes);
		getAsObjectParameterTypes.add(JavaType.STRING);
		bodyBuilder.indent();
		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("for (" + entity.getSimpleTypeName() + " " + StringUtils.uncapitalize(entity.getSimpleTypeName()) + " : " + findAllMethod.getMethodCall() + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(new StringBuilder("String ").append(StringUtils.uncapitalize(entity.getSimpleTypeName())).append("Str = ").append(builderString).toString());
		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(entity.getSimpleTypeName()) +"Str.equals(value)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return " + StringUtils.uncapitalize(entity.getSimpleTypeName()) + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return null;");
		bodyBuilder.indentRemove();
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, OBJECT, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getGetAsStringMethod(final String builderString, final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("getAsString");
		List<JavaType> parameterTypes = Arrays.asList(FACES_CONTEXT, UI_COMPONENT, OBJECT);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(entity);
		imports.addImport(FACES_CONTEXT);
		imports.addImport(UI_COMPONENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("context"), new JavaSymbolName("component"), new JavaSymbolName("value"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		final List<JavaType> getAsStringParameterTypes = new ArrayList<JavaType>(parameterTypes);
		getAsStringParameterTypes.add(OBJECT);
		bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " " + StringUtils.uncapitalize(entity.getSimpleTypeName()) + " = (" + entity.getSimpleTypeName() + ") value;" );
		bodyBuilder.appendFormalLine(new StringBuilder("return ").append(builderString).toString());
		bodyBuilder.indentRemove();
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private List<MethodMetadata> getConverterMethods() {
		final List<MethodMetadata> converterMethods = new LinkedList<MethodMetadata>();
		for (final FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			if (field.getCustomData() != null && field.getCustomData().keySet().contains(CONVERTER_FIELD_CUSTOM_DATA_KEY)) {
				converterMethods.add(locatedFieldsAndAccessors.get(field));
			}
		}
		return converterMethods;
	}
	
	private String getBuilderString(final List<MethodMetadata> converterMethods) {
		final StringBuilder sb = new StringBuilder("new StringBuilder()");
		for (int i = 0; i < converterMethods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(").append(StringUtils.uncapitalize(entity.getSimpleTypeName())).append(".").append(converterMethods.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		return sb.toString();
	}
	
	
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

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
