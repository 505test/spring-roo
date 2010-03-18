package org.springframework.roo.classpath.operations;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Provides convenience methods that can be used to create source code and install the
 * JSR 303 validation API when required.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class ClasspathOperations {
	private ProjectOperations projectOperations;
	private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	private MetadataService metadataService;
	private FileManager fileManager;
	
	public ClasspathOperations(ProjectOperations projectOperations, MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider, MetadataService metadataService, FileManager fileManager) {
		Assert.notNull(projectOperations, "Project operations required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		this.projectOperations = projectOperations;
		this.physicalTypeMetadataProvider = physicalTypeMetadataProvider;
		this.metadataService = metadataService;
		this.fileManager = fileManager;
	}

	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
	
	public boolean isProjectAvailable() {
		return getPathResolver() != null;
	}
	
	public boolean isPersistentClassAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}
	
	public String getPhysicalLocationCanonicalPath(JavaType javaType, Path path) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(path, "Path required");
		PathResolver pathResolver = getPathResolver();
		Assert.notNull(pathResolver, "Cannot computed metadata ID of a type because the path resolver is presently unavailable");
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		String physicalLocationCanonicalPath = pathResolver.getIdentifier(path, relativePath);
		return physicalLocationCanonicalPath;
	}
	
	public String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		PathResolver pathResolver = getPathResolver();
		Assert.notNull(pathResolver, "Cannot computed metadata ID of a type because the path resolver is presently unavailable");
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
		Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		String physicalLocationCanonicalPath = pathResolver.getIdentifier(path, relativePath);
		return physicalLocationCanonicalPath;
	}

	/**
	 * Obtains the superclass, if one is specified.
	 * 
	 * <p>
	 * Throws an exception is a superclass was requested but could not be parsed or found.
	 * 
	 * @param superclass requested superclass (can be null)
	 * @return null if a superclass is not requested or not required (ie java.lang.Object)
	 */
	public ClassOrInterfaceTypeDetails getSuperclass(JavaType superclass) {
		if (superclass.equals(new JavaType("java.lang.Object"))) {
			return null;
		}
		return getClassOrInterface(superclass);
	}
	
	/**
	 * Obtains the requested {@link JavaType}, assuming it is a class or interface that exists at this time and can be parsed.
	 * If these assumption are not met, an exception will be thrown.
	 * 
	 * @param requiredClassOrInterface that should be parsed (required)
	 * @return the details (never returns null)
	 */
	public ClassOrInterfaceTypeDetails getClassOrInterface(JavaType requiredClassOrInterface) {
		String superclassMetadataId = physicalTypeMetadataProvider.findIdentifier(requiredClassOrInterface);
		Assert.notNull(superclassMetadataId, "Unable to locate requested type'" + requiredClassOrInterface.getFullyQualifiedTypeName() + "'");
		PhysicalTypeMetadata superclassMetadata = (PhysicalTypeMetadata) metadataService.get(superclassMetadataId);
		PhysicalTypeDetails superclassPhysicalDetails = superclassMetadata.getPhysicalTypeDetails();
		Assert.notNull(superclassPhysicalDetails, "Type '" + requiredClassOrInterface.getFullyQualifiedTypeName() + "' exists on disk but cannot be parsed");
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, superclassPhysicalDetails, "Type '" + requiredClassOrInterface.getFullyQualifiedTypeName() + "' is not an interface or class");
		return (ClassOrInterfaceTypeDetails) superclassPhysicalDetails;
	}
	/**
	 * Creates a new class, with the location name name provided in the details.
	 * 
	 * <p>
	 * An exception is thrown if the class already exists.
	 * 
	 * @param details to create (required)
	 */
	public void generateClassFile(ClassOrInterfaceTypeDetails details) {
		Assert.isTrue(isProjectAvailable(), "Class file cannot be generated at this time");
		Assert.notNull(details, "Details required");
		
		// Determine the canonical filename
		String physicalLocationCanonicalPath = getPhysicalLocationCanonicalPath(details.getDeclaredByMetadataId());
		
		// Check the file doesn't already exist
		Assert.isTrue(!fileManager.exists(physicalLocationCanonicalPath), getPathResolver().getFriendlyName(physicalLocationCanonicalPath) + " already exists");
		
		// Compute physical location
		PhysicalTypeMetadata toCreate = new DefaultPhysicalTypeMetadata(details.getDeclaredByMetadataId(), physicalLocationCanonicalPath, details);
		physicalTypeMetadataProvider.createPhysicalType(toCreate);
	}
	
	public void addEnumConstant(String physicalTypeIdentifier, JavaSymbolName constantName) {
		Assert.notNull(isProjectAvailable(), "Cannot add a constant at this time");
		Assert.hasText(physicalTypeIdentifier, "Type identifier not provided");
		Assert.notNull(constantName, "Constant name required");
		
		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		// Ensure it's an enum
		Assert.isTrue(mutableTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION,  PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier) + " is not an enum");
		
		mutableTypeDetails.addEnumConstant(constantName);
	}
	
	/**
	 * Adds a new field to an existing class.
	 * 
	 * <p>
	 * An exception is thrown if the class does not exist, cannot be modified or a field with the requested name is already declared.
	 *  
	 * @param fieldMetadata to add (required)
	 */
	public void addField(FieldMetadata fieldMetadata) {
		Assert.notNull(isProjectAvailable(), "Field cannot be added at this time");
		Assert.notNull(fieldMetadata, "Field metadata not provided");
		
		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(fieldMetadata.getDeclaredByMetadataId());
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;
		
		// Automatically add JSR 303 (Bean Validation API) support if there is no current JSR 303 support but a JSR 303 annotation is present
		boolean jsr303Required = false;
		for (AnnotationMetadata annotation : fieldMetadata.getAnnotations()) {
			if (annotation.getAnnotationType().getFullyQualifiedTypeName().startsWith("javax.validation")) {
				jsr303Required = true;
				break;
			}
		}
		
		if (jsr303Required) {
			// It's more likely the version below represents a later version than any specified in the user's own dependency list
			projectOperations.dependencyUpdate(new Dependency("javax.validation", "com.springsource.javax.validation", "1.0.0.GA"));
		}
		
		mutableTypeDetails.addField(fieldMetadata);
	}

	/**
	 * Creates an integration test for the entity. Automatically produces a data on demand if one does not exist.
	 * Silently returns if the integration test file already exists.
	 * 
	 * @param entity to produce an integration test for (required)
	 */
	public void newIntegrationTest(JavaType entity) {
		Assert.notNull(entity, "Entity to produce an integration test for is required");

		// Verify the requested entity actually exists as a class and is not abstract
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getEntity(entity);
		Assert.isTrue(!Modifier.isAbstract(classOrInterfaceTypeDetails.getModifier()), "Type " + entity.getFullyQualifiedTypeName() + " is abstract");
		
		newDod(entity, new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand"), Path.SRC_TEST_JAVA);
		
		JavaType name = new JavaType(entity + "IntegrationTest");
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_TEST_JAVA);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}

		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.test.RooIntegrationTest"), config));

		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<AnnotationMetadata> methodAnnotations = new ArrayList<AnnotationMetadata>();
		methodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
		MethodMetadata method = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("testMarkerMethod"), JavaType.VOID_PRIMITIVE, null, null, methodAnnotations, null, null);
		methods.add(method);

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, annotations, null);
		generateClassFile(details);
	}

	/**
	 * Creates a new data on demand provider for the entity. Silently returns if the DOD already exists.
	 * 
	 * @param entity to produce a data on demand provider for (required)
	 * @param name the name of the new data on demand class (required)
	 * @param path the location for the new data on demand class (required)
	 */
	public void newDod(JavaType entity, JavaType name, Path path) {
		Assert.notNull(entity, "Entity to produce a data on demand provider for is required");
		Assert.notNull(name, "Name of the new data on demand provider is required");
		Assert.notNull(path, "Location of the new data on demand provider is required");

		// Verify the requested entity actually exists as a class and is not abstract
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getEntity(entity);
		Assert.isTrue(classOrInterfaceTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS, "Type " + entity.getFullyQualifiedTypeName() + " is not a class");
		Assert.notNull(MemberFindingUtils.getDeclaredTypeAnnotation(classOrInterfaceTypeDetails, new JavaType("javax.persistence.Entity")), "Type " + entity.getFullyQualifiedTypeName() + " must be an @Entity");
		
		// Everything is OK to proceed
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> dodConfig = new ArrayList<AnnotationAttributeValue<?>>();
		dodConfig.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.dod.RooDataOnDemand"), dodConfig));

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, null, null, null, null, annotations, null);
		generateClassFile(details);
	}
	
	/**
	 * @param entity the entity to lookup required 
	 * @return the type details (never null; throws an exception if it cannot be obtained or parsed)
	 */
	private ClassOrInterfaceTypeDetails getEntity(JavaType entity) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		return (ClassOrInterfaceTypeDetails) ptd;
	}
}
