package net.atif.buildnotes.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// NOTE: This class is SERVER-ONLY. It does not use any client-side classes.
public class ServerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NOTES_FILE_NAME = "notes.json";
    private static final String BUILDS_FILE_NAME = "builds.json";
    private static final String MOD_DATA_SUBFOLDER = "buildnotes";

    private final Path storagePath;

    public ServerDataManager(MinecraftServer server) {
        this.storagePath = server.getSavePath(WorldSavePath.ROOT).resolve(MOD_DATA_SUBFOLDER);
    }

    private <T> List<T> loadFromFile(String fileName, Type type) {
        try {
            Path filePath = storagePath.resolve(fileName);
            if (Files.notExists(filePath)) return new ArrayList<>();

            try (FileReader reader = new FileReader(filePath.toFile())) {
                List<T> data = GSON.fromJson(reader, type);
                return data != null ? data : new ArrayList<>();
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to load server data from " + fileName, e);
            return new ArrayList<>();
        }
    }

    private <T> void writeToFile(String fileName, List<T> data) {
        try {
            Files.createDirectories(storagePath);
            try (FileWriter writer = new FileWriter(storagePath.resolve(fileName).toFile())) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to save server data to " + fileName, e);
        }
    }

    // Note Methods
    public List<Note> getNotes() { return loadFromFile(NOTES_FILE_NAME, new TypeToken<ArrayList<Note>>(){}.getType()); }

    public void saveNote(Note note) {
        List<Note> notes = getNotes();
        notes.removeIf(n -> n.getId().equals(note.getId()));
        notes.add(note);
        writeToFile(NOTES_FILE_NAME, notes);
    }

    public void deleteNote(UUID noteId) {
        List<Note> notes = getNotes();
        if (notes.removeIf(n -> n.getId().equals(noteId))) {
            writeToFile(NOTES_FILE_NAME, notes);
        }
    }

    // Build Methods
    public List<Build> getBuilds() { return loadFromFile(BUILDS_FILE_NAME, new TypeToken<ArrayList<Build>>(){}.getType()); }

    public void saveBuild(Build build) {
        List<Build> builds = getBuilds();
        builds.removeIf(b -> b.getId().equals(build.getId()));
        builds.add(build);
        writeToFile(BUILDS_FILE_NAME, builds);
    }

    public void deleteBuild(UUID buildId) {
        List<Build> builds = getBuilds();
        if (builds.removeIf(b -> b.getId().equals(buildId))) {
            writeToFile(BUILDS_FILE_NAME, builds);
        }
    }
}