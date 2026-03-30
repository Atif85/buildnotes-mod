package net.atif.buildnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.client.ClientCache;
import net.atif.buildnotes.client.ClientImageTransferManager;
import net.atif.buildnotes.network.packet.c2s.DeleteBuildC2SPacket;
import net.atif.buildnotes.network.packet.c2s.DeleteNoteC2SPacket;
import net.atif.buildnotes.network.packet.c2s.SaveBuildC2SPacket;
import net.atif.buildnotes.network.packet.c2s.SaveNoteC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

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
    public static class PinnedState {
        public UUID pinnedNoteId = null;
        public UUID pinnedBuildId = null;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final String NOTES_FILE_NAME = "notes.json";
    private static final String BUILDS_FILE_NAME = "builds.json";
    private static final String MOD_DATA_SUBFOLDER = "buildnotes";
    private static final String PER_SERVER_SUBFOLDER = "servers";
    private static final String PINNED_FILE_NAME = "pinned.json";

    private PinnedState pinnedState = new PinnedState();
    private Note cachedPinnedNote = null;
    private boolean needsPinnedRefresh = true;

    private static DataManager instance;

    private DataManager() {
        loadPinnedState();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // Pinned state logic
    public void markPinnedDirty() {
        this.needsPinnedRefresh = true;
    }

    private void loadPinnedState() {
        File file = getGlobalPath().resolve(PINNED_FILE_NAME).toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PinnedState loaded = GSON.fromJson(reader, PinnedState.class);
                if (loaded != null) this.pinnedState = loaded;
            } catch (Exception e) { Buildnotes.LOGGER.warn("Could not load pinned state", e); }
        }
    }

    private void savePinnedState() {
        try {
            Files.createDirectories(getGlobalPath());
            try (FileWriter writer = new FileWriter(getGlobalPath().resolve(PINNED_FILE_NAME).toFile())) {
                GSON.toJson(pinnedState, writer);
            }
        } catch (Exception e) { Buildnotes.LOGGER.error("Could not save pinned state", e); }
    }

    public Note getPinnedNote() {
        if (needsPinnedRefresh) {
            UUID id = getPinnedNoteId();
            if (id == null) {
                cachedPinnedNote = null;
            } else {
                // Only do the expensive file reading/searching when data has changed
                cachedPinnedNote = getNotes().stream()
                        .filter(n -> n.getId().equals(id))
                        .findFirst().orElse(null);
            }
            needsPinnedRefresh = false;
        }
        return cachedPinnedNote;
    }

    public void clearCachedPinnedNote() {
        cachedPinnedNote = null;
        needsPinnedRefresh = true;
    }

    public UUID getPinnedNoteId() { return pinnedState.pinnedNoteId; }
    public UUID getPinnedBuildId() { return pinnedState.pinnedBuildId; }

    // Toggle a pin on and off
    public void togglePinNote(UUID id) {
        if (id.equals(pinnedState.pinnedNoteId)) {
            pinnedState.pinnedNoteId = null;
        } else {
            pinnedState.pinnedNoteId = id;
        }
        savePinnedState();
        markPinnedDirty();
    }

    public void togglePinBuild(UUID id) {
        if (id.equals(pinnedState.pinnedBuildId)) pinnedState.pinnedBuildId = null;
        else pinnedState.pinnedBuildId = id;
        savePinnedState();
    }

    // --- Path Helper Methods ---
    private Path getGlobalPath() { return CONFIG_DIR.resolve(MOD_DATA_SUBFOLDER); }
    private Path getSinglePlayerWorldPath() {
        Minecraft client = Minecraft.getInstance();
        if (client.isSingleplayer() && client.getSingleplayerServer() != null) {
            return client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve(MOD_DATA_SUBFOLDER);
        }
        return null;
    }
    private Path getPerServerPath() {
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() != null) {
            String serverAddress = client.getCurrentServer().ip;
            String sanitizedAddress = serverAddress.replace(":", "_").replaceAll("[^a-zA-Z0-9_.-]", "_");
            return getGlobalPath().resolve(PER_SERVER_SUBFOLDER).resolve(sanitizedAddress);
        }
        return null;
    }
    private Path getLocalPath() {
        Path spPath = getSinglePlayerWorldPath();
        return spPath != null ? spPath : getPerServerPath();
    }

    private Path getLocalImagePath(UUID buildId) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(MOD_DATA_SUBFOLDER)
                .resolve("images")
                .resolve(buildId.toString());
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
            Buildnotes.LOGGER.warn("Could not load from {}, creating new list...", file.getAbsolutePath(), e);
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
            Buildnotes.LOGGER.error("Could not save to {}", path.resolve(fileName), e);
        }
    }

    // --- Private Delete Helpers to prevent duplication ---

    private void deleteNoteFromLocalFiles(UUID id) {
        // Delete from Global
        Path globalPath = getGlobalPath();
        List<Note> globalNotes = loadFromFile(globalPath, NOTES_FILE_NAME, new TypeToken<ArrayList<Note>>() {}.getType());
        if (globalNotes.removeIf(n -> n .getId().equals(id))) {
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
                .sorted((n1, n2) -> {
                    boolean isN1Pinned = n1.getId().equals(pinnedState.pinnedNoteId);
                    boolean isN2Pinned = n2.getId().equals(pinnedState.pinnedNoteId);
                    if (isN1Pinned && !isN2Pinned) return -1;
                    if (!isN1Pinned && isN2Pinned) return 1;
                    return Long.compare(n2.getLastModified(), n1.getLastModified()); // Fallback to newest
                }).collect(Collectors.toList());
    }

    public void saveNote(Note noteToSave) {
        if (noteToSave.getScope() == Scope.SERVER) {
            deleteNoteFromLocalFiles(noteToSave.getId());

            // Use the typed packet
            ClientPlayNetworking.send(new SaveNoteC2SPacket(noteToSave));

            if (noteToSave.getId().equals(getPinnedNoteId())) markPinnedDirty();
            return;
        }

        // --- Handle moving a note AWAY from the server ---
        // Before saving locally, check if a note with this ID exists in the server cache.
        final UUID noteId = noteToSave.getId();
        boolean wasServerNote = ClientCache.getNotes().stream().anyMatch(n -> n.getId().equals(noteId));

        if (wasServerNote) {
            // If it was a server note, its new scope is now local.
            // We must tell the server to delete its copy.
            ClientPlayNetworking.send(new DeleteNoteC2SPacket(noteId));
        }

        deleteNoteFromLocalFiles(noteId);

        Path path = noteToSave.getScope() == Scope.GLOBAL ? getGlobalPath() : getLocalPath();
        Type type = new TypeToken<ArrayList<Note>>() {}.getType();
        List<Note> notes = loadFromFile(path, NOTES_FILE_NAME, type);
        notes.add(noteToSave);
        writeToFile(notes, path, NOTES_FILE_NAME);

        if (noteId.equals(getPinnedNoteId())) markPinnedDirty();
    }

    public void deleteNote(Note noteToDelete) {
        // If the note being deleted is currently pinned, remove the pin!
        if (noteToDelete.getId().equals(getPinnedNoteId())) {
            pinnedState.pinnedNoteId = null;
            savePinnedState();
            markPinnedDirty();
        }

        if (noteToDelete.getScope() == Scope.SERVER) {
            // Send a C2S packet to the server
            ClientPlayNetworking.send(new DeleteNoteC2SPacket(noteToDelete.getId()));
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
                .sorted((b1, b2) -> {
                    boolean isB1Pinned = b1.getId().equals(pinnedState.pinnedBuildId);
                    boolean isB2Pinned = b2.getId().equals(pinnedState.pinnedBuildId);
                    if (isB1Pinned && !isB2Pinned) return -1;
                    if (!isB1Pinned && isB2Pinned) return 1;
                    return Long.compare(b2.getLastModified(), b1.getLastModified()); // Fallback to newest
                }).collect(Collectors.toList());
    }

    public void saveBuild(Build buildToSave) {
        if (buildToSave.getScope() == Scope.SERVER) {
            // 1. Delete any local copies to handle scope changes
            deleteBuildFromLocalFiles(buildToSave.getId());

            // 2. Identify which of the build's images exist locally and need to be uploaded.
            List<Path> imagesToUpload = new ArrayList<>();
            // Use the new helper method to get the directory for this build's images
            Path imageDir = getLocalImagePath(buildToSave.getId());
            for (String filename : buildToSave.getImageFileNames()) {
                Path localPath = imageDir.resolve(filename);
                if (Files.exists(localPath)) {
                    imagesToUpload.add(localPath);
                }
                }

            // 3. Schedule them for upload. The Transfer Manager will handle the background process.
            if (!imagesToUpload.isEmpty()) {
                ClientImageTransferManager.scheduleUploads(buildToSave.getId(), imagesToUpload);
            }

            // 4. Send the main build metadata packet immediately.
            ClientPlayNetworking.send(new SaveBuildC2SPacket(buildToSave));
            return;
        }
        // --- Handle moving a build AWAY from the server ---
        final UUID buildId = buildToSave.getId();
        boolean wasServerBuild = ClientCache.getBuilds().stream().anyMatch(b -> b.getId().equals(buildId));

        if (wasServerBuild) {
            // If it was a server build, tell the server to delete its copy.
            ClientPlayNetworking.send(new DeleteBuildC2SPacket(buildId));
        }

        // This is the original logic for saving locally.
        deleteBuildFromLocalFiles(buildId);

        Path path = buildToSave.getScope() == Scope.GLOBAL ? getGlobalPath() : getLocalPath();
        Type type = new TypeToken<ArrayList<Build>>() {}.getType();
        List<Build> builds = loadFromFile(path, BUILDS_FILE_NAME, type);
        builds.add(buildToSave);
        writeToFile(builds, path, BUILDS_FILE_NAME);
    }

    public void deleteBuild(Build buildToDelete) {
        if (buildToDelete.getScope() == Scope.SERVER) {
            // Send a C2S packet to the server
            ClientPlayNetworking.send(new DeleteBuildC2SPacket(buildToDelete.getId()));
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
