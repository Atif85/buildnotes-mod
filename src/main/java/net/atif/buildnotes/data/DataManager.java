package net.atif.buildnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.util.PropertySource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path GLOBAL_PATH = FabricLoader.getInstance().getConfigDir();
    private static final String NOTES_FILE_NAME = "notes.json";
    private static final String BUILDS_FILE_NAME = "builds.json";
    private static final String MOD_DATA_SUBFOLDER = "buildnotes";

    private static DataManager instance;

    private DataManager() {}

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    private Path getWorldSpecificPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        // Check if we are in a single-player world
        if (client.isIntegratedServerRunning() && client.getServer() != null) {
            // This is the correct, robust way to get the world's save directory
            return client.getServer().getSavePath(WorldSavePath.ROOT).resolve(MOD_DATA_SUBFOLDER);
        }
        // If not in a world (e.g., on the main menu), return null
        return null;
    }

    public List<Note> getNotes() {
        List<Note> globalNotes = loadNotes(GLOBAL_PATH.resolve(MOD_DATA_SUBFOLDER));
        Path worldPath = getWorldSpecificPath();
        List<Note> worldNotes = new ArrayList<>();
        if (worldPath != null) {
            worldNotes = loadNotes(worldPath);
        }
        List<Note> combined = Stream.concat(worldNotes.stream(), globalNotes.stream()).collect(Collectors.toList());
        combined.sort(Comparator.comparingLong(Note::getLastModified).reversed());
        return combined;
    }

    public void saveNote(Note noteToSave) {
        List<Note> allNotes = getNotes();
        // Remove old version if it exists
        allNotes.removeIf(n -> n.getId().equals(noteToSave.getId()));
        allNotes.add(noteToSave); // Add the new/updated version

        // Split notes into global and world-specific lists
        List<Note> globalNotes = allNotes.stream().filter(Note::isGlobal).collect(Collectors.toList());
        List<Note> worldNotes = allNotes.stream().filter(n -> !n.isGlobal()).collect(Collectors.toList());

        // Save them to their respective files
        writeNotesToFile(globalNotes, GLOBAL_PATH.resolve(MOD_DATA_SUBFOLDER));
        Path worldPath = getWorldSpecificPath();
        if (worldPath != null) {
            writeNotesToFile(worldNotes, worldPath);
        }
    }

    // NEW METHOD for deleting a single note
    public void deleteNote(Note noteToDelete) {
        List<Note> allNotes = getNotes();
        allNotes.removeIf(n -> n.getId().equals(noteToDelete.getId()));

        List<Note> globalNotes = allNotes.stream().filter(Note::isGlobal).collect(Collectors.toList());
        List<Note> worldNotes = allNotes.stream().filter(n -> !n.isGlobal()).collect(Collectors.toList());

        writeNotesToFile(globalNotes, GLOBAL_PATH.resolve(MOD_DATA_SUBFOLDER));
        Path worldPath = getWorldSpecificPath();
        if (worldPath != null) {
            writeNotesToFile(worldNotes, worldPath);
        }
    }


    private void writeNotesToFile(List<Note> notes, Path path) {
        try {
            Files.createDirectories(path);
            try (FileWriter writer = new FileWriter(path.resolve(NOTES_FILE_NAME).toFile())) {
                GSON.toJson(notes, writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Could not save notes to " + path.toString(), e);
        }
    }

    private List<Note> loadNotes(Path path) {
        File notesFile = path.resolve(NOTES_FILE_NAME).toFile();
        if (!notesFile.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(notesFile)) {
            Type type = new TypeToken<ArrayList<Note>>() {}.getType();
            List<Note> loadedNotes = GSON.fromJson(reader, type);
            return loadedNotes != null ? loadedNotes : new ArrayList<>();
        } catch (IOException e) {
            Buildnotes.LOGGER.warn("Could not load notes from " + path.toString() + ", creating new list...");
            return new ArrayList<>();
        }
    }

    public List<Build> getBuilds() {
        List<Build> globalBuilds = loadBuilds(GLOBAL_PATH.resolve(MOD_DATA_SUBFOLDER));
        Path worldPath = getWorldSpecificPath();
        List<Build> worldBuilds = new ArrayList<>();
        if (worldPath != null) {
            worldBuilds = loadBuilds(worldPath);
        }
        List<Build> combined = Stream.concat(worldBuilds.stream(), globalBuilds.stream()).collect(Collectors.toList());
        // Sort by last modified timestamp, descending (newest first)
        combined.sort(Comparator.comparingLong(Build::getLastModified).reversed());
        return combined;
    }

    public void saveBuild(Build buildToSave) {
        List<Build> allBuilds = getBuilds();
        allBuilds.removeIf(b -> b.getId().equals(buildToSave.getId()));
        allBuilds.add(buildToSave);

        List<Build> globalBuilds = allBuilds.stream().filter(Build::isGlobal).collect(Collectors.toList());
        List<Build> worldBuilds = allBuilds.stream().filter(b -> !b.isGlobal()).collect(Collectors.toList());

        writeBuildsToFile(globalBuilds, GLOBAL_PATH.resolve(MOD_DATA_SUBFOLDER));
        Path worldPath = getWorldSpecificPath();
        if (worldPath != null) {
            writeBuildsToFile(worldBuilds, worldPath);
        }
    }

    // NEW METHOD for deleting a single build
    public void deleteBuild(Build buildToDelete) {
        List<Build> allBuilds = getBuilds();
        allBuilds.removeIf(b -> b.getId().equals(buildToDelete.getId()));

        List<Build> globalBuilds = allBuilds.stream().filter(Build::isGlobal).collect(Collectors.toList());
        List<Build> worldBuilds = allBuilds.stream().filter(b -> !b.isGlobal()).collect(Collectors.toList());

        writeBuildsToFile(globalBuilds, GLOBAL_PATH.resolve(MOD_DATA_SUBFOLDER));
        Path worldPath = getWorldSpecificPath();
        if (worldPath != null) {
            writeBuildsToFile(worldBuilds, worldPath);
        }
    }

    private void writeBuildsToFile(List<Build> builds, Path path) {
        try {
            Files.createDirectories(path);
            try (FileWriter writer = new FileWriter(path.resolve(BUILDS_FILE_NAME).toFile())) {
                GSON.toJson(builds, writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Could not save builds to " + path.toString(), e);
        }
    }

    private List<Build> loadBuilds(Path path) {
        File buildsFile = path.resolve(BUILDS_FILE_NAME).toFile();
        if (!buildsFile.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(buildsFile)) {
            Type type = new TypeToken<ArrayList<Build>>() {}.getType();
            List<Build> loadedBuilds = GSON.fromJson(reader, type);
            return loadedBuilds != null ? loadedBuilds : new ArrayList<>();
        } catch (IOException e) {
            Buildnotes.LOGGER.warn("Could not load builds from " + path.toString() + ", creating new list...");
            return new ArrayList<>();
        }
    }
}