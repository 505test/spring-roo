package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;

/**
 * Represents a reference between a column in the local table and a column in another table.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Reference implements Serializable {
	private static final long serialVersionUID = 7930959797342530749L;

	/** The sequence value within the key. */
	private Short sequenceValue;

	/** The local column. */
	private Column localColumn;

	/** The foreign column. */
	private Column foreignColumn;

	/** The name of the local column. */
	private String localColumnName;

	/** The name of the foreign column. */
	private String foreignColumnName;

	/**
	 * Creates a new, empty reference.
	 */
	Reference() {
	}

	/**
	 * Creates a new reference between the two given columns.
	 * 
	 * @param localColumn The local column
	 * @param foreignColumn The remote column
	 */
	public Reference(Column localColumn, Column foreignColumn) {
		setLocalColumn(localColumn);
		setForeignColumn(foreignColumn);
	}

	public Short getSequenceValue() {
		return sequenceValue;
	}

	public void setSequenceValue(Short sequenceValue) {
		this.sequenceValue = sequenceValue;
	}

	public Column getLocalColumn() {
		return localColumn;
	}

	public void setLocalColumn(Column localColumn) {
		this.localColumn = localColumn;
		this.localColumnName = localColumn == null ? null : localColumn.getName();
	}

	public Column getForeignColumn() {
		return foreignColumn;
	}

	public void setForeignColumn(Column foreignColumn) {
		this.foreignColumn = foreignColumn;
		this.foreignColumnName = foreignColumn == null ? null : foreignColumn.getName();
	}

	public String getLocalColumnName() {
		return localColumnName;
	}

	public void setLocalColumnName(String localColumnName) {
		this.localColumnName = localColumnName;
	}

	public String getForeignColumnName() {
		return foreignColumnName;
	}

	public void setForeignColumnName(String foreignColumnName) {
		this.foreignColumnName = foreignColumnName;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((foreignColumnName == null) ? 0 : foreignColumnName.hashCode());
		result = prime * result + ((localColumnName == null) ? 0 : localColumnName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Reference)) {
			return false;
		}
		Reference other = (Reference) obj;
		if (foreignColumnName == null) {
			if (other.foreignColumnName != null) {
				return false;
			}
		} else if (!foreignColumnName.equals(other.foreignColumnName)) {
			return false;
		}
		if (localColumnName == null) {
			if (other.localColumnName != null) {
				return false;
			}
		} else if (!localColumnName.equals(other.localColumnName)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return String.format("Reference [sequenceValue=%s, localColumnName=%s, foreignColumnName=%s]", sequenceValue, localColumnName, foreignColumnName);
	}
}
