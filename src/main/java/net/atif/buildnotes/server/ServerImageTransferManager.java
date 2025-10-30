package net.atif.buildnotes.server;

import com.google.common.collect.Maps;
import net.atif.buildnotes.Buildnotes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class ServerImageTransferManager {

    // A simple record to uniquely identify a file transfer
    private record TransferKey(UUID buildId, String filename) {}

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
            // Calculate total size to create the final byte array
            int totalSize = 0;
            for (byte[] chunk : chunks) {
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

    // This map holds all ongoing uploads. We key it by the player to handle disconnects.
    private static final Map<UUID, Map<TransferKey, ImageAssembler>> IN_PROGRESS_UPLOADS = Maps.newConcurrentMap();

    public static void handleChunk(ServerPlayerEntity player, UUID buildId, String filename, int totalChunks, int chunkIndex, byte[] data) {
        UUID playerId = player.getUuid();
        IN_PROGRESS_UPLOADS.computeIfAbsent(playerId, k -> Maps.newHashMap());

        Map<TransferKey, ImageAssembler> playerUploads = IN_PROGRESS_UPLOADS.get(playerId);
        TransferKey key = new TransferKey(buildId, filename);

        ImageAssembler assembler = playerUploads.computeIfAbsent(key, k -> new ImageAssembler(totalChunks));

        if (assembler.addChunk(chunkIndex, data)) {
            // The image is complete!
            byte[] fullImageData = assembler.reassemble();
            saveImage(buildId, filename, fullImageData);
            playerUploads.remove(key); // Clean up
            Buildnotes.LOGGER.info("Successfully received image '{}' for build {}", filename, buildId);
        }
    }

    private static void saveImage(UUID buildId, String filename, byte[] data) {
        try {
            Path destDir = Buildnotes.SERVER_DATA_MANAGER.getImageStoragePath(buildId);
            Files.createDirectories(destDir);
            Path destPath = destDir.resolve(filename);
            Files.write(destPath, data);
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to save server-side image {} for build {}", filename, buildId, e);
        }
    }

    // This is the crucial cleanup method!
    public static void onPlayerDisconnect(UUID playerId) {
        int discarded = 0;
        if (IN_PROGRESS_UPLOADS.containsKey(playerId)) {
            discarded = IN_PROGRESS_UPLOADS.get(playerId).size();
            IN_PROGRESS_UPLOADS.remove(playerId);
        }
        if (discarded > 0) {
            Buildnotes.LOGGER.warn("Discarded {} incomplete image uploads for disconnected player {}", discarded, playerId);
        }
    }
}