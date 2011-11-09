package org.springframework.roo.project;

import java.util.List;

import org.springframework.roo.model.JavaType;

/**
 * A strategy for resolving logical {@link Path}s to physical file system locations.
 *
 * @author James Tyrrell
 * @since 1.2.0
 */
public interface PathResolvingStrategy {

	/**
	 * Produces a canonical path for the presented {@link ContextualPath} and relative path.
	 *
	 * @param path to use (required)
	 * @param relativePath to use (cannot be null, but may be empty if referring to the path itself)
	 * @return the canonical path to the file (never null)
	 */
	String getIdentifier(ContextualPath path, String relativePath);

	/**
	 * Attempts to determine which {@link Path} the specified canonical path falls under.
	 *
	 * @param identifier to lookup (required)
	 * @return the {@link Path}, or null if the identifier refers to a location not under a know path.
	 */
	ContextualPath getPath(String identifier);

	/**
	 * Returns a canonical path that represents the root of the presented {@link ContextualPath}.
	 *
	 * @param path to lookup (required)
	 * @return <code>null</code> if the root path cannot be determined
	 */
	String getRoot(ContextualPath path);

	/**
	 * Attempts to determine which {@link Path} the specified canonical path falls under,
	 * and then returns the relative portion of the file name.
	 *
	 * <p>
	 * See {@link org.springframework.roo.file.monitor.event.FileDetails#getRelativeSegment(String)} for related information.
	 *
	 * @param identifier to resolve (required)
	 * @return the relative segment (which may be an empty string if the identifier referred to the
	 * {@link Path} directly), or null if the identifier does not have a corresponding {@link Path}
	 */
	String getRelativeSegment(String identifier);

	/**
	 * Converts the presented canonical path into a human-friendly name.
	 *
	 * @param identifier to resolve (required)
	 * @return a human-friendly name for the identifier (required)
	 */
	String getFriendlyName(String identifier);

	/**
	 * Returns all source code {@link ContextualPath}s known to this strategy.
	 *
	 * <p>
	 * Whilst generally add-ons should know which paths contain source and which do not, this method
	 * abstracts add-ons from direct knowledge of available paths.
	 *
	 * <p>
	 * By default this method will return, in the following order:
	 * <ul>
	 * <li>{@link Path#SRC_MAIN_JAVA}</li>
	 * <li>{@link Path#SRC_MAIN_RESOURCES}</li>
	 * <li>{@link Path#SRC_TEST_JAVA}</li>
	 * <li>{@link Path#SRC_TEST_RESOURCES}</li>
	 * </ul>
	 *
	 * @return the paths, in order of compilation priority (never null and never empty)
	 */
	List<ContextualPath> getSourcePaths();

	/**
	 * Returns all known project paths.
	 *
	 * @return a non-<code>null</code> list (might be empty)
	 */
	List<ContextualPath> getPaths();

	/**
	 * Indicates whether this strategy is active. The {@link PathResolver} will
	 * typically expect exactly one strategy to be active at a time.
	 * 
	 * @return see above
	 */
	boolean isActive();

	/**
	 * @return the directory where Roo was launched
	 */
	String getRoot();

	/**
	 *
	 * @param path the focus path
	 * @param javaType the type t
	 * @return
	 */
	String getCanonicalPath(ContextualPath path, JavaType javaType);

	/**
	 *
	 * @param path
	 * @param relativePath
	 * @return
	 */
	String getFocusedIdentifier(Path path, String relativePath);

	/**
	 *
	 * @param path
	 * @return
	 */
	String getFocusedRoot(Path path);

	/**
	 *
	 * @param path
	 * @return
	 */
	ContextualPath getFocusedPath(Path path);

	/**
	 *
	 * @param path
	 * @param javaType
	 * @return
	 */
	String getFocusedCanonicalPath(Path path, JavaType javaType);
}
