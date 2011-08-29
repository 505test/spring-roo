package org.springframework.roo.shell.jline.osgi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jline.ANSIBuffer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.Parser;
import org.springframework.roo.shell.jline.JLineShell;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * OSGi component launcher for {@link JLineShell}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class JLineShellComponent extends JLineShell {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	// Fields
    @Reference private ExecutionStrategy executionStrategy;
    @Reference private Parser parser;
	@Reference private UrlInputStreamService urlInputStreamService;
	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
		Thread thread = new Thread(this, "Spring Roo JLine Shell");
		thread.start();
	}

	protected void deactivate(ComponentContext context) {
		this.context = null;
		closeShell();
	}
	
	@Override
	protected Collection<URI> findResources(final String path) {
		// For an OSGi bundle search, we add the root prefix to the given path
		return OSGiUtils.findEntriesByPath(context.getBundleContext(), OSGiUtils.ROOT_PATH + path);
	}

	@Override
	protected ExecutionStrategy getExecutionStrategy() {
		return executionStrategy;
	}

	@Override
	protected Parser getParser() {
		return parser;
	}

	@Override
	public String getStartupNotifications() {
		return getLatestFavouriteTweet();
	}

	private String getLatestFavouriteTweet() {
		// Access Twitter's REST API
		String string = sendGetRequest("http://api.twitter.com/1/favorites.json", "id=SpringRoo&count=1");
		if (StringUtils.hasText(string)) {
			// Parse the returned JSON. This is a once off operation so we can used JSONValue.parse without penalty
			JSONArray object = (JSONArray) JSONValue.parse(string);
			JSONObject jsonObject = (JSONObject) object.get(0);
			String tweet = (String) jsonObject.get("text");
			// We only want one line
			tweet = tweet.replaceAll(LINE_SEPARATOR, " ");
			List<String> words = Arrays.asList(tweet.split(" "));
			StringBuilder sb = new StringBuilder();
			// Add in Roo's twitter account to give context to the notification
			sb.append(ANSIBuffer.ANSICodes.attrib(7)).append("@SpringRoo").append(ANSIBuffer.ANSICodes.attrib(0));
			sb.append(" ");
			// We want to colourise certain words. The codes used here should be moved to a ShellUtils and include a few helper methods
			// This is a basic attempt at pattern identification, it should be adequate in most cases although may be incorrect for URLs.
			// For example url.com/ttym: is valid by may mean "url.com/ttym" + ":"
			for (String word : words) {
				if (word.startsWith("http://") || word.startsWith("https://")) {
					// Green and underlined for URLs
					sb.append(ANSIBuffer.ANSICodes.attrib(32)).append(ANSIBuffer.ANSICodes.attrib(4)).append(word).append(ANSIBuffer.ANSICodes.attrib(0));
				} else if (word.startsWith("@")) {
					// Magenta for user references
					sb.append(ANSIBuffer.ANSICodes.attrib(35)).append(word).append(ANSIBuffer.ANSICodes.attrib(0));
				} else if (word.startsWith("#")) {
					// Cyan for hash tags
					sb.append(ANSIBuffer.ANSICodes.attrib(36)).append(word).append(ANSIBuffer.ANSICodes.attrib(0));
				} else {
					// All else default yellow
					sb.append(word);
				}
				// Add back separator
				sb.append(" ");
			}
			return sb.toString();
		}
		return null;
	}

	// TODO: This should probably be moved to a HTTP service of some sort - JTT 29/08/11
	private String sendGetRequest(String endpoint, String requestParameters) {
		String result = null;
		if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
			// Send a GET request to the servlet
			try {
				// Send data
				String urlStr = endpoint;
				if (requestParameters != null && requestParameters.length() > 0) {
					urlStr += "?" + requestParameters;
				}
				URL url = new URL(urlStr);
				InputStream inputStream = urlInputStreamService.openConnection(url);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				result = sb.toString();
			} catch (Exception ignored) {}
		}
		return result;
	}
}