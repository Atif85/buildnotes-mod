package net.atif.buildnotes.data.undoredo;

// A simple container to store an action and the state of the cursor/selection *before* it was executed.
public record UndoableAction(TextAction action, int cursorBefore, int selectionStartBefore, int selectionEndBefore) {
}