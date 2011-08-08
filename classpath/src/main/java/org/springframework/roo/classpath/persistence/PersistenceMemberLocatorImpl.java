package org.springframework.roo.classpath.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

/**
 * This implementation of {@link PersistenceMemberLocator} scans for the presence of 
 * persistence ID tags for {@link MemberDetails} for a given domain type.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 *
 */
@Component(immediate=true)
@Service
public class PersistenceMemberLocatorImpl implements PersistenceMemberLocator, MetadataNotificationListener {
	
	private static final Map<JavaType, List<FieldMetadata>> domainTypeIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
	private static final Map<JavaType, List<FieldMetadata>> domainTypeEmbeddedIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
	private static final Map<JavaType, FieldMetadata> domainTypeVersionFieldCache = new HashMap<JavaType, FieldMetadata>();
	private static final Map<JavaType, MethodMetadata> domainTypeIdAccessorCache = new HashMap<JavaType, MethodMetadata>();
	private static final Map<JavaType, MethodMetadata> domainTypeVersionAccessorCache = new HashMap<JavaType, MethodMetadata>();
	
	// Fields
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public List<FieldMetadata> getEmbeddedIdentifierFields(final JavaType domainType) {
		if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
			return domainTypeEmbeddedIdFieldsCache.get(domainType);
		}
		return new ArrayList<FieldMetadata>();
	}
	
	public MethodMetadata getIdentifierAccessor(final JavaType domainType) {
		return domainTypeIdAccessorCache.get(domainType);
	}
	
	public List<FieldMetadata> getIdentifierFields(final JavaType domainType) {
		if (domainTypeIdFieldsCache.containsKey(domainType)) {
			return domainTypeIdFieldsCache.get(domainType);
		} else if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
			return domainTypeEmbeddedIdFieldsCache.get(domainType);
		}
		return new ArrayList<FieldMetadata>();
	}
	
	public MethodMetadata getVersionAccessor(final JavaType domainType) {
		return domainTypeVersionAccessorCache.get(domainType);
	}

	public FieldMetadata getVersionField(JavaType domainType) {
		return domainTypeVersionFieldCache.get(domainType);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (!PhysicalTypeIdentifier.isValid(upstreamDependency)) {
			return;
		}
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(upstreamDependency);
		if (physicalTypeMetadata == null) {
			return;
		}
		MemberHoldingTypeDetails memberHoldingTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (memberHoldingTypeDetails == null || !(memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails)) {
			return;
		}
		MemberDetails details = memberDetailsScanner.getMemberDetails(getClass().getName(), (ClassOrInterfaceTypeDetails) memberHoldingTypeDetails);
		
		if (MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(details, PersistenceCustomDataKeys.PERSISTENT_TYPE) == null) {
			return;
		}
		
		// Get normal persistence ID fields
		JavaType type = memberHoldingTypeDetails.getName();
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		if (!idFields.isEmpty()) {
			domainTypeIdFieldsCache.put(type, idFields);
		} else if (domainTypeIdFieldsCache.containsKey(type)) {
			domainTypeIdFieldsCache.remove(type);
		}
		
		// Get embedded ID fields 
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (!embeddedIdFields.isEmpty()) {
			domainTypeEmbeddedIdFieldsCache.remove(type);
			domainTypeEmbeddedIdFieldsCache.put(type, new ArrayList<FieldMetadata>());
			final List<FieldMetadata> fields = MemberFindingUtils.getFields(getMemberDetails(embeddedIdFields.get(0).getFieldType()));
			for (final FieldMetadata field : fields) {
				if (!field.getCustomData().keySet().contains("SERIAL_VERSION_UUID_FIELD")) {
					domainTypeEmbeddedIdFieldsCache.get(type).add(field);
				}
			}
		} else if (domainTypeEmbeddedIdFieldsCache.containsKey(type)) {
			domainTypeEmbeddedIdFieldsCache.remove(type);
		}
		
		// Get ID accessor
		MethodMetadata idAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(details, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		if (idAccessor != null) {
			domainTypeIdAccessorCache.put(type, idAccessor);
		} else if (domainTypeIdAccessorCache.containsKey(type)) {
			domainTypeIdAccessorCache.remove(type);
		}
		
		// Get version accessor
		MethodMetadata versionAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(details, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
		if (versionAccessor != null) {
			domainTypeVersionAccessorCache.put(type, versionAccessor);
		} else if (domainTypeVersionAccessorCache.containsKey(type)) {
			domainTypeVersionAccessorCache.remove(type);
		}
	}

	private MemberDetails getMemberDetails(final JavaType type) {
		final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type));
		if (physicalTypeMetadata == null || !(physicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			return null;
		}
		return memberDetailsScanner.getMemberDetails(getClass().getName(), (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails());
	}
}
