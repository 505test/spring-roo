package org.springframework.roo.addon.displayname;

import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAYS;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.DATE_FORMAT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDisplayName}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class DisplayNameMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = DisplayNameMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final int MAX_LIST_VIEW_FIELDS = 4;
	
	// Fields
	private final DisplayNameAnnotationValues annotationValues;
	private final List<MethodMetadata> locatedAccessors;
	private final MethodMetadata identifierAccessor;
	private String methodName;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues 
	 * @param locatedAccessors
	 * @param identifierAccessor 
	 */
	public DisplayNameMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, DisplayNameAnnotationValues annotationValues, List<MethodMetadata> locatedAccessors, MethodMetadata identifierAccessor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(locatedAccessors, "Located accessors required");

		this.annotationValues = annotationValues;
		this.locatedAccessors = locatedAccessors;
		this.identifierAccessor = identifierAccessor;

		// Generate the display name method
		final MethodMetadata displayNameMethod = getDisplayNameMethod();
		if (displayNameMethod != null) {
			builder.addMethod(displayNameMethod);
			methodName = displayNameMethod.getMethodName().getSymbolName();
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	public String getMethodName() {
		return methodName;
	}

	/**
	 * Obtains the display name method for this type, if available.
	 * <p>
	 * If the user provided a non-default name for "getDisplayName", that method will be returned.
	 * 
	 * @return the display name method declared on this type or that will be introduced (or null if undeclared and not introduced)
	 */
	private MethodMetadata getDisplayNameMethod() {
		String displayNameMethod = annotationValues.getMethodName();
		if (!StringUtils.hasText(displayNameMethod)) {
			return null;
		}

		// Compute the relevant toString method name
		JavaSymbolName methodName = new JavaSymbolName(displayNameMethod);

		// See if the type itself declared the method
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		final Set<String> fieldsSet = new HashSet<String>();
		String[] fields = annotationValues.getFields();
		if (fields != null && fields.length > 0) {
			Collections.addAll(fieldsSet, fields);
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		int methodCount = 0;
		final List<String> displayMethods = new ArrayList<String>();
		for (MethodMetadata accessor : locatedAccessors) {
			String accessorName = accessor.getMethodName().getSymbolName();
			String accessorText;
			if (accessor.getReturnType().isCommonCollectionType()) {
				accessorText = accessorName + "() == null ? \"\" : " + accessorName + "().size()";
			} else if (accessor.getReturnType().isArray()) {
				imports.addImport(ARRAYS);
				accessorText = "Arrays.toString(" + accessorName + "())";
			} else if (CALENDAR.equals(accessor.getReturnType())) {
				imports.addImport(DATE_FORMAT);
				accessorText = accessorName + "() == null ? \"\" : DateFormat.getDateInstance(DateFormat.LONG).format(" + accessorName + "().getTime())";
			} else if (DATE.equals(accessor.getReturnType())) {
				imports.addImport(DATE_FORMAT);
				accessorText = accessorName + "() == null ? \"\" : DateFormat.getDateInstance(DateFormat.LONG).format(" + accessorName + "())";
			} else {
				accessorText = accessorName + "()";
			}

			if (!fieldsSet.isEmpty()) {
				String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
				if (fieldsSet.contains(StringUtils.uncapitalize(fieldName))) {
					displayMethods.add(accessorText);
				}
				continue;
			}
			
			if (methodCount <= MAX_LIST_VIEW_FIELDS) {
				methodCount++;
				if (identifierAccessor != null && accessor.hasSameName(identifierAccessor)) {
					displayMethods.add(0, accessorText);
				} else {
					displayMethods.add(accessorText);
				}
			}
		}

		if (displayMethods.isEmpty()) {
			return null;
		}
		
		String separator = StringUtils.defaultIfEmpty(annotationValues.getSeparator(), " ");
		final StringBuilder builder = new StringBuilder("return new StringBuilder()");
		for (int i = 0; i < displayMethods.size(); i++) {
			if (i > 0) {
				builder.append(".append(\"").append(separator).append("\")");
			}
			builder.append(".append(").append(displayMethods.get(i)).append(")");
		}
		builder.append(".toString();");

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(builder.toString());

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, STRING, bodyBuilder);
		return methodBuilder.build();
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

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
