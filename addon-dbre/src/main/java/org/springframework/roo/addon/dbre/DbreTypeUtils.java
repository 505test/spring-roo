package org.springframework.roo.addon.dbre;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides methods to find types based on table names and to suggest type and field 
 * names from table and column names respectively.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class DbreTypeUtils {
	private static final JavaType JPA_TABLE_ANNOTATION = new JavaType("javax.persistence.Table");
	private static final JavaType ROO_ENTITY_ANNOTATION = new JavaType("org.springframework.roo.addon.entity.RooEntity");
	private static final JavaType ROO_JPA_ENTITY_ANNOTATION = new JavaType("org.springframework.roo.addon.entity.RooJpaEntity");

	private static final JavaSymbolName NAME_ATTRIBUTE = new JavaSymbolName("name");
	private static final JavaSymbolName SCHEMA_ATTRIBUTE = new JavaSymbolName("schema");
	private static final JavaSymbolName TABLE_ATTRIBUTE = new JavaSymbolName("table");

	// The annotation attributes from which to read the db table name
	// Linked to preserve the iteration order below
	private static final Map<JavaType, JavaSymbolName> TABLE_ATTRIBUTES = new LinkedHashMap<JavaType, JavaSymbolName>();
	static {
		TABLE_ATTRIBUTES.put(JPA_TABLE_ANNOTATION, NAME_ATTRIBUTE);
		TABLE_ATTRIBUTES.put(ROO_JPA_ENTITY_ANNOTATION, TABLE_ATTRIBUTE);
		TABLE_ATTRIBUTES.put(ROO_ENTITY_ANNOTATION, TABLE_ATTRIBUTE);
	}
	
	// The annotation attributes from which to read the db schema name
	// Linked to preserve the iteration order below
	private static final Map<JavaType, JavaSymbolName> SCHEMA_ATTRIBUTES = new LinkedHashMap<JavaType, JavaSymbolName>();
	static {
		SCHEMA_ATTRIBUTES.put(JPA_TABLE_ANNOTATION, SCHEMA_ATTRIBUTE);
		SCHEMA_ATTRIBUTES.put(ROO_JPA_ENTITY_ANNOTATION, SCHEMA_ATTRIBUTE);
		SCHEMA_ATTRIBUTES.put(ROO_ENTITY_ANNOTATION, SCHEMA_ATTRIBUTE);
	}

	/**
	 * Locates the type associated with the presented table name.
	 * 
	 * @param managedEntities a set of database-managed entities to search (required)
	 * @param tableName the table to locate (required)
	 * @param schemaName the table's schema name
	 * @return the type (if known) or null (if not found)
	 */
	public static JavaType findTypeForTableName(Set<ClassOrInterfaceTypeDetails> managedEntities, String tableName, String schemaName) {
		Assert.notNull(managedEntities, "Set of managed entities required");
		Assert.hasText(tableName, "Table name required");

		for (ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
			String managedSchemaName = getSchemaName(managedEntity);
			if (tableName.equals(getTableName(managedEntity)) && (!DbreModelService.NO_SCHEMA_REQUIRED.equals(managedSchemaName) || schemaName.equals(managedSchemaName))) {
				return managedEntity.getName();
			}
		}

		return null;
	}

	/**
	 * Locates the type associated with the presented table.
	 * 
	 * @param managedEntities a set of database-managed entities to search (required)
	 * @param table the table to locate (required)
	 * @return the type (if known) or null (if not found)
	 */
	public static JavaType findTypeForTable(Set<ClassOrInterfaceTypeDetails> managedEntities, Table table) {
		Assert.notNull(managedEntities, "Set of managed entities required");
		Assert.notNull(table, "Table required");
		return findTypeForTableName(managedEntities, table.getName(), table.getSchema().getName());
	}
	
	/**
	 * Returns the database schema for the given entity.
	 * 
	 * @param entityDetails the type to search (required) 
	 * @return the schema name (if known) or null (if not found)
	 */
	public static String getSchemaName(final MemberHoldingTypeDetails entityDetails) {
		Assert.notNull(entityDetails, "MemberHoldingTypeDetails type required");
		return getFirstNonBlankAttributeValue(entityDetails, SCHEMA_ATTRIBUTES);
	}
	
	/**
	 * Returns the database table for the given entity.
	 * 
	 * @param entityDetails the type to search (required)
	 * @return the table (if known) or null (if not found)
	 */
	public static String getTableName(final MemberHoldingTypeDetails entityDetails) {
		Assert.notNull(entityDetails, "MemberHoldingTypeDetails type required");
		return getFirstNonBlankAttributeValue(entityDetails, TABLE_ATTRIBUTES);
	}
	
	/**
	 * Reads the given attributes of the given annotations on the given type,
	 * returning the first non-blank one found.
	 * 
	 * @param annotatedType the type for which to read the annotations (required)
	 * @param annotationAttributes the annotation/attribute pairs to read for that type
	 * @return <code>null</code> if none of those annotations provide a non-blank schema name
	 */
	private static String getFirstNonBlankAttributeValue(final MemberHoldingTypeDetails annotatedType, final Map<JavaType, JavaSymbolName> annotationAttributes) {
		for (final Entry<JavaType, JavaSymbolName> entry : annotationAttributes.entrySet()) {
			final String attributeValue = getAnnotationAttribute(annotatedType, entry.getKey(), entry.getValue());
			if (StringUtils.hasText(attributeValue)) {
				return attributeValue;
			}
		}
		return null;
	}
	
	/**
	 * Returns the value of the given attribute of the given annotation on the
	 * given type
	 * 
	 * @param <T> the expected annotation value type
	 * @param type the type whose annotations to read (required)
	 * @param annotationType the annotation to read (required)
	 * @param attributeName the annotation attribute to read (required)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getAnnotationAttribute(final MemberHoldingTypeDetails type, final JavaType annotationType, final JavaSymbolName attributeName) {
		final AnnotationMetadata typeAnnotation = MemberFindingUtils.getTypeAnnotation(type, annotationType);
		if (typeAnnotation == null) {
			return null;
		}
		final AnnotationAttributeValue<?> attributeValue = typeAnnotation.getAttribute(attributeName);
		if (attributeValue == null) {
			return null;
		}
		return (T) attributeValue.getValue();
	}
	
	/**
	 * Returns a JavaType given a table identity.
	 * 
	 * @param tableName the table name to convert (required)
	 * @param javaPackage the Java package to use for the type
	 * @return a new JavaType
	 */
	public static JavaType suggestTypeNameForNewTable(String tableName, JavaPackage javaPackage) {
		Assert.hasText(tableName, "Table name required");

		StringBuilder result = new StringBuilder();
		if (javaPackage != null && StringUtils.hasText(javaPackage.getFullyQualifiedPackageName())) {
			result.append(javaPackage.getFullyQualifiedPackageName());
			result.append(".");
		}
		result.append(getName(tableName, false));
		return new JavaType(result.toString());
	}

	/**
	 * Returns a field name for a given database table or column name;
	 * 
	 * @param name the name of the table or column (required)
	 * @return a String representing the table or column
	 */
	public static String suggestFieldName(String name) {
		Assert.hasText(name, "Table or column name required");
		return getName(name, true);
	}

	/**
	 * Returns a field name for a given database table;
	 * 
	 * @param table the the table (required)
	 * @return a String representing the table or column.
	 */
	public static String suggestFieldName(Table table) {
		Assert.notNull(table, "Table required");
		return getName(table.getName(), true);
	}
	
	public static String suggestPackageName(String str) {
		StringBuilder result = new StringBuilder();
		char[] value = str.toCharArray();
		for (int i = 0; i < value.length; i++) {
			char c = value[i];
			if (i == 0 && ('1' == c || '2' == c || '3' == c || '4' == c || '5' == c || '6' == c || '7' == c || '8' == c || '9' == c || '0' == c)) {
				result.append("p");
				result.append(c);
			} else if ('.' == c || '/' == c || ' ' == c || '*' == c || '>' == c || '<' == c || '!' == c || '@' == c || '%' == c || '^' == c ||
				'?' == c || '(' == c || ')' == c || '~' == c || '`' == c || '{' == c || '}' == c || '[' == c || ']' == c ||
				'|' == c || '\\' == c || '\'' == c || '+' == c || '-' == c)  {
				result.append("");
			} else {
				result.append(Character.toLowerCase(c));
			}
		}
		return result.toString();
	}

	private static String getName(String str, boolean isField) {
		StringBuilder result = new StringBuilder();
		boolean isDelimChar = false;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (i == 0) {
				if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
					result.append(isField ? "f" : "T");
					result.append(c);
				} else {
					result.append(isField ? Character.toLowerCase(c) : Character.toUpperCase(c));
				}
				continue;
			} else if (i > 0 && (c == '_' || c == '-' || c == '\\' || c == '/') || c == '.') {
				isDelimChar = true;
				continue;
			}
			
			if (isDelimChar) {
				result.append(Character.toUpperCase(c));
				isDelimChar = false;
			} else {
				if (i > 1 && Character.isLowerCase(str.charAt(i - 1)) && Character.isUpperCase(c)) {
					result.append(c);
				} else {
					result.append(Character.toLowerCase(c));
				}
			}
		}
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(result.toString())) {
			result.append("1");
		}
		return result.toString();
	}
}
