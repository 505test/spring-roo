package org.springframework.roo.addon.layers.repository.jpa;

import java.util.Arrays;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * Metadata for {@link RooJpaRepository}.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryJpaMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = RepositoryJpaMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final String SPRING_JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String SPRING_JPA_SPECIFICATION_EXECUTOR = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
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
     * @param identifier the identifier for this item of metadata (required)
     * @param aspectName the Java type of the ITD (required)
     * @param governorPhysicalTypeMetadata the governor, which is expected to
     *            contain a {@link ClassOrInterfaceTypeDetails} (required)
     * @param annotationValues (required)
     * @param identifierType the type of the entity's identifier field
     *            (required)
     */
    public RepositoryJpaMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final RepositoryJpaAnnotationValues annotationValues,
            final JavaType identifierType) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Assert.notNull(annotationValues, "Annotation values required");
        Assert.notNull(identifierType, "Id type required");

        // Make the user's Repository interface extend Spring Data's
        // JpaRepository interface if it doesn't already
        ensureGovernorExtends(new JavaType(SPRING_JPA_REPOSITORY, 0,
                DataType.TYPE, null, Arrays.asList(
                        annotationValues.getDomainType(), identifierType)));

        // ... and likewise extend JpaSpecificationExecutor<Foo>, to allow query
        // by specification
        ensureGovernorExtends(new JavaType(SPRING_JPA_SPECIFICATION_EXECUTOR,
                0, DataType.TYPE, null, Arrays.asList(annotationValues
                        .getDomainType())));

        builder.addAnnotation(new AnnotationMetadataBuilder(
                SpringJavaType.REPOSITORY));

        // Build the ITD
        itdTypeDetails = builder.build();
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
}
