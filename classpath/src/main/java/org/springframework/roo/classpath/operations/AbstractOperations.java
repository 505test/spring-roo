package org.springframework.roo.classpath.operations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.apache.commons.io.IOUtils;
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

    protected static Logger LOGGER = HandlerUtils
            .getLogger(AbstractOperations.class);

    protected ComponentContext context;
    @Reference protected FileManager fileManager;

    protected void activate(final ComponentContext context) {
        this.context = context;
    }

    /**
     * This method will copy the contents of a directory to another if the
     * resource does not already exist in the target directory
     * 
     * @param sourceAntPath the source path
     * @param targetDirectory the target directory
     */
    public void copyDirectoryContents(final String sourceAntPath,
            String targetDirectory, final boolean replace) {
        Validate.notBlank(sourceAntPath, "Source path required");
        Validate.notBlank(targetDirectory, "Target directory required");

        if (!targetDirectory.endsWith("/")) {
            targetDirectory += "/";
        }

        if (!fileManager.exists(targetDirectory)) {
            fileManager.createDirectory(targetDirectory);
        }

        final String path = FileUtils.getPath(getClass(), sourceAntPath);
        final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(
                context.getBundleContext(), path);
        Validate.notNull(urls,
                "Could not search bundles for resources for Ant Path '" + path
                        + "'");
        for (final URL url : urls) {
            final String fileName = url.getPath().substring(
                    url.getPath().lastIndexOf("/") + 1);
            if (replace) {
                BufferedReader in = null;
                final StringBuilder sb = new StringBuilder();
                try {
                    in = new BufferedReader(new InputStreamReader(url
                            .openConnection().getInputStream()));
                    while (true) {
                        final int ch = in.read();
                        if (ch < 0) {
                            break;
                        }
                        sb.append((char) ch);
                    }
                }
                catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
                finally {
                    IOUtils.closeQuietly(in);
                }
                fileManager.createOrUpdateTextFileIfRequired(targetDirectory
                        + fileName, sb.toString(), false);
            }
            else {
                if (!fileManager.exists(targetDirectory + fileName)) {
                    try {
                        FileCopyUtils.copy(url.openStream(), fileManager
                                .createFile(targetDirectory + fileName)
                                .getOutputStream());
                    }
                    catch (final IOException e) {
                        throw new IllegalStateException(
                                "Encountered an error during copying of resources for the add-on.",
                                e);
                    }
                }
            }
        }
    }

    public Document getDocumentTemplate(final String templateName) {
        return XmlUtils.readXml(FileUtils.getInputStream(getClass(),
                templateName));
    }
}
