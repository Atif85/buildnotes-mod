package net.atif.buildnotes.client;

import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.network.NetworkConstants;
import net.atif.buildnotes.network.PacketIdentifiers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ClientImageTransferManager {

    // --- DOWNLOAD HANDLING ---
    private record FileKey(UUID buildId, String filename) {}

    // A helper class to reassemble chunks in memory
    private static class ImageAssembler {
        private final byte[][] chunks;
        private int receivedChunks = 0;

        ImageAssembler(int totalChunks) {
            this.chunks = new byte[totalChunks][];
        }

        public boolean addChunk(int index, byte[] data) {
            if (index < chunks.length && chunks[index] == null) {
                chunks[index] = data;
                receivedChunks++;
            }
            return isComplete();
        }

        public boolean isComplete() {
            return receivedChunks == chunks.length;
        }

        public byte[] reassemble() {
            int totalSize = 0;
            for (byte[] chunk : chunks) {
                if (chunk == null) return null; // Should not happen with correct logic, but a good safeguard
                totalSize += chunk.length;
            }

            byte[] fullData = new byte[totalSize];
            int currentPos = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, fullData, currentPos, chunk.length);
                currentPos += chunk.length;
            }
            return fullData;
        }
    }

    private static final Map<FileKey, ImageAssembler> IN_PROGRESS_DOWNLOADS = Maps.newConcurrentMap();
    private static final Map<FileKey, Runnable> COMPLETION_CALLBACKS = Maps.newConcurrentMap();


    // --- UPLOAD HANDLING ---
    private static final Queue<FileKey> UPLOAD_QUEUE = new LinkedList<>();
    private static boolean isUploading = false;


    public static void requestImage(UUID buildId, String filename, Runnable onComplete) {
        try {
            if (buildId == null || filename == null) {
                Buildnotes.LOGGER.error("[FATAL] requestImage called with null values!");
                return;
            }

            FileKey key = new FileKey(buildId, filename);

            if (COMPLETION_CALLBACKS.putIfAbsent(key, onComplete) != null) {
                Buildnotes.LOGGER.info("[DIAGNOSTIC] Download for '{}' is already in progress. Aborting new network request.", filename);
                return;
            }

            Buildnotes.LOGGER.info("Requesting image '{}' for build {}", filename, key.buildId);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeUuid(buildId);
            buf.writeString(filename);
            ClientPlayNetworking.send(PacketIdentifiers.REQUEST_IMAGE_C2S, buf);

        } catch (Exception e) {
            Buildnotes.LOGGER.error("[CRITICAL] An unexpected exception occurred inside requestImage!", e);
        }
    }


    public static void handleChunk(UUID buildId, String filename, int totalChunks, int chunkIndex, byte[] data) {
        FileKey key = new FileKey(buildId, filename);

        if (totalChunks <= 0 || totalChunks > 10000) {
            Buildnotes.LOGGER.error("Invalid totalChunks: {}", totalChunks);
            return;
        }

        ImageAssembler assembler = IN_PROGRESS_DOWNLOADS.computeIfAbsent(key, k -> new ImageAssembler(totalChunks));

        if (assembler.addChunk(chunkIndex, data)) {
            byte[] fullImageData = assembler.reassemble();
            if (fullImageData != null) {
                saveImage(buildId, filename, fullImageData);
            }
            IN_PROGRESS_DOWNLOADS.remove(key);

            if (COMPLETION_CALLBACKS.containsKey(key)) {
                Runnable callback = COMPLETION_CALLBACKS.remove(key);
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    private static Path getLocalImagePath(UUID buildId, String filename) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("buildnotes")
                .resolve("images")
                .resolve(buildId.toString())
                .resolve(filename);
    }

    private static void saveImage(UUID buildId, String filename, byte[] data) {
        try {
            Path imagePath = getLocalImagePath(buildId, filename);
            Files.createDirectories(imagePath.getParent());
            Files.write(imagePath, data);
            Buildnotes.LOGGER.info("Successfully saved downloaded image '{}' for build {}", filename, buildId);
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to save client-side image {} for build {}", filename, buildId, e);
        }
    }

    // ADDED: Method to handle a failed download.
    public static void onDownloadFailed(UUID buildId, String filename) {
        FileKey key = new FileKey(buildId, filename);
        Buildnotes.LOGGER.warn("Server reported image not found: '{}' for build {}", filename, buildId);

        // Remove the download from the in-progress map
        IN_PROGRESS_DOWNLOADS.remove(key);

        if (COMPLETION_CALLBACKS.containsKey(key)) {
            Runnable callback = COMPLETION_CALLBACKS.remove(key);
            if (callback != null) {
                callback.run();
            }
        }
    }

    // --- Upload Logic ---

    public static void scheduleUploads(UUID buildId, List<Path> localImagePaths) {
        for (Path path : localImagePaths) {
            UPLOAD_QUEUE.add(new FileKey(buildId, path.getFileName().toString()));
        }
        if (!isUploading) {
            processUploadQueue();
        }
    }

    private static void processUploadQueue() {
        if (UPLOAD_QUEUE.isEmpty()) {
            isUploading = false;
            return;
        }
        isUploading = true;

        FileKey key = UPLOAD_QUEUE.poll();
        Path localPath = getLocalImagePath(key.buildId, key.filename);

        if (Files.notExists(localPath)) {
            Buildnotes.LOGGER.warn("Tried to upload non-existent local image: {}", localPath);
            processUploadQueue(); // Skip to the next item
            return;
        }

        Buildnotes.LOGGER.info("Starting upload for image: {}", localPath);

        // Run file I/O and networking on a separate thread to avoid blocking the main client thread
        CompletableFuture.runAsync(() -> {
            try {
                byte[] fullData = Files.readAllBytes(localPath);
                int totalChunks = (int) Math.ceil((double) fullData.length / NetworkConstants.CHUNK_SIZE);

                for (int i = 0; i < totalChunks; i++) {
                    int offset = i * NetworkConstants.CHUNK_SIZE;
                    int length = Math.min(NetworkConstants.CHUNK_SIZE, fullData.length - offset);
                    byte[] chunkData = new byte[length];
                    System.arraycopy(fullData, offset, chunkData, 0, length);

                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeUuid(key.buildId);
                    buf.writeString(key.filename);
                    buf.writeVarInt(totalChunks);
                    buf.writeVarInt(i);
                    buf.writeByteArray(chunkData);
                    ClientPlayNetworking.send(PacketIdentifiers.UPLOAD_IMAGE_CHUNK_C2S, buf);
                }
                Buildnotes.LOGGER.info("Finished sending {} chunks for image '{}'", totalChunks, key.filename);
            } catch (IOException e) {
                Buildnotes.LOGGER.error("Failed to read and chunk local image for upload: {}", localPath, e);
            }
        }).thenRun(ClientImageTransferManager::processUploadQueue); // When this async task is done, trigger the next upload
    }
}