package org.springframework.roo.project;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Convenient superclass for {@link PathResolvingStrategy} implementations.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractPathResolvingStrategy implements
        PathResolvingStrategy {

    // Constants
    protected static final String ROOT_MODULE = "";

    // Fields
    private String rootPath;

    // ------------ OSGi component methods ----------------

    protected void activate(final ComponentContext context) {
        final File projectDirectory = new File(StringUtils.defaultIfEmpty(
                OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
        rootPath = FileUtils.getCanonicalPath(projectDirectory);
    }

    // ------------ PathResolvingStrategy methods ----------------

    public String getFriendlyName(final String identifier) {
        Assert.notNull(identifier, "Identifier required");
        final LogicalPath p = getPath(identifier);
        if (p == null) {
            return identifier;
        }
        return p.getName() + getRelativeSegment(identifier);
    }

    public LogicalPath getPath(final String identifier) {
        final PhysicalPath parent = getApplicablePhysicalPath(identifier);
        if (parent == null) {
            return null;
        }
        return parent.getLogicalPath();
    }

    public Collection<LogicalPath> getPaths() {
        return getPaths(false);
    }

    public Collection<LogicalPath> getSourcePaths() {
        return getPaths(true);
    }

    /**
     * Obtains the {@link Path}s.
     * 
     * @param requireSource <code>true</code> to return only paths containing
     *            Java source code, or <code>false</code> to return all paths
     * @return the matching paths (never <code>null</code>)
     */
    protected abstract Collection<LogicalPath> getPaths(boolean sourceOnly);

    public String getRelativeSegment(final String identifier) {
        final PhysicalPath parent = getApplicablePhysicalPath(identifier);
        if (parent == null) {
            return null;
        }
        final FileDetails parentFile = new FileDetails(parent.getLocation(),
                null);
        return parentFile.getRelativeSegment(identifier);
    }

    protected abstract PhysicalPath getApplicablePhysicalPath(String identifier);

    public String getRoot() {
        return rootPath;
    }
}
