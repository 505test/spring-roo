package org.springframework.roo.bootstrap;

import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ExitShellRequest;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands related to add-on maintenance.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class AddOnCommands implements CommandMarker {

	private AddOnOperations addOnOperations;
	
	public AddOnCommands(AddOnOperations addOnOperations) {
		Assert.notNull(addOnOperations, "Add on operations required");
		this.addOnOperations = addOnOperations;
	}

	@CliCommand(value="addon cleanup", help="Cleans the $ROO_HOME/work directory so it only contains correct JARs as per $ROO_HOME/add-ons")
	public ExitShellRequest cleanUpCmd() {
		boolean restart = addOnOperations.cleanUp();
		return restart ? ExitShellRequest.EXIT_AND_RESTART : null;
	}
	
	@CliCommand(value="addon install", help="Installs a new add-on to the $ROO_HOME/add-ons directory")
	public ExitShellRequest installCmd(
			@CliOption(key={"","url"}, mandatory=true, help="The URL to obtain the add-on ZIP file from") String url) {
		boolean restart = addOnOperations.install(url);
		return restart ? ExitShellRequest.EXIT_AND_RESTART : null;
	}

	@CliCommand(value="addon uninstall", help="Removes an existing add-on from the $ROO_HOME/add-ons directory")
	public ExitShellRequest uninstallCmd(
			@CliOption(key={"","pattern"}, mandatory=true, help="The filename pattern to remove") String pattern) {
		boolean restart = addOnOperations.uninstall(pattern);
		return restart ? ExitShellRequest.EXIT_AND_RESTART_AFTER_ADDON_UNINSTALL : null;
	}
	
	@CliCommand(value="addon list", help="Lists add-ons installed in the $ROO_HOME/add-ons directory")
	public void listCmd() {
		addOnOperations.list();
	}


}
