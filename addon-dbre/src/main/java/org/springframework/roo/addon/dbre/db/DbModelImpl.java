package org.springframework.roo.addon.dbre.db;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.DbrePath;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link DbModel).
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbModelImpl implements DbModel {
	private static final String[] TYPES = { TableType.TABLE.name(), TableType.VIEW.name() };
	private static final String PACKAGE = "package";
	@Reference private PropFileOperations propFileOperations;
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	private Set<Table> tables;
	private JavaPackage javaPackage;

	public String getDbMetadata() {
		return getDbMetadata(new IdentifiableTable(null, null, null));
	}

	public String getDbMetadata(IdentifiableTable identifiableTable) {
		populateTablesFromDb(identifiableTable.getCatalog(), identifiableTable.getSchema(), identifiableTable.getTable());

		StringBuilder builder = new StringBuilder();
		if (!tables.isEmpty()) {
			for (Table table : tables) {
				builder.append(table.toString());
				builder.append(System.getProperty("line.separator"));
			}
		}
		return builder.toString();
	}

	public JavaPackage getJavaPackage() {
		return this.javaPackage;
	}

	public void setJavaPackage(JavaPackage javaPackage) {
		this.javaPackage = javaPackage;
	}

	public void serialize() {
		populateTablesFromDb(null, null, null);

		String dbrePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
		MutableFile dbreMutableFile = null;

		Document dbre;
		try {
			if (fileManager.exists(dbrePath)) {
				dbreMutableFile = fileManager.updateFile(dbrePath);
				dbre = XmlUtils.getDocumentBuilder().parse(dbreMutableFile.getInputStream());
			} else {
				dbreMutableFile = fileManager.createFile(dbrePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), DbrePath.DBRE_XML_TEMPLATE.getPath());
				Assert.notNull(templateInputStream, "Could not acquire dbre xml template");
				dbre = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dbMetadataElement = dbre.getDocumentElement();

		// Calculate java package name to store new entities in
		String packageName;
		if (javaPackage != null) {
			packageName = javaPackage.getFullyQualifiedPackageName();
		} else if (StringUtils.hasText(dbMetadataElement.getAttribute(PACKAGE))) {
			packageName = dbMetadataElement.getAttribute(PACKAGE);
		} else {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			packageName = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName();
		}
		dbMetadataElement.setAttribute(PACKAGE, packageName);

		Set<String> tableData = new HashSet<String>();

		// Add or update tables in xml file
		for (Table table : getTables()) {
			String tableId = table.getIdentifiableTable().getId();
			tableData.add(tableId);
			String tableName = table.getIdentifiableTable().getTable();
			String tableXPath = getSpecifiedTableXPath(tableName);
			Element tableElement = XmlUtils.findFirstElement(tableXPath, dbMetadataElement);
			if (tableElement == null) {
				// Create new table in xml
				tableElement = dbre.createElement("table");
			}
			tableElement.setAttribute("id", tableId);
			tableElement.setAttribute("name", tableName);
			tableElement.setAttribute("catalog", table.getIdentifiableTable().getCatalog());
			tableElement.setAttribute("schema", table.getIdentifiableTable().getSchema());
			tableElement.setAttribute("tableType", table.getIdentifiableTable().getTableType().name());

			// Iterate through columns
			for (Column column : table.getColumns()) {
				String columnId = tableId + "." + column.getId();
				tableData.add(columnId);

				String columnName = column.getName();
				Element columnElement = XmlUtils.findFirstElement(tableXPath + "/column[@name = '" + columnName + "']", dbMetadataElement);
				if (columnElement == null) {
					// Create new column for table in xml
					columnElement = dbre.createElement("column");
				}

				columnElement.setAttribute("id", columnId);
				columnElement.setAttribute("name", columnName);
				columnElement.setAttribute("type", column.getType().getName());
				columnElement.setAttribute("typeName", column.getTypeName());
				columnElement.setAttribute("dataType", String.valueOf(column.getDataType()));
				columnElement.setAttribute("columnSize", String.valueOf(column.getColumnSize()));
				columnElement.setAttribute("decimalDigits", String.valueOf(column.getDecimalDigits()));
				columnElement.setAttribute("nullable", String.valueOf(column.isNullable()));
				columnElement.setAttribute("remarks", column.getRemarks());

				tableElement.appendChild(columnElement);
			}

			// Iterate through primary keys
			for (PrimaryKey primaryKey : table.getPrimaryKeys()) {
				String primaryKeyId = tableId + "." + primaryKey.getId();
				tableData.add(primaryKeyId);

				String primaryKeyName = primaryKey.getName();
				Element primaryKeyElement = XmlUtils.findFirstElement(tableXPath + "/primaryKey[@name = '" + primaryKeyName + "']", dbMetadataElement);
				if (primaryKeyElement == null) {
					// Create new primary key for table in xml
					primaryKeyElement = dbre.createElement("primaryKey");
				}
				primaryKeyElement.setAttribute("id", primaryKeyId);
				primaryKeyElement.setAttribute("name", primaryKeyName);
				primaryKeyElement.setAttribute("columnName", primaryKey.getColumnName());
				primaryKeyElement.setAttribute("keySeq", String.valueOf(primaryKey.getKeySeq()));

				tableElement.appendChild(primaryKeyElement);
			}

			// Iterate through foreign keys
			for (ForeignKey foreignKey : table.getForeignKeys()) {
				String foreignKeyId = tableId + "." + foreignKey.getId();
				tableData.add(foreignKeyId);

				String foreignKeyName = foreignKey.getName();
				Element foreignKeyElement = XmlUtils.findFirstElement(tableXPath + "/foreignKey[@name = '" + foreignKeyName + "']", dbMetadataElement);
				if (foreignKeyElement == null) {
					// Create new foreign key for table in xml
					foreignKeyElement = dbre.createElement("foreignKey");
				}
				foreignKeyElement.setAttribute("id", foreignKeyId);
				foreignKeyElement.setAttribute("name", foreignKeyName);
				foreignKeyElement.setAttribute("fkTable", foreignKey.getFkTable());

				tableElement.appendChild(foreignKeyElement);
			}

			// Iterate through indexes
			for (Index index : table.getIndexes()) {
				String indexId = tableId + "." + index.getId();
				tableData.add(indexId);

				String indexName = index.getName();
				Element indexElement = XmlUtils.findFirstElement(tableXPath + "/index[@name = '" + indexName + "']", dbMetadataElement);
				if (indexElement == null) {
					// Create new index for table in xml
					indexElement = dbre.createElement("index");
				}
				indexElement.setAttribute("id", indexId);
				indexElement.setAttribute("name", indexName);
				indexElement.setAttribute("columnName", index.getColumnName());
				indexElement.setAttribute("nonUnique", new Boolean(index.isNonUnique()).toString());
				indexElement.setAttribute("type", String.valueOf(index.getType()));

				tableElement.appendChild(indexElement);
			}

			dbMetadataElement.appendChild(tableElement);
		}

		// Check for deleted tables and columns etc in schema and remove from xml
		List<Element> xmlTables = XmlUtils.findElements(DbrePath.DBRE_TABLE_XPATH.getPath(), dbMetadataElement);
		for (Element tableElement : xmlTables) {
			if (!tableData.contains(tableElement.getAttribute("id"))) {
				dbMetadataElement.removeChild(tableElement);
			} else {
				removeTableAttributes("column", tableData, tableElement);
				removeTableAttributes("primaryKey", tableData, tableElement);
				removeTableAttributes("foreignKey", tableData, tableElement);
				removeTableAttributes("index", tableData, tableElement);
			}
		}

		XmlUtils.removeTextNodes(dbMetadataElement);
		XmlUtils.writeXml(dbreMutableFile.getOutputStream(), dbre);
	}

	public void deserialize() {
		String dbrePath = DbrePath.DBRE_XML_FILE.getPath();
		File dbreFile = new File(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, dbrePath));
		if (dbreFile.exists()) {
			try {
				Document dbre = XmlUtils.getDocumentBuilder().parse(dbreFile);
				populateTablesFromXml(dbre);
			} catch (Exception e) {
				throw new IllegalStateException("Unable to parse " + dbrePath, e);
			}
		} else {
			throw new IllegalStateException(dbrePath + " does not exist");
		}
	}

	public Set<Table> getTables() {
		return this.tables;
	}

	private void populateTablesFromDb(String catalog, String schema, String table) {
		this.tables = new HashSet<Table>();

		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		DbConnectionProvider connectionProvider = new DbConnectionProviderImpl(map);
		Connection connection = null;
		try {
			connection = connectionProvider.getConnection();
			if (connection == null) {
				throw new IllegalStateException("Failed to get database connection");
			}
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			ResultSet rs = null;
			try {
				rs = getTablesRs(catalog, schema, table, databaseMetaData);
				while (rs.next()) {
					IdentifiableTable identifiableTable = new IdentifiableTable(rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), TableType.valueOf(rs.getString("TABLE_TYPE")));
					tables.add(new JdbcTableImpl(identifiableTable,  databaseMetaData));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		} finally {
			if (connection != null) {
				connectionProvider.closeConnection(connection);
			}
		}
	}

	private void populateTablesFromXml(Document dbre) {
		Assert.notNull(dbre, "Document is null");
		this.tables = new HashSet<Table>();

		Element dbMetadataElement = dbre.getDocumentElement();
		setJavaPackage(new JavaPackage(dbMetadataElement.getAttribute("package")));
		
		List<Element> tableElements = XmlUtils.findElements(DbrePath.DBRE_TABLE_XPATH.getPath(), dbMetadataElement);
		for (Element tableElement : tableElements) {
			String catalog = tableElement.getAttribute("catalog");
			String schema = tableElement.getAttribute("schema");
			String table = tableElement.getAttribute("name");
			TableType tableType = TableType.valueOf(tableElement.getAttribute("tableType"));

			tables.add(new XmlTableImpl(new IdentifiableTable(catalog, schema, table, tableType), tableElement));
		}
	}

	private ResultSet getTablesRs(String catalog, String schema, String table, DatabaseMetaData databaseMetaData) throws SQLException {
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getTables(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schema), StringUtils.toUpperCase(table), TYPES);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getTables(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schema), StringUtils.toLowerCase(table), TYPES);
		} else {
			rs = databaseMetaData.getTables(catalog, schema, table, TYPES);
		}
		return rs;
	}

	private void removeTableAttributes(String tableAttribute, Set<String> tableData, Element tableElement) {
		NodeList nodeList = tableElement.getElementsByTagName(tableAttribute);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (!tableData.contains(element.getAttribute("id"))) {
					tableElement.removeChild(element);
				}
			}
		}
	}

	private String getSpecifiedTableXPath(String tableName) {
		return DbrePath.DBRE_TABLE_XPATH.getPath() + "[@name = '" + tableName + "']";
	}
}
