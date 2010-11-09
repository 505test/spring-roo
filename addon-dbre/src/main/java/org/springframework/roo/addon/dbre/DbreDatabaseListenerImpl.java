package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.Identifier;
import org.springframework.roo.addon.entity.IdentifierMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Responds to discovery of database structural information from {@link DbreModelService} and creates and manages entities based on this.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class DbreDatabaseListenerImpl extends AbstractHashCodeTrackingMetadataNotifier implements DbreDatabaseListener {
	private static final String IDENTIFIER_TYPE = "identifierType";
	private static final String VERSION_FIELD = "versionField";
	private static final String VERSION = "version";
	private static final String PRIMARY_KEY_SUFFIX = "PK";
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	@Reference private DbreModelService dbreModelService;
	@Reference private DbreTypeResolutionService dbreTypeResolutionService;
	@Reference private Shell shell;
	private Map<JavaType, List<Identifier>> identifierResults = null;
	private JavaPackage destinationPackage = null;

	// This method will be called when the database becomes available for the first time and the rest of Roo has started up OK
	public void notifyDatabaseRefreshed(Database newDatabase) {
		processDatabase(newDatabase);
	}

	private void processDatabase(Database database) {
		if (database == null) {
			return;
		}
		if (database != null && database.hasTables()) {
			identifierResults = new HashMap<JavaType, List<Identifier>>();
			reverseEngineer(database);
		} else {
			identifierResults = null;
			deleteManagedTypes();
		}
	}

	private void reverseEngineer(Database database) {
		// Lookup the relevant destination package if not explicitly given
		SortedSet<JavaType> managedEntityTypes = dbreTypeResolutionService.getManagedEntityTypes();
		JavaPackage destinationToUse = destinationPackage;
		if (destinationToUse == null) {
			if (!managedEntityTypes.isEmpty()) {
				// Take the package of the first one
				destinationToUse = managedEntityTypes.first().getPackage();
			}
		}
		// Fallback to project's top level package
		if (destinationToUse == null) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			destinationToUse = projectMetadata.getTopLevelPackage();
		}

		Set<Table> tables = database.getTables();
		for (Table table : tables) {
			// Don't create types from join tables in many-to-many associations
			if (!database.isJoinTable(table)) {
				JavaType javaType = dbreTypeResolutionService.findTypeForTableName(table.getName(), destinationToUse);
				if (javaType == null) {
					createNewManagedEntityFromTable(table, destinationToUse);
				} else {
					updateExistingManagedEntity(javaType, table);
				}
			}
		}

		deleteManagedTypesNotInModel(tables, managedEntityTypes);

		for (JavaType managedEntityType : managedEntityTypes) {
			String dbreMid = DbreMetadata.createIdentifier(managedEntityType, Path.SRC_MAIN_JAVA);
			MetadataItem metadataItem = metadataService.get(dbreMid, true);
			if (metadataItem != null) {
				notifyIfRequired(metadataItem);
			}
		}
		
		SortedSet<JavaType> managedIdentifierTypes = dbreTypeResolutionService.getManagedIdentifierTypes();
		for (JavaType managedIdentifierType : managedIdentifierTypes) {
			String identifierMid = IdentifierMetadata.createIdentifier(managedIdentifierType, Path.SRC_MAIN_JAVA);
			MetadataItem metadataItem = metadataService.get(identifierMid, true);
			if (metadataItem != null) {
				notifyIfRequired(metadataItem);
			}
		}
	}

	private void createNewManagedEntityFromTable(Table table, JavaPackage javaPackage) {
		JavaType javaType = dbreTypeResolutionService.suggestTypeNameForNewTable(table.getName(), javaPackage);

		// Create type annotations for new entity
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
		
		// Find primary key from db metadata and add identifier attributes to @RooEntity
		AnnotationMetadataBuilder rooEntityBuilder = new AnnotationMetadataBuilder(new JavaType(RooEntity.class.getName()));
		manageEntityIdentifier(javaType, rooEntityBuilder, new HashSet<JavaSymbolName>(), table);

		if (!hasVersionField(table)) {
			rooEntityBuilder.addStringAttribute(VERSION_FIELD, "");
		}
		
		if (StringUtils.hasText(table.getName())) {
			rooEntityBuilder.addStringAttribute("table", table.getName());
		}
		if (StringUtils.hasText(table.getCatalog())) {
			rooEntityBuilder.addStringAttribute("catalog", table.getCatalog());
		}
		if (table.getSchema() != null && StringUtils.hasText(table.getSchema().getName())) {
			rooEntityBuilder.addStringAttribute("schema", table.getSchema().getName());
		}

		annotations.add(rooEntityBuilder);

		// Add @RooDbManaged
		AnnotationMetadataBuilder rooDbManagedBuilder = new AnnotationMetadataBuilder(new JavaType(RooDbManaged.class.getName()));
		rooDbManagedBuilder.addBooleanAttribute("automaticallyDelete", true);
		annotations.add(rooDbManagedBuilder);

		JavaType superclass = new JavaType("java.lang.Object");
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		// Create entity class
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, javaType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		typeDetailsBuilder.setAnnotations(annotations);
	
		classpathOperations.generateClassFile(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + javaType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
		shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
	}

	private void updateExistingManagedEntity(JavaType javaType, Table table) {
		// Update changes to @RooEntity attributes
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		if (MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, new JavaType(RooDbManaged.class.getName())) == null) {
			return;
		}

		AnnotationMetadata rooEntityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, new JavaType(RooEntity.class.getName()));
		Assert.notNull(rooEntityAnnotation, "@RooEntity annotation not found on " + javaType.getFullyQualifiedTypeName());
		AnnotationMetadataBuilder rooEntityBuilder = new AnnotationMetadataBuilder(rooEntityAnnotation);

		// Get new @RooEntity attributes
		Set<JavaSymbolName> attributesToDeleteIfPresent = new HashSet<JavaSymbolName>();

		manageEntityIdentifier(javaType, rooEntityBuilder, attributesToDeleteIfPresent, table);

		// Manage versionField attribute
		AnnotationAttributeValue<?> versionFieldAttribute = rooEntityAnnotation.getAttribute(new JavaSymbolName(VERSION_FIELD));
		if (versionFieldAttribute != null) {
			String versionFieldValue = (String) versionFieldAttribute.getValue();
			if (hasVersionField(table) && (!StringUtils.hasText(versionFieldValue) || VERSION.equals(versionFieldValue))) {
				attributesToDeleteIfPresent.add(new JavaSymbolName(VERSION_FIELD));
			}
		} else {
			if (hasVersionField(table)) {
				attributesToDeleteIfPresent.add(new JavaSymbolName(VERSION_FIELD));
			} else {
				rooEntityBuilder.addStringAttribute(VERSION_FIELD, "");
			}
		}
				
		// Update the annotation on disk
		mutableTypeDetails.updateTypeAnnotation(rooEntityBuilder.build(), attributesToDeleteIfPresent);
	}

	private boolean hasVersionField(Table table) {
		for (Column column : table.getColumns()) {
			if (VERSION.equalsIgnoreCase(column.getName())) {
				return true;
			}
		}
		return false;
	}
	
	private void manageEntityIdentifier(JavaType javaType, AnnotationMetadataBuilder rooEntityBuilder, Set<JavaSymbolName> attributesToDeleteIfPresent, Table table) {
		JavaType identifierType = getIdentifierType(javaType);
		PhysicalTypeMetadata identifierPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);
		
		// Process primary keys and add 'identifierType' attribute
		int pkCount = table.getPrimaryKeyCount();
		if (pkCount == 1) {
			// Table has one primary key
			// Check for redundant, managed identifier class and delete if found
			if (isIdentifierDeletable(identifierType)) {
				deleteManagedType(identifierType);
			}
			
			attributesToDeleteIfPresent.add(new JavaSymbolName(IDENTIFIER_TYPE));
			
			// We don't need a PK class, so we just tell the EntityMetadataProvider via IdentifierService the column name, field type and field name to use
			List<Identifier> identifiers = getIdentifiersFromPrimaryKeys(table.getPrimaryKeys());
			identifierResults.put(javaType, identifiers);
		} else if (pkCount == 0 || pkCount > 1) {
			// Table has either no primary keys or more than one primary key so create a composite key

			// Check if identifier class already exists and if not, create it
			if (identifierPhysicalTypeMetadata == null || !identifierPhysicalTypeMetadata.isValid() || !(identifierPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(identifierType);
			}

			rooEntityBuilder.addClassAttribute(IDENTIFIER_TYPE, identifierType);

			// We need a PK class, so we tell the IdentifierMetadataProvider via IdentifierService the various column names, field types and field names to use
			// For tables with no primary keys, create a composite key using all the table's columns
			List<Identifier> identifiers = pkCount == 0 ? getIdentifiersFromColumns(table.getColumns()) : getIdentifiersFromPrimaryKeys(table.getPrimaryKeys());
			identifierResults.put(identifierType, identifiers);
		}
	}

	public List<Identifier> getIdentifiers(JavaType pkType) {
		if (identifierResults == null) {
			// Need to populate the identifier results before returning from this method
			processDatabase(dbreModelService.getDatabase(null));
		}
		if (identifierResults == null) {
			// It's still null, so maybe the DBRE XML file isn't available at this time or similar
			return null;
		}
		return identifierResults.get(pkType);
	}

	private void createIdentifierClass(JavaType identifierType) {
		List<AnnotationMetadataBuilder> identifierAnnotations = new ArrayList<AnnotationMetadataBuilder>();

		AnnotationMetadataBuilder identifierBuilder = new AnnotationMetadataBuilder(new JavaType(RooIdentifier.class.getName()));
		identifierBuilder.addBooleanAttribute("dbManaged", true);
		identifierAnnotations.add(identifierBuilder);

		// Produce identifier itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder idTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL, identifierType, PhysicalTypeCategory.CLASS);
		idTypeDetailsBuilder.setAnnotations(identifierAnnotations);
		classpathOperations.generateClassFile(idTypeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + identifierType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
		shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
	}
	
	private List<Identifier> getIdentifiersFromPrimaryKeys(Set<Column> primaryKeys) {
		return getIdentifiersFromColumns(primaryKeys);
	}

	private List<Identifier> getIdentifiersFromColumns(Set<Column> columns) {
		List<Identifier> result = new ArrayList<Identifier>();

		// Add fields to the identifier class
		for (Column column : columns) {
			JavaSymbolName fieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(column.getName()));
			JavaType fieldType = column.getType().getJavaType();
			String columnName = column.getName();
			result.add(new Identifier(fieldName, fieldType, columnName));
		}

		return result;
	}
	
	private void deleteManagedTypes(JavaType javaType) {
		if (isEntityDeletable(javaType)) {
			deleteManagedType(javaType);

			JavaType identifierType = getIdentifierType(javaType);
			Set<JavaType> managedIdentifierTypes = dbreTypeResolutionService.getManagedIdentifierTypes();
			if (managedIdentifierTypes.contains(identifierType) && isIdentifierDeletable(identifierType)) {
				deleteManagedType(identifierType);
			}
		}
	}

	private void deleteManagedTypes() {
		for (JavaType javaType : dbreTypeResolutionService.getManagedEntityTypes()) {
			deleteManagedTypes(javaType);
		}
	}

	private void deleteManagedTypesNotInModel(Set<Table> tables, SortedSet<JavaType> managedEntityTypes) {
		for (JavaType javaType : managedEntityTypes) {
			// Check for existence of entity from table model and delete if not in database model
			if (!isDetectedEntityInModel(javaType, tables)) {
				deleteManagedTypes(javaType);
			}
		}
	}

	private boolean isDetectedEntityInModel(JavaType javaType, Set<Table> tables) {
		String tableName = getTableFromRooEntity(javaType);
		for (Table table : tables) {
			if (table.getName().equals(tableName)) {
				return true;
			}
		}
		return false;
	}

	private String getTableFromRooEntity(JavaType javaType) {
		AnnotationMetadata rooEntityAnnotation = getRooEntityAnnotation(javaType);
		if (rooEntityAnnotation != null) {
			AnnotationAttributeValue<?> tableAttribute = rooEntityAnnotation.getAttribute(new JavaSymbolName("table"));
			if (tableAttribute != null) {
				return (String) tableAttribute.getValue();
			}
		}
		return null;
	}

	private boolean isEntityDeletable(JavaType javaType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		if (governorPhysicalTypeMetadata == null) {
			return false;
		}

		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		AnnotationMetadata dbManagedAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName()));
		AnnotationAttributeValue<?> attribute = null;
		if (dbManagedAnnotation == null || (attribute = dbManagedAnnotation.getAttribute(new JavaSymbolName("automaticallyDelete"))) == null || !(Boolean) attribute.getValue()) {
			return false;
		}

		// Check type annotations
		List<? extends AnnotationMetadata> typeAnnotations = typeDetails.getAnnotations();

		boolean hasRequiredAnnotations = true;
		Iterator<? extends AnnotationMetadata> typeAnnotationIterator = typeAnnotations.iterator();
		while (hasRequiredAnnotations && typeAnnotationIterator.hasNext()) {
			JavaType annotationType = typeAnnotationIterator.next().getAnnotationType();
			hasRequiredAnnotations &= (annotationType.getFullyQualifiedTypeName().equals(RooDbManaged.class.getName()) || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.javabean.RooJavaBean") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.tostring.RooToString") || annotationType.getFullyQualifiedTypeName().equals(RooEntity.class.getName()));
		}

		if (!hasRequiredAnnotations || typeAnnotations.size() != 4) {
			return false;
		}

		// Finally, check for added constructors, fields and methods
		return typeDetails.getDeclaredConstructors().isEmpty() && typeDetails.getDeclaredFields().isEmpty() && typeDetails.getDeclaredMethods().isEmpty();
	}

	private boolean isIdentifierDeletable(JavaType identifierType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);
		if (governorPhysicalTypeMetadata == null) {
			return false;
		}

		// Check for added constructors, fields and methods
		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		return typeDetails.getDeclaredConstructors().isEmpty() && typeDetails.getDeclaredFields().isEmpty() && typeDetails.getDeclaredMethods().isEmpty();
	}

	private void deleteManagedType(JavaType javaType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		if (governorPhysicalTypeMetadata != null) {
			String filePath = governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath();
			if (fileManager.exists(filePath)) {
				fileManager.delete(filePath);
				shell.flash(Level.FINE, "Deleted " + javaType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
			}
			
			shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
		}
	}

	private JavaType getIdentifierType(JavaType javaType) {
		AnnotationMetadata rooEntityAnnotation = getRooEntityAnnotation(javaType);
		if (rooEntityAnnotation != null) {
			AnnotationAttributeValue<?> identifierTypeAttribute = rooEntityAnnotation.getAttribute(new JavaSymbolName(IDENTIFIER_TYPE));
			if (identifierTypeAttribute != null) {
				// Attribute identifierType exists so get the value
				JavaType identifierType = (JavaType) identifierTypeAttribute.getValue();
				if (identifierType != null && !identifierType.getFullyQualifiedTypeName().startsWith("java.lang")) {
					return identifierType;
				}
			}
		}

		// @RooEntity identifierType attribute does not exist or is not a simple type, so return a default
		return new JavaType(javaType.getFullyQualifiedTypeName() + PRIMARY_KEY_SUFFIX);
	}

	private AnnotationMetadata getRooEntityAnnotation(JavaType javaType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		if (governorPhysicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
			return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooEntity.class.getName()));
		}
		return null;
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}

	public void setDestinationPackage(JavaPackage destinationPackage) {
		this.destinationPackage = destinationPackage;
	}
}
