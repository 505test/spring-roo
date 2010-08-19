package org.springframework.roo.addon.json;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;


/**
 * Provides {@link JsonMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component
@Service
public final class JsonMetadataProvider extends AbstractItdMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJson.class.getName()));
		addMetadataTrigger(new JavaType("org.springframework.roo.addon.entity.RooIdentifier"));
		addMetadataTrigger(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJson.class.getName()));	
		removeMetadataTrigger(new JavaType("org.springframework.roo.addon.entity.RooIdentifier"));
		removeMetadataTrigger(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// Acquire bean info (we need getters details, specifically)
		JavaType javaType = JsonMetadata.getJavaType(metadataIdentificationString);
		Path path = JsonMetadata.getPath(metadataIdentificationString);
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);
		
		// We need to parse the annotation, if it is not present we will simply get the default annotation values
		JsonAnnotationValues annotationValues = new JsonAnnotationValues(governorPhysicalTypeMetadata);
		
		// We want to be notified if the getter info changes in any way 
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		
		// Abort if we don't have getter information available
		if (beanInfoMetadata == null) {
			return null;
		}
		
		return new JsonMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, beanInfoMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Json";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JsonMetadata.getJavaType(metadataIdentificationString);
		Path path = JsonMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JsonMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsonMetadata.getMetadataIdentiferType();
	}
}