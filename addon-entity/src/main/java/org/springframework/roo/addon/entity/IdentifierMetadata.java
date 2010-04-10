package org.springframework.roo.addon.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIdentifier}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifierMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = IdentifierMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType EMBEDDABLE = new JavaType("javax.persistence.Embeddable");

	private boolean noArgConstructor;
	private IdentifierMetadata parent;
	
	// From annotation
	@AutoPopulate private boolean gettersByDefault = true;
	@AutoPopulate private boolean settersByDefault = true;

	public IdentifierMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, IdentifierMetadata parent, boolean noArgConstructor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		this.noArgConstructor = noArgConstructor;
		this.parent = parent;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooIdentifier.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Add @java.persistence.Embeddable annotation
		builder.addTypeAnnotation(getEmbeddableAnnotation());

		// Obtain a no-arg constructor, if one is appropriate to provide
		builder.addConstructor(getNoArgConstructor());

		// Add declared fields and accessors and mutators
		List<FieldMetadata> fields = getFields();
		for (FieldMetadata field : fields) {
			builder.addField(field);
		}
		if (gettersByDefault) {
			List<MethodMetadata> accessors = getAccessors();
			for (MethodMetadata accessor : accessors) {
				builder.addMethod(accessor);
			}
		}
		if (settersByDefault) {
			List<MethodMetadata> mutators = getMutators();
			for (MethodMetadata mutator : mutators) {
				builder.addMethod(mutator);
			}
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	public AnnotationMetadata getEmbeddableAnnotation() {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, EMBEDDABLE) == null) {
			return new DefaultAnnotationMetadata(EMBEDDABLE, new ArrayList<AnnotationAttributeValue<?>>());
		}
		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, EMBEDDABLE);
	}

	/**
	 * Locates declared fields.
	 * 
	 * <p>
	 * If no parent is defined, one will be located or created. All declared fields will be returned.
	 * 
	 * @return fields (never returns null)
	 */
	public List<FieldMetadata> getFields() {
		// Locate all declared fields
		List<FieldMetadata> declaredFields = MemberFindingUtils.getDeclaredFields(governorTypeDetails);
		if (!declaredFields.isEmpty()) {
			return declaredFields;
		}

		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

		if (parent == null) {
			// Ensure there isn't already a field called "id"; if so, compute a unique name (it's not really a fatal situation at the end of the day)
			int index = -1;
			JavaSymbolName idField = null;
			while (true) {
				// Compute the required field name
				index++;
				String fieldName = "";
				for (int i = 0; i < index; i++) {
					fieldName = fieldName + "_";
				}
				fieldName = fieldName + "id";

				idField = new JavaSymbolName(fieldName);
				if (MemberFindingUtils.getField(governorTypeDetails, idField) == null) {
					// Found a usable field name
					break;
				}
			}

			// We need to create one
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

			// Compute the column name, as required
			String columnName = idField.getSymbolName();
			List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), columnName));
			AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes);
			annotations.add(columnAnnotation);

			fields.add(new DefaultFieldMetadata(getId(), Modifier.PRIVATE, idField, new JavaType(Long.class.getName()), null, annotations));
		}
		return fields;
	}

	/**
	 * Locates the accessor methods.
	 * 
	 * <p>
	 * If {@link #getFields()} returns fields created by this ITD, public accessors will automatically be produced in the declaring class. 
	 * 
	 * @return the accessors (never returns null)
	 */
	public List<MethodMetadata> getAccessors() {
		List<MethodMetadata> accessors = new LinkedList<MethodMetadata>();

		// Locate the declared fields, and compute the names of the accessors that will be produced
		List<FieldMetadata> fields = getFields();
		for (FieldMetadata field : fields) {
			String requiredAccessorName = getRequiredAccessorName(field);

			// See if the user provided the field and the accessor method
			if (!getId().equals(field.getDeclaredByMetadataId())) {
				MethodMetadata accessor = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
				if (accessor != null) {
					Assert.isTrue(Modifier.isPublic(accessor.getModifier()), "User provided field but failed to provide a public '" + requiredAccessorName + "()' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
				} else {
					accessor = getAccessor(field);
				}
				accessors.add(accessor);				
			}
		}

		if (!accessors.isEmpty()) {
			return accessors;
		}

		// No accessor declared in governor so produce a public accessor for the default ITD field if there is no parent
		if (!fields.isEmpty()) {
			FieldMetadata id = fields.get(0);
			accessors.add(getAccessor(id));
		}

		return accessors;
	}
	
	private String getRequiredAccessorName(FieldMetadata field) {
		return "get" + StringUtils.capitalize(field.getFieldName().getSymbolName());
	}

	private MethodMetadata getAccessor(FieldMetadata field) {
		String requiredAccessorName = getRequiredAccessorName(field);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), field.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	/**
	 * Locates the mutator methods.
	 * 
	 * <p>
	 * If {@link #getFields()} returns fields created by this ITD, public mutators will automatically be produced in the declaring class. 
	 * 
	 * @return the mutators (never returns null)
	 */
	public List<MethodMetadata> getMutators() {
		List<MethodMetadata> mutators = new LinkedList<MethodMetadata>();

		// Locate the declared fields, and compute the names of the mutators that will be produced
		List<FieldMetadata> fields = getFields();
		for (FieldMetadata field : fields) {
			String requiredMutatorName = getRequiredMutatorName(field);

			List<JavaType> paramTypes = new ArrayList<JavaType>();
			paramTypes.add(field.getFieldType());

			// See if the user provided the field and the mutator method
			if (!getId().equals(field.getDeclaredByMetadataId())) {
				MethodMetadata mutator = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), paramTypes);
				if (mutator != null) {
					Assert.isTrue(Modifier.isPublic(mutator.getModifier()), "User provided field but failed to provide a public '" + requiredMutatorName + "(" + field.getFieldName().getSymbolName() + ")' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
				} else {
					mutator = getMutator(field);
				}
				mutators.add(mutator);
			}
		}

		if (!mutators.isEmpty()) {
			return mutators;
		}

		// No mutator declared in governor so produce a public mutator for the default ITD field if there is no parent
		if (!fields.isEmpty()) {
			FieldMetadata id = fields.get(0);
			mutators.add(getMutator(id));
		}

		return mutators;
	}
	
	private String getRequiredMutatorName(FieldMetadata field) {
		return "set" + StringUtils.capitalize(field.getFieldName().getSymbolName());
	}

	private MethodMetadata getMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(field.getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(field.getFieldName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	/**
	 * Locates the no-arg constructor for this class, if available.
	 * 
	 * <p>
	 * If a class defines a no-arg constructor, it is returned (irrespective of access modifiers).
	 * 
	 * <p>
	 * If a class does not define a no-arg constructor, one might be created. It will only be created if the {@link #noArgConstructor} is true AND there is at least one other constructor declared in
	 * the source file. If a constructor is created, it will have a private access modifier.
	 * 
	 * @return the constructor (may return null if no constructor is to be produced)
	 */
	public ConstructorMetadata getNoArgConstructor() {
		// Search for an existing constructor
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		ConstructorMetadata result = MemberFindingUtils.getDeclaredConstructor(governorTypeDetails, paramTypes);
		if (result != null) {
			// Found an existing no-arg constructor on this class, so return it
			return result;
		}

		// To get this far, the user did not define a no-arg constructor

		if (!noArgConstructor) {
			// This metadata instance is prohibited from making a no-arg constructor
			return null;
		}

		// Create the constructor
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");
		return new DefaultConstructorMetadata(getId(), Modifier.PRIVATE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
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
