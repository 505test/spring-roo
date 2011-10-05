package org.springframework.roo.process.manager.internal;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.process.manager.CommandCallback;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a {@link CommandCallback} to start or stop monitoring a particular file path.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class MonitoringRequestCommand implements CommandCallback<Boolean> {

	// Fields
	private final FileMonitorService fileMonitorService;
	private final MonitoringRequest monitoringRequest;
	private final boolean add;

	public MonitoringRequestCommand(final FileMonitorService fileMonitorService, final MonitoringRequest monitoringRequest, final boolean add) {
		Assert.notNull(fileMonitorService, "File monitor service required");
		Assert.notNull(monitoringRequest, "Request required");
		this.fileMonitorService = fileMonitorService;
		this.monitoringRequest = monitoringRequest;
		this.add = add;
	}

	public Boolean callback() {
		boolean result;
		if (add) {
			result = this.fileMonitorService.add(monitoringRequest);
		} else {
			result = this.fileMonitorService.remove(monitoringRequest);
		}
		this.fileMonitorService.scanAll();
		return result;
	}
}
