package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * A request to monitor a particular directory.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class DirectoryMonitoringRequest extends MonitoringRequest {
	
	// Fields
	private final boolean watchSubtree;

	/**
	 * Constructor that accepts an array of operations
	 *
	 * @param directory the directory to monitor; must be an existing directory
	 * @param watchSubtree whether to also monitor the sub-directories of the given directory
	 * @param notifyOn the operations to notify upon (can't be empty)
	 */
	public DirectoryMonitoringRequest(final File file, final boolean watchSubtree, final FileOperation... notifyOn) {
		this(file, watchSubtree, Arrays.asList(notifyOn));
	}
	
	/**
	 * Constructor that accepts a Collection of operations
	 *
	 * @param directory the directory to monitor; must be an existing directory
	 * @param watchSubtree whether to also monitor the sub-directories of the given directory
	 * @param notifyOn the operations to notify upon (can't be empty)
	 */
	public DirectoryMonitoringRequest(final File directory, final boolean watchSubtree, final Collection<FileOperation> notifyOn) {
		super(directory, notifyOn);
		Assert.isTrue(directory.isDirectory(), "File '" + directory + "' must be a directory");
		this.watchSubtree = watchSubtree;
	}

	/**
	 * @return whether all files and folders under this directory should also be monitored (to an unlimited depth).
	 */
	public boolean isWatchSubtree() {
		return watchSubtree;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("directory", getFile());
		tsc.append("watchSubtree", watchSubtree);
		tsc.append("notifyOn", getNotifyOn());
		return tsc.toString();
	}
}
