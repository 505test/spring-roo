package org.springframework.roo.addon.web.mvc.jsp;

import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata built from {@link WebScaffoldMetadata}. A single {@link JspMetadata} represents all JSPs for an associated controller.
 * The metadata identifier for a {@link JspMetadata} is the fully qualifier name of the controller, and the source {@link Path}
 * of the controller. This can be created using {@link #createIdentifier(JavaType, Path)}.
 *
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class JspMetadata extends AbstractMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = JspMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private final WebScaffoldMetadata webScaffoldMetadata;
	private final WebScaffoldAnnotationValues annotationValues;

	public JspMetadata(final String identifier, final WebScaffoldMetadata webScaffoldMetadata) {
		super(identifier);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(webScaffoldMetadata, "Web scaffold metadata required");

		this.webScaffoldMetadata = webScaffoldMetadata;
		this.annotationValues = webScaffoldMetadata.getAnnotationValues();
	}

	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("web scaffold metadata id", webScaffoldMetadata.getId());
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final ContextualPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static ContextualPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
