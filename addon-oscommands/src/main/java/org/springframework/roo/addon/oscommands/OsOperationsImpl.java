package org.springframework.roo.addon.oscommands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.MavenOperationsImpl;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link OsOperations} interface.
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class OsOperationsImpl implements OsOperations {
	private static final Logger logger = HandlerUtils.getLogger(MavenOperationsImpl.class);
	@Reference private PathResolver pathResolver;
	@Reference private ProcessManager processManager;

	public void executeCommand(String command) throws IOException {
		File root = new File(getProjectRoot());
		Assert.isTrue(root.isDirectory() && root.exists(), "Project root does not currently exist as a directory ('" + root.getCanonicalPath() + "')");
		
		Thread.currentThread().setName(""); // Prevent thread name from being presented in Roo shell
		Process p = Runtime.getRuntime().exec(command, null, root);
		// Ensure separate threads are used for logging, as per ROO-652
		LoggingInputStream input = new LoggingInputStream(p.getInputStream(), processManager);
		LoggingInputStream errors = new LoggingInputStream(p.getErrorStream(), processManager);

		p.getOutputStream().close();
		input.start();
		errors.start();

		try {
			if (p.waitFor() != 0) {
				logger.warning("The command '" + command + "' did not complete successfully");
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private String getProjectRoot() {
		return pathResolver.getRoot(Path.ROOT);
	}
	
	private class LoggingInputStream extends Thread {
		private BufferedReader reader;
		private ProcessManager processManager;

		public LoggingInputStream(InputStream inputStream, ProcessManager processManager) {
			this.reader = new BufferedReader(new InputStreamReader(inputStream));
			this.processManager = processManager;
		}

		@Override
		public void run() {
			ActiveProcessManager.setActiveProcessManager(processManager);
			Thread.currentThread().setName(""); // Prevent thread name from being presented in Roo shell
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("[ERROR]")) {
						logger.severe(line);
					} else if (line.startsWith("[WARNING]")) {
						logger.warning(line);
					} else {
						logger.info(line);
					}
				}
			} catch (IOException e) {
				if (e.getMessage().contains("No such file or directory") || // For *nix/Mac
					e.getMessage().contains("CreateProcess error=2")) { // For Windows
					logger.severe("Could not locate executable; please ensure command is in your path");
				}
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ignored) {
					}
				}
				ActiveProcessManager.clearActiveProcessManager();
			}
		}
	}	
	
}