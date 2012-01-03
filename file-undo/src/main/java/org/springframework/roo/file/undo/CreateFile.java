package org.springframework.roo.file.undo;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * {@link UndoableOperation} to create a file.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class CreateFile implements UndoableOperation {

    // Constants
    private static final Logger LOGGER = HandlerUtils
            .getLogger(CreateFile.class);

    // Fields
    private final FilenameResolver filenameResolver;
    private final File actual;

    public CreateFile(final UndoManager undoManager,
            final FilenameResolver filenameResolver, final File actual) {
        Assert.notNull(undoManager, "Undo manager required");
        Assert.notNull(actual, "Actual file required");
        Assert.notNull(filenameResolver, "Filename resolver required");
        Assert.isTrue(!actual.exists(), "Actual file '" + actual
                + "' cannot exist");
        this.filenameResolver = filenameResolver;
        this.actual = actual;
        try {
            this.actual.createNewFile();
        }
        catch (IOException ioe) {
            throw new IllegalStateException("Unable to create file '"
                    + this.actual + "'", ioe);
        }
        undoManager.add(this);
    }

    public void reset() {
    }

    public boolean undo() {
        boolean success = this.actual.delete();
        if (success) {
            LOGGER.fine("Undo create "
                    + filenameResolver.getMeaningfulName(actual));
        }
        else {
            LOGGER.fine("Undo failed "
                    + filenameResolver.getMeaningfulName(actual));
        }
        return success;
    }
}
