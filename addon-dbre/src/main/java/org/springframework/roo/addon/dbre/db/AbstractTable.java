package org.springframework.roo.addon.dbre.db;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Table metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class AbstractTable {
	protected final IdentifiableTable identifiableTable;
	protected final Set<Column> columns = new HashSet<Column>();
	protected final SortedSet<PrimaryKey> primaryKeys = new TreeSet<PrimaryKey>(new PrimaryKeyComparator());
	protected final SortedSet<ForeignKey> foreignKeys = new TreeSet<ForeignKey>(new ForeignKeyComparator());
	protected final Set<Index> indexes = new HashSet<Index>();

	AbstractTable(IdentifiableTable identifiableTable) {
		this.identifiableTable = identifiableTable;
	}
	
	public IdentifiableTable getIdentifiableTable() {
		return identifiableTable;
	}

	public Set<Column> getColumns() {
		return this.columns;
	}

	public SortedSet<PrimaryKey> getPrimaryKeys() {
		return this.primaryKeys;
	}

	public SortedSet<ForeignKey> getForeignKeys() {
		return this.foreignKeys;
	}

	public Set<Index> getIndexes() {
		return this.indexes;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifiableTable == null) ? 0 : identifiableTable.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractTable)) {
			return false;
		}
		AbstractTable other = (AbstractTable) obj;
		if (identifiableTable == null) {
			if (other.identifiableTable != null) {
				return false;
			}
		} else if (!identifiableTable.equals(other.identifiableTable)) {
			return false;
		}
		return true;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		final String lineSeparator = System.getProperty("line.separator");

		builder.append(identifiableTable.getTableType().name() + " " + identifiableTable.getTable());
		builder.append(lineSeparator);

		if (!columns.isEmpty()) {
			builder.append("  COLUMNS ");
			builder.append(lineSeparator);
			for (Column column : columns) {
				builder.append(column.toString());
				builder.append(lineSeparator);
			}
		}

		if (!primaryKeys.isEmpty()) {
			builder.append("  PRIMARY KEYS ");
			builder.append(lineSeparator);
			for (PrimaryKey primaryKey : getPrimaryKeys()) {
				builder.append(primaryKey.toString());
				builder.append(lineSeparator);
			}
		}

		if (!foreignKeys.isEmpty()) {
			builder.append("  FOREIGN KEYS ");
			builder.append(lineSeparator);
			for (ForeignKey foreignKey : getForeignKeys()) {
				builder.append(foreignKey.toString());
				builder.append(lineSeparator);
			}
		}

		if (!indexes.isEmpty()) {
			builder.append("  INDEXES ");
			builder.append(lineSeparator);
			for (Index index : getIndexes()) {
				builder.append(index.toString());
				builder.append(lineSeparator);
			}
		}

		return builder.toString();
	}
}
