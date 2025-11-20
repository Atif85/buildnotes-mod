package net.atif.buildnotes.gui.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorParser {

    private static final Pattern RGBA_PATTERN = Pattern.compile("rgba\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*([0-9.]+)\\s*\\)");
    private static final Pattern RGB_PATTERN = Pattern.compile("rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)");

    private ColorParser() {}

    /**
     * Parses a color string into an ARGB integer.
     * Supported formats:
     * - #RRGGBB (e.g., #FF0000 for red)
     * - #AARRGGBB (e.g., #80FF0000 for semi-transparent red)
     * - rgb(r, g, b) (e.g., rgb(255, 0, 0))
     * - rgba(r, g, b, a) (e.g., rgba(255, 0, 0, 0.5))
     *
     * @param colorString The color string to parse.
     * @return The ARGB integer representation of the color.
     * @throws IllegalArgumentException if the color string format is invalid.
     */
    public static int parse(String colorString) {
        if (colorString == null || colorString.trim().isEmpty()) {
            throw new IllegalArgumentException("Color string cannot be null or empty.");
        }

        String trimmed = colorString.trim();

        if (trimmed.startsWith("#")) {
            return parseHex(trimmed);
        }

        if (trimmed.startsWith("rgba")) {
            Matcher matcher = RGBA_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                int r = Integer.parseInt(matcher.group(1));
                int g = Integer.parseInt(matcher.group(2));
                int b = Integer.parseInt(matcher.group(3));
                float a = Float.parseFloat(matcher.group(4));
                int alpha = (int) (a * 255);
                return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
            }
        }

        if (trimmed.startsWith("rgb")) {
            Matcher matcher = RGB_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                int r = Integer.parseInt(matcher.group(1));
                int g = Integer.parseInt(matcher.group(2));
                int b = Integer.parseInt(matcher.group(3));
                return 0xFF000000 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
            }
        }

        throw new IllegalArgumentException("Invalid color format: " + colorString);
    }

    private static int parseHex(String hex) {
        long longVal;
        try {
            longVal = Long.parseLong(hex.substring(1), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color: " + hex, e);
        }

        if (hex.length() == 7) { // #RRGGBB
            return (int) (0xFF000000 | longVal);
        } else if (hex.length() == 9) { // #AARRGGBB
            return (int) longVal;
        }

        throw new IllegalArgumentException("Invalid hex color length: " + hex);
    }
}