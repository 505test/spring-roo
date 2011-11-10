package org.springframework.roo.addon.web.mvc.controller.scaffold;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link WebScaffoldMetadataProvider}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class WebScaffoldMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements WebScaffoldMetadataProvider {

	// Fields
	@Reference private WebMetadataService webMetadataService;

	private final Map<JavaType, String> entityToWebScaffoldMidMap = new LinkedHashMap<JavaType, String>();
	private final Map<String, JavaType> webScaffoldMidToEntityMap = new LinkedHashMap<String, JavaType>();

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_WEB_SCAFFOLD);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_WEB_SCAFFOLD);
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		final JavaType governor = itdTypeDetails.getName();

		// If the governor is a form backing object, refresh its local metadata
		final String localMid = entityToWebScaffoldMidMap.get(governor);
		if (localMid != null) {
			return localMid;
		}

		// If the governor is a layer component that manages a form backing object, refresh that object's local metadata
		return getWebScaffoldMidIfLayerComponent(governor);
	}

	/**
	 * If the given governor is a layer component (service, repository, etc.)
	 * that manages an entity for which we maintain web scaffold metadata,
	 * returns the ID of that metadata, otherwise returns <code>null</code>.
	 *
	 * TODO doesn't handle the case where the governor is a component that
	 * manages multiple entities, as it always returns the MID for the first
	 * entity found (in annotation order) for which we provide web metadata. We
	 * would need to enhance {@link AbstractMemberDiscoveringItdMetadataProvider#getLocalMidToRequest}
	 * to return a list of MIDs, rather than only one.
	 *
	 * @param governor the governor to check (required)
	 * @return see above
	 */
	private String getWebScaffoldMidIfLayerComponent(final JavaType governor) {
		final MemberHoldingTypeDetails governorDetails = typeLocationService.getTypeDetails(governor);
		if (governorDetails != null) {
			for (final JavaType type : governorDetails.getLayerEntities()) {
				final String localMid = entityToWebScaffoldMidMap.get(type);
				if (localMid != null) {
					/*
					 * The ITD's governor is a layer component that manages an entity for which we maintain
					 * web scaffold metadata => refresh that MD in case a layer has appeared or gone away.
					 */
					return localMid;
				}
			}
		}
		return null;
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		final WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalType);
		final JavaType formBackingType = annotationValues.getFormBackingObject();
		if (!annotationValues.isAnnotationFound() || formBackingType == null) {
			return null;
		}

		final MemberDetails formBackingObjectMemberDetails = getMemberDetails(formBackingType);
		if (formBackingObjectMemberDetails == null) {
			return null;
		}

		final MemberHoldingTypeDetails formBackingMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(formBackingObjectMemberDetails, PERSISTENT_TYPE);
		if (formBackingMemberHoldingTypeDetails == null) {
			return null;
		}

		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = webMetadataService.getCrudAdditions(formBackingType, metadataId);
		if (CollectionUtils.isEmpty(crudAdditions)) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(formBackingMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataId);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing any previous association
		final JavaType oldEntity = webScaffoldMidToEntityMap.get(metadataId);
		if (oldEntity != null) {
			entityToWebScaffoldMidMap.remove(oldEntity);
		}
		entityToWebScaffoldMidMap.put(formBackingType, metadataId);
		webScaffoldMidToEntityMap.put(metadataId, formBackingType);

		final SortedMap<JavaType, JavaTypeMetadataDetails> relatedApplicationTypeMetadata = webMetadataService.getRelatedApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataId);
		final List<JavaTypeMetadataDetails> dependentApplicationTypeMetadata = webMetadataService.getDependentApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataId);
		final Map<JavaSymbolName, DateTimeFormatDetails> datePatterns = webMetadataService.getDatePatterns(formBackingType, formBackingObjectMemberDetails, metadataId);

		return new WebScaffoldMetadata(metadataId, aspectName, governorPhysicalType, annotationValues, relatedApplicationTypeMetadata, dependentApplicationTypeMetadata, datePatterns, crudAdditions);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Controller";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = WebScaffoldMetadata.getJavaType(metadataIdentificationString);
		LogicalPath path = WebScaffoldMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
		return WebScaffoldMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return WebScaffoldMetadata.getMetadataIdentiferType();
	}
}