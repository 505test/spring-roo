package org.springframework.roo.shell.jline;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Logger;

import jline.ANSIBuffer;
import jline.ConsoleReader;
import jline.WindowsTerminal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ExitShellRequest;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatusListener;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.ClassUtils;

/**
 * Uses the feature-rich <a href="http://jline.sourceforge.net/">JLine</a> library to provide an interactive shell.
 * 
 * <p>
 * Due to Windows' lack of color ANSI services out-of-the-box, this implementation automatically detects the classpath
 * presence of <a href="http://jansi.fusesource.org/">Jansi</a> and uses it if present. If in addition to Jansi, the
 * ANSI support will include colour if <a href="https://jna.dev.java.net/">JNA</a> library is also available. Neither
 * of these libraries are necessary for *nix machines, which support colour ANSI without any special effort. This
 * implementation has been written to use reflection in order to avoid hard dependencies on Jansi or JNA.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public final class JLineShell extends AbstractShell implements CommandMarker, Shell, Runnable {

	private static final String ANSI_CONSOLE_CLASSNAME = "org.fusesource.jansi.AnsiConsole";
	private static final boolean JANSI_AVAILABLE = ClassUtils.isPresent(ANSI_CONSOLE_CLASSNAME, JLineShell.class.getClassLoader());
	private ComponentContext context;
	
    private ConsoleReader reader;
    private boolean developmentMode = false;
    private FileWriter fileLog;
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ShellStatusListener statusListener; // ROO-836
	
	protected void activate(ComponentContext context) {
		this.context = context;
        Thread thread = new Thread(this, "JLine Shell");
        thread.start();
	}
	
	protected void deactivate(ComponentContext context) {
		this.context = null;
		if (statusListener != null) {
			removeShellStatusListener(statusListener);
		}
	}
	
	public void run() {
		try {
			if (JANSI_AVAILABLE && JLineLogHandler.WINDOWS_OS) {
				try {
					reader = createAnsiWindowsReader();
				} catch (Exception e) {
					// try again using default ConsoleReader constructor 
					logger.warning("Can't initialize jansi AnsiConsole, falling back to default: " + e);
				}
			}
			if (reader == null) reader = new ConsoleReader();
		} catch (IOException ioe) {
			throw new IllegalStateException("Cannot start console class", ioe);
		}
		
		setPromptPath(null);
		
        JLineLogHandler handler = new JLineLogHandler(reader, this);
        JLineLogHandler.prohibitRedraw(); // affects this thread only
		Logger mainLogger = Logger.getLogger("");
        removeHandlers(mainLogger);
        mainLogger.addHandler(handler);

        reader.addCompletor(new JLineCompletorAdapter(getParser()));
		
		reader.setBellEnabled(true);
		if (Boolean.getBoolean("jline.nobell")) {
        	reader.setBellEnabled(false);
		}
                
		// reader.setDebug(new PrintWriter(new FileWriter("writer.debug", true)));
		
		openFileLogIfPossible();
		
        logger.info(version(null));
        logger.info("Welcome to Spring Roo. For assistance press " + completionKeys + " or type \"hint\" then hit ENTER.");
        
        setShellStatus(ShellStatus.STARTED);

        // Handle any "execute-then-quit" operation
        String rooArgs = System.getProperty("roo.args");
        if (rooArgs != null && !"".equals(rooArgs)) {
            setShellStatus(ShellStatus.USER_INPUT);
        	boolean success = executeCommand(rooArgs);
            if (exitShellRequest == null) {
            	// The command itself did not specify an exit shell code, so we'll fall back to something sensible here
                executeCommand("quit"); // ROO-839
               	exitShellRequest = success ? ExitShellRequest.NORMAL_EXIT : ExitShellRequest.FATAL_EXIT;
            }
            setShellStatus(ShellStatus.SHUTTING_DOWN);
        } else {
            // Normal RPEL processing
        	promptLoop();
        }
	}

	private void removeHandlers(Logger l) {
		Handler[] handlers = l.getHandlers();
		if (handlers != null && handlers.length > 0) {
			for (Handler h : handlers) {
				l.removeHandler(h);
			}
		}
	}
	
	public void stop() {}
	
	@Override
	public void setPromptPath(String path) {
        if (reader.getTerminal().isANSISupported()) {
        	ANSIBuffer ansi = JLineLogHandler.getANSIBuffer();
            if ("".equals(path) || path == null) {
            	shellPrompt = ansi.yellow("roo> ").toString();
    		} else {
    			shellPrompt = ansi.cyan(path).yellow(" roo> ").toString();
    		}
        } else {
        	// the superclass will do for this non-ANSI terminal
        	super.setPromptPath(path);
        }
        
		// the shellPrompt is now correct; let's ensure it now gets used
        reader.setDefaultPrompt(JLineShell.shellPrompt);
	}

	private ConsoleReader createAnsiWindowsReader() throws Exception {
		// get decorated OutputStream that parses ANSI-codes
		@SuppressWarnings("unchecked")
		final PrintStream ansiOut = (PrintStream) ClassUtils.forName(ANSI_CONSOLE_CLASSNAME, JLineShell.class.getClassLoader()).getMethod("out").invoke(null);
		WindowsTerminal ansiTerminal = new WindowsTerminal() {
			public boolean isANSISupported() { return true; }
		};
		ansiTerminal.initializeTerminal();
		// make sure to reset the original shell's colors on shutdown by closing the stream
		statusListener = new ShellStatusListener() {
			public void onShellStatusChange(ShellStatus oldStatus, ShellStatus newStatus) {
				if (newStatus == ShellStatus.SHUTTING_DOWN) {
					ansiOut.close();
				}
			}
		};
		addShellStatusListener(statusListener);
		return new ConsoleReader(
			new FileInputStream(FileDescriptor.in),
        	new PrintWriter(
        		new OutputStreamWriter(ansiOut,
        			// default to Cp850 encoding for Windows console output (ROO-439)
        			System.getProperty("jline.WindowsTerminal.output.encoding", "Cp850"))),
    		null,
			ansiTerminal
		);
	}
	
    public void promptLoop() {
    	setShellStatus(ShellStatus.USER_INPUT);
    	String line;
    	
    	try {
            while (exitShellRequest == null && ( (line = reader.readLine() ) != null) ) {
            	JLineLogHandler.resetMessageTracking();
            	setShellStatus(ShellStatus.USER_INPUT);

            	if ("".equals(line)) {
                	continue;
                }
                
                executeCommand(line);
            }
        } catch (IOException ioe) {
        	throw new IllegalStateException("Shell line reading failure", ioe);
        }
        
        setShellStatus(ShellStatus.SHUTTING_DOWN);
    }
    
	public void setDevelopmentMode(boolean developmentMode) {
		JLineLogHandler.setIncludeThreadName(developmentMode);
		this.developmentMode = developmentMode;
	}

	public boolean isDevelopmentMode() {
		return this.developmentMode;
	}
	
	private void openFileLogIfPossible() {
		try {
			fileLog = new FileWriter("log.roo", true);
			// first write, so let's record the date and time of the first user command
			fileLog.write("// Spring Roo " + versionInfo() + " log opened at " + df.format(new Date()) + "\n");
			fileLog.flush();
		} catch (IOException ignoreIt) {}
	}
	
	@Override
	protected void logCommandToOutput(String processedLine) {
		if (fileLog == null) {
			openFileLogIfPossible();
			if (fileLog == null) {
				// still failing, so give up
				return;
			}
		}
		try {
			fileLog.write(processedLine + "\n"); // unix line endings only from Roo
			fileLog.flush(); // so tail -f will show it's working
			if (getExitShellRequest() != null) {
				// shutting down, so close our file (we can always reopen it later if needed)
				fileLog.write("// Spring Roo " + versionInfo() + " log closed at " + df.format(new Date()) + "\n");
				fileLog.flush();
				fileLog.close();
				fileLog = null;
			}
		} catch (IOException ignoreIt) {}
	}

	/**
	 * Obtains the "roo.home" from the system property, throwing an exception if missing.
	 *
	 * @return the 'roo.home' system property
	 */
	@Override
	protected String getHomeAsString() {
		String rooHome = System.getProperty("roo.home");
		Assert.hasText(rooHome, "roo.home system property is not set");
		return rooHome;
	}

	@Override
	protected ComponentContext getContext() {
		return context;
	}
	
}
