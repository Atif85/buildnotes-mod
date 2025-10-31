package net.atif.buildnotes.data.undoredo;

import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;

import java.util.Stack;

public class UndoManager {

    private final Stack<UndoableAction> undoStack = new Stack<>();
    private final Stack<UndoableAction> redoStack = new Stack<>();
    private final MultiLineTextFieldWidget widget;

    public UndoManager(MultiLineTextFieldWidget widget) {
        this.widget = widget;
    }

    /**
     * Performs an action and adds it to the undo stack.
     */
    public void perform(TextAction action) {
        // Store the state *before* the action is executed
        int cursor = widget.getCursorAbsolute();
        int selStart = widget.getSelectionStartAbsolute();
        int selEnd = widget.getSelectionEndAbsolute();

        undoStack.push(new UndoableAction(action, cursor, selStart, selEnd));
        redoStack.clear(); // A new action clears the redo history
        action.execute();
    }

    /**
     * Undoes the last action.
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableAction undoableAction = undoStack.pop();
            redoStack.push(undoableAction);
            undoableAction.action().undo();

            // Restore the previous cursor/selection state
            widget.setCursorFromAbsolute(undoableAction.cursorBefore());
            widget.setSelectionAbsolute(undoableAction.selectionStartBefore(), undoableAction.selectionEndBefore());
        }
    }

    /**
     * Re-does the last undone action.
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableAction undoableAction = redoStack.pop();
            undoStack.push(undoableAction);
            undoableAction.action().execute();
        }
    }
}