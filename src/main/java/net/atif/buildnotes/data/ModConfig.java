package net.atif.buildnotes.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {
    // --- HUD SETTINGS ---
    public String hudPosition = "BOTTOM_RIGHT"; // TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    public float hudWidthPercent = 0.30f;
    public float hudHeightPercent = 0.40f;
    public int hudEdgePadding = 4;

    // --- DOCUMENTATION ---
    public Map<String, String> _comments = new LinkedHashMap<>();

    public ModConfig() {
        // Comments for the user
        _comments.put("hudPosition", "Valid values: TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT");
        _comments.put("hudWidthPercent", "0.1 to 1.0 (Percentage of screen width)");
        _comments.put("hudHeightPercent", "0.1 to 1.0 (Percentage of screen height)");
    }
}