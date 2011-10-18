package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default representation of a {@link ClassOrInterfaceTypeDetails}.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultClassOrInterfaceTypeDetails extends AbstractMemberHoldingTypeDetails implements ClassOrInterfaceTypeDetails {

	// Fields
	private final ClassOrInterfaceTypeDetails superclass;
	private final JavaType name;
	private List<ClassOrInterfaceTypeDetails> declaredInnerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<InitializerMetadata> declaredInitializers = new ArrayList<InitializerMetadata>();
	private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private final PhysicalTypeCategory physicalTypeCategory;
	private Set<ImportMetadata> registeredImports = new HashSet<ImportMetadata>();

	/**
	 * Constructor is package protected to mandate the use of
	 * {@link ClassOrInterfaceTypeDetailsBuilder}
	 *
	 * @param customData
	 * @param declaredByMetadataId
	 * @param modifier
	 * @param annotations
	 * @param name
	 * @param physicalTypeCategory
	 * @param declaredConstructors
	 * @param declaredFields
	 * @param declaredMethods
	 * @param declaredInnerTypes
	 * @param declaredInitializers
	 * @param superclass
	 * @param extendsTypes
	 * @param implementsTypes
	 * @param enumConstants
	 * @param registeredImports
	 */
	DefaultClassOrInterfaceTypeDetails(final CustomData customData,
		final String declaredByMetadataId,
		final int modifier,
		final List<AnnotationMetadata> annotations,
		final JavaType name,
		final PhysicalTypeCategory physicalTypeCategory,
		final List<ConstructorMetadata> declaredConstructors,
		final List<FieldMetadata> declaredFields,
		final List<MethodMetadata> declaredMethods,
		final List<ClassOrInterfaceTypeDetails> declaredInnerTypes,
		final List<InitializerMetadata> declaredInitializers,
		final ClassOrInterfaceTypeDetails superclass,
		final List<JavaType> extendsTypes,
		final List<JavaType> implementsTypes,
		final List<JavaSymbolName> enumConstants,
		final Collection<ImportMetadata> registeredImports) {

		super(customData, declaredByMetadataId, modifier, annotations);
		Assert.notNull(name, "Name required");
		Assert.notNull(physicalTypeCategory, "Physical type category required");

		this.name = name;
		this.physicalTypeCategory = physicalTypeCategory;
		this.superclass = superclass;

		if (declaredConstructors != null) {
			this.declaredConstructors = declaredConstructors;
		}

		if (declaredFields != null) {
			this.declaredFields = declaredFields;
		}

		if (declaredMethods != null) {
			this.declaredMethods = declaredMethods;
		}

		if (declaredInnerTypes != null) {
			this.declaredInnerTypes = declaredInnerTypes;
		}

		if (declaredInitializers != null) {
			this.declaredInitializers = declaredInitializers;
		}

		if (extendsTypes != null) {
			this.extendsTypes = extendsTypes;
		}

		if (implementsTypes != null) {
			this.implementsTypes = implementsTypes;
		}

		if (enumConstants != null && physicalTypeCategory == PhysicalTypeCategory.ENUMERATION) {
			this.enumConstants = enumConstants;
		}

		this.registeredImports = new HashSet<ImportMetadata>();
		if (registeredImports != null) {
			this.registeredImports.addAll(registeredImports);
		}
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public JavaType getName() {
		return name;
	}

	public List<? extends ConstructorMetadata> getDeclaredConstructors() {
		return Collections.unmodifiableList(declaredConstructors);
	}

	public List<JavaSymbolName> getEnumConstants() {
		return Collections.unmodifiableList(enumConstants);
	}

	public List<? extends FieldMetadata> getDeclaredFields() {
		return Collections.unmodifiableList(declaredFields);
	}

	public List<? extends MethodMetadata> getDeclaredMethods() {
		return Collections.unmodifiableList(declaredMethods);
	}

	public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
		return Collections.unmodifiableList(declaredInnerTypes);
	}

	public List<InitializerMetadata> getDeclaredInitializers() {
		return Collections.unmodifiableList(declaredInitializers);
	}

	public List<JavaType> getExtendsTypes() {
		return Collections.unmodifiableList(extendsTypes);
	}

	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}

	public ClassOrInterfaceTypeDetails getSuperclass() {
		return superclass;
	}

	public Set<ImportMetadata> getRegisteredImports() {
		return Collections.unmodifiableSet(registeredImports);
	}

	public boolean extendsType(final JavaType type) {
		return this.extendsTypes.contains(type);
	}

	public boolean implementsAny(final JavaType... types) {
		for (final JavaType type : types) {
			if (this.implementsTypes.contains(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name);
		tsc.append("modifier", Modifier.toString(getModifier()));
		tsc.append("physicalTypeCategory", physicalTypeCategory);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("declaredConstructors", declaredConstructors);
		tsc.append("declaredFields", declaredFields);
		tsc.append("declaredMethods", declaredMethods);
		tsc.append("enumConstants", enumConstants);
		tsc.append("superclass", superclass);
		tsc.append("extendsTypes", extendsTypes);
		tsc.append("implementsTypes", implementsTypes);
		tsc.append("annotations", getAnnotations());
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}

	@SuppressWarnings("unchecked")
	public List<String> getDynamicFinderNames() {
		final List<String> dynamicFinders = new ArrayList<String>();
		final Object finders = getCustomData().get(CustomDataKeys.DYNAMIC_FINDER_NAMES);
		if (finders instanceof Collection) {
			dynamicFinders.addAll((Collection<String>) finders);
		}
		return dynamicFinders;
	}

	public boolean declaresField(final JavaSymbolName fieldName) {
		return getDeclaredField(fieldName) != null;
	}
}
