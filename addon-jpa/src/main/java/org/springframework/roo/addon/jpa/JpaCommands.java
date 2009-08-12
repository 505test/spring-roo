package org.springframework.roo.addon.jpa;

import java.util.SortedSet;

import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'logging' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class JpaCommands implements CommandMarker {
	
	private JpaOperations jpaOperations;
	private PropFileOperations propFileOperations;
	private MetadataService metadataService;
	
	public JpaCommands(StaticFieldConverter staticFieldConverter, JpaOperations jpaOperations, PropFileOperations propFileOperations, MetadataService metadataService) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(jpaOperations, "JPA operations required");
		Assert.notNull(propFileOperations, "Property file operations required");
		Assert.notNull(metadataService, "Metadata service required");
		staticFieldConverter.add(JdbcDatabase.class);
		staticFieldConverter.add(OrmProvider.class);
		this.jpaOperations = jpaOperations;
		this.propFileOperations = propFileOperations;
		this.metadataService = metadataService;
	}
	
	/**
	 * @return true if the "install jpa" command is available at this moment
	 */
	@CliAvailabilityIndicator("install jpa")
	public boolean isInstallJpaAvailable() {
		return jpaOperations.isInstallJpaAvailable();
	}
	
	@CliCommand(value="install jpa", help="Install a JPA persistence provider in your project")
	public void installJpa(@CliOption(key={"provider"}, mandatory=true, help="The persistence provider to support") OrmProvider ormProvider,
			@CliOption(key={"","database"}, mandatory=true, help="The database to support") JdbcDatabase jdbcDatabase,			
			@CliOption(key={"jndiDataSource"}, mandatory=false, help="The JNDI datasource to use") String jndi) {
		jpaOperations.configureJpa(ormProvider, jdbcDatabase, jndi, true);
	}
	
	@CliCommand(value="install jpa exception translation", help="Installs support for JPA exception translation")
	public void exceptionTranslation(@CliOption(key={"package"}, mandatory=false, help="The package in which the JPA exception translation aspect will be installed") JavaPackage aspectPackage) {		
		if (aspectPackage == null) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			aspectPackage = projectMetadata.getTopLevelPackage();
		} 
		jpaOperations.installExceptionTranslation(aspectPackage);
	}
	
	@CliCommand(value="database properties", help="Shows database configuration details")
	public SortedSet<String> databaseProperties() {
		return propFileOperations.getPropertyKeys(Path.SPRING_CONFIG_ROOT, "database.properties", true);
	}
	
	@CliCommand(value="database set", help="Changes a particular database property")
	public void databaseSet(@CliOption(key="key", mandatory=true, help="The property key that should be changed") String key, @CliOption(key="value", mandatory=true, help="The new vale for this property key") String value) {
		propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", key, value);
	}

	@CliCommand(value="database remove", help="Removes a particular database property")
	public void databaseRemove(@CliOption(key={"","key"}, mandatory=true, help="The property key that should be removed") String key) {
		propFileOperations.removeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", key);
	}
	
	/**
	 * @return true if the commands are available at this moment
	 */
	@CliAvailabilityIndicator({"update jpa", "database remove", "database set", "database properties", "install jpa exception translation"})
	public boolean isUpdateJpaAvailable() {
		return jpaOperations.isUpdateJpaAvailable();
	}
	
	@CliCommand(value="update jpa", help="Update the JPA persistence provider in your project")
	public void updateJpa(@CliOption(key={"provider"}, mandatory=true, help="The persistence provider to support") OrmProvider ormProvider,
			@CliOption(key={"","database"}, mandatory=true, help="The database to support") JdbcDatabase jdbcDatabase,			
			@CliOption(key={"jndiDataSource"}, mandatory=false, help="The JNDI datasource to use") String jndi) {
		jpaOperations.configureJpa(ormProvider, jdbcDatabase, jndi, false);
	}
}