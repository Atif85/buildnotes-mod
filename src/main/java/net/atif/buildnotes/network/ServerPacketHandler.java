package net.atif.buildnotes.network;

import io.netty.buffer.Unpooled;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.server.ServerDataManager;
import net.atif.buildnotes.server.ServerImageTransferManager;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.List;
import java.util.UUID;

public class ServerPacketHandler {

    private static boolean hasEditPermission(MinecraftServer server, ServerPlayerEntity player) {
        return Buildnotes.PERMISSION_MANAGER.isAllowedToEdit(player);
    }

    public static void handleRequestInitialData(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            // Access SERVER_DATA_MANAGER statically from the main mod class
            ServerDataManager dataManager = Buildnotes.SERVER_DATA_MANAGER;
            List<Note> notes = dataManager.getNotes();
            List<Build> builds = dataManager.getBuilds();

            PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer());
            responseBuf.writeCollection(notes, (b, n) -> n.writeToBuf(b));
            responseBuf.writeCollection(builds, (b, B) -> B.writeToBuf(b));

            ServerPlayNetworking.send(player, PacketIdentifiers.INITIAL_SYNC_S2C, responseBuf);
        });
    }

    public static void refreshPlayerPermissions(ServerPlayerEntity player) {
        if (player == null) return;

        boolean canEdit = Buildnotes.PERMISSION_MANAGER.isAllowedToEdit(player);
        PermissionLevel level = canEdit ? PermissionLevel.CAN_EDIT : PermissionLevel.VIEW_ONLY;

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(level); // Write the Enum

        ServerPlayNetworking.send(player, PacketIdentifiers.UPDATE_PERMISSION_S2C, buf);
    }

    public static void handleSaveNote(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Note receivedNote = Note.fromBuf(buf);
        server.execute(() -> {
            if (!hasEditPermission(server, player)) return;

            Buildnotes.SERVER_DATA_MANAGER.saveNote(receivedNote);

            // Broadcast the update to all players
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                PacketByteBuf copy = new PacketByteBuf(Unpooled.buffer());
                receivedNote.writeToBuf(copy);
                ServerPlayNetworking.send(p, PacketIdentifiers.UPDATE_NOTE_S2C, copy);
            }
        });
    }

    // Handler for saving a build
    public static void handleSaveBuild(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Build receivedBuild = Build.fromBuf(buf);
        server.execute(() -> {
            if (!hasEditPermission(server, player)) return;

            Buildnotes.SERVER_DATA_MANAGER.saveBuild(receivedBuild);

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                PacketByteBuf copy = new PacketByteBuf(Unpooled.buffer());
                receivedBuild.writeToBuf(copy);
                ServerPlayNetworking.send(p, PacketIdentifiers.UPDATE_BUILD_S2C, copy);
            }
        });
    }

    // Handler for deleting a note
    public static void handleDeleteNote(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID noteId = buf.readUuid();
        server.execute(() -> {
            if (!hasEditPermission(server, player)) return;

            Buildnotes.SERVER_DATA_MANAGER.deleteNote(noteId);

            PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer());
            responseBuf.writeUuid(noteId);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, PacketIdentifiers.DELETE_NOTE_S2C, responseBuf);
            }
        });
    }

    // Handler for deleting a build
    public static void handleDeleteBuild(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID buildId = buf.readUuid();
        server.execute(() -> {
            if (!hasEditPermission(server, player)) return;

            Buildnotes.SERVER_DATA_MANAGER.deleteBuild(buildId);

            PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer());
            responseBuf.writeUuid(buildId);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, PacketIdentifiers.DELETE_BUILD_S2C, responseBuf);
            }
        });
    }

    public static void handleImageChunkUpload(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID buildId = buf.readUuid();
        String filename = buf.readString();
        int totalChunks = buf.readVarInt();
        int chunkIndex = buf.readVarInt();
        byte[] data = buf.readByteArray();

        // Must execute on the server thread to ensure thread safety with the map
        server.execute(() -> ServerImageTransferManager.handleChunk(player, buildId, filename, totalChunks, chunkIndex, data));
    }

    public static void handleImageRequest(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID buildId = buf.readUuid();
        String filename = buf.readString();

        server.execute(() -> {
            // This now needs to be implemented: a method to read and send an image from the server
            Buildnotes.SERVER_DATA_MANAGER.sendImageToPlayer(player, buildId, filename);
        });
    }
}