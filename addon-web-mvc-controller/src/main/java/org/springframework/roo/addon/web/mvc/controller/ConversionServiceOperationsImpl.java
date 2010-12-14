package org.springframework.roo.addon.web.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.XmlTemplate.DomElementCallback;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A default implementation of {@link ConversionServiceOperations}.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
@Component 
@Service
public class ConversionServiceOperationsImpl implements ConversionServiceOperations {

	public static final String CONVERSION_SERVICE_SIMPLE_TYPE = "ApplicationConversionServiceFactoryBean";
	public static final String CONVERSION_SERVICE_BEAN_NAME = "applicationConversionService";
	
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ClasspathOperations classpathOperations;

	public ConversionServiceOperationsImpl() {
		// For testing
	}

	public ConversionServiceOperationsImpl(FileManager fileManager, PathResolver pathResolver, ClasspathOperations classpathOperations) {
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.classpathOperations = classpathOperations;
	}

	public void installConversionService(JavaPackage thePackage) {
		installJavaClass(thePackage);
		manageWebMvcConfig(thePackage);
		fileManager.scan();
	}

	/* Private methods */
	
	void installJavaClass(JavaPackage thePackage) {
		JavaType javaType = new JavaType(thePackage.getFullyQualifiedPackageName() + "." + CONVERSION_SERVICE_SIMPLE_TYPE);
		String physicalPath = classpathOperations.getPhysicalLocationCanonicalPath(javaType, Path.SRC_MAIN_JAVA);
		if (fileManager.exists(physicalPath)) {
			return;
		}
		try {
			InputStream template = TemplateUtils.getTemplate(getClass(), CONVERSION_SERVICE_SIMPLE_TYPE + "-template._java");
			String input = FileCopyUtils.copyToString(new InputStreamReader(template));
			input = input.replace("__PACKAGE__", thePackage.getFullyQualifiedPackageName());
			MutableFile mutableFile = fileManager.createFile(physicalPath);
			FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
		}
	}

	void manageWebMvcConfig(final JavaPackage thePackage) {
		String webMvcConfigPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		Assert.isTrue(fileManager.exists(webMvcConfigPath), webMvcConfigPath + " doesn't exists");		

		new XmlTemplate(fileManager).update(webMvcConfigPath, new DomElementCallback() {
			public boolean doWithElement(Document document, Element root) {
				Element annotationDriven = XmlUtils.findFirstElementByName("mvc:annotation-driven", root);
				if (isConversionServiceConfigured(root, annotationDriven)) {
					return false;
				}
				annotationDriven.setAttribute("conversion-service", CONVERSION_SERVICE_BEAN_NAME);

				Element conversionServiceBean = document.createElement("bean");
				conversionServiceBean.setAttribute("id", CONVERSION_SERVICE_BEAN_NAME);
				conversionServiceBean.setAttribute("class", thePackage.getFullyQualifiedPackageName() + "." + CONVERSION_SERVICE_SIMPLE_TYPE);
				
				root.appendChild(conversionServiceBean);
				root.insertBefore(document.createTextNode("\n\t"), conversionServiceBean);
				root.insertBefore(document.createComment("Installs application converters and formatters"), conversionServiceBean);
				root.insertBefore(document.createTextNode("\n\t"), conversionServiceBean);
				
				return true;
			}
		});
	}

	boolean isConversionServiceConfigured(Element root, Element annotationDriven) {
		String beanName = annotationDriven.getAttribute("conversion-service");
		if (! StringUtils.hasText(beanName)) {
			return false;
		} else {
			Element bean = XmlUtils.findFirstElement("/beans/bean[@id=\"" + beanName + "\"]", root);
			String classAttribute = bean.getAttribute("class");
			Assert.isTrue(classAttribute.endsWith(CONVERSION_SERVICE_SIMPLE_TYPE), 
					"Tried to install conversion service but found a different one configured already." +
					"If you have an existing conversion service, considering removing the conversion-sevice " +
					"attribute from the mvc-annotation driven element and then add your custom " +
					"converters and formatters to the conversion service that will be installed by ROO.");
			return true;
		}
	}
	
}
