package net.atif.buildnotes.gui.helper;

public final class MainScreenLayouts {

    // --- General Layout ---
    public static final int TOP_MARGIN = 40;

    // --- Tab Dimensions ---
    public static final int TAB_HEIGHT = 20;
    public static final int TAB_WIDTH = 80;

    // --- Search Bar ---
    public static final int SEARCH_BAR_HEIGHT = 20;
    public static final int SEARCH_FIELD_WIDTH = 160;

    /**
     * Private constructor to prevent instantiation.
     */
    private MainScreenLayouts() {}

    /**
     * Calculates the total bottom margin for the main screen,
     * accounting for the button row, search bar, and padding.
     * @return The calculated bottom margin.
     */
    public static int getBottomMargin() {
        return UIHelper.BUTTON_HEIGHT + SEARCH_BAR_HEIGHT + (UIHelper.OUTER_PADDING * 3);
    }
}