package org.springframework.roo.project;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
@Reference(name = "pathResolvingStrategy", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PathResolvingStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultPathResolvingStrategy implements PathResolvingStrategy {

	// Constants
	static final String ROOT_MODULE = "";

	// Fields
	private final Collection<PathResolvingStrategy> otherPathResolvingStrategies = new ArrayList<PathResolvingStrategy>();
	
	private final List<PathInformation> pathOrder = new ArrayList<PathInformation>();
	private final Map<Path, PathInformation> pathCache = new LinkedHashMap<Path, PathInformation>();
	private final Collection<PathInformation> pathInformation = new ArrayList<PathInformation>();
	
	private String rootPath;
	
	protected void bindPathResolvingStrategy(final PathResolvingStrategy pathResolvingStrategy) {
		if (pathResolvingStrategy != this) {
			otherPathResolvingStrategies.add(pathResolvingStrategy);
		}
	}
	
	protected void unbindPathResolvingStrategy(final PathResolvingStrategy pathResolvingStrategy) {
		otherPathResolvingStrategies.remove(pathResolvingStrategy);
	}

	protected void activate(final ComponentContext context) {
		final File projectDirectory = new File(StringUtils.defaultIfEmpty(OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
		rootPath = FileUtils.getCanonicalPath(projectDirectory);
		populatePaths(projectDirectory);
		initialisePathCollections();
	}
	
	private void populatePaths(final File projectDirectory) {
		for (final Path subPath : Path.values()) {
			pathInformation.add(subPath.getRootModulePath(projectDirectory.getPath()));
		}
	}

	public boolean isActive() {
		for (final PathResolvingStrategy otherStrategy : otherPathResolvingStrategies) {
			if (otherStrategy.isActive()) {
				return false;
			}
		}
		return true;
	}

	public String getIdentifier(final ContextualPath path, final String relativePath) {
		return FileUtils.ensureTrailingSeparator(pathCache.get(path.getPath()).getLocationPath()) + relativePath;
	}

	/**
	 * Called by the {@link #initialisePathCollections()} method when it wishes to obtain a list of paths to register.
	 *
	 * @return a copy of this list
	 */
	protected List<PathInformation> getPathInformation() {
		return new ArrayList<PathInformation>(pathInformation);
	}

	/**
	 * Called by the subclass when they are ready to complete initialization.
	 * This means their {@link #getPathInformation()} method is ready to be called.
	 */
	protected void initialisePathCollections()  {
		final List<PathInformation> pathInformation = getPathInformation();
		Assert.notEmpty(pathInformation, "Path information required");
		for (final PathInformation pi : pathInformation) {
			Assert.isTrue(!pathCache.containsKey(pi.getPath()), "Cannot specify '" + pi.getPath() + "' more than once");
			pathOrder.add(pi);
			pathCache.put(pi.getPath(), pi);
		}
	}

	public String getFriendlyName(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		final ContextualPath p = getPath(identifier);
		if (p == null) {
			return identifier;
		}
		return p.getName() + getRelativeSegment(identifier);
	}

	public String getRoot(final ContextualPath contextualPath) {
		Assert.notNull(contextualPath, "Path required");
		final PathInformation pathInfo = pathCache.get(contextualPath.getPath());
		Assert.notNull(pathInfo, "Unable to determine information for path '" + contextualPath + "'");
		final File root = pathInfo.getLocation();
		return FileUtils.getCanonicalPath(root);
	}

	/**
	 * Obtains the {@link Path}s.
	 *
	 * @param requireSource <code>true</code> to return only paths containing
	 * Java source code, or <code>false</code> to return all paths
	 * @return the matching paths (never <code>null</code>)
	 */
	private Collection<ContextualPath> getPaths(final boolean sourceOnly) {
		final List<ContextualPath> result = new ArrayList<ContextualPath>();
		for (final PathInformation modulePath : pathOrder) {
			if (!sourceOnly || modulePath.isSource()) {
				result.add(modulePath.getContextualPath());
			}
		}
		return result;
	}

	public Collection<ContextualPath> getPaths() {
		return getPaths(false);
	}

	public Collection<ContextualPath> getSourcePaths() {
		return getPaths(true);
	}

	/**
	 * Locates the first {@link PathInformation} which can be construed as a parent
	 * of the presented identifier.
	 *
	 * @param identifier to locate the parent of (required)
	 * @return the first matching parent, or null if not found
	 */
	private PathInformation getApplicablePathInformation(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		for (final PathInformation pi : pathOrder) {
			final FileDetails possibleParent = new FileDetails(pi.getLocation(), null);
			if (possibleParent.isParentOf(identifier)) {
				return pi;
			}
		}
		return null;
	}

	public ContextualPath getPath(final String identifier) {
		final PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		return parent.getContextualPath();
	}

	public String getRelativeSegment(final String identifier) {
		final PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		final FileDetails parentFile = new FileDetails(parent.getLocation(), null);
		return parentFile.getRelativeSegment(identifier);
	}

	public String getRoot() {
		return rootPath;
	}

	public String getCanonicalPath(final ContextualPath path, final JavaType javaType) {
		return null;
	}

	public String getFocusedIdentifier(final Path path, final String relativePath) {
		return null;
	}

	public String getFocusedRoot(final Path path) {
		return null;
	}

	public ContextualPath getFocusedPath(final Path path) {
		return null;
	}

	public String getFocusedCanonicalPath(final Path path, final JavaType javaType) {
		return null;
	}
}
