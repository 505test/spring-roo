package org.springframework.roo.process.manager;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.process.manager.event.ProcessManagerStatusProvider;

/**
 * Provides coordinated execution of major ROO operations.
 * 
 * <p>
 * A {@link ProcessManager} delivers:
 * <ul>
 * <li>A well-defined state publication model via {@link ProcessManagerStatusProvider}</li>
 * <li>Startup-time registration of a {@link InitialMonitoringRequest} (after the 
 * {@link #completeStartup()} method has been called)</li>
 * <li>Specific polling of {@link FileMonitorService} at well-defined times</li>
 * <li>The ability to execute {@link CommandCallback}s for user-requested operations</li>
 * <li>An assurance the above is conducted within a "transaction-like" model</li>
 * </ul>
 * 
 * <p>
 * Once constructed, all methods in {@link ProcessManager} operate in a "transaction-like" manner.
 * This is achieved by use of a {@link FileManager} that shares the same {@link UndoManager}.
 * 
 * <p>
 * Once available, a {@link ProcessManager} will ordinarily wait for either {@link #backgroundPoll()}
 * or {@link #execute(CommandCallback)}. It will block until the state of the {@link ProcessManager}
 * returns to {@link ProcessManagerStatus#AVAILABLE}. 
 * 
 * <p>
 * {@link ProcessManager} guarantees:
 * <ul>
 * <li>The status will remain {@link ProcessManagerStatus#STARTING} until {@link #completeStartup()} has been called.
 * This is intended to allow objects depending on {@link ProcessManager} or other {@link MetadataService}s to
 * be constructed and register for events. The {@link InitialMonitoringRequest} will only be registered
 * during {@link #completeStartup()}, which therefore simplifies the design of dependent objects as they generally
 * (i) don't need to retrieve metadata produced before they were listening and (ii) can freely modify the
 * file system pursuant to {@link FileManager} with assurance of correct "transaction" behaviour.</li>
 * <li>At the end of a successful startup, background poll or command execution, {@link UndoManager#reset()} 
 * method will be called.</li>
 * <li>An uncaught exception will cause {@link UndoManager#undo()} to be called (before re-throwing the
 * exception).</li>
 * <li>A {@link FileMonitorService#scanAll()} will be called after a command is executed, and will continue
 * to be called until such time as it does not return any further changes. Such calls will occur within
 * the scope of the same "transaction" as used for the command.</li>
 * </ul>
 * 
 * <p>
 * {@link ProcessManager} implementations also guarantee to update {@link ActiveProcessManager} whenever
 * running an operation, and clear it when an operation completes.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ProcessManager extends ProcessManagerStatusProvider {
	
	/**
	 * Change the status from {@link ProcessManagerStatus#STARTING} to {@link ProcessManagerStatus#AVAILABLE}
	 * and then immediately register the current working directory for monitoring. These functions should
	 * occur within the scope of a "transaction".
	 * 
	 * <p>
	 * This method may not throw any {@link RuntimeException}, as this will generally cause the dependency management
	 * container to abort. If there was a failure, a normal roll-back should occur and the message be logged.
	 * 
	 * <p>
	 * This method should throw an exception if the status is not {@link ProcessManagerStatus#STARTING}.
	 */
	void completeStartup();
	
	/**
	 * Perform a file system poll within a "transaction". This method returns immediately if the
	 * status is not {@link ProcessManagerStatus#AVAILABLE}.
	 * 
	 * <p>
	 * This method may throw {@link RuntimeException}s that occurred while background processing.
	 * 
	 * @return whether the poll was actually performed or not
	 */
	boolean backgroundPoll();
	
	/**
	 * Execute a user command within a "transaction". This method blocks until
     * {@link ProcessManagerStatus#AVAILABLE}.
	 * 
	 * <p>
	 * This method may throw {@link RuntimeException}s that occurred while executing.
	 * 
	 * @param <T> the class of the object that {@link CommandCallback#callback()} will return (required)
	 * @param callback the callback to actually executed (required)
	 * @return the result of executing the callback
	 */
	<T> T execute(CommandCallback<T> callback);
	
	/**
	 * @return true if the system is in development mode, which generally means more detailed 
	 * diagnostics are requested from add-ons (defaults to false)
	 */
	boolean isDevelopmentMode();

	public void timerBasedPoll();
	
	public void setDevelopmentMode(boolean developmentMode);

	public void setMinimumDelayBetweenPoll(long minimumDelayBetweenPoll);

	public long getMinimumDelayBetweenPoll();

	public long getLastPollDuration();
	
}
