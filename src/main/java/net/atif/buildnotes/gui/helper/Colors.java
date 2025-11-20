package net.atif.buildnotes.gui.helper;

import net.atif.buildnotes.data.ColorConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Colors {

    // --- Panel ---
    public static int PANEL_BACKGROUND = 0x77000000;

    // --- Text ---
    public static int TEXT_PRIMARY = 0xFFFFFFFF;
    public static int TEXT_MUTED = 0xFFCCCCCC;
    public static int TEXT_DISABLED = 0xFF888888;

    // --- Selection ---
    public static int SELECTION_BACKGROUND = 0x8855AADD;

    // --- Caret ---
    public static int CARET_PRIMARY = 0xFFFFFFFF;

    // --- Buttons ---
    public static int BUTTON_DISABLED = 0x44000000;
    public static int BUTTON_HOVER = 0xAA000000;

    // --- Tabs ---
    public static int TAB_INACTIVE = 0x44000000;

    // --- Scrollbar ---
    public static int SCROLLBAR_THUMB_INACTIVE = 0x88FFFFFF;
    public static int SCROLLBAR_THUMB_ACTIVE = 0xFFFFFFFF;

    // --- Background ---
    public static int GRADIENT_START = 0xC0101010;
    public static int GRADIENT_END = 0xD0101010;

    // --- Fade Gradient ---
    public static int FADE_GRADIENT_TOP = 0x60000000;
    public static int FADE_GRADIENT_BOTTOM = 0x00000000;

    private Colors() {}

    public static void reload() {
        ColorConfig.loadColors();
    }

    public static Map<String, String> getColorsAsMap() {
        Map<String, String> colorMap = new LinkedHashMap<>();
        for (Field field : Colors.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                try {
                    int colorValue = (Integer) field.get(null);
                    int a = (colorValue >> 24) & 0xFF;
                    int r = (colorValue >> 16) & 0xFF;
                    int g = (colorValue >> 8) & 0xFF;
                    int b = colorValue & 0xFF;
                    float alpha = a / 255.0f;

                    colorMap.put(field.getName(), String.format("rgba(%d, %d, %d, %.3f)", r, g, b, alpha));
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }
        return colorMap;
    }
}