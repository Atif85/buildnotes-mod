package net.atif.buildnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.gui.helper.ColorParser;
import net.atif.buildnotes.gui.helper.Colors;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Map;

public class ColorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("buildnotes/colors.json");

    public static void loadColors() {
        try {
            File configFile = CONFIG_PATH.toFile();
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    Map<String, String> colorMap = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                    if (colorMap != null) {
                        applyColors(colorMap);
                    }
                }
            } else {
                saveDefaultColors();
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Error loading color config", e);
        }
    }

    private static void applyColors(Map<String, String> colorMap) {
        boolean isMissingKeys = false;

        for (Field field : Colors.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                String fieldName = field.getName();

                // If the key is in the JSON, apply it
                if (colorMap.containsKey(fieldName)) {
                    try {
                        int colorValue = ColorParser.parse(colorMap.get(fieldName));
                        field.set(null, colorValue);
                    } catch (Exception e) {
                        Buildnotes.LOGGER.warn("Could not apply color '{}': {}", fieldName, e.getMessage());
                    }
                } else {
                    isMissingKeys = true;
                }
            }
        }

        // Re-save the file so the missing keys are appended to the user's old config automatically
        if (isMissingKeys) {
            saveDefaultColors();
        }
    }

    private static void saveDefaultColors() {
        try {
            File configFile = CONFIG_PATH.toFile();
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(Colors.getColorsAsMap(), writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Error saving default color config", e);
        }
    }
}