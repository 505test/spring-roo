package org.springframework.roo.addon.maven;

import java.io.InputStream;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Spring application context-related operations.
 *  
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public class ApplicationContextOperations {

	private FileManager fileManager;
	private MetadataService metadataService;
	
	public ApplicationContextOperations(FileManager fileManager, MetadataService metadataService) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(metadataService, "Metadata service required");
		this.fileManager = fileManager;
		this.metadataService = metadataService;
	}
	
	public void createMiddleTierApplicationContext() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");
		
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "applicationContext-template.xml");
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		
		Element rootElement = (Element) pom.getFirstChild();
		XmlUtils.findFirstElementByName("context:component-scan", rootElement).setAttribute("base-package", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());

		PathResolver pathResolver = projectMetadata.getPathResolver();
		MutableFile mutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml"));
		XmlUtils.writeXml(mutableFile.getOutputStream(), pom);

		fileManager.scanAll();
	}
}
