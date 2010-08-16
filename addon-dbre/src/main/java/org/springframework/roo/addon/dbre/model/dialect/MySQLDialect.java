package org.springframework.roo.addon.dbre.model.dialect;

/**
 * An SQL dialect for the MySQL database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class MySQLDialect extends AbstractDialect implements Dialect {

	protected String getDatabaseName() {
		return "MySQL";
	}
}
