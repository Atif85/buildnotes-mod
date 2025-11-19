package net.atif.buildnotes.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.network.NetworkConstants;
import net.atif.buildnotes.network.packet.s2c.ImageChunkS2CPacket;
import net.atif.buildnotes.network.packet.s2c.ImageNotFoundS2CPacket;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
import java.util.concurrent.CompletableFuture;

// NOTE: This class is SERVER-ONLY. It does not use any client-side classes.
public class ServerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NOTES_FILE_NAME = "notes.json";
    private static final String BUILDS_FILE_NAME = "builds.json";
    private static final String MOD_DATA_SUBFOLDER = "buildnotes";

    private final MinecraftServer server;
    private final Path storagePath;

    public ServerDataManager(MinecraftServer server) {
        this.server = server;
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

    public Path getImageStoragePath(UUID buildId) {
        return this.storagePath.resolve("images").resolve(buildId.toString());
    }

    // The logic to read and send an image file
    public void sendImageToPlayer(ServerPlayerEntity player, UUID buildId, String filename) {
        // Read the file off-thread but schedule actual packet sends on the server thread.
        CompletableFuture.runAsync(() -> {
            Path imagePath = getImageStoragePath(buildId).resolve(filename);
            if (Files.notExists(imagePath)) {
                Buildnotes.LOGGER.warn("Player {} requested non-existent image '{}' for build {}", player.getName().getString(), filename, buildId);
                server.execute(() -> {
                    ServerPlayNetworking.send(player, new ImageNotFoundS2CPacket(buildId, filename));
                });

                return;
            }

            try {
                byte[] fullData = Files.readAllBytes(imagePath);
                int totalChunks = (int) Math.ceil((double) fullData.length / NetworkConstants.CHUNK_SIZE);

                for (int i = 0; i < totalChunks; i++) {
                    int offset = i * NetworkConstants.CHUNK_SIZE;
                    int length = Math.min(NetworkConstants.CHUNK_SIZE, fullData.length - offset);
                    byte[] chunkData = new byte[length];
                    System.arraycopy(fullData, offset, chunkData, 0, length);
                    int finalI = i;
                    server.execute(() -> ServerPlayNetworking.send(player, new ImageChunkS2CPacket(buildId, filename, totalChunks, finalI, chunkData)));
                }
                Buildnotes.LOGGER.info("Sent image '{}' ({} chunks) to player {}", filename, totalChunks, player.getName().getString());

            } catch (IOException e) {
                Buildnotes.LOGGER.error("Failed to read and send image {} for build {}", filename, buildId, e);
            }
        });
    }
}