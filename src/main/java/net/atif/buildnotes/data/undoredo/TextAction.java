package net.atif.buildnotes.data.undoredo;

public interface TextAction {
    /**
     * Executes the action for the first time or re-does it.
     */
    void execute();

    /**
     * Reverts the changes made by this action.
     */
    void undo();
}