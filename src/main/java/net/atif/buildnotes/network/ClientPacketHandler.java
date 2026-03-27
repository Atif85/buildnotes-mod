package net.atif.buildnotes.network;

import net.atif.buildnotes.client.ClientCache;
import net.atif.buildnotes.client.ClientImageTransferManager;
import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.gui.screen.MainScreen; // ADDED
import net.atif.buildnotes.network.packet.c2s.RequestDataC2SPacket;
import net.atif.buildnotes.network.packet.s2c.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {

    private static void refreshMainScreen(Minecraft client) {
        if (client.screen instanceof MainScreen) {
            ((MainScreen) client.screen).refreshData();
        }
    }

    public static void handleHandshake(Minecraft client, HandshakeS2CPacket packet) {
        final var permission = packet.permission();
        ClientSession.joinServer(permission);
        // After joining, immediately request data from the server
        ClientPlayNetworking.send(new RequestDataC2SPacket());
    }

    public static void handleUpdatePermission(Minecraft client, UpdatePermissionS2CPacket packet) {
        PermissionLevel newLevel = packet.permission();

        // Update the static session data
        ClientSession.updatePermissionLevel(newLevel);

        // Refresh the screen (e.g., to show/hide the "Add Note" button immediately)
        refreshMainScreen(client);
    }

    // --- Typed packet handlers ---
    public static void handleInitialSync(Minecraft client, InitialSyncS2CPacket packet) {
        List<Note> notes = packet.notes();
        List<Build> builds = packet.builds();
        ClientCache.setNotes(notes);
        ClientCache.setBuilds(builds);
        refreshMainScreen(client);
    }

    public static void handleUpdateNote(Minecraft client, UpdateNoteS2CPacket packet) {
        Note updatedNote = packet.note();
        ClientCache.addOrUpdateNote(updatedNote);
        refreshMainScreen(client);
    }

    public static void handleUpdateBuild(Minecraft client, UpdateBuildS2CPacket packet) {
        Build updatedBuild = packet.build();
        ClientCache.addOrUpdateBuild(updatedBuild);
        refreshMainScreen(client);
    }

    public static void handleDeleteNote(Minecraft client, DeleteNoteS2CPacket packet) {
        UUID noteId = packet.noteId();
        ClientCache.removeNoteById(noteId);
        refreshMainScreen(client);
    }

    public static void handleDeleteBuild(Minecraft client, DeleteBuildS2CPacket packet) {
        UUID buildId = packet.buildId();
        ClientCache.removeBuildById(buildId);
        refreshMainScreen(client);
    }

    public static void handleImageChunk(Minecraft client, ImageChunkS2CPacket packet) {
        ClientImageTransferManager.handleChunk(packet.buildId(), packet.filename(), packet.totalChunks(), packet.chunkIndex(), packet.data());
    }

    public static void handleImageNotFound(Minecraft client, ImageNotFoundS2CPacket packet) {
        ClientImageTransferManager.onDownloadFailed(packet.buildId(), packet.filename());
    }
}