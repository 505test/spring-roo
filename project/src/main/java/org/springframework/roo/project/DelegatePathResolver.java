package org.springframework.roo.project;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;

/**
 * Abstract {@link PathResolver} implementation.
 * 
 * <p>
 * Subclasses should be created for common build system structures.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
@Reference(name = "pathResolvingStrategy", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PathResolvingStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DelegatePathResolver implements PathResolver {

	private final Set<PathResolvingStrategy> pathResolvingStrategies = new HashSet<PathResolvingStrategy>();

	// Mutex
	private final Object lock = new Object();

	protected void bindPathResolvingStrategy(PathResolvingStrategy strategy) {
		synchronized (lock) {
			pathResolvingStrategies.add(strategy);
		}
	}

	protected void unbindPathResolvingStrategy(PathResolvingStrategy strategy) {
		synchronized (lock) {
			pathResolvingStrategies.remove(strategy);
		}
	}

	private PathResolvingStrategy getStrategy() {
		PathResolvingStrategy chosenStrategy = null;
		for (PathResolvingStrategy pathResolvingStrategy : pathResolvingStrategies) {
			if (pathResolvingStrategy.isActive()) {
				if (chosenStrategy != null) {
					throw new IllegalArgumentException("Multiple path resolving strategies are active :<");
				} else {
					chosenStrategy = pathResolvingStrategy;
				}
			}
		}
		return chosenStrategy;
	}

	public String getIdentifier(ContextualPath path, String relativePath) {
		return getStrategy().getIdentifier(path, relativePath);
	}

	public ContextualPath getPath(String identifier) {
		return getStrategy().getPath(identifier);
	}

	public String getRoot(ContextualPath path) {
		return getStrategy().getRoot(path);
	}

	public String getRelativeSegment(String identifier) {
		return getStrategy().getRelativeSegment(identifier);
	}

	public String getFriendlyName(String identifier) {
		return getStrategy().getFriendlyName(identifier);
	}

	public List<ContextualPath> getSourcePaths() {
		return getStrategy().getSourcePaths();
	}

	public List<ContextualPath> getNonSourcePaths() {
		return getStrategy().getNonSourcePaths();
	}

	public List<ContextualPath> getPaths() {
		return getStrategy().getPaths();
	}

	public String getRoot() {
		return getStrategy().getRoot();
	}

	public String getCanonicalPath(ContextualPath path, JavaType javaType) {
		return getStrategy().getCanonicalPath(path, javaType);
	}

	public String getFocusedCanonicalPath(Path path, JavaType javaType) {
		return getStrategy().getFocusedCanonicalPath(path, javaType);
	}

	public String getFocusedIdentifier(Path path, String relativePath) {
		return getStrategy().getFocusedIdentifier(path, relativePath);
	}

	public String getFocusedRoot(Path path) {
		return getStrategy().getFocusedRoot(path);
	}

	public ContextualPath getFocusedPath(Path path) {
		return getStrategy().getFocusedPath(path);
	}
}
