package org.springframework.roo.addon.entity;

import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link EntityOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
@Component
@Service
public class EntityOperationsImpl implements EntityOperations {
	
	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isPersistentClassAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void newEntity(final JavaType name, final boolean createAbstract, final JavaType superclass, final List<AnnotationMetadataBuilder> annotations) {
		Assert.notNull(name, "Entity name required");
		
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);
		
		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier |= Modifier.ABSTRACT;
		}
		
		final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);

		if (!superclass.equals(OBJECT)) {
			final ClassOrInterfaceTypeDetails superclassClassOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(superclass);
			if (superclassClassOrInterfaceTypeDetails != null) {
				typeDetailsBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(superclassClassOrInterfaceTypeDetails));
			}
		}
		
		typeDetailsBuilder.setExtendsTypes(Arrays.asList(superclass));
		typeDetailsBuilder.setAnnotations(annotations);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
	}

	public void newEmbeddableClass(JavaType name, boolean serializable) {
		Assert.notNull(name, "Embeddable name required");
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);

		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(ROO_JAVA_BEAN));
		annotations.add(new AnnotationMetadataBuilder(ROO_TO_STRING));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Embeddable")));
		
		if (serializable) {
			annotations.add(new AnnotationMetadataBuilder(ROO_SERIALIZABLE));
		}

		int modifier = Modifier.PUBLIC;
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);
		
		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
	}

	public void newIdentifier(JavaType identifierType, String identifierField, String identifierColumn) {
		Assert.notNull(identifierType, "Identifier type required");
		
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		final List<AnnotationMetadataBuilder> identifierAnnotations = Arrays.asList(new AnnotationMetadataBuilder(ROO_TO_STRING), new AnnotationMetadataBuilder(ROO_IDENTIFIER));
		final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL, identifierType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(identifierAnnotations);
		
		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
	}
}
