package org.springframework.roo.shell.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.ExitShellRequest;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.event.AbstractShellStatusPublisher;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Provides a base {@link Shell} implementation.
 * 
 * @author Ben Alex
 *
 */
public abstract class AbstractShell extends AbstractShellStatusPublisher implements Shell {

	protected final Logger logger = HandlerUtils.getLogger(getClass());
    protected boolean inBlockComment = false;
    protected ExitShellRequest exitShellRequest = null;
    protected ExecutionStrategy executionStrategy;
	public static String shellPrompt = "roo> ";
	public static String completionKeys = "TAB";
	
    protected AbstractShell(ExecutionStrategy executionStrategy) {
        Assert.notNull(executionStrategy, "Execution strategy required");
        this.executionStrategy = executionStrategy;
    }
    
	@CliCommand(value={"script"}, help="Parses the specified resource file and executes its commands")
	public void script(@CliOption(key={"","file"}, help="The file to locate and execute", mandatory=true) File resource,
						@CliOption(key="lineNumbers", mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="false", help="Display line numbers when executing the script") boolean lineNumbers) {
		Assert.notNull(resource, "Resource to parser is required");
		
		long started = new Date().getTime();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(resource);
		} catch (FileNotFoundException tryTheClassLoaderInstead) {}
		
		if (inputStream == null) {
			// Try to find the resource via the classloader
			inputStream = getClass().getResourceAsStream("/" + resource.getName());
			Assert.notNull(inputStream, "Resource '" + resource + "' not found on disk or in classpath");
		}
		
