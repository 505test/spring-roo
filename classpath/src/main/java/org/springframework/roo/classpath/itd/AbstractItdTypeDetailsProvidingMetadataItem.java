package org.springframework.roo.classpath.itd;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;

import java.util.Arrays;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract implementation of {@link ItdTypeDetailsProvidingMetadataItem}, which assumes the subclass will require
 * a non-null {@link ClassOrInterfaceTypeDetails} representing the governor and wishes to build an ITD via the
 * {@link ItdTypeDetailsBuilder} mechanism. 
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractItdTypeDetailsProvidingMetadataItem extends AbstractMetadataItem implements ItdTypeDetailsProvidingMetadataItem {
	
	// Fields
	protected ClassOrInterfaceTypeDetails governorTypeDetails;
	protected ItdTypeDetails itdTypeDetails;
	protected ItdTypeDetailsBuilder builder;
	protected JavaType destination;
	protected JavaType aspectName;
	protected PhysicalTypeMetadata governorPhysicalTypeMetadata;
	
	/**
	 * Validates input and constructs a superclass that implements {@link ItdTypeDetailsProvidingMetadataItem}.
	 * 
	 * <p>
	 * Exposes the {@link ClassOrInterfaceTypeDetails} of the governor, if available. If they are not available, ensures
	 * {@link #isValid()} returns false.
	 * 
	 * <p>
	 * Subclasses should generally return immediately if {@link #isValid()} is false. Subclasses should also attempt to set the
	 * {@link #itdTypeDetails} to contain the output of their ITD where {@link #isValid()} is true.
	 * 
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 */
	public AbstractItdTypeDetailsProvidingMetadataItem(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier);
		Assert.notNull(aspectName, "Aspect name required");
		Assert.notNull(governorPhysicalTypeMetadata, "Governor physical type metadata required");
		
		this.aspectName = aspectName;
		this.governorPhysicalTypeMetadata = governorPhysicalTypeMetadata;

		PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (physicalTypeDetails == null || !(physicalTypeDetails instanceof ClassOrInterfaceTypeDetails)) {
			// There is a problem
			valid = false;
		} else {
			// We have reliable physical type details
			governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
		}

		this.destination = governorTypeDetails.getName();
		
		// Provide the subclass a builder, to make preparing an ITD even easier
		this.builder = new ItdTypeDetailsBuilder(getId(), governorTypeDetails, aspectName, true);
	}
	
	public final ItdTypeDetails getMemberHoldingTypeDetails() {
		return itdTypeDetails;
	}
	
	/**
	 * Returns the metadata for an annotation of the given type if the governor
	 * does not already have one.
	 * 
	 * @param annotationType the type of annotation to generate (required)
	 * @return <code>null</code> if the governor already has that annotation
	 */
	protected AnnotationMetadata getTypeAnnotation(final JavaType annotationType) {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, annotationType) != null) {
			return null;
		}
		return new AnnotationMetadataBuilder(annotationType).build();
	}

	/**
	 * Determines if the presented class (or any of its superclasses) implements the target interface.
	 * 
	 * @param clazz the cid to search
	 * @param interfaceTarget the interface to locate
	 * @return true if the class or any of its superclasses contains the specified interface
	 */
	protected boolean isImplementing(final ClassOrInterfaceTypeDetails clazz, final JavaType interfaceTarget) {
		if (clazz.getImplementsTypes().contains(interfaceTarget)) {
			return true;
		}
		if (clazz.getSuperclass() != null) {
			return isImplementing(clazz.getSuperclass(), interfaceTarget);
		}
		return false;
	}
	
	/**
	 * Returns the given method of the governor.
	 * 
	 * @param methodName the name of the method for which to search
	 * @param parameterTypes the method's parameter types
	 * @return null if there was no such method
	 * @see MemberFindingUtils#getDeclaredMethod(org.springframework.roo.classpath.details.MemberHoldingTypeDetails, JavaSymbolName, List)
	 * @since 1.2.0
	 */
	protected MethodMetadata getGovernorMethod(final JavaSymbolName methodName, final JavaType... parameterTypes) {
		return getGovernorMethod(methodName, Arrays.asList(parameterTypes));
	}
	
	/**
	 * Returns the given method of the governor.
	 * 
	 * @param methodName the name of the method for which to search
	 * @param parameterTypes the method's parameter types
	 * @return null if there was no such method
	 * @see MemberFindingUtils#getDeclaredMethod(org.springframework.roo.classpath.details.MemberHoldingTypeDetails, JavaSymbolName, List)
	 * @since 1.2.0 (previously called methodExists)
	 */
	protected MethodMetadata getGovernorMethod(final JavaSymbolName methodName, final List<JavaType> parameterTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, parameterTypes);
	}
	
	protected FieldMetadata getField(final JavaSymbolName fieldName, final JavaType fieldType) {
		return getField(PRIVATE, fieldName, fieldType, null);
	}

	/**
	 * Convenience method for returning a simple private field based on the field name, type, and initializer.
	 * 
	 * @param fieldName the field name
	 * @param fieldType the field type
	 * @param fieldInitializer the string to initialize the field with
	 * @return null if the field exists on the governor, otherwise a new field with the given field name and type
	 */
	protected FieldMetadata getField(final int modifier, final JavaSymbolName fieldName, final JavaType fieldType, final String fieldInitializer) {
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return null;
		
		addToImports(Arrays.asList(fieldType));
		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), modifier, fieldName, fieldType, fieldInitializer);
		return fieldBuilder.build();
	}

	protected MethodMetadata getAccessorMethod(final JavaSymbolName fieldName, final JavaType fieldType) {
		return getAccessorMethod(fieldName, fieldType, InvocableMemberBodyBuilder.getInstance().appendFormalLine("return " + fieldName.getSymbolName() + ";"));
	}
	
	protected MethodMetadata getAccessorMethod(final JavaSymbolName fieldName, final JavaType fieldType, final InvocableMemberBodyBuilder bodyBuilder) {
		return getMethod(PUBLIC, BeanInfoUtils.getAccessorMethodName(fieldName, fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)), fieldType, null, null, bodyBuilder);
	}
	
	protected MethodMetadata getMutatorMethod(final JavaSymbolName fieldName, final JavaType parameterType) {
		return getMutatorMethod(fieldName, parameterType, InvocableMemberBodyBuilder.getInstance().appendFormalLine("this." + fieldName.getSymbolName() + " = " + fieldName.getSymbolName() + ";"));
	}

	protected MethodMetadata getMutatorMethod(final JavaSymbolName fieldName, final JavaType parameterType, final InvocableMemberBodyBuilder bodyBuilder) {
		return getMethod(PUBLIC, BeanInfoUtils.getMutatorMethodName(fieldName), JavaType.VOID_PRIMITIVE, Arrays.asList(parameterType), Arrays.asList(fieldName), bodyBuilder);
	}

	/**
	 * Returns a public method given the method name, return type, parameter types, parameter names, and method body.
	 * 
	 * @param methodName the method name
	 * @param returnType the return type
	 * @param parameterTypes a list of parameter types
	 * @param parameterNames a list of parameter names
	 * @param bodyBuilder the method body
	 * @return null if the method exists on the governor, otherwise a new method
	 */
	protected MethodMetadata getMethod(final int modifier, final JavaSymbolName methodName, final JavaType returnType, final List<JavaType> parameterTypes, final List<JavaSymbolName> parameterNames, final InvocableMemberBodyBuilder bodyBuilder) {
		final MethodMetadata method = getGovernorMethod(methodName, parameterTypes);
		if (method != null) return null;
		
		addToImports(parameterTypes);
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private void addToImports(final List<JavaType> parameterTypes) {
		if (parameterTypes != null) {
			final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			for (JavaType parameterType : parameterTypes) {
				if (!parameterType.getFullyQualifiedTypeName().startsWith("java.lang")) {
					imports.addImport(parameterType);
				}
			}
		}
	}
	
	/**
	 * Ensures that the governor extends the given type, i.e. introduces that
	 * type as a supertype iff it's not already one
	 * 
	 * @param javaType the type to extend (required)
	 * @since 1.2.0
	 */
	protected final void ensureGovernorExtends(final JavaType javaType) {
		if (!governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().extendsType(javaType)) {
			builder.addExtendsTypes(javaType);
		}
	}
	
	@Override
	public int hashCode() {
		return builder.build().hashCode();
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}
}
