package org.springframework.roo.addon.gwt.request;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

public class GwtRequestMetadata extends AbstractMetadataItem {

    // Constants
    private static final String PROVIDES_TYPE_STRING = GwtRequestMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    public static String getMetadataIdentifierType() {
        return PROVIDES_TYPE;
    }

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    // Fields
    private final String requestTypeContents;

    /**
     * Constructor
     * 
     * @param id the id of this
     *            {@link org.springframework.roo.metadata.MetadataItem}
     * @param requestTypeContents the Java source code for the entity-specific
     *            Request interface (required)
     */
    public GwtRequestMetadata(final String id, final String requestTypeContents) {
        super(id);
        Assert.hasText(requestTypeContents, "Invalid contents '"
                + requestTypeContents + "'");
        this.requestTypeContents = requestTypeContents;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof GwtRequestMetadata))
            return false;
        final GwtRequestMetadata other = (GwtRequestMetadata) obj;
        return StringUtils.equals(requestTypeContents,
                other.requestTypeContents);
    }

    @Override
    public int hashCode() {
        return requestTypeContents.hashCode();
    }
}
