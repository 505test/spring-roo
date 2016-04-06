package org.springframework.roo.addon.security.addon.security;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.security.annotations.RooSecurityConfiguration;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.*;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooSecurityConfiguration}.
 * <p>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class SecurityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = SecurityMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType AUDITOR_AWARE = new JavaType(
      "org.springframework.data.domain.AuditorAware");
  private static final JavaType BEAN = new JavaType("org.springframework.context.annotation.Bean");
  private static final JavaType ENABLE_JPA_AUDITING = new JavaType(
      "org.springframework.data.jpa.repository.config.EnableJpaAuditing");
  private static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");

  private final JavaType authenticationAuditorAware;
  private final SecurityConfigurationAnnotationValues annnotationValues;

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues 
   */
  public SecurityMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JavaType authenticationAuditorAware,
      SecurityConfigurationAnnotationValues annotationValues) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.authenticationAuditorAware = authenticationAuditorAware;
    this.annnotationValues = annotationValues;

    if (annotationValues.getEnableJpaAuditing()) {

      // Generate the auditorProvider method
      builder.addMethod(getAuditorProviderMethod());

      // Add @EnableJpaAuditing
      builder.addAnnotation(new AnnotationMetadataBuilder(ENABLE_JPA_AUDITING).build());

      // Add @Configuration
      builder.addAnnotation(new AnnotationMetadataBuilder(CONFIGURATION).build());

      // Create a representation of the desired output ITD
      itdTypeDetails = builder.build();
    }
  }

  /**
   * Obtains the "auditorProvider" method for this type, if available.
   * 
   * @return the "auditorProvider" method declared on this type or that will be
   *         introduced (or null if undeclared and not introduced)
   */
  private MethodMetadataBuilder getAuditorProviderMethod() {

    // Compute the relevant auditorProvider method name
    final JavaSymbolName methodName = new JavaSymbolName("auditorProvider");

    // See if the type itself declared the method
    if (governorHasMethod(methodName)) {
      return null;
    }

    builder.getImportRegistrationResolver().addImports(authenticationAuditorAware, AUDITOR_AWARE);

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    bodyBuilder.appendFormalLine("return new " + "AuthenticationAuditorAware();");

    MethodMetadataBuilder method = new MethodMetadataBuilder(getId());
    method.setModifier(Modifier.PUBLIC);
    method.setMethodName(methodName);
    method.setBodyBuilder(bodyBuilder);
    method.setReturnType(new JavaType(AUDITOR_AWARE.getFullyQualifiedTypeName(), 0, DataType.TYPE,
        null, Arrays.asList(JavaType.STRING)));
    method.addAnnotation(new AnnotationMetadataBuilder(BEAN).build());

    return method;
  }
}
