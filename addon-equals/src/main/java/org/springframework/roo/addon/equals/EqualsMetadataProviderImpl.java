package org.springframework.roo.addon.equals;

import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Implementation of  {@link EqualsMetadataProvider}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class EqualsMetadataProviderImpl extends AbstractItdMetadataProvider implements EqualsMetadataProvider {

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_EQUALS);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_EQUALS);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		final EqualsAnnotationValues annotationValues = new EqualsAnnotationValues(governorPhysicalTypeMetadata);
		final String[] excludeFields = annotationValues.getExcludeFields();

		final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		if (memberDetails == null) {
			return null;
		}

		final JavaType javaType = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();
		List<FieldMetadata> locatedFields = locateFields(javaType, excludeFields, memberDetails, metadataIdentificationString);
		if (locatedFields.isEmpty()) {
			return null;
		}

		return new EqualsMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, locatedFields);
	}

	private List<FieldMetadata> locateFields(final JavaType javaType, final String[] excludeFields, final MemberDetails memberDetails, final String metadataIdentificationString) {
		final SortedSet<FieldMetadata> locatedFields = new TreeSet<FieldMetadata>(new Comparator<FieldMetadata>() {
			public int compare(final FieldMetadata l, final FieldMetadata r) {
				return l.getFieldName().compareTo(r.getFieldName());
			}
		});

		final Set<JavaSymbolName> excludeFieldsSet = new HashSet<JavaSymbolName>();
		if (excludeFields != null && excludeFields.length > 0) {
			for (String excludeField : excludeFields) {
				excludeFieldsSet.add(new JavaSymbolName(excludeField));
			}
		}

		for (final FieldMetadata field : memberDetails.getFields()) {
			if (!excludeFieldsSet.isEmpty()) {
				if (excludeFieldsSet.contains(field.getFieldName())) {
					continue;
				}
			}
			if (Modifier.isStatic(field.getModifier()) || Modifier.isTransient(field.getModifier()) || field.getFieldType().isCommonCollectionType()) {
				continue;
			}
			if (persistenceMemberLocator.getVersionField(javaType) != null && field.getFieldName().equals(persistenceMemberLocator.getVersionField(javaType).getFieldName())) {
				continue;
			}

			metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
			locatedFields.add(field);
		}

		return new ArrayList<FieldMetadata>(locatedFields);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Equals";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = EqualsMetadata.getJavaType(metadataIdentificationString);
		Path path = EqualsMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return EqualsMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EqualsMetadata.getMetadataIdentiferType();
	}
}
