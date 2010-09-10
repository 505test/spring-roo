package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.ItdMetadataScanner;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Provides Controller configuration operations.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component 
@Service 
public class ControllerOperationsImpl implements ControllerOperations {
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ClasspathOperations classpathOperations;
	@Reference private WebMvcOperations webMvcOperations;
	@Reference private ItdMetadataScanner itdMetadataScanner;
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private TypeLocationService typeLocationService;

	public void generateAll(final JavaPackage javaPackage) {
		// TODO Will need to revisit this as a result of ROO-1372
		Set<ClassOrInterfaceTypeDetails> cids = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooEntity.class.getName()));
		each_file: for (ClassOrInterfaceTypeDetails cid : cids) {
			if (Modifier.isAbstract(cid.getModifier())) {
				continue;
			}

			Set<MetadataItem> metadata = itdMetadataScanner.getMetadata(cid.getDeclaredByMetadataId());
			for (MetadataItem item : metadata) {
				if (item instanceof EntityMetadata) {
					EntityMetadata entityMetadata = (EntityMetadata) item;
					Set<String> downstream = dependencyRegistry.getDownstream(entityMetadata.getId());
					// Check to see if this entity metadata has a web scaffold metadata listening to it
					for (String ds : downstream) {
						if (WebScaffoldMetadata.isValid(ds)) {
							// There is already a controller for this entity
							continue each_file;
						}
					}
					// To get here, there is no listening controller, so add one
					JavaType entity = cid.getName();
					JavaType controller = new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Controller");
					createAutomaticController(controller, entity, new HashSet<String>(), entityMetadata.getPlural().toLowerCase());
					break;
				}
			}
		}
		return;
	}

	public boolean isNewControllerAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}

	public void createAutomaticController(JavaType controller, JavaType entity, Set<String> disallowedOperations, String path) {
		Assert.notNull(controller, "Controller Java Type required");
		Assert.notNull(entity, "Entity Java Type required");
		Assert.notNull(disallowedOperations, "Set of disallowed operations required");
		Assert.hasText(path, "Controller base path required");

		String ressourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);

		// Create annotation @RooWebScaffold(path = "/test", formBackingObject = MyObject.class)
		List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		rooWebScaffoldAttributes.add(new StringAttributeValue(new JavaSymbolName("path"), path));
		rooWebScaffoldAttributes.add(new ClassAttributeValue(new JavaSymbolName("formBackingObject"), entity));
		for (String operation : disallowedOperations) {
			rooWebScaffoldAttributes.add(new BooleanAttributeValue(new JavaSymbolName(operation), false));
		}
		AnnotationMetadata rooWebScaffold = new DefaultAnnotationMetadata(new JavaType(RooWebScaffold.class.getName()), rooWebScaffoldAttributes);

		// Create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + path));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		// Create annotation @Controller
		List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		AnnotationMetadata controllerAnnotation = new DefaultAnnotationMetadata(new JavaType("org.springframework.stereotype.Controller"), controllerAttributes);

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, pathResolver.getPath(ressourceIdentifier));
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(rooWebScaffold);
		annotations.add(requestMapping);
		annotations.add(controllerAnnotation);
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, controller, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, annotations);

		classpathOperations.generateClassFile(details);

		webMvcOperations.installAllWebMvcArtifacts();
	}
}
