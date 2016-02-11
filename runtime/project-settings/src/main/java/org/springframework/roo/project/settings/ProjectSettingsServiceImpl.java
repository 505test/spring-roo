package org.springframework.roo.project.settings;

import java.io.File;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Provides a service to manage all necessary properties located on project
 * configuration files.
 *
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class ProjectSettingsServiceImpl implements ProjectSettingsService {

	private static final Path PROJECT_CONFIG_FOLDER_LOCATION = Path.ROOT_ROO_CONFIG;
	private static final String PROJECT_CONFIG_FILE_FOLDER = "config/";
	private static final String PROJECT_CONFIG_FILE_NAME = "project";
	private static final String PROJECT_CONFIG_FILE_EXTENSION = ".properties";

	protected final static Logger LOGGER = HandlerUtils.getLogger(ProjectSettingsServiceImpl.class);

	private String projectRootDirectory;

	private PropFilesManagerService propFilesManager;
	private FileManager fileManager;

	// ------------ OSGi component attributes ----------------
	private BundleContext context;

	protected void activate(final ComponentContext context) {
		this.context = context.getBundleContext();
		final File projectDirectory = new File(
				StringUtils.defaultIfEmpty(OSGiUtils.getRooWorkingDirectory(context), FileUtils.CURRENT_DIRECTORY),
				PROJECT_CONFIG_FOLDER_LOCATION.getDefaultLocation());
		projectRootDirectory = FileUtils.getCanonicalPath(projectDirectory);
	}

	@Override
	public void addProperty(final String key, final String value, final boolean force) {
		getPropFilesManager().addPropertyIfNotExists(LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""),
				getProjectSettingsFileName(), key, value, force);
	}
	
    @Override
    public void removeProperty(final String key) {
        getPropFilesManager().removeProperty(
                LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""),
                getProjectSettingsFileName(), key);
    }

	@Override
	public Map<String, String> getProperties() {
		return getPropFilesManager().getProperties(LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""),
				getProjectSettingsFileName());
	}

	@Override
	public SortedSet<String> getPropertyKeys(boolean includeValues) {
		return getPropFilesManager().getPropertyKeys(LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""),
				getProjectSettingsFileName(), includeValues);
	}

	@Override
	public String getProperty(final String key) {
		return getPropFilesManager().getProperty(LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""),
				getProjectSettingsFileName(), key);
	}

	@Override
	public String getProjectSettingsLocation() {
		return projectRootDirectory.concat("/").concat(getProjectSettingsFileName());
	}

	@Override
	public boolean existsProjectSettingsFile() {
		return getFileManager().exists(getProjectSettingsLocation());
	}

	/**
	 * Method that generates application configuration file name using project setting
	 * folder, file name and file extension.
	 *
	 * @return
	 */
	private String getProjectSettingsFileName() {
		String fileName = PROJECT_CONFIG_FILE_FOLDER;
		fileName = fileName.concat(PROJECT_CONFIG_FILE_NAME).concat(PROJECT_CONFIG_FILE_EXTENSION);
			
		return fileName;
	}

	@Override
	public void createProjectSettingsFile() {
		getFileManager().createFile(getProjectSettingsLocation());
	}

	public PropFilesManagerService getPropFilesManager() {
		if (propFilesManager == null) {
			// Get all Services implement PropFilesManagerService interface
			try {
				ServiceReference<?>[] references = this.context
						.getAllServiceReferences(PropFilesManagerService.class.getName(), null);

				for (ServiceReference<?> ref : references) {
					propFilesManager = (PropFilesManagerService) this.context.getService(ref);
					return propFilesManager;
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load PropFilesManagerService on ProjectSettingsServiceImpl.");
				return null;
			}
		} else {
			return propFilesManager;
		}

	}

	public FileManager getFileManager() {
		if (fileManager == null) {
			// Get all Services implement FileManager interface
			try {
				ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(),
						null);

				for (ServiceReference<?> ref : references) {
					fileManager = (FileManager) this.context.getService(ref);
					return fileManager;
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load FileManager on ProjectSettingsServiceImpl.");
				return null;
			}
		} else {
			return fileManager;
		}

	}
}
