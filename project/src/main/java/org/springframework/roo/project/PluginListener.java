package org.springframework.roo.project;

/**
 * Plugin listener interface that clients can implement in order
 * to be notified of changes to project build plugins
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface PluginListener {

	void pluginAdded(Plugin p);

	void pluginRemoved(Plugin p);
}
