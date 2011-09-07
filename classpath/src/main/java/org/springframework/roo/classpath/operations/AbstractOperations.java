package org.springframework.roo.classpath.operations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Abstract base class for operations classes. Contains common methods.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractOperations {
	
	// Constants
	protected static Logger logger = HandlerUtils.getLogger(AbstractOperations.class);
	
	// Fields
	@Reference protected FileManager fileManager;
	protected ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
	}
	
	public Document getDocumentTemplate(String templateName) {
		return XmlUtils.readXml(TemplateUtils.getTemplate(getClass(), templateName));
	}

	/**
	 * This method will copy the contents of a directory to another if the resource does not already exist in the target directory
	 * 
	 * @param sourceAntPath the source path
	 * @param targetDirectory the target directory
	 */
	public void copyDirectoryContents(String sourceAntPath, String targetDirectory, boolean replace) {
		Assert.hasText(sourceAntPath, "Source path required");
		Assert.hasText(targetDirectory, "Target directory required");

		if (!targetDirectory.endsWith("/")) {
			targetDirectory += "/";
		}

		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");
		for (final URL url : urls) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
			if (replace) {
				BufferedReader in = null;
				StringBuilder sb = new StringBuilder();
				try {
					in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
					while (true) {
						int ch = in.read();
						if (ch < 0) {
							break;
						}
						sb.append((char) ch);
					}
				} catch (Exception e) {
					throw new IllegalStateException(e);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException ignored) {}
					}
				}
				fileManager.createOrUpdateTextFileIfRequired(targetDirectory + fileName, sb.toString(), false);
			} else {
				if (!fileManager.exists(targetDirectory + fileName)) {
					try {
						FileCopyUtils.copy(url.openStream(), fileManager.createFile(targetDirectory + fileName).getOutputStream());
					} catch (IOException e) {
						throw new IllegalStateException("Encountered an error during copying of resources for the add-on.", e);
					}
				}
			}
		}
	}
}
