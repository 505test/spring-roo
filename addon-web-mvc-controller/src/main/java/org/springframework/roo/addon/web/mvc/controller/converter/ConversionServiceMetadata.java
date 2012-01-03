package org.springframework.roo.addon.web.mvc.controller.converter;

import static org.springframework.roo.model.SpringJavaType.CONFIGURABLE;
import static org.springframework.roo.model.SpringJavaType.FORMATTER_REGISTRY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.roo.addon.json.CustomDataJsonTags;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents metadata for the application-wide conversion service. Generates
 * the following ITD methods:
 * <ul>
 * <li>afterPropertiesSet() - overrides InitializingBean lifecycle parent method
 * </li>
 * <li>installLabelConverters(FormatterRegistry registry) - registers all
 * converter methods</li>
 * <li>a converter method for all scaffolded domain types as well their
 * associations</li>
 * </ul>
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
public class ConversionServiceMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    // Constants
    private static final JavaType BASE_64 = new JavaType(
            "org.apache.commons.codec.binary.Base64");
    private static final String CONVERTER = "Converter";
    private static final JavaSymbolName INSTALL_LABEL_CONVERTERS = new JavaSymbolName(
            "installLabelConverters");

    // Fields
    private Map<JavaType, MemberTypeAdditions> findMethods;
    private Map<JavaType, JavaType> idTypes;
    private Set<JavaType> relevantDomainTypes;
    private Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes;
    private Map<JavaType, List<MethodMetadata>> toStringMethods;

    /**
     * Production constructor
     * 
     * @param identifier
     * @param aspectName
     * @param governorPhysicalTypeMetadata
     * @param findMethods
     * @param idTypes the ID types of the domain types for which to generate
     *            converters (required); must be one for each domain type
     * @param relevantDomainTypes the types for which to generate converters
     *            (required)
     * @param compositePrimaryKeyTypes (required)
     */
    public ConversionServiceMetadata(
            final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final Map<JavaType, MemberTypeAdditions> findMethods,
            final Map<JavaType, JavaType> idTypes,
            final Set<JavaType> relevantDomainTypes,
            final Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes,
            final Map<JavaType, List<MethodMetadata>> toStringMethods) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Assert.notNull(findMethods, "Find methods required");
        Assert.notNull(compositePrimaryKeyTypes, "List of PK types required");
        Assert.notNull(idTypes, "List of ID types required");
        Assert.notNull(relevantDomainTypes,
                "List of relevant domain types required");
        Assert.isTrue(relevantDomainTypes.size() == idTypes.size(),
                "Expected " + relevantDomainTypes.size()
                        + " ID types, but was " + idTypes.size());
        Assert.notNull(toStringMethods, "ToString methods required");

        if (!isValid()
                || (relevantDomainTypes.isEmpty() && compositePrimaryKeyTypes
                        .isEmpty())) {
            valid = false;
            return;
        }

        this.findMethods = findMethods;
        this.compositePrimaryKeyTypes = compositePrimaryKeyTypes;
        this.idTypes = idTypes;
        this.relevantDomainTypes = relevantDomainTypes;
        this.toStringMethods = toStringMethods;

        builder.addAnnotation(getTypeAnnotation(CONFIGURABLE));
        builder.addMethod(getInstallLabelConvertersMethod());
        builder.addMethod(getAfterPropertiesSetMethod());

        itdTypeDetails = builder.build();
    }

    private MethodMetadataBuilder getInstallLabelConvertersMethod() {
        List<JavaType> sortedRelevantDomainTypes = new ArrayList<JavaType>(
                relevantDomainTypes);
        Collections.sort(sortedRelevantDomainTypes);

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        final Set<String> methodNames = new HashSet<String>();
        for (final JavaType formBackingObject : sortedRelevantDomainTypes) {
            String simpleName = formBackingObject.getSimpleTypeName();
            while (methodNames.contains(simpleName)) {
                simpleName += "_";
            }
            methodNames.add(simpleName);

            JavaSymbolName toIdMethodName = new JavaSymbolName("get"
                    + simpleName + "ToStringConverter");
            builder.addMethod(getToStringConverterMethod(formBackingObject,
                    toIdMethodName, toStringMethods.get(formBackingObject)));
            bodyBuilder.appendFormalLine("registry.addConverter("
                    + toIdMethodName.getSymbolName() + "());");

            JavaSymbolName toTypeMethodName = new JavaSymbolName("getIdTo"
                    + simpleName + CONVERTER);
            final MethodMetadataBuilder toTypeConverterMethod = getToTypeConverterMethod(
                    formBackingObject, toTypeMethodName,
                    findMethods.get(formBackingObject),
                    idTypes.get(formBackingObject));
            if (toTypeConverterMethod != null) {
                builder.addMethod(toTypeConverterMethod);
                bodyBuilder.appendFormalLine("registry.addConverter("
                        + toTypeMethodName.getSymbolName() + "());");
            }

            // Only allow conversion if ID type is not String already.
            if (!idTypes.get(formBackingObject).equals(JavaType.STRING)) {
                JavaSymbolName stringToTypeMethodName = new JavaSymbolName(
                        "getStringTo" + simpleName + CONVERTER);
                builder.addMethod(getStringToTypeConverterMethod(
                        formBackingObject, stringToTypeMethodName,
                        idTypes.get(formBackingObject)));
                bodyBuilder.appendFormalLine("registry.addConverter("
                        + stringToTypeMethodName.getSymbolName() + "());");
            }
        }

        for (final Entry<JavaType, Map<Object, JavaSymbolName>> entry : compositePrimaryKeyTypes
                .entrySet()) {
            final JavaType targetType = entry.getKey();
            final Map<Object, JavaSymbolName> jsonMethodNames = entry
                    .getValue();

            final MethodMetadataBuilder jsonToConverterMethod = getJsonToConverterMethod(
                    targetType,
                    jsonMethodNames.get(CustomDataJsonTags.FROM_JSON_METHOD));
            builder.addMethod(jsonToConverterMethod);
            bodyBuilder.appendFormalLine("registry.addConverter("
                    + jsonToConverterMethod.getMethodName().getSymbolName()
                    + "());");

            final MethodMetadataBuilder toJsonConverterMethod = getToJsonConverterMethod(
                    targetType,
                    jsonMethodNames.get(CustomDataJsonTags.TO_JSON_METHOD));
            builder.addMethod(toJsonConverterMethod);
            bodyBuilder.appendFormalLine("registry.addConverter("
                    + toJsonConverterMethod.getMethodName().getSymbolName()
                    + "());");
        }

        final JavaType parameterType = FORMATTER_REGISTRY;
        if (governorHasMethod(INSTALL_LABEL_CONVERTERS, parameterType)) {
            return null;
        }

        List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(
                "registry"));
        builder.getImportRegistrationResolver().addImport(parameterType);

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                INSTALL_LABEL_CONVERTERS, JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private MethodMetadataBuilder getJsonToConverterMethod(
            final JavaType targetType, final JavaSymbolName jsonMethodName) {
        final JavaSymbolName methodName = new JavaSymbolName("getJsonTo"
                + targetType.getSimpleTypeName() + CONVERTER);
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType converterJavaType = SpringJavaType.getConverterType(
                JavaType.STRING, targetType);

        String base64Name = BASE_64.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
        String typeName = targetType.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return new "
                + converterJavaType.getNameIncludingTypeParameters() + "() {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("public " + targetType.getSimpleTypeName()
                + " convert(String encodedJson) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return " + typeName + "."
                + jsonMethodName.getSymbolName() + "(new String(" + base64Name
                + ".decodeBase64(encodedJson)));");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("};");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                converterJavaType, bodyBuilder);
    }

    private MethodMetadataBuilder getToJsonConverterMethod(
            final JavaType targetType, final JavaSymbolName jsonMethodName) {
        JavaSymbolName methodName = new JavaSymbolName("get"
                + targetType.getSimpleTypeName() + "ToJsonConverter");
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType converterJavaType = SpringJavaType.getConverterType(
                targetType, JavaType.STRING);

        String base64Name = BASE_64.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
        String targetTypeName = StringUtils.uncapitalize(targetType
                .getSimpleTypeName());

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return new "
                + converterJavaType.getNameIncludingTypeParameters() + "() {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("public String convert("
                        + targetType.getSimpleTypeName() + " " + targetTypeName
                        + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return " + base64Name
                + ".encodeBase64URLSafeString(" + targetTypeName + "."
                + jsonMethodName.getSymbolName() + "().getBytes());");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("};");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                converterJavaType, bodyBuilder);
    }

    private MethodMetadataBuilder getToStringConverterMethod(
            final JavaType targetType, final JavaSymbolName methodName,
            List<MethodMetadata> toStringMethods) {
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType converterJavaType = SpringJavaType.getConverterType(
                targetType, JavaType.STRING);
        final String targetTypeName = StringUtils.uncapitalize(targetType
                .getSimpleTypeName());

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return new "
                + converterJavaType.getNameIncludingTypeParameters() + "() {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("public String convert("
                        + targetType.getSimpleTypeName() + " " + targetTypeName
                        + ") {");
        bodyBuilder.indent();
        StringBuilder sb = new StringBuilder("return new StringBuilder()");
        for (int i = 0; i < toStringMethods.size(); i++) {
            if (i > 0) {
                sb.append(".append(\" \")");
            }
            sb.append(".append(")
                    .append(targetTypeName)
                    .append(".")
                    .append(toStringMethods.get(i).getMethodName()
                            .getSymbolName()).append("())");
        }
        sb.append(".toString();");
        bodyBuilder.appendFormalLine(sb.toString());
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("};");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                converterJavaType, bodyBuilder);
    }

    /**
     * Returns the "string to type" converter method to be generated, if any
     * 
     * @param targetType the type being converted into (required)
     * @param methodName the name of the method to generate if necessary
     *            (required)
     * @param idType the ID type of the given target type (required)
     * @return <code>null</code> if none is to be generated
     */
    private MethodMetadataBuilder getStringToTypeConverterMethod(
            final JavaType targetType, final JavaSymbolName methodName,
            final JavaType idType) {
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType converterJavaType = SpringJavaType.getConverterType(
                JavaType.STRING, targetType);
        final String idTypeName = idType.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return new "
                + converterJavaType.getNameIncludingTypeParameters() + "() {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("public "
                + targetType.getFullyQualifiedTypeName()
                + " convert(String id) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("return getObject().convert(getObject().convert(id, "
                        + idTypeName
                        + ".class), "
                        + targetType.getSimpleTypeName() + ".class);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("};");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                converterJavaType, bodyBuilder);
    }

    private MethodMetadataBuilder getToTypeConverterMethod(
            final JavaType targetType, final JavaSymbolName methodName,
            final MemberTypeAdditions findMethod, final JavaType idType) {
        MethodMetadata toTypeConverterMethod = getGovernorMethod(methodName);
        if (findMethod == null) {
            return null;
        }
        if (toTypeConverterMethod != null) {
            return new MethodMetadataBuilder(toTypeConverterMethod);
        }

        findMethod.copyAdditionsTo(builder, governorTypeDetails);
        final JavaType converterJavaType = SpringJavaType.getConverterType(
                idType, targetType);

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return new "
                + converterJavaType.getNameIncludingTypeParameters() + "() {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("public "
                + targetType.getFullyQualifiedTypeName() + " convert(" + idType
                + " id) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall()
                + ";");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("};");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                converterJavaType, bodyBuilder);
    }

    private MethodMetadataBuilder getAfterPropertiesSetMethod() {
        JavaSymbolName methodName = new JavaSymbolName("afterPropertiesSet");
        if (governorHasMethod(methodName)) {
            return null;
        }

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("super.afterPropertiesSet();");
        bodyBuilder.appendFormalLine(INSTALL_LABEL_CONVERTERS.getSymbolName()
                + "(getObject());");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                JavaType.VOID_PRIMITIVE, bodyBuilder);
    }
}