package net.atif.buildnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.atif.buildnotes.Buildnotes;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("buildnotes/config.json");
    private static ModConfig instance = new ModConfig();

    public static ModConfig getConfig() { return instance; }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                instance = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                Buildnotes.LOGGER.error("Failed to load config", e);
            }
        } else {
            save(); // Create default
        }
    }

    public static void save() {
        try {
            CONFIG_PATH.toFile().getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to save config", e);
        }
    }
}