package org.springframework.roo.addon.web.mvc.json;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Operations for Web MVC Json functionality.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface WebJsonOperations {
	
	boolean isSetupAvailable();
	
	boolean isCommandAvailable();
	
	void annotateType(JavaType type, JavaType jsonType);
	
	void annotateAll(JavaPackage javaPackage);
}
