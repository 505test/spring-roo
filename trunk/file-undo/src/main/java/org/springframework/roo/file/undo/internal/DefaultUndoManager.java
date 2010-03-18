package org.springframework.roo.file.undo.internal;

import java.util.Stack;

import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.file.undo.UndoableOperation;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of the {@link UndoManager} interface.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class DefaultUndoManager implements UndoManager {

	private Stack<UndoableOperation> stack = new Stack<UndoableOperation>();
	
	public void add(UndoableOperation undoableOperation) {
		Assert.notNull(undoableOperation, "Undoable operation required");
		this.stack.push(undoableOperation);
	}

	public void reset() {
		while (!this.stack.empty()) {
			UndoableOperation op = this.stack.pop();
			try {
				op.reset();
			} catch (Throwable t) {
				throw new IllegalStateException("UndoableOperation '" + op + "' threw an exception, in violation of the interface contract");
			}
		}
	}

	public boolean undo() {
		boolean undoMode = true;
		while (!this.stack.empty()) {
			UndoableOperation op = this.stack.pop();
			try {
				if (undoMode) {
					if (!op.undo()) {
						// undo failed, so switch to reset mode going forward
						undoMode = false;
					}
				} else {
					// in reset mode
					op.reset();
				}
			} catch (Throwable t) {
				throw new IllegalStateException("UndoableOperation '" + op + "' threw an exception, in violation of the interface contract");
			}
		}
		return undoMode;
	}

}
