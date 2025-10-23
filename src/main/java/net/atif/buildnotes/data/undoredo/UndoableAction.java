package net.atif.buildnotes.data.undoredo;

// A simple container to store an action and the state of the cursor/selection *before* it was executed.
public class UndoableAction {
    public final TextAction action;
    public final int cursorBefore;
    public final int selectionStartBefore;
    public final int selectionEndBefore;

    public UndoableAction(TextAction action, int cursorBefore, int selectionStartBefore, int selectionEndBefore) {
        this.action = action;
        this.cursorBefore = cursorBefore;
        this.selectionStartBefore = selectionStartBefore;
        this.selectionEndBefore = selectionEndBefore;
    }
}