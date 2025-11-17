package net.atif.buildnotes.gui.helper;

public final class NoteScreenLayouts {

    // --- General Layout ---
    public static final double CONTENT_WIDTH_RATIO = 0.6;
    public static final int TOP_MARGIN = 20;
    public static final int PANEL_SPACING = 5;

    // --- Field Heights ---
    public static final int TITLE_PANEL_HEIGHT = 25;

    /**
     * Private constructor to prevent instantiation.
     */
    private NoteScreenLayouts() {}

    /**
     * Calculates the bottom margin needed to accommodate one row of buttons.
     * @return The total height of the bottom margin area.
     */
    public static int getBottomMarginSingleRow() {
        return UIHelper.BUTTON_HEIGHT + (UIHelper.OUTER_PADDING * 2);
    }

    /**
     * Calculates the bottom margin needed to accommodate two rows of buttons.
     * @return The total height of the bottom margin area.
     */
    public static int getBottomMarginDoubleRow() {
        return (UIHelper.BUTTON_HEIGHT * 2) + (UIHelper.OUTER_PADDING * 2) + UIHelper.BUTTON_ROW_SPACING;
    }
}