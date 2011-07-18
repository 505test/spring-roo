package org.springframework.roo.addon.dbre;

import java.util.Set;

import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
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
	 * Locates the table using the presented ClassOrInterfaceTypeDetails.
	 * 
	 * <p>
	 * The search for the table names starts on the @Table annotation and if not present, the
	 * {@link RooEntity @RooEntity} "table" attribute is checked. If not present on either, the method returns null.
	 * 
	 * @param classOrInterfaceTypeDetails the type to search (required)
	 * @return the table (if known) or null (if not found)
	 */
	public static String getTableName(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		Assert.notNull(classOrInterfaceTypeDetails, "ClassOrInterfaceTypeDetails type required");
		// Try to locate a table name, which can be specified either via the "name" attribute on
		// @Table, eg @Table(name = "foo") or via the "table" attribute on @RooEntity, eg @RooEntity(table = "foo")
		return getTableOrSchemaName(classOrInterfaceTypeDetails, "name", "table");
	}

	/**
	 * Locates the table's schema using the presented ClassOrInterfaceTypeDetails.
	 * 
	 * <p>
	 * The search for the table names starts on the @Table annotation and if not present, the
	 * {@link RooEntity @RooEntity} "table" attribute is checked. If not present on either, the method returns null.
	 * 
	 * @param classOrInterfaceTypeDetails the type to search (required) 
	 * @return the schema name (if known) or null (if not found)
	 */
	public static String getSchemaName(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		Assert.notNull(classOrInterfaceTypeDetails, "ClassOrInterfaceTypeDetails type required");
		// Try to locate a schema name, which can be specified either via the "schema" attribute on
		// @Table, eg @Table(schema = "foo") or via the "schema" attribute on @RooEntity, eg @RooEntity(schema = "foo")
		return getTableOrSchemaName(classOrInterfaceTypeDetails, "schema", "schema");
	}
	
	private static String getTableOrSchemaName(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, String tableAttribute, String rooEntityAttraibute) {
		String attributeValue = null;

		AnnotationMetadata tableAnnotation = MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, new JavaType("javax.persistence.Table"));
		if (tableAnnotation != null) {
			AnnotationAttributeValue<?> attribute = tableAnnotation.getAttribute(new JavaSymbolName(tableAttribute));
			if (attribute != null) {
				attributeValue = (String) attribute.getValue();
			}
		}

		if (!StringUtils.hasText(attributeValue)) {
			// The search continues...
			AnnotationMetadata rooEntityAnnotation = MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, new JavaType("org.springframework.roo.addon.entity.RooEntity"));
			if (rooEntityAnnotation != null) {
				AnnotationAttributeValue<?> attribute = rooEntityAnnotation.getAttribute(new JavaSymbolName(rooEntityAttraibute));
				if (attribute != null) {
					attributeValue = (String) attribute.getValue();
				}
			}
		}

		return attributeValue;
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
