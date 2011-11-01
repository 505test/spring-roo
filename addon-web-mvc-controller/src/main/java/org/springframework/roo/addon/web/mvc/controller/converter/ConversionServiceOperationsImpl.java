package org.springframework.roo.addon.web.mvc.controller.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;

/**
 * A default implementation of {@link ConversionServiceOperations}.
 *
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
@Deprecated
@Component
@Service
public class ConversionServiceOperationsImpl implements ConversionServiceOperations {

	// Fields
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private WebMvcOperations webMvcOperations;

	public ConversionServiceOperationsImpl() {}

	public ConversionServiceOperationsImpl(final FileManager fileManager) {
		// For testing
		this.fileManager = fileManager;
	}

	public void installConversionService(final JavaPackage thePackage) {
		installJavaClass(thePackage);
		webMvcOperations.installConversionService(thePackage);
		fileManager.scan();
	}

	void installJavaClass(final JavaPackage thePackage) {
		JavaType javaType = new JavaType(thePackage.getFullyQualifiedPackageName() + "." + CONVERSION_SERVICE_SIMPLE_TYPE);
		String physicalPath = pathResolver.getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, javaType);
		if (fileManager.exists(physicalPath)) {
			return;
		}
		InputStream template = null;
		try {
			template = FileUtils.getInputStream(getClass(), CONVERSION_SERVICE_SIMPLE_TYPE + "-template._java");
			String input = FileCopyUtils.copyToString(new InputStreamReader(template));
			input = input.replace("__PACKAGE__", thePackage.getFullyQualifiedPackageName());
			MutableFile mutableFile = fileManager.createFile(physicalPath);
			FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
		} finally {
			IOUtils.closeQuietly(template);
		}
	}
}
