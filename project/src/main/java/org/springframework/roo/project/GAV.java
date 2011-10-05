package org.springframework.roo.project;

import java.util.Arrays;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * The combination of Maven-style groupId, artifactId, and version
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GAV {

	/**
	 * Returns an instance based on the given concatenated Maven coordinates
	 *
	 * @param coordinates the groupId, artifactId, and version, separated by
	 * {@link MavenUtils#COORDINATE_SEPARATOR}
	 * @return a non-blank instance
	 * @throws IllegalArgumentException if the string is not formatted as
	 * explained above, or if any of the elements are themselves invalid.
	 */
	public static GAV getInstance(final String coordinates) {
		final String[] coordinateArray = StringUtils.delimitedListToStringArray(coordinates, MavenUtils.COORDINATE_SEPARATOR);
		Assert.isTrue(coordinateArray.length == 3, "Expected three coordinates, but found " + coordinateArray.length + ": " + Arrays.toString(coordinateArray) + "; did you use the '" + MavenUtils.COORDINATE_SEPARATOR + "' separator?");
		return new GAV(coordinateArray[0], coordinateArray[1], coordinateArray[2]);
	}

	// Fields
	private final String groupId;
	private final String artifactId;
	private final String version;

	/**
	 * Constructor
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	public GAV(final String groupId, final String artifactId, final String version) {
		// Check
		Assert.isTrue(MavenUtils.isValidMavenId(groupId), "Invalid groupId '" + groupId + "'");
		Assert.isTrue(MavenUtils.isValidMavenId(artifactId), "Invalid artifactId '" + artifactId + "'");
		Assert.hasText(version, "Version is required");

		// Assign
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getVersion() {
		return version;
	}
}
