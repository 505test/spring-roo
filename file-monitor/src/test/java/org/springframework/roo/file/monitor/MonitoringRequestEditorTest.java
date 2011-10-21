package org.springframework.roo.file.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.file.monitor.event.FileOperation.DELETED;
import static org.springframework.roo.file.monitor.event.FileOperation.RENAMED;
import static org.springframework.roo.file.monitor.event.FileOperation.UPDATED;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * Unit test of {@link MonitoringRequestEditor}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MonitoringRequestEditorTest {

	// Constants
	private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
	
	// Fixture
	private File testDirectory;
	private File testFile;
	private MonitoringRequestEditor editor;
	
	@Before
	public void setUp() throws Exception {
		this.editor = new MonitoringRequestEditor();
		this.testDirectory = new File(TEMP_DIR, getClass().getSimpleName());
		this.testDirectory.mkdir();
		this.testFile = File.createTempFile(getClass().getSimpleName(), null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testSettingTextWithNoCommaIsInvalid() {
		this.editor.setAsText("foo");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testSettingTextWithNoOperationCodesIsInvalid() {
		this.editor.setAsText("foo,");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testMonitoringSubTreeOfFileIsInvalid() {
		this.editor.setAsText(this.testFile.getAbsolutePath() + ",C,**");
	}
	
	@Test
	public void testSettingNullAsTextCreatesNullValue() {
		assertCreatesNullMonitoringRequest(null);
	}
	
	@Test
	public void testSettingEmptyAsTextCreatesNullValue() {
		assertCreatesNullMonitoringRequest("");
	}
	
	/**
	 * Asserts that passing the given text to the {@link MonitoringRequestEditor}
	 * results in a <code>null</code> {@link MonitoringRequest}.
	 * 
	 * @param text the text to pass (can be blank)
	 */
	private void assertCreatesNullMonitoringRequest(final String text) {
		// Set up
		this.editor.setAsText(text);
		
		// Invoke
		final MonitoringRequest monitoringRequest = this.editor.getValue();
		
		// Check
		assertNull(monitoringRequest);
	}
	
	/**
	 * Asserts that passing the given text to {@link MonitoringRequestEditor#setAsText(String)}
	 * results in a {@link MonitoringRequest} with the given values
	 * 
	 * @param text the text to parse as a {@link MonitoringRequest}
	 * @param expectedFile the file we expect to be monitored
	 * @param expectedFileOperations the operations about which we expect to be notified
	 * @return the generated {@link MonitoringRequest} for any further assertions
	 */
	private MonitoringRequest assertMonitoringRequest(final String text, final File expectedFile, final FileOperation... expectedFileOperations) {
		// Set up
		this.editor.setAsText(text);
		
		// Invoke
		final MonitoringRequest monitoringRequest = this.editor.getValue();
		
		// Check
		assertEquals(expectedFile, monitoringRequest.getFile());
		final Collection<FileOperation> notifyOn = monitoringRequest.getNotifyOn();
		assertEquals(expectedFileOperations.length, notifyOn.size());
		assertTrue("Expected " + Arrays.toString(expectedFileOperations) + " but was " + notifyOn, notifyOn.containsAll(Arrays.asList(expectedFileOperations)));
		return monitoringRequest;
	}
	
	@Test
	public void testMonitorFileForRenameUpdateOrDelete() {
		assertMonitoringRequest(this.testFile.getAbsolutePath() + ",RUD", this.testFile, RENAMED, UPDATED, DELETED);
	}
	
	@Test
	public void testMonitorDirectoryButNotSubtreeForRename() {
		final MonitoringRequest monitoringRequest = assertMonitoringRequest(this.testDirectory.getAbsolutePath() + ",R", this.testDirectory, RENAMED);
		final DirectoryMonitoringRequest directoryMonitoringRequest = (DirectoryMonitoringRequest) monitoringRequest;
		assertFalse(directoryMonitoringRequest.isWatchSubtree());
	}
	
	@Test
	public void testMonitorDirectoryAndSubtreeForDelete() {
		final MonitoringRequest monitoringRequest = assertMonitoringRequest(this.testDirectory.getAbsolutePath() + ",D,**", this.testDirectory, DELETED);
		final DirectoryMonitoringRequest directoryMonitoringRequest = (DirectoryMonitoringRequest) monitoringRequest;
		assertTrue(directoryMonitoringRequest.isWatchSubtree());
	}
	
	@After
	public void tearDown() {
		this.testDirectory.delete();
		this.testFile.delete();
	}
}
