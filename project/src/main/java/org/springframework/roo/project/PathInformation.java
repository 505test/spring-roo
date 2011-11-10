package org.springframework.roo.project;

import java.io.File;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * Used by {@link DelegatePathResolver} to permit subclasses to register path details.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PathInformation {

	// Fields
	private final ContextualPath contextualPath;
	private final boolean source;
	private final File location;
	
	/**
	 * Constructor
	 *
	 * @param contextualPath (required)
	 * @param source whether this path contains source code
	 * @param location the physical location of this path (required)
	 */
	public PathInformation(final ContextualPath contextualPath, final boolean source, final File location) {
		Assert.notNull(contextualPath, "Module path required");
		Assert.notNull(location, "Location required");
		this.contextualPath = contextualPath;
		this.source = source;
		this.location = location;
	}

	public ContextualPath getContextualPath() {
		return contextualPath;
	}

	public boolean isSource() {
		return source;
	}

	public File getLocation() {
		return location;
	}

	/**
	 * Returns the canonical path of this {@link PathInformation}
	 * 
	 * @return a non-blank canonical path
	 */
	public String getLocationPath() {
		return FileUtils.getCanonicalPath(location);
	}

	public Path getPath() {
		return contextualPath.getPath();
	}
	
	@Override
	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("contextualPath", contextualPath);
		tsc.append("source", source);
		tsc.append("location", location);
		return tsc.toString();
	}
}
