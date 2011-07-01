package org.springframework.roo.addon.layers.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.LayerService;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component(immediate=true)
@Service
public class ServiceClassMetadataProvider extends AbstractItdMetadataProvider {
	
	@Reference private LayerService layerService;
	private static final int LAYER_POSITION = LayerType.SERVICE.getPosition();
	private static final Path SRC = Path.SRC_MAIN_JAVA;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		setIgnoreTriggerAnnotations(true);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}
	
	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		ServiceInterfaceMetadata serviceInterfaceMetadata = null;
		for (JavaType type : coitd.getImplementsTypes()) {
			if ((serviceInterfaceMetadata = (ServiceInterfaceMetadata) metadataService.get(ServiceInterfaceMetadata.createIdentifier(type, SRC))) != null) {
				break;
			}
		}
		if (serviceInterfaceMetadata == null || !serviceInterfaceMetadata.isValid()) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(serviceInterfaceMetadata.getId(), metadataIdentificationString);
		ServiceAnnotationValues serviceAnnotationValues = serviceInterfaceMetadata.getServiceAnnotationValues();
		JavaType[] domainTypes = serviceAnnotationValues.getDomainTypes();
		if (domainTypes == null) {
			return null;
		}
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), coitd);
		
		Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
		Map<JavaType,Map<String, MemberTypeAdditions>> allCrudAdditions = new HashMap<JavaType,Map<String, MemberTypeAdditions>>();
		Map<String, LinkedHashMap<JavaSymbolName, Object>> requiredMethods = new HashMap<String, LinkedHashMap<JavaSymbolName,Object>>();
		requiredMethods.put(PersistenceCustomDataKeys.FIND_ALL_METHOD.name(), new LinkedHashMap<JavaSymbolName, Object>());
		for (JavaType domainType : domainTypes) {
			metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, SRC), metadataIdentificationString);
			Map<String, MemberTypeAdditions> methodAdditions = new HashMap<String, MemberTypeAdditions>();
			for (String method : requiredMethods.keySet()) {
				methodAdditions.put(method, layerService.getMemberTypeAdditions(metadataIdentificationString, method, domainType, requiredMethods.get(method), LAYER_POSITION));
			}
			allCrudAdditions.put(domainType, methodAdditions);
			
			String pluralId = PluralMetadata.createIdentifier(domainType, Path.SRC_MAIN_JAVA);
			PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
			if (pluralMetadata == null) {
				return null;
			}
			metadataDependencyRegistry.registerDependency(pluralId, metadataIdentificationString);
			domainTypePlurals.put(domainType, pluralMetadata.getPlural());
		}
		return new ServiceClassMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberDetails, serviceAnnotationValues, allCrudAdditions, domainTypePlurals);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Service";
	}

	public String getProvidesType() {
		return ServiceClassMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ServiceClassMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ServiceClassMetadata.getJavaType(metadataIdentificationString);
		Path path = ServiceClassMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
}
