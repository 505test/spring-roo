package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Convenient superclass for {@link MemberHoldingTypeDetails} implementations.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class AbstractMemberHoldingTypeDetails extends AbstractIdentifiableAnnotatedJavaStructureProvider implements MemberHoldingTypeDetails {

	/**
	 * Constructor
	 *
	 * @param customData
	 * @param declaredByMetadataId
	 * @param modifier
	 * @param annotations
	 */
	protected AbstractMemberHoldingTypeDetails(final CustomData customData, final String declaredByMetadataId, final int modifier, final Collection<AnnotationMetadata> annotations) {
		super(customData, declaredByMetadataId, modifier, annotations);
	}

	public ConstructorMetadata getDeclaredConstructor(final List<JavaType> parameters) {
		final Collection<JavaType> parameterList = CollectionUtils.populate(new ArrayList<JavaType>(), parameters);
		for (final ConstructorMetadata constructor : getDeclaredConstructors()) {
			if (parameterList.equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(constructor.getParameterTypes()))) {
				return constructor;
			}
		}
		return null;
	}
	
	/**
	 * Locates the specified field.
	 * 
	 * @param fieldName to locate (required)
	 * @return the field, or <code>null</code> if not found
	 */
	public FieldMetadata getDeclaredField(final JavaSymbolName fieldName) {
		Assert.notNull(fieldName, "Field name required");
		for (FieldMetadata field : getDeclaredFields()) {
			if (field.getFieldName().equals(fieldName)) {
				return field;
			}
		}
		return null;
	}

	public JavaSymbolName getUniqueFieldName(final String proposedName, final boolean prepend) {
		Assert.hasText(proposedName, "Proposed field name is required");
		String candidateName = proposedName;
		while (MemberFindingUtils.getField(this, new JavaSymbolName(candidateName)) != null) {
			// The proposed field name is taken; differentiate it
			if (prepend) {
				candidateName = "_" + candidateName;
			} else {
				// Append
				candidateName = candidateName + "_";
			}
		}
		// We've derived a unique name
		return new JavaSymbolName(candidateName);
	}
}