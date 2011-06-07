package org.springframework.roo.addon.entity;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.LayerAdapter;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.project.layers.Priority;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class EntityLayerProvider extends LayerAdapter {
	@Reference private MetadataService metadataService;
	private static final Path PATH = Path.SRC_MAIN_JAVA;

	@Override
	public MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		EntityAnnotationValues rooEntityAnnotation = getRooEntityAnnotationValues(entityType);
		String plural = getPlural(entityType);
		if (rooEntityAnnotation == null || !StringUtils.hasText(rooEntityAnnotation.getFindAllMethod()) || plural == null) {
			return null;
		}
		
		return new MemberTypeAdditions(new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId), entityType.getFullyQualifiedTypeName() + "." + rooEntityAnnotation.getFindAllMethod() + plural + "()");
	}
	
	private EntityAnnotationValues getRooEntityAnnotationValues(JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			return null;
		}
		return new EntityAnnotationValues(physicalTypeMetadata);
	}
	
	private String getPlural(JavaType javaType) {
		String key = PluralMetadata.createIdentifier(javaType, PATH);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(key);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		return pluralMetadata.getPlural();
	}

	public LayerType getLayerType() {
		return LayerType.ACTIVE_RECORD;
	}

	public int priority() {
		return Priority.LOW.getNumericValue();
	}
	
}
