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
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            try {
                Field field = Colors.class.getField(entry.getKey());
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                    int colorValue = ColorParser.parse(entry.getValue());
                    field.set(null, colorValue);
                }
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                Buildnotes.LOGGER.warn("Could not apply color '{}' with value '{}': {}", entry.getKey(), entry.getValue(), e.getMessage());
            } catch (Exception e) {
                Buildnotes.LOGGER.error("An unexpected error occurred while applying color '{}'", entry.getKey(), e);
            }
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