package org.springframework.roo.addon.jpa;

import java.util.SortedSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the JPA add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class JpaCommands implements CommandMarker {
	private static Logger logger = HandlerUtils.getLogger(JpaCommands.class);
	@Reference private JpaOperations jpaOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private StaticFieldConverter staticFieldConverter;

	protected void activate(ComponentContext context) {
		staticFieldConverter.add(JdbcDatabase.class);
		staticFieldConverter.add(OrmProvider.class);
	}

	protected void deactivate(ComponentContext context) {
		staticFieldConverter.remove(JdbcDatabase.class);
		staticFieldConverter.remove(OrmProvider.class);
	}

	@CliAvailabilityIndicator("persistence setup")
	public boolean isInstallJpaAvailable() {
		return jpaOperations.isJpaInstallationPossible() || jpaOperations.isJpaInstalled();
	}
	
	@CliCommand(value = "persistence setup", help = "Install or updates a JPA persistence provider in your project")
	public void installJpa(
		@CliOption(key = { "provider" }, mandatory = true, help = "The persistence provider to support") OrmProvider ormProvider, 
		@CliOption(key = { "database" }, mandatory = true, help = "The database to support") JdbcDatabase jdbcDatabase, 
		@CliOption(key = { "applicationId" }, mandatory = false, unspecifiedDefaultValue = "the project's name", help = "The Google App Engine application identifier to use") String applicationId, 
		@CliOption(key = { "jndiDataSource" }, mandatory = false, help = "The JNDI datasource to use") String jndi, 
		@CliOption(key = { "databaseName" }, mandatory = false, help = "The database name to use") String databaseName, 
		@CliOption(key = { "userName" }, mandatory = false, help = "The username to use") String userName, 
		@CliOption(key = { "password" }, mandatory = false, help = "The password to use") String password) {

		if (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE && ormProvider != OrmProvider.DATANUCLEUS) {
			logger.warning("Provider must be " + OrmProvider.DATANUCLEUS.name() + " for the Google App Engine");
			return;
		}

		jpaOperations.configureJpa(ormProvider, jdbcDatabase, jndi, applicationId, databaseName, userName, password);
	}

	@CliCommand(value = "database properties list", help = "Shows database configuration details")
	public SortedSet<String> databaseProperties() {
		return jpaOperations.getDatabaseProperties();
	}

	@CliCommand(value = "database properties set", help = "Changes a particular database property")
	public void databaseSet(
		@CliOption(key = "key", mandatory = true, help = "The property key that should be changed") String key, 
		@CliOption(key = "value", mandatory = true, help = "The new vale for this property key") String value) {

		propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", key, value);
	}

	@CliCommand(value = "database properties remove", help = "Removes a particular database property")
	public void databaseRemove(
		@CliOption(key = { "", "key" }, mandatory = true, help = "The property key that should be removed") String key) {

		propFileOperations.removeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", key);
	}

	@CliAvailabilityIndicator("database properties list")
	public boolean isJpaInstalled() {
		return jpaOperations.isJpaInstalled();
	}

	@CliAvailabilityIndicator({ "database properties remove", "database properties set" })
	public boolean hasDatabaseProperties() {
		return isJpaInstalled() && jpaOperations.hasDatabaseProperties();
	}
}