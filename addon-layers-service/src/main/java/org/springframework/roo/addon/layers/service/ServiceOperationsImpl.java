package org.springframework.roo.addon.layers.service;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.classpath.PhysicalTypeCategory.CLASS;
import static org.springframework.roo.classpath.PhysicalTypeCategory.INTERFACE;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

import java.util.Arrays;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.uaa.client.util.Assert;

/**
 * The {@link ServiceOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class ServiceOperationsImpl implements ServiceOperations {

    // Fields
    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeManagementService typeManagementService;

    public boolean isServiceInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }

    public void setupService(final JavaType interfaceType,
            final JavaType classType, final JavaType domainType) {
        Assert.notNull(interfaceType, "Interface type required");
        createServiceInterface(interfaceType, domainType);
        createServiceClass(interfaceType, classType);
    }

    private void createServiceInterface(final JavaType interfaceType,
            final JavaType domainType) {
        final String interfaceIdentifier = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);
        if (fileManager.exists(interfaceIdentifier)) {
            return; // Type already exists - nothing to do
        }
        Assert.notNull(domainType, "Domain type required");
        final AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_SERVICE);
        interfaceAnnotationMetadata
                .addAttribute(new ArrayAttributeValue<ClassAttributeValue>(
                        new JavaSymbolName("domainTypes"), Arrays
                                .asList(new ClassAttributeValue(
                                        new JavaSymbolName("foo"), domainType))));
        final String interfaceMid = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, pathResolver.getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMid, PUBLIC, interfaceType, INTERFACE);
        interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());
        typeManagementService.createOrUpdateTypeOnDisk(interfaceTypeBuilder
                .build());
    }

    private void createServiceClass(final JavaType interfaceType,
            final JavaType classType) {
        Assert.notNull(classType, "Class type required");
        final String classIdentifier = pathResolver.getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, classType);
        if (fileManager.exists(classIdentifier)) {
            return; // Type already exists - nothing to do
        }
        final String classMid = PhysicalTypeIdentifier.createIdentifier(
                classType, pathResolver.getPath(classIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder classTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                classMid, PUBLIC, classType, CLASS);
        classTypeBuilder.addImplementsType(interfaceType);
        typeManagementService
                .createOrUpdateTypeOnDisk(classTypeBuilder.build());
    }
}
