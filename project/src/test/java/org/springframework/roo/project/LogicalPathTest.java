package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;

import org.junit.Test;

/**
 * Unit test of {@link LogicalPath}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class LogicalPathTest {
	
	// Constants
	private static final Path PATH = Path.SRC_TEST_JAVA;	// arbitrary; can't be mocked
	private static final String MODULE_NAME = "web";
	private static final String MODULE_PLUS_PATH = MODULE_NAME + MODULE_PATH_SEPARATOR + PATH.name();

	@Test
	public void testGetInstanceWithNullModuleName() {
		assertGetInstance(null, "", PATH.toString());
	}

	@Test
	public void testGetInstanceWithEmptyModuleName() {
		assertGetInstance("", "", PATH.toString());
	}

	@Test
	public void testGetInstanceWithBlankModuleName() {
		assertGetInstance(" ", "", PATH.toString());
	}

	@Test
	public void testGetInstanceWithNonBlankModuleName() {
		assertGetInstance(MODULE_NAME, MODULE_NAME, MODULE_NAME + MODULE_PATH_SEPARATOR + PATH.toString());
	}
	
	@Test
	public void testGetInstanceFromPath() {
		// Invoke
		final LogicalPath instance = LogicalPath.getInstance(PATH);
		
		// Check
		assertContextualPath(instance, PATH.toString(), "");
	}
	
	/**
	 * Asserts that calling {@link LogicalPath#getInstance(Path, String)}
	 * with the given module name results in the expected behaviour
	 * 
	 * @param inputModuleName
	 * @param expectedModuleName
	 * @param expectedInstanceName
	 */
	private void assertGetInstance(final String inputModuleName, final String expectedModuleName, final String expectedInstanceName) {
		// Set up
		
		// Invoke
		final LogicalPath instance = LogicalPath.getInstance(PATH, inputModuleName);
		
		// Check
		assertContextualPath(instance, expectedInstanceName, expectedModuleName);
	}

	/**
	 * Asserts that the given instance has the expected values
	 * 
	 * @param instance the instance to check (required)
	 * @param expectedInstanceName
	 * @param expectedModuleName
	 */
	private void assertContextualPath(final LogicalPath instance, final String expectedInstanceName, final String expectedModuleName) {
		assertEquals(expectedInstanceName, instance.getName());
		assertEquals(expectedModuleName, instance.getModule());
		assertEquals(PATH, instance.getPath());
		assertEquals(instance.getName(), instance.toString());
	}
	
	@Test
	public void testSamePathsInSameModuleAreEqual() {
		// Set up
		final LogicalPath instance1 = LogicalPath.getInstance(PATH, MODULE_NAME);
		final LogicalPath instance2 = LogicalPath.getInstance(PATH, MODULE_NAME);
		
		// Invoke
		final boolean equal = instance1.equals(instance2) && instance2.equals(instance1);
		
		// Check
		assertTrue(equal);
	}
	
	@Test
	public void testSamePathsInDifferentModulesAreNotEqual() {
		// Set up
		final LogicalPath instance1 = LogicalPath.getInstance(PATH, "module1");
		final LogicalPath instance2 = LogicalPath.getInstance(PATH, "module2");
		
		// Invoke
		final boolean equal = instance1.equals(instance2) || instance2.equals(instance1);
		
		// Check
		assertFalse(equal);
	}
	
	@Test
	public void testDoesNotEqualOtherType() {
		assertFalse(LogicalPath.getInstance(PATH).equals(PATH));
	}
	
	@Test
	public void testGetInstanceFromPathNameOnly() {
		// Invoke
		final LogicalPath instance = LogicalPath.getInstance(PATH.name());
		
		// Check
		assertContextualPath(instance, PATH.name(), "");
	}
	
	@Test
	public void testGetInstanceFromCombinedPathAndModuleName() {
		// Invoke
		final LogicalPath instance = LogicalPath.getInstance(MODULE_PLUS_PATH);
		
		// Check
		assertContextualPath(instance, MODULE_PLUS_PATH, MODULE_NAME);
	}
	
	@Test(expected = NullPointerException.class)
	public void testCompareToNull() {
		LogicalPath.getInstance(PATH).compareTo(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetInstanceFromNullString() {
		LogicalPath.getInstance((String) null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetInstanceFromEmptyString() {
		LogicalPath.getInstance("");
	}	
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetInstanceFromBlankString() {
		LogicalPath.getInstance(" ");
	}
	
	@Test
	public void testModuleRootIsNotProjectRoot() {
		assertFalse(LogicalPath.getInstance(Path.ROOT, "web").isProjectRoot());
	}
	
	@Test
	public void testNonRootPathIsNotProjectRoot() {
		assertFalse(LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, null).isProjectRoot());
	}
	
	@Test
	public void testProjectRootIsProjectRoot() {
		assertTrue(LogicalPath.getInstance(Path.ROOT, null).isProjectRoot());
	}
}
