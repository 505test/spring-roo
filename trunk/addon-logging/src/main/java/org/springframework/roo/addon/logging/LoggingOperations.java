package org.springframework.roo.addon.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Provides logging configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class LoggingOperations {
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;	
	
	public LoggingOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
	}
	
	public boolean isConfigureLoggingAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	public void configureLogging(LogLevel logLevel, LoggerPackage loggerPackage) {
		Assert.notNull(logLevel, "LogLevel required");
		Assert.notNull(loggerPackage, "LoggerPackage required");
		
		setupProperties(logLevel, loggerPackage);
//		setupWebXml();
	}
	
	private void setupProperties(LogLevel logLevel, LoggerPackage loggerPackage) {
		String filePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties");
		MutableFile log4jMutableFile = null;
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(filePath)) {
				log4jMutableFile = fileManager.updateFile(filePath);
				props.load(log4jMutableFile.getInputStream());
			} else {
				log4jMutableFile = fileManager.createFile(filePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "log4j-template.properties");
				Assert.notNull(templateInputStream, "Could not acquire log4j configuration template");
				props.load(templateInputStream);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is unavailable");
		JavaPackage topLevelPackage = projectMetadata.getTopLevelPackage();
		
		for (String packageName : loggerPackage.getPackageNames()) {
			if (LoggerPackage.ROOT.equals(loggerPackage)) {
				props.remove("log4j.rootLogger");
				props.setProperty("log4j.rootLogger", logLevel.getKey() + ", stdout, R");
			} else {						
				packageName = packageName.equals("TO_BE_CHANGED_BY_LISTENER") ? topLevelPackage.getFullyQualifiedPackageName() : packageName;
				props.remove("log4j.logger." + packageName);
				props.setProperty("log4j.logger." + packageName, logLevel.getKey());
			}
		}
		
		try {
			props.store(log4jMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
}
