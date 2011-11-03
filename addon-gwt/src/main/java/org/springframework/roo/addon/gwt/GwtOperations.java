package org.springframework.roo.addon.gwt;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Provides GWT operations.
 *
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.1
 */
public interface GwtOperations {

	boolean isSetupAvailable();

	boolean isGwtEnabled();

	boolean isGaeEnabled();

	void setup();

	void proxyAll(JavaPackage proxyPackage);

	void proxyType(JavaPackage proxyPackage, JavaType type);

	void requestAll(JavaPackage requestPackage);

	void requestType(JavaPackage requestPackage, JavaType type);

	void proxyAndRequestAll(JavaPackage proxyAndRequestPackage);

	void proxyAndRequestType(JavaPackage proxyAndRequestPackage, JavaType type);

	void scaffoldAll(JavaPackage proxyPackage, JavaPackage requestPackage);

	void scaffoldType(JavaPackage proxyPackage, JavaPackage requestPackage, JavaType type);

	void updateGaeConfiguration();
}