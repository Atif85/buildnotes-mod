package net.atif.buildnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.client.ClientCache;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;

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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final String NOTES_FILE_NAME = "notes.json";
    private static final String BUILDS_FILE_NAME = "builds.json";
    private static final String MOD_DATA_SUBFOLDER = "buildnotes";
    private static final String PER_SERVER_SUBFOLDER = "servers";

    private static DataManager instance;

    private DataManager() {}

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // --- Path Helper Methods ---
    private Path getGlobalPath() { return CONFIG_DIR.resolve(MOD_DATA_SUBFOLDER); }
    private Path getSinglePlayerWorldPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isIntegratedServerRunning() && client.getServer() != null) {
            return client.getServer().getSavePath(WorldSavePath.ROOT).resolve(MOD_DATA_SUBFOLDER);
        }
        return null;
    }
    private Path getPerServerPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address;
            String sanitizedAddress = serverAddress.replace(":", "_").replaceAll("[^a-zA-Z0-9_.-]", "_");
            return getGlobalPath().resolve(PER_SERVER_SUBFOLDER).resolve(sanitizedAddress);
        }
        return null;
    }
    private Path getLocalPath() {
        Path spPath = getSinglePlayerWorldPath();
        return spPath != null ? spPath : getPerServerPath();
    }

    // --- Generic Load/Write Methods ---
    private <T> List<T> loadFromFile(Path path, String fileName, Type type) {
        if (path == null) return new ArrayList<>();
        File file = path.resolve(fileName).toFile();
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            List<T> loadedData = GSON.fromJson(reader, type);
            return loadedData != null ? loadedData : new ArrayList<>();
        } catch (IOException e) {
            Buildnotes.LOGGER.warn("Could not load from " + file.getAbsolutePath() + ", creating new list...", e);
            return new ArrayList<>();
        }
    }
    private <T> void writeToFile(List<T> data, Path path, String fileName) {
        if (path == null) return;
        try {
            Files.createDirectories(path);
            try (FileWriter writer = new FileWriter(path.resolve(fileName).toFile())) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Could not save to " + path.resolve(fileName).toString(), e);
        }
    }

    // --- Private Delete Helpers to prevent duplication ---

    private void deleteNoteFromLocalFiles(UUID id) {
        // Delete from Global
        Path globalPath = getGlobalPath();
        List<Note> globalNotes = loadFromFile(globalPath, NOTES_FILE_NAME, new TypeToken<ArrayList<Note>>() {}.getType());
        if (globalNotes.removeIf(n -> n.getId().equals(id))) {
            writeToFile(globalNotes, globalPath, NOTES_FILE_NAME);
        }
        // Delete from World/Per-Server
        Path localPath = getLocalPath();
        List<Note> localNotes = loadFromFile(localPath, NOTES_FILE_NAME, new TypeToken<ArrayList<Note>>() {}.getType());
        if (localNotes.removeIf(n -> n.getId().equals(id))) {
            writeToFile(localNotes, localPath, NOTES_FILE_NAME);
        }
    }

    private void deleteBuildFromLocalFiles(UUID id) {
        // Delete from Global
        Path globalPath = getGlobalPath();
        List<Build> globalBuilds = loadFromFile(globalPath, BUILDS_FILE_NAME, new TypeToken<ArrayList<Build>>() {}.getType());
        if (globalBuilds.removeIf(b -> b.getId().equals(id))) {
            writeToFile(globalBuilds, globalPath, BUILDS_FILE_NAME);
        }
        // Delete from World/Per-Server
        Path localPath = getLocalPath();
        List<Build> localBuilds = loadFromFile(localPath, BUILDS_FILE_NAME, new TypeToken<ArrayList<Build>>() {}.getType());
        if (localBuilds.removeIf(b -> b.getId().equals(id))) {
            writeToFile(localBuilds, localPath, BUILDS_FILE_NAME);
        }
    }

    // --- Note Management ---
    public List<Note> getNotes() {
        List<Note> globalNotes = loadFromFile(getGlobalPath(), NOTES_FILE_NAME, new TypeToken<ArrayList<Note>>() {}.getType());
        List<Note> localNotes = loadFromFile(getLocalPath(), NOTES_FILE_NAME, new TypeToken<ArrayList<Note>>() {}.getType());
        List<Note> serverNotes = ClientCache.getNotes();
        return Stream.of(globalNotes, localNotes, serverNotes).flatMap(List::stream)
                .sorted(Comparator.comparingLong(Note::getLastModified).reversed()).collect(Collectors.toList());
    }

    public void saveNote(Note noteToSave) {
        if (noteToSave.getScope() == Scope.SERVER) {
            // TODO: Send C2S_SaveNotePacket to the server
            Buildnotes.LOGGER.info("Attempted to save a SERVER scope note. Networking not yet implemented.");
            return;
        }

        // First, delete the note from all possible locations to prevent duplication
        deleteNoteFromLocalFiles(noteToSave.getId());

        // Now, save the note to its new correct location
        Path path = noteToSave.getScope() == Scope.GLOBAL ? getGlobalPath() : getLocalPath();
        Type type = new TypeToken<ArrayList<Note>>() {}.getType();
        List<Note> notes = loadFromFile(path, NOTES_FILE_NAME, type);
        notes.add(noteToSave);
        writeToFile(notes, path, NOTES_FILE_NAME);
    }

    public void deleteNote(Note noteToDelete) {
        if (noteToDelete.getScope() == Scope.SERVER) {
            // TODO: Send C2S_DeleteNotePacket to the server
            Buildnotes.LOGGER.info("Attempted to delete a SERVER scope note. Networking not yet implemented.");
            return;
        }
        deleteNoteFromLocalFiles(noteToDelete.getId());
    }

    // --- Build Management ---
    public List<Build> getBuilds() {
        List<Build> globalBuilds = loadFromFile(getGlobalPath(), BUILDS_FILE_NAME, new TypeToken<ArrayList<Build>>() {}.getType());
        List<Build> localBuilds = loadFromFile(getLocalPath(), BUILDS_FILE_NAME, new TypeToken<ArrayList<Build>>() {}.getType());
        List<Build> serverBuilds = ClientCache.getBuilds();
        return Stream.of(globalBuilds, localBuilds, serverBuilds).flatMap(List::stream)
                .sorted(Comparator.comparingLong(Build::getLastModified).reversed()).collect(Collectors.toList());
    }

    public void saveBuild(Build buildToSave) {
        if (buildToSave.getScope() == Scope.SERVER) {
            // TODO: Send C2S_SaveBuildPacket to the server
            Buildnotes.LOGGER.info("Attempted to save a SERVER scope build. Networking not yet implemented.");
            return;
        }
        deleteBuildFromLocalFiles(buildToSave.getId());
        Path path = buildToSave.getScope() == Scope.GLOBAL ? getGlobalPath() : getLocalPath();
        Type type = new TypeToken<ArrayList<Build>>() {}.getType();
        List<Build> builds = loadFromFile(path, BUILDS_FILE_NAME, type);
        builds.add(buildToSave);
        writeToFile(builds, path, BUILDS_FILE_NAME);
    }

    public void deleteBuild(Build buildToDelete) {
        if (buildToDelete.getScope() == Scope.SERVER) {
            // TODO: Send C2S_DeleteBuildPacket to the server
            Buildnotes.LOGGER.info("Attempted to delete a SERVER scope build. Networking not yet implemented.");
            return;
        }
        deleteBuildFromLocalFiles(buildToDelete.getId());
        try {
            Path imageDir = CONFIG_DIR.resolve(MOD_DATA_SUBFOLDER).resolve("images").resolve(buildToDelete.getId().toString());
            if (Files.exists(imageDir)) {
                try (Stream<Path> walk = Files.walk(imageDir)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to delete image directory for build: {}", buildToDelete.getId(), e);
        }
    }
}