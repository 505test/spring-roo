package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jvnet.inflector.Noun;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.ForeignKey;
import org.springframework.roo.addon.dbre.model.JoinTable;
import org.springframework.roo.addon.dbre.model.Reference;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.IdentifierMetadata;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDbManaged}.
 * <p>
 * Creates and manages entity relationships, such as many-valued and single-valued associations.
 * <p>
 * One-to-many and one-to-one associations are created based on the following laws:
 * <ul>
 * <li>Primary Key (PK) - Foreign Key (FK) LAW #1: If the foreign key column is part of the primary key (or part of an index) then the relationship between the tables will be one to many (1:M).
 * <li>Primary Key (PK) - Foreign Key (FK) LAW #2: If the foreign key column represents the entire primary key (or the entire index) then the relationship between the tables will be one to one (1:1).
 * </ul>
 * <p>
 * Many-to-many associations are created if a join table is detected. To be identified as a many-to-many join table, the table must have have exactly two primary keys and have exactly two foreign-keys
 * pointing to other entity tables and have no other columns.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DbreMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = DbreMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType ID = new JavaType("javax.persistence.Id");
	private static final JavaType COLUMN = new JavaType("javax.persistence.Column");
	private static final JavaType ONE_TO_ONE = new JavaType("javax.persistence.OneToOne");
	private static final JavaType ONE_TO_MANY = new JavaType("javax.persistence.OneToMany");
	private static final JavaType MANY_TO_ONE = new JavaType("javax.persistence.ManyToOne");
	private static final JavaType MANY_TO_MANY = new JavaType("javax.persistence.ManyToMany");
	private static final JavaType JOIN_COLUMN = new JavaType("javax.persistence.JoinColumn");
	private static final JavaType JOIN_COLUMNS = new JavaType("javax.persistence.JoinColumns");
	private static final String NAME = "name";
	private static final String VALUE = "value";
	private static final String MAPPED_BY = "mappedBy";
	private static final String REFERENCED_COLUMN = "referencedColumnName";

	private EntityMetadata entityMetadata;
	private IdentifierMetadata identifierMetadata;
	private Set<ClassOrInterfaceTypeDetails> managedEntities;

	public DbreMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata entityMetadata, IdentifierMetadata identifierMetadata, Set<ClassOrInterfaceTypeDetails> managedEntities, Database database) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(managedEntities, "Managed entities required");

		this.entityMetadata = entityMetadata;
		this.identifierMetadata = identifierMetadata;
		this.managedEntities = managedEntities;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooDbManaged.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		Table table = database.findTable(DbreTypeUtils.getTableName(governorTypeDetails));
		if (table == null) {
			// System.out.println("Table for type " + javaType.getFullyQualifiedTypeName() + " not found " + System.nanoTime());
			return;
		}

		// Add fields for many-valued associations with many-to-many multiplicity
		addManyToManyFields(database, table);

		// Add fields for single-valued associations to other entities that have one-to-one multiplicity
		addOneToOneFields(database, table);

		// Add fields for many-valued associations with one-to-many multiplicity
		addOneToManyFields(database, table);

		// Add fields for single-valued associations to other entities that have many-to-one multiplicity
		addManyToOneFields(database, table);

		// Add remaining fields from columns
		addOtherFields(table);

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private void addManyToManyFields(Database database, Table table) {
		Map<Table, Integer> owningSideTables = new LinkedHashMap<Table, Integer>();

		for (JoinTable joinTable : database.getJoinTables()) {
			String errMsg = "' in join table '" + joinTable.getTableName() + "' for many-to-many relationship could not be found. Note table names are case sensitive in some databases such as MySQL.";

			Table owningSideTable = joinTable.getOwningSideTable();
			Assert.notNull(owningSideTable, "Owning-side table '" + joinTable.getOwningSideTableName() + errMsg);

			Table inverseSideTable = joinTable.getInverseSideTable();
			Assert.notNull(inverseSideTable, "Inverse-side table '" + joinTable.getInverseSideTableName() + errMsg);

			Integer tableCount = owningSideTables.containsKey(owningSideTable) ? owningSideTables.get(owningSideTable) + 1 : 0;
			owningSideTables.put(owningSideTable, tableCount);
			String fieldSuffix = owningSideTables.get(owningSideTable) > 0 ? String.valueOf(owningSideTables.get(owningSideTable)) : "";

			boolean sameTable = joinTable.isOwningSideSameAsInverseSide();

			if (owningSideTable.equals(table)) {
				JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(DbreTypeUtils.suggestFieldName(inverseSideTable.getName())) + (sameTable ? "1" : fieldSuffix));
				FieldMetadata field = getManyToManyOwningSideField(fieldName, joinTable, governorTypeDetails.getName().getPackage());
				addToBuilder(field);
			}

			if (inverseSideTable.equals(table)) {
				JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(DbreTypeUtils.suggestFieldName(owningSideTable.getName())) + (sameTable ? "2" : fieldSuffix));
				JavaSymbolName mappedByFieldName = new JavaSymbolName(getInflectorPlural(DbreTypeUtils.suggestFieldName(inverseSideTable.getName())) + (sameTable ? "1" : fieldSuffix));
				FieldMetadata field = getManyToManyInverseSideField(fieldName, mappedByFieldName, joinTable, governorTypeDetails.getName().getPackage());
				addToBuilder(field);
			}
		}
	}

	private void addOneToOneFields(Database database, Table table) {
		// Add unique one-to-one fields
		Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (ForeignKey foreignKey : table.getImportedKeys()) {
			if (isOneToOne(table, foreignKey)) {
				String foreignTableName = foreignKey.getForeignTableName();
				Short keySequence = foreignKey.getKeySequence();
				String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
				JavaSymbolName fieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(foreignTableName) + fieldSuffix);
				JavaType fieldType = DbreTypeUtils.findTypeForTableName(managedEntities, foreignTableName);
				Assert.notNull(fieldType, getErrorMsg(foreignTableName, table.getName()));

				// Fields are stored in a field-keyed map first before adding them to the builder.
				// This ensures the fields from foreign keys with multiple columns will only get created once.
				FieldMetadata field = getOneToOneOrManyToOneField(fieldName, fieldType, foreignKey.getReferences(), ONE_TO_ONE, false);
				uniqueFields.put(fieldName, field);
			}
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);

			// Exclude these fields in @RooToString to avoid circular references - ROO-1399
			excludeFieldsInToStringAnnotation(field.getFieldName().getSymbolName());
		}

		// Add one-to-one mapped-by fields
		if (!database.isJoinTable(table)) {
			for (ForeignKey exportedKey : table.getExportedKeys()) {
				if (!database.isJoinTable(exportedKey.getForeignTable())) {
					String foreignTableName = exportedKey.getForeignTableName();
					Table foreignTable = database.findTable(foreignTableName);
					Assert.notNull(foreignTable, "Related table '" + foreignTableName + "' could not be found but was referenced by table '" + table.getName() + "'");

					if (isOneToOne(foreignTable, foreignTable.getImportedKey(exportedKey.getName()))) {
						Short keySequence = exportedKey.getKeySequence();
						String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
						JavaSymbolName fieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(foreignTableName) + fieldSuffix);

						JavaType fieldType = DbreTypeUtils.findTypeForTableName(managedEntities, foreignTableName);
						Assert.notNull(fieldType, getErrorMsg(foreignTableName));

						// Check for existence of same field - ROO-1691
						while (true) {
							if (!hasFieldInItd(fieldName)) {
								break;
							}
							fieldName = new JavaSymbolName(fieldName.getSymbolName() + "_");
						}

						JavaSymbolName mappedByFieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(table.getName()) + fieldSuffix);

						FieldMetadata field = getOneToOneMappedByField(fieldName, fieldType, mappedByFieldName);
						addToBuilder(field);
					}
				}
			}
		}
	}

	private void addOneToManyFields(Database database, Table table) {
		if (!database.isJoinTable(table)) {
			for (ForeignKey exportedKey : table.getExportedKeys()) {
				if (!database.isJoinTable(exportedKey.getForeignTable())) {
					String foreignTableName = exportedKey.getForeignTableName();
					Table foreignTable = database.findTable(foreignTableName);
					Assert.notNull(foreignTable, "Related table '" + foreignTableName + "' could not be found but was referenced by table '" + table.getName() + "'");

					if (!isOneToOne(foreignTable, foreignTable.getImportedKey(exportedKey.getName()))) {
						Short keySequence = exportedKey.getKeySequence();
						String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
						JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(DbreTypeUtils.suggestFieldName(foreignTableName)) + fieldSuffix);
						JavaSymbolName mappedByFieldName = null;
						if (exportedKey.getReferenceCount() == 1) {
							Reference reference = exportedKey.getReferences().iterator().next();
							mappedByFieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(reference.getForeignColumnName()));
						} else {
							mappedByFieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(table.getName()) + fieldSuffix);
						}

						// Check for existence of same field - ROO-1691
						while (true) {
							if (!hasFieldInItd(fieldName)) {
								break;
							}
							fieldName = new JavaSymbolName(fieldName.getSymbolName() + "_");
						}

						FieldMetadata field = getOneToManyMappedByField(fieldName, mappedByFieldName, foreignTableName);
						addToBuilder(field);
					}
				}
			}
		}
	}

	private void addManyToOneFields(Database database, Table table) {
		// Add unique many-to-one fields
		Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (ForeignKey foreignKey : table.getImportedKeys()) {
			if (!isOneToOne(table, foreignKey)) {
				// Assume many-to-one multiplicity
				JavaSymbolName fieldName = null;
				String foreignTableName = foreignKey.getForeignTableName();
				if (foreignKey.getReferenceCount() == 1) {
					Reference reference = foreignKey.getReferences().iterator().next();
					fieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(reference.getLocalColumnName()));
				} else {
					Short keySequence = foreignKey.getKeySequence();
					String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
					fieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(foreignTableName) + fieldSuffix);
				}
				JavaType fieldType = DbreTypeUtils.findTypeForTableName(managedEntities, foreignTableName);
				Assert.notNull(fieldType, getErrorMsg(foreignTableName, table.getName()));

				// Fields are stored in a field-keyed map first before adding them to the builder.
				// This ensures the fields from foreign keys with multiple columns will only get created once.
				FieldMetadata field = getOneToOneOrManyToOneField(fieldName, fieldType, foreignKey.getReferences(), MANY_TO_ONE, true);
				uniqueFields.put(fieldName, field);
			}
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);
		}
	}

	private FieldMetadata getManyToManyOwningSideField(JavaSymbolName fieldName, JoinTable joinTable, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();
		JavaType element = DbreTypeUtils.findTypeForTableName(managedEntities, joinTable.getInverseSideTable().getName());
		Assert.notNull(element, getErrorMsg(joinTable.getInverseSideTable().getName()));
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.MANY_TO_MANY);

		// Add annotations to field
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

		// Add @ManyToMany annotation
		annotations.add(new AnnotationMetadataBuilder(MANY_TO_MANY));

		// Add @JoinTable annotation
		AnnotationMetadataBuilder joinTableBuilder = new AnnotationMetadataBuilder(new JavaType("javax.persistence.JoinTable"));
		List<AnnotationAttributeValue<?>> joinTableAnnotationAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		joinTableAnnotationAttributes.add(new StringAttributeValue(new JavaSymbolName(NAME), joinTable.getTable().getName()));

		// Add "joinColumns" attribute containing nested @JoinColumn annotations
		List<NestedAnnotationAttributeValue> joinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
		Iterator<ForeignKey> iter = joinTable.getTable().getImportedKeys().iterator();
		Set<Reference> firstKeyReferences = iter.next().getReferences();
		for (Reference reference : firstKeyReferences) {
			AnnotationMetadataBuilder joinColumnBuilder = getJoinColumnAnnotation(reference, (firstKeyReferences.size() > 1));
			joinColumnArrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName(VALUE), joinColumnBuilder.build()));
		}
		joinTableAnnotationAttributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("joinColumns"), joinColumnArrayValues));

		// Add "inverseJoinColumns" attribute containing nested @JoinColumn annotations
		List<NestedAnnotationAttributeValue> inverseJoinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
		Set<Reference> lastLastReferences = iter.next().getReferences();
		for (Reference reference : lastLastReferences) {
			AnnotationMetadataBuilder joinColumnBuilder = getJoinColumnAnnotation(reference, (lastLastReferences.size() > 1));
			inverseJoinColumnArrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName(VALUE), joinColumnBuilder.build()));
		}
		joinTableAnnotationAttributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("inverseJoinColumns"), inverseJoinColumnArrayValues));

		// Add attributes to a @JoinTable annotation builder
		joinTableBuilder.setAttributes(joinTableAnnotationAttributes);
		annotations.add(joinTableBuilder);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldDetails.getFieldName(), fieldDetails.getFieldType());
		return fieldBuilder.build();
	}

	private FieldMetadata getManyToManyInverseSideField(JavaSymbolName fieldName, JavaSymbolName mappedByFieldName, JoinTable joinTable, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();
		JavaType element = DbreTypeUtils.findTypeForTableName(managedEntities, joinTable.getOwningSideTable().getName());
		Assert.notNull(element, getErrorMsg(joinTable.getOwningSideTable().getName()));
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.MANY_TO_MANY);

		// Add annotations to field
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		AnnotationMetadataBuilder manyToManyBuilder = new AnnotationMetadataBuilder(MANY_TO_MANY);
		manyToManyBuilder.addStringAttribute(MAPPED_BY, mappedByFieldName.getSymbolName());
		annotations.add(manyToManyBuilder);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldDetails.getFieldName(), fieldDetails.getFieldType());
		return fieldBuilder.build();
	}

	private FieldMetadata getOneToOneMappedByField(JavaSymbolName fieldName, JavaType fieldType, JavaSymbolName mappedByFieldName) {
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		AnnotationMetadataBuilder oneToOneBuilder = new AnnotationMetadataBuilder(ONE_TO_ONE);
		oneToOneBuilder.addStringAttribute(MAPPED_BY, mappedByFieldName.getSymbolName());
		annotations.add(oneToOneBuilder);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldName, fieldType);
		return fieldBuilder.build();
	}

	private FieldMetadata getOneToOneOrManyToOneField(JavaSymbolName fieldName, JavaType fieldType, Set<Reference> references, JavaType annotationType, boolean referencedColumn) {
		// Add annotations to field
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

		// Add annotation
		annotations.add(new AnnotationMetadataBuilder(annotationType));

		if (references.size() == 1) {
			// Add @JoinColumn annotation
			annotations.add(getJoinColumnAnnotation(references.iterator().next(), referencedColumn, fieldType));
		} else {
			// Add @JoinColumns annotation
			annotations.add(getJoinColumnsAnnotation(references, fieldType));
		}

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldName, fieldType);
		return fieldBuilder.build();
	}

	private void excludeFieldsInToStringAnnotation(String fieldName) {
		PhysicalTypeDetails ptd = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd);
		MutableClassOrInterfaceTypeDetails mutable = (MutableClassOrInterfaceTypeDetails) ptd;

		JavaType toStringType = new JavaType("org.springframework.roo.addon.tostring.RooToString");
		AnnotationMetadata toStringAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, toStringType);
		if (toStringAnnotation != null) {
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			List<StringAttributeValue> ignoreFields = new ArrayList<StringAttributeValue>();

			// Copy the existing attributes, excluding the "ignoreFields" attribute
			boolean alreadyAdded = false;
			AnnotationAttributeValue<?> value = toStringAnnotation.getAttribute(new JavaSymbolName("excludeFields"));
			if (value != null) {
				// Ensure we have an array of strings
				final String errMsg = "Annotation RooToString attribute 'excludeFields' must be an array of strings";
				if (!(value instanceof ArrayAttributeValue<?>)) {
					throw new IllegalStateException(errMsg);
				}

				ArrayAttributeValue<?> arrayVal = (ArrayAttributeValue<?>) value;
				for (Object obj : arrayVal.getValue()) {
					if (!(obj instanceof StringAttributeValue)) {
						throw new IllegalStateException(errMsg);
					}

					StringAttributeValue sv = (StringAttributeValue) obj;
					if (sv.getValue().equals(fieldName)) {
						alreadyAdded = true;
					}
					ignoreFields.add(sv);
				}
			}

			// Add the desired field to ignore to the end
			if (!alreadyAdded) {
				ignoreFields.add(new StringAttributeValue(new JavaSymbolName("ignored"), fieldName));
			}

			attributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("excludeFields"), ignoreFields));
			AnnotationMetadataBuilder toStringAnnotationBuilder = new AnnotationMetadataBuilder(toStringType, attributes);
			mutable.updateTypeAnnotation(toStringAnnotationBuilder.build(), new HashSet<JavaSymbolName>());
		}
	}

	private FieldMetadata getOneToManyMappedByField(JavaSymbolName fieldName, JavaSymbolName mappedByFieldName, String foreignTableName) {
		List<JavaType> params = new ArrayList<JavaType>();

		JavaType element = DbreTypeUtils.findTypeForTableName(managedEntities, foreignTableName);
		Assert.notNull(element, getErrorMsg(foreignTableName));
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.ONE_TO_MANY);

		// Add @OneToMany annotation
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		AnnotationMetadataBuilder oneToManyBuilder = new AnnotationMetadataBuilder(ONE_TO_MANY);
		oneToManyBuilder.addStringAttribute(MAPPED_BY, mappedByFieldName.getSymbolName());
		annotations.add(oneToManyBuilder);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldDetails.getFieldName(), fieldDetails.getFieldType());
		return fieldBuilder.build();
	}

	private AnnotationMetadataBuilder getJoinColumnAnnotation(Reference reference, boolean referencedColumn) {
		return getJoinColumnAnnotation(reference, referencedColumn, null);
	}

	private AnnotationMetadataBuilder getJoinColumnAnnotation(Reference reference, boolean referencedColumn, JavaType fieldType) {
		AnnotationMetadataBuilder joinColumnBuilder = new AnnotationMetadataBuilder(JOIN_COLUMN);
		joinColumnBuilder.addStringAttribute(NAME, reference.getLocalColumn().getEscapedName());

		if (referencedColumn) {
			joinColumnBuilder.addStringAttribute(REFERENCED_COLUMN, reference.getForeignColumn().getEscapedName());
		}

		if (reference.getLocalColumn().isRequired()) {
			joinColumnBuilder.addBooleanAttribute("nullable", false);
		}

		if (fieldType != null) {
			boolean isCompositeKeyColumn = isCompositeKeyColumn(reference.getLocalColumn(), fieldType);
			if (isCompositeKeyColumn || reference.getLocalColumn().isPrimaryKey() || !reference.isInsertableOrUpdatable()) {
				addOtherJoinColumnAttributes(joinColumnBuilder);
			}
		}
		
		return joinColumnBuilder;
	}

	private void addOtherJoinColumnAttributes(AnnotationMetadataBuilder joinColumnBuilder) {
		joinColumnBuilder.addBooleanAttribute("insertable", false);
		joinColumnBuilder.addBooleanAttribute("updatable", false);
	}

	private AnnotationMetadataBuilder getJoinColumnsAnnotation(Set<Reference> references, JavaType fieldType) {
		List<NestedAnnotationAttributeValue> arrayValues = new ArrayList<NestedAnnotationAttributeValue>();

		for (Reference reference : references) {
			AnnotationMetadataBuilder joinColumnAnnotation = getJoinColumnAnnotation(reference, true, fieldType);
			arrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName(VALUE), joinColumnAnnotation.build()));
		}
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName(VALUE), arrayValues));
		return new AnnotationMetadataBuilder(JOIN_COLUMNS, attributes);
	}

	private boolean isOneToOne(Table table, ForeignKey foreignKey) {
		Assert.notNull(table, "Table must not be null in determining a one-to-one relationship");
		Assert.notNull(foreignKey, "Foreign key must not be null in determining a one-to-one relationship");
		boolean equals = table.getPrimaryKeyCount() == foreignKey.getReferenceCount();
		Iterator<Column> primaryKeyIterator = table.getPrimaryKeys().iterator();
		while (equals && primaryKeyIterator.hasNext()) {
			equals &= foreignKey.hasLocalColumn(primaryKeyIterator.next());
		}
		return equals;
	}

	private void addOtherFields(Table table) {
        Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (Column column : table.getColumns()) {
			FieldMetadata field = null;
			String columnName = column.getName();
			JavaSymbolName fieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(columnName));

			boolean isCompositeKeyField = isCompositeKeyField(fieldName);
			boolean isIdFieldDeclaredOnGovernor = isIdField(fieldName) && !column.isPrimaryKey();
			boolean isVersionFieldDeclaredOnGovernor = isCompositeKeyField || isVersionField(columnName);
			boolean isForeignKey = table.findImportedKeyByLocalColumnName(columnName) != null;

			if (isVersionFieldDeclaredOnGovernor && !isIdFieldDeclaredOnGovernor || isForeignKey) {
				continue;
			}

			boolean isEmdeddedIdFieldDeclaredOnGovernor = !isCompositeKeyField && isEmbeddedIdField(fieldName);
			if (isEmdeddedIdFieldDeclaredOnGovernor) {
				fieldName = getUniqueFieldName(fieldName);
			}

			field = getField(fieldName, column);
			uniqueFields.put(fieldName, field);
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);
		}
	}

	private boolean isCompositeKeyField(JavaSymbolName fieldName) {
		List<FieldMetadata> identifierFields = getIdentifierFields(destination);
		for (FieldMetadata field : identifierFields) {
			if (fieldName.equals(field.getFieldName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isCompositeKeyColumn(Column column, JavaType javaType) {
		List<FieldMetadata> identifierFields = getIdentifierFields(javaType);
		for (FieldMetadata field : identifierFields) {
			for (AnnotationMetadata annotation : field.getAnnotations()) {
				if (annotation.getAnnotationType().equals(COLUMN)) {
					AnnotationAttributeValue<?> nameAttribute = annotation.getAttribute(new JavaSymbolName(NAME));
					if (nameAttribute != null) {
						String name = (String) nameAttribute.getValue();
						if (column.getName().equals(name)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private List<FieldMetadata> getIdentifierFields(JavaType javaType) {
		return identifierMetadata != null ? identifierMetadata.getFields() : new ArrayList<FieldMetadata>();
	}

	private boolean isVersionField(String columnName) {
		FieldMetadata versionField = entityMetadata.getVersionField();
		return versionField != null && versionField.getFieldName().getSymbolName().equals(columnName);
	}

	private boolean isIdField(JavaSymbolName fieldName) {
		return isAnnotatedField(fieldName, ID, "@Id");
	}

	private boolean isEmbeddedIdField(JavaSymbolName fieldName) {
		return isAnnotatedField(fieldName, new JavaType("javax.persistence.EmbeddedId"), "@EmbeddedId");
	}

	private boolean isAnnotatedField(JavaSymbolName fieldName, JavaType annotationType, String annotationName) {
		List<FieldMetadata> fields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, annotationType);
		if (fields.size() > 0) {
			Assert.isTrue(fields.size() == 1, "More than one field was annotated with " + annotationName + " in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return fields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}

	private FieldMetadata getField(JavaSymbolName fieldName, Column column) {
		JavaType fieldType = column.getJavaType();

		// Add annotations to field
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

		// Add @Column annotation
		AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
		columnBuilder.addStringAttribute(NAME, column.getEscapedName());
		columnBuilder.addStringAttribute("columnDefinition", column.getTypeName());

		// Add length attribute for Strings
		if (column.getColumnSize() < 4000 && fieldType.equals(JavaType.STRING_OBJECT)) {
			columnBuilder.addIntegerAttribute("length", column.getColumnSize());
		}

		// Add precision and scale attributes for numeric fields
		if (column.getScale() > 0 && (fieldType.equals(JavaType.DOUBLE_OBJECT) || fieldType.equals(JavaType.DOUBLE_PRIMITIVE) || fieldType.equals(new JavaType("java.math.BigDecimal")))) {
			columnBuilder.addIntegerAttribute("precision", column.getColumnSize());
			columnBuilder.addIntegerAttribute("scale", column.getScale());
		}

		// Add unique = true to @Column if applicable
		if (column.isUnique()) {
			columnBuilder.addBooleanAttribute("unique", true);
		}
		
		annotations.add(columnBuilder);

		// Add @NotNull if applicable
		if (column.isRequired()) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.NotNull")));
		}

		// Add JSR 220 @Temporal annotation to date fields
		if (fieldType.equals(new JavaType("java.util.Date"))) {
			AnnotationMetadataBuilder temporalBuilder = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Temporal"));
			temporalBuilder.addEnumAttribute(VALUE, new EnumDetails(new JavaType("javax.persistence.TemporalType"), new JavaSymbolName(column.getJdbcType())));
			annotations.add(temporalBuilder);

			AnnotationMetadataBuilder dateTimeFormatBuilder = new AnnotationMetadataBuilder(new JavaType("org.springframework.format.annotation.DateTimeFormat"));
			dateTimeFormatBuilder.addStringAttribute("style", "S-");
			annotations.add(dateTimeFormatBuilder);
		}

		// Add @Lob for CLOB fields if applicable
		if (column.getJdbcType().equals("CLOB")) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Lob")));
		}

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldName, fieldType);
		return fieldBuilder.build();
	}

	private void addToBuilder(FieldMetadata field) {
		if (field != null && !hasField(field)) {
			builder.addField(field);

			// Check for an existing accessor in the governor or in the entity metadata
			if (!hasAccessor(field)) {
				builder.addMethod(getAccessor(field));
			}

			// Check for an existing mutator in the governor or in the entity metadata
			if (!hasMutator(field)) {
				builder.addMethod(getMutator(field));
			}
		}
	}

	private JavaSymbolName getUniqueFieldName(JavaSymbolName fieldName) {
		int index = -1;
		JavaSymbolName uniqueField = null;
		while (true) {
			// Compute the required field name
			index++;
			String uniqueFieldName = "";
			for (int i = 0; i < index; i++) {
				uniqueFieldName = uniqueFieldName + "_";
			}
			uniqueFieldName = uniqueFieldName + fieldName;
			uniqueField = new JavaSymbolName(uniqueFieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, uniqueField) == null) {
				// Found a usable field name
				break;
			}
		}
		return uniqueField;
	}

	private boolean hasField(FieldMetadata field) {
		// Check governor for field
		if (MemberFindingUtils.getField(governorTypeDetails, field.getFieldName()) != null) {
			return true;
		}

		// Check @Column annotation on fields in governor with same 'name'
		// attribute as the 'name' attribute in the @JoinColumn for the generated field
		List<FieldMetadata> governorFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, COLUMN);
		governorFields.addAll(MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, JOIN_COLUMN));
		for (FieldMetadata governorField : governorFields) {
			for (AnnotationMetadata governorAnnotation : governorField.getAnnotations()) {
				if (governorAnnotation.getAnnotationType().equals(COLUMN) || governorAnnotation.getAnnotationType().equals(JOIN_COLUMN)) {
					AnnotationAttributeValue<?> name = governorAnnotation.getAttribute(new JavaSymbolName(NAME));
					if (name != null) {
						for (AnnotationMetadata annotationMetadata : field.getAnnotations()) {
							if (annotationMetadata.getAnnotationType().equals(JOIN_COLUMN)) {
								AnnotationAttributeValue<?> columnName = annotationMetadata.getAttribute(new JavaSymbolName(NAME));
								if (columnName != null && columnName.equals(name)) {
									return true;
								}
							}
						}
					}
				}
			}
		}

		// Check entity ITD for field
		List<? extends FieldMetadata> itdFields = entityMetadata.getMemberHoldingTypeDetails().getDeclaredFields();
		for (FieldMetadata itdField : itdFields) {
			if (itdField.getFieldName().equals(field.getFieldName())) {
				return true;
			}
		}

		return false;
	}

	private boolean hasFieldInItd(JavaSymbolName fieldName) {
		// Check this ITD for field
		List<FieldMetadataBuilder> declaredFields = builder.getDeclaredFields();
		for (FieldMetadataBuilder declaredField : declaredFields) {
			if (declaredField.getFieldName().equals(fieldName)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAccessor(FieldMetadata field) {
		String requiredAccessorName = getRequiredAccessorName(field);

		// Check governor for accessor method
		if (MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>()) != null) {
			return true;
		}

		// Check entity ITD for accessor method
		List<? extends MethodMetadata> itdMethods = entityMetadata.getMemberHoldingTypeDetails().getDeclaredMethods();
		for (MethodMetadata method : itdMethods) {
			if (method.getMethodName().equals(new JavaSymbolName(requiredAccessorName))) {
				return true;
			}
		}

		return false;
	}

	private MethodMetadata getAccessor(FieldMetadata field) {
		Assert.notNull(field, "Field required");

		// Compute the accessor method name
		String requiredAccessorName = getRequiredAccessorName(field);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), field.getFieldType(), bodyBuilder);
		return methodBuilder.build();
	}

	private String getRequiredAccessorName(FieldMetadata field) {
		String methodName;
		if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
			methodName = "is" + StringUtils.capitalize(field.getFieldName().getSymbolName());
		} else {
			methodName = "get" + StringUtils.capitalize(field.getFieldName().getSymbolName());
		}
		return methodName;
	}

	private boolean hasMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		// Check governor for mutator method
		if (MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), new ArrayList<JavaType>()) != null) {
			return true;
		}

		// Check entity ITD for mutator method
		List<? extends MethodMetadata> itdMethods = entityMetadata.getMemberHoldingTypeDetails().getDeclaredMethods();
		for (MethodMetadata method : itdMethods) {
			if (method.getMethodName().equals(new JavaSymbolName(requiredMutatorName))) {
				return true;
			}
		}

		return false;
	}

	private MethodMetadata getMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(field.getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(field.getFieldName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private String getRequiredMutatorName(FieldMetadata field) {
		return "set" + StringUtils.capitalize(field.getFieldName().getSymbolName());
	}

	private String getInflectorPlural(String term) {
		try {
			return Noun.pluralOf(term, Locale.ENGLISH);
		} catch (RuntimeException re) {
			// Inflector failed (see for example ROO-305), so don't pluralize it
			return term;
		}
	}

	private String getErrorMsg(String tableName) {
		return "Type for table '" + tableName + "' could not be found";
	}

	private String getErrorMsg(String tableName, String referenceTableName) {
		return getErrorMsg(tableName) + " but was referenced by table '" + referenceTableName + "'";
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
