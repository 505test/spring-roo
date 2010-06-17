package org.springframework.roo.addon.dbre.db;

import java.util.List;

import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Represents table metadata from XML.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class XmlTableImpl extends AbstractTable implements Table {

	XmlTableImpl(IdentifiableTable identifiableTable, Element tableElement) {
		super(identifiableTable);
		setColumns(tableElement);
		setPrimaryKeys(tableElement);
		setForeignKeys(tableElement);
		setIndexes(tableElement);
	}

	private void setColumns(Element tableElement) {
		columns.clear();
		
		List<Element> columnElements = XmlUtils.findElements("/column", tableElement);
		for (Element columnElement : columnElements) {
			String name = columnElement.getAttribute("name");
			int dataType = Integer.parseInt(columnElement.getAttribute("dataType"));
			int columnSize = Integer.parseInt(columnElement.getAttribute("columnSize"));
			int decimalDigits = Integer.parseInt(columnElement.getAttribute("decimalDigits"));
			boolean nullable = Boolean.parseBoolean(columnElement.getAttribute("nullable"));
			String remarks = columnElement.getAttribute("remarks");
			String typeName = columnElement.getAttribute("typeName");

			columns.add(new Column(name, dataType, columnSize, decimalDigits, nullable, remarks, typeName));
		}
	}

	private void setPrimaryKeys(Element tableElement) {
		primaryKeys.clear();
		
		List<Element> primaryKeyElements = XmlUtils.findElements("/primaryKey", tableElement);
		for (Element primaryKeyElement : primaryKeyElements) {
			primaryKeys.add(new PrimaryKey(primaryKeyElement.getAttribute("name"), primaryKeyElement.getAttribute("columnName"), new Short(primaryKeyElement.getAttribute("keySeq"))));
		}
	}

	private void setForeignKeys(Element tableElement) {
		foreignKeys.clear();

		List<Element> foreignKeyElements = XmlUtils.findElements("/foreignKey", tableElement);
		for (Element foreignKeyElement : foreignKeyElements) {
			foreignKeys.add(new ForeignKey(foreignKeyElement.getAttribute("name"), foreignKeyElement.getAttribute("fkTable")));
		}
	}

	private void setIndexes(Element tableElement) {
		indexes.clear();

		List<Element> indexElements = XmlUtils.findElements("/index", tableElement);
		for (Element indexElement : indexElements) {
			String name = indexElement.getAttribute("name");
			String columnName = indexElement.getAttribute("columnName");
			boolean nonUnique = Boolean.parseBoolean(indexElement.getAttribute("nonUnique"));
			Short type = new Short(indexElement.getAttribute("type"));

			indexes.add(new Index(name, columnName, nonUnique, type));
		}
	}
}
