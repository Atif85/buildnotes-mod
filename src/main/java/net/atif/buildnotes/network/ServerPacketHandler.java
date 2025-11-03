package net.atif.buildnotes.network;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.network.packet.c2s.*;
import net.atif.buildnotes.network.packet.s2c.*;
import net.atif.buildnotes.server.ServerDataManager;
import net.atif.buildnotes.server.ServerImageTransferManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.List;
import java.util.UUID;

public class ServerPacketHandler {

    private static boolean hasEditPermission(MinecraftServer server, ServerPlayerEntity player) {
        return Buildnotes.PERMISSION_MANAGER.isAllowedToEdit(player);
    }

    // --- Typed packet handlers (C2S) ---
    public static void handleRequestInitialData(MinecraftServer server, ServerPlayerEntity player, RequestDataC2SPacket packet) {
        ServerDataManager dataManager = Buildnotes.SERVER_DATA_MANAGER;
        List<Note> notes = dataManager.getNotes();
        List<Build> builds = dataManager.getBuilds();

        // Send typed S2C packet
        ServerPlayNetworking.send(player, new InitialSyncS2CPacket(notes, builds));
    }

    public static void handleSaveNote(MinecraftServer server, ServerPlayerEntity player, SaveNoteC2SPacket packet) {
        Note receivedNote = packet.note();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.saveNote(receivedNote);

        // Broadcast the update to all players
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new UpdateNoteS2CPacket(receivedNote));
        }
    }

    public static void handleSaveBuild(MinecraftServer server, ServerPlayerEntity player, SaveBuildC2SPacket packet) {
        Build receivedBuild = packet.build();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.saveBuild(receivedBuild);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new UpdateBuildS2CPacket(receivedBuild));
        }
    }

    public static void handleDeleteNote(MinecraftServer server, ServerPlayerEntity player, DeleteNoteC2SPacket packet) {
        UUID noteId = packet.noteId();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.deleteNote(noteId);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new DeleteNoteS2CPacket(noteId));
        }
    }

    public static void handleDeleteBuild(MinecraftServer server, ServerPlayerEntity player, DeleteBuildC2SPacket packet) {
        UUID buildId = packet.buildId();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.deleteBuild(buildId);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new DeleteBuildS2CPacket(buildId));
        }
    }

    public static void handleImageChunkUpload(MinecraftServer server, ServerPlayerEntity player, UploadImageChunkC2SPacket packet) {
        UUID buildId = packet.buildId();
        String filename = packet.filename();
        int totalChunks = packet.totalChunks();
        int chunkIndex = packet.chunkIndex();
        byte[] data = packet.data();

        ServerImageTransferManager.handleChunk(player, buildId, filename, totalChunks, chunkIndex, data);
    }

    public static void handleImageRequest(MinecraftServer server, ServerPlayerEntity player, RequestImageC2SPacket packet) {
        UUID buildId = packet.buildId();
        String filename = packet.filename();

        Buildnotes.SERVER_DATA_MANAGER.sendImageToPlayer(player, buildId, filename);
    }
}