	    try {
	    	BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
	        String line;
	        int i = 0;
	        while ((line = in.readLine()) != null) {
	        	i++;
	        	if (lineNumbers) {
		        	logger.fine("Line " + i + ": " + line);
	        	} else {
		        	logger.fine(line);
	        	}
	        	if (!"".equals(line.trim())) {
		        	boolean success = executeCommand(line);
		        	if (!success) {
		        		// Abort script processing, given something went wrong
		        		throw new IllegalStateException("Script execution aborted");
		        	}
	        	}
	        }
	        in.close();
	    } catch (IOException e) {
	    	throw new IllegalStateException(e);
	    }
	    logger.fine("Milliseconds required: " + (new Date().getTime() - started));
	}
	
	public boolean executeCommand(String line) {
		// another command was attempted
    	setShellStatus(ShellStatus.PARSING);

		try {
			// We support simple block comments; ie a single pair per line
			if (!inBlockComment && line.contains("/*")) {
				blockCommentBegin();
				String lhs = line.substring(0, line.lastIndexOf("/*"));
				if (line.contains("*/")) {
					line = lhs + line.substring(line.lastIndexOf("*/")+2);
					blockCommentFinish();
				} else {
					line = lhs;
				}
			}
			if (inBlockComment) {
				if (!line.contains("*/")) {
					return true;
				}
				blockCommentFinish();
				line = line.substring(line.lastIndexOf("*/")+2);
			}
			// We also support inline comments
			if (!inBlockComment && line.contains("//")) {
				line = line.substring(0, line.indexOf("//"));
			}
			if ("".equals(line.trim())) {
		    	setShellStatus(ShellStatus.EXECUTION_COMPLETE);
				return true;
			}
			ParseResult parseResult = getParser().parse(line);
			if (parseResult == null) {
				return false;
			} else {
		    	setShellStatus(ShellStatus.EXECUTING);
		    	Object result = executionStrategy.execute(parseResult);
		    	setShellStatus(ShellStatus.EXECUTION_RESULT_PROCESSING);
		    	if (result != null) {
		    		if (result instanceof ExitShellRequest) {
		    			exitShellRequest = (ExitShellRequest) result;
		    		} else if (result instanceof Iterable<?>) {
		    			for (Object o : (Iterable<?>)result) {
		    				logger.info(o.toString());
		    			}
		    		} else {
	    				logger.info(result.toString());
		    		}
		    	}
		    }
		} catch (RuntimeException ex) {
	    	setShellStatus(ShellStatus.EXECUTION_RESULT_PROCESSING);
			// We rely on execution strategy to log it
	    	//Throwable root = ExceptionUtils.extractRootCause(ex);
			//logger.log(Level.FINE, root.getMessage());
	    	try {
	    		logCommandIfRequired(line, false);
	    	} catch (Exception ignoreIt) {}
			return false;
		} finally {
			setShellStatus(ShellStatus.EXECUTION_COMPLETE);
		}
		logCommandIfRequired(line, true);
		return true;
	}
	
	/**
	 * Allows a subclass to log the execution of a well-formed command. This is invoked after a command
	 * has completed, and indicates whether the command returned normally or returned an exception. Note
	 * that attempted commands that are not well-formed (eg they are missing a mandatory argument) will
	 * never be presented to this method, as the command execution is never actually attempted in those
	 * cases. This method is only invoked if an attempt is made to execute a particular command.
	 * 
	 * <p>
	 * Implementations should consider specially handling the "script" commands, and also
	 * indicating whether a command was successful or not. Implementations that wish to behave
	 * consistently with other {@link AbstractShell} subclasses are encouraged to simply override
	 * {@link #logCommandToOutput(String)} instead, and only override this method if you actually
	 * need to fine-tune the output logic.
	 *  
	 * @param line the parsed line (any comments have been removed; never null)
	 * @param successful if the command was successful or not
	 */
	protected void logCommandIfRequired(String line, boolean successful) {
		if (line.startsWith("script")) {
			logCommandToOutput((successful ? "// " : "// [failed] ") + line);
		} else {
			logCommandToOutput((successful ? "" : "// [failed] ") + line);
		}
	}
	
	/**
	 * Allows a subclass to actually write the resulting logged command to some form of output. This
	 * frees subclasses from needing to implement the logic within {@link #logCommandIfRequired(String, boolean)}.
	 *
	 * <p>
	 * Implementations should invoke {@link #getExitShellRequest()} to monitor any attempts to exit the shell and
	 * release resources such as output log files.
	 * 
	 * @param processedLine the line that should be appended to some type of output (excluding the \n character)
	 */
	protected void logCommandToOutput(String processedLine) {
		// logger.severe(processedLine);
	}

	/**
	 * Base implementation of the {@link Shell#setPromptPath(String)} method, designed for simple shell
	 * implementations. Advanced implementations (eg those that support ANSI codes etc) will likely want
	 * to override this method and set the {@link #shellPrompt} variable directly.
	 * 
	 * @param path to set (can be null or empty; must NOT be formatted in any special way eg ANSI codes)
	 */
	public void setPromptPath(String path) {
		if ("".equals(path) || path == null) {
			shellPrompt = "roo> ";
		} else {
			shellPrompt = path + " roo> ";
		}
	}

	public ExitShellRequest getExitShellRequest() {
		return exitShellRequest;
	}

	@CliCommand(value={"exit", "quit"}, help="Exits the shell")
	public ExitShellRequest requestExit() {
		return ExitShellRequest.NORMAL_EXIT;
	}

	@CliCommand(value={"//", ";"}, help="Inline comment markers (start of line only)")
	public void inlineComment() {}

	@CliCommand(value={"/*"}, help="Start of block comment")
	public void blockCommentBegin() {
		Assert.isTrue(!inBlockComment, "Cannot open a new block comment when one already active");
		inBlockComment = true;
	}

	@CliCommand(value={"*/"}, help="End of block comment")
	public void blockCommentFinish() {
		Assert.isTrue(inBlockComment, "Cannot close a block comment when it has not been opened");
		inBlockComment = false;
	}

	@CliCommand(value={"system properties"}, help="Shows the shell's properties")
	public String props() {
		Properties properties = System.getProperties();
		SortedSet<String> data = new TreeSet<String>();
		for (Object property : properties.keySet()) {
			Object value = properties.get(property);
			data.add(property + " = " + value);
		}
		
		StringBuilder sb = new StringBuilder();
		for (String line : data) {
			sb.append(line).append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	@CliCommand(value={"date"}, help="Displays the local date and time")
	public String date() {
		return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date());
	}

	@CliCommand(value={"version"}, help="Displays shell version")
	public String version(@CliOption(key="", help="Special version flags") String extra) {
    	StringBuilder sb = new StringBuilder();
		
    	if ("jaime".equals(extra)) {
    		sb.append("               /\\ /l").append(System.getProperty("line.separator"));
    		sb.append("               ((.Y(!").append(System.getProperty("line.separator"));
    		sb.append("                \\ |/").append(System.getProperty("line.separator"));
    		sb.append("                /  6~6,").append(System.getProperty("line.separator"));
    		sb.append("                \\ _    +-.").append(System.getProperty("line.separator"));
    		sb.append("                 \\`-=--^-' \\").append(System.getProperty("line.separator"));
    		sb.append("                  \\   \\     |\\--------------------------+").append(System.getProperty("line.separator"));
    		sb.append("                 _/    \\    |  Thanks for loading ROO!  |").append(System.getProperty("line.separator"));
    		sb.append("                (  .    Y   +---------------------------+").append(System.getProperty("line.separator"));
    		sb.append("               /\"\\ `---^--v---.").append(System.getProperty("line.separator"));
    		sb.append("              / _ `---\"T~~\\/~\\/").append(System.getProperty("line.separator"));
    		sb.append("             / \" ~\\.      !").append(System.getProperty("line.separator"));
    		sb.append("       _    Y      Y.~~~ /'").append(System.getProperty("line.separator"));
    		sb.append("      Y^|   |      | ROO 7").append(System.getProperty("line.separator"));
    		sb.append("      | l   |     / .   /'").append(System.getProperty("line.separator"));
    		sb.append("      | `L  | Y .^/   ~T").append(System.getProperty("line.separator"));
    		sb.append("      |  l  ! | |/  | |               ____  ____  ____").append(System.getProperty("line.separator"));
    		sb.append("      | .`\\/' | Y   | !              / __ \\/ __ \\/ __ \\").append(System.getProperty("line.separator"));
    		sb.append("      l  \"~   j l   j L______       / /_/ / / / / / / /").append(System.getProperty("line.separator"));
    		sb.append("       \\,____{ __\"\" ~ __ ,\\_,\\_    / _, _/ /_/ / /_/ /").append(System.getProperty("line.separator"));
    		sb.append("    ~~~~~~~~~~~~~~~~~~~~~~~~~~~   /_/ |_|\\____/\\____/").append(" ").append(versionInfo()).append(System.getProperty("line.separator"));
    		return sb.toString();
    	}
    	
    	sb.append("    ____  ____  ____  ").append(System.getProperty("line.separator")); 
		sb.append("   / __ \\/ __ \\/ __ \\ ").append(System.getProperty("line.separator"));
		sb.append("  / /_/ / / / / / / / ").append(System.getProperty("line.separator"));
		sb.append(" / _, _/ /_/ / /_/ /  ").append(System.getProperty("line.separator"));
		sb.append("/_/ |_|\\____/\\____/   ").append(" ").append(versionInfo()).append(System.getProperty("line.separator"));
		sb.append(System.getProperty("line.separator"));
		
		return sb.toString();
	}

	public static String versionInfo() {
		// Try to determine the SVN version
		String svnRev = null;
		try {
			String classContainer = AbstractShell.class.getProtectionDomain().getCodeSource().getLocation().toString();
			
			if (classContainer.endsWith(".jar")) {
				// Attempt to obtain the SVN version from the manifest
				URL manifestUrl = new URL("jar:" + classContainer + "!/META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(manifestUrl.openStream());
				svnRev = manifest.getMainAttributes().getValue("Implementation-Build");
			} else {
				// We're likely in development mode, so try to obtain it via the "svnversion" external tool
				if (classContainer.startsWith("file:")) {
					String location = classContainer.substring(5);
					File f = new File(location).getParentFile().getParentFile().getParentFile();
					if (f.exists()) {
						String line;
						Process p = Runtime.getRuntime().exec("svnversion -n " + f.getCanonicalPath());
					    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					    try {
						    while ((line = input.readLine()) != null) {
						    	svnRev = line;
						    }
					    } finally {
				    		input.close();
					    }
					}
				}
			}
			
		} catch (Exception ignoreAndMoveOn) {}
		
		String formalBuild = AbstractShell.class.getPackage().getImplementationVersion();
		
		// Build the version details
		StringBuilder sb = new StringBuilder();
		if (formalBuild == null) {
			sb.append("ENGINEERING BUILD");
		} else {
			sb.append(formalBuild);
		}

		if (svnRev == null) {
			sb.append(" [rev unknown]");
		} else {
			sb.append(" [rev ").append(svnRev).append("]");
		}
		
		return sb.toString();
	}

	public String getShellPrompt() {
		return shellPrompt;
	}
	
	/**
	 * Obtains the home directory for the current shell instance.
	 *
	 * <p>
	 * Note: calls the {@link #getHomeAsString()} method to allow subclasses to provide the home directory location as 
	 * string using different environment-specific strategies. 
 	 *
	 * <p>
	 * If the path indicated by {@link #getHomeAsString()} exists and refers to a directory, that directory
	 * is returned.
	 * 
	 * <p>
	 * If the path indicated by {@link #getHomeAsString()} exists and refers to a file, an exception is thrown.
	 * 
	 * <p>
	 * If the path indicated by {@link #getHomeAsString()} does not exist, it will be created as a directory.
	 * If this fails, an exception will be thrown.
	 * 
	 * @return the home directory for the current shell instance (which is guaranteed to exist and be a directory)
	 */
	public File getHome() {
		String rooHome = getHomeAsString();
		File f = new File(rooHome);
		Assert.isTrue(!f.exists() || (f.exists() && f.isDirectory()), "Path '" + f.getAbsolutePath() + "' must be a directory, or it must not exist");
		if (!f.exists()) {
			f.mkdirs();
		}
		Assert.isTrue(f.exists() && f.isDirectory(), "Path '" + f.getAbsolutePath() + "' is not a directory; please specify roo.home system property correctly");
		return f;
	}

	protected abstract String getHomeAsString();
}
