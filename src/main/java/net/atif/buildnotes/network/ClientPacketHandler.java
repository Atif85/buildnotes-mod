package net.atif.buildnotes.network;

import net.atif.buildnotes.client.ClientCache;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.screen.MainScreen; // ADDED
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {

    private static void refreshMainScreen(MinecraftClient client) {
        if (client.currentScreen instanceof MainScreen) {
            ((MainScreen) client.currentScreen).refreshData();
        }
    }

    public static void handleInitialSync(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        List<Note> notes = buf.readList(Note::fromBuf);
        List<Build> builds = buf.readList(Build::fromBuf);
        client.execute(() -> {
            ClientCache.setNotes(notes);
            ClientCache.setBuilds(builds);
            // After the very first sync, refresh the screen if it's open
            refreshMainScreen(client);
        });
    }

    public static void handleUpdateNote(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Note updatedNote = Note.fromBuf(buf);
        client.execute(() -> {
            ClientCache.addOrUpdateNote(updatedNote);
            refreshMainScreen(client);
        });
    }

    public static void handleUpdateBuild(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Build updatedBuild = Build.fromBuf(buf);
        client.execute(() -> {
            ClientCache.addOrUpdateBuild(updatedBuild);
            refreshMainScreen(client);
        });
    }

    public static void handleDeleteNote(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID noteId = buf.readUuid();
        client.execute(() -> {
            ClientCache.removeNoteById(noteId);
            refreshMainScreen(client);
        });
    }

    public static void handleDeleteBuild(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID buildId = buf.readUuid();
        client.execute(() -> {
            ClientCache.removeBuildById(buildId);
            refreshMainScreen(client);
        });
    }
}