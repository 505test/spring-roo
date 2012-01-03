package org.springframework.roo.addon.dod;

import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.SpringJavaType.PERSISTENT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link DataOnDemandOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
@Component
@Service
public class DataOnDemandOperationsImpl implements DataOnDemandOperations {

    // Fields
    @Reference private MetadataService metadataService;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public boolean isDataOnDemandInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations.isFeatureInstalledInFocusedModule(
                        FeatureNames.JPA, FeatureNames.MONGO);
    }

    public void newDod(final JavaType entity, final JavaType name) {
        Assert.notNull(entity,
                "Entity to produce a data on demand provider for is required");
        Assert.notNull(name,
                "Name of the new data on demand provider is required");

        final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA,
                projectOperations.getFocusedModuleName());
        Assert.notNull(path,
                "Location of the new data on demand provider is required");

        // Verify the requested entity actually exists as a class and is not
        // abstract
        ClassOrInterfaceTypeDetails cid = getEntity(entity);
        Assert.isTrue(
                cid.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS,
                "Type " + entity.getFullyQualifiedTypeName()
                        + " is not a class");
        Assert.isTrue(!Modifier.isAbstract(cid.getModifier()),
                "Type " + entity.getFullyQualifiedTypeName() + " is abstract");

        // Check if the requested entity is a JPA @Entity
        MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(
                DataOnDemandOperationsImpl.class.getName(), cid);
        AnnotationMetadata entityAnnotation = memberDetails
                .getAnnotation(ENTITY);
        AnnotationMetadata persistentAnnotation = memberDetails
                .getAnnotation(PERSISTENT);
        Assert.isTrue(entityAnnotation != null || persistentAnnotation != null,
                "Type " + entity.getFullyQualifiedTypeName()
                        + " must be a persistent type");

        // Everything is OK to proceed
        String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(
                name, path);

        if (metadataService.get(declaredByMetadataId) != null) {
            // The file already exists
            return;
        }

        List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        List<AnnotationAttributeValue<?>> dodConfig = new ArrayList<AnnotationAttributeValue<?>>();
        dodConfig.add(new ClassAttributeValue(new JavaSymbolName("entity"),
                entity));
        annotations.add(new AnnotationMetadataBuilder(
                RooJavaType.ROO_DATA_ON_DEMAND, dodConfig));

        ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, name,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(annotations);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    /**
     * @param entity the entity to lookup required
     * @return the type details (never null; throws an exception if it cannot be
     *         obtained or parsed)
     */
    private ClassOrInterfaceTypeDetails getEntity(final JavaType entity) {
        ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(entity);
        Assert.notNull(cid, "Java source code details unavailable for type '"
                + entity + "'");
        return cid;
    }
}
