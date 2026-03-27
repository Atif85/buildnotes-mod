package net.atif.buildnotes.network;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.network.packet.c2s.*;
import net.atif.buildnotes.network.packet.s2c.*;
import net.atif.buildnotes.server.ServerDataManager;
import net.atif.buildnotes.server.ServerImageTransferManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.UUID;

public class ServerPacketHandler {

    private static boolean hasEditPermission(MinecraftServer server, ServerPlayer player) {
        return Buildnotes.PERMISSION_MANAGER.isAllowedToEdit(player);
    }

    // --- Typed packet handlers (C2S) ---
    public static void handleRequestInitialData(MinecraftServer server, ServerPlayer player, RequestDataC2SPacket packet) {
        ServerDataManager dataManager = Buildnotes.SERVER_DATA_MANAGER;
        List<Note> notes = dataManager.getNotes();
        List<Build> builds = dataManager.getBuilds();

        // Send typed S2C packet
        ServerPlayNetworking.send(player, new InitialSyncS2CPacket(notes, builds));
    }


    // Re-evaluates a specific player's permissions and sends them the update packet.
    public static void refreshPlayerPermissions(ServerPlayer player) {
        if (player == null) return;

        boolean canEdit = Buildnotes.PERMISSION_MANAGER.isAllowedToEdit(player);
        PermissionLevel level = canEdit ? PermissionLevel.CAN_EDIT : PermissionLevel.VIEW_ONLY;

        ServerPlayNetworking.send(player, new UpdatePermissionS2CPacket(level));
    }

    public static void handleSaveNote(MinecraftServer server, ServerPlayer player, SaveNoteC2SPacket packet) {
        Note receivedNote = packet.note();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.saveNote(receivedNote);

        // Broadcast the update to all players
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, new UpdateNoteS2CPacket(receivedNote));
        }
    }

    public static void handleSaveBuild(MinecraftServer server, ServerPlayer player, SaveBuildC2SPacket packet) {
        Build receivedBuild = packet.build();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.saveBuild(receivedBuild);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, new UpdateBuildS2CPacket(receivedBuild));
        }
    }

    public static void handleDeleteNote(MinecraftServer server, ServerPlayer player, DeleteNoteC2SPacket packet) {
        UUID noteId = packet.noteId();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.deleteNote(noteId);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, new DeleteNoteS2CPacket(noteId));
        }
    }

    public static void handleDeleteBuild(MinecraftServer server, ServerPlayer player, DeleteBuildC2SPacket packet) {
        UUID buildId = packet.buildId();
        if (!hasEditPermission(server, player)) return;

        Buildnotes.SERVER_DATA_MANAGER.deleteBuild(buildId);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, new DeleteBuildS2CPacket(buildId));
        }
    }

    public static void handleImageChunkUpload(MinecraftServer server, ServerPlayer player, UploadImageChunkC2SPacket packet) {
        UUID buildId = packet.buildId();
        String filename = packet.filename();
        int totalChunks = packet.totalChunks();
        int chunkIndex = packet.chunkIndex();
        byte[] data = packet.data();

        ServerImageTransferManager.handleChunk(player, buildId, filename, totalChunks, chunkIndex, data);
    }

    public static void handleImageRequest(MinecraftServer server, ServerPlayer player, RequestImageC2SPacket packet) {
        UUID buildId = packet.buildId();
        String filename = packet.filename();

        Buildnotes.SERVER_DATA_MANAGER.sendImageToPlayer(player, buildId, filename);
    }
}
