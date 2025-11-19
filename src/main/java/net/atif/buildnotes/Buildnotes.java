package net.atif.buildnotes;

import net.atif.buildnotes.data .PermissionLevel;
import net.atif.buildnotes.network.ModPackets;
import net.atif.buildnotes.network.ServerPacketHandler;
import net.atif.buildnotes.network.packet.c2s.*;
import net.atif.buildnotes.network.packet.s2c.HandshakeS2CPacket;
import net.atif.buildnotes.server.PermissionManager;
import net.atif.buildnotes.server.ServerDataManager;
import net.atif.buildnotes.server.ServerImageTransferManager;
import net.atif.buildnotes.server.command.BuildNotesCommands;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Buildnotes implements ModInitializer {
    public static final String MOD_ID = "buildnotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ServerDataManager SERVER_DATA_MANAGER;
    public static PermissionManager PERMISSION_MANAGER;

    @Override
    public void onInitialize() {
        LOGGER.info("BuildNotes Initialized!");

        ModPackets.registerC2SPackets();
        ModPackets.registerS2CPackets();

        // Use a server lifecycle event to get the server instance safely
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER_DATA_MANAGER = new ServerDataManager(server);
            PERMISSION_MANAGER = new PermissionManager(server);
        });

        BuildNotesCommands.register();

        // Register the server-side event for when a player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // This is a good check for dedicated servers, but for testing in a client/server environment,
            // you might want to temporarily disable it. For now, it's correct.
            if (!server.isDedicated()) return;

            // Determine the player's permission level.
            PermissionLevel permission = PERMISSION_MANAGER.isAllowedToEdit(player)
                    ? PermissionLevel.CAN_EDIT
                    : PermissionLevel.VIEW_ONLY;

            // Send the typed handshake packet to the client
            ServerPlayNetworking.send(player, new HandshakeS2CPacket(permission));
            LOGGER.info("Sent handshake packet to {}", player.getName().getString());
        });

        // Register the disconnect event for cleaning up image transfers
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerImageTransferManager.onPlayerDisconnect(handler.player.getUuid()));

        ServerPlayNetworking.registerGlobalReceiver(DeleteNoteC2SPacket.ID, (packet, context) -> {
                    MinecraftServer server = context.server();
                    server.execute(() -> ServerPacketHandler.handleDeleteNote(server, context.player(), packet));
        });

        // Register C2S receivers using the new packet types
        ServerPlayNetworking.registerGlobalReceiver(RequestDataC2SPacket.ID, (packet, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> ServerPacketHandler.handleRequestInitialData(server, context.player(), packet));
        });

        ServerPlayNetworking.registerGlobalReceiver(SaveNoteC2SPacket.ID, (packet, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> ServerPacketHandler.handleSaveNote(server, context.player(), packet));
        });

        ServerPlayNetworking.registerGlobalReceiver(SaveBuildC2SPacket.ID, (packet, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> ServerPacketHandler.handleSaveBuild(server, context.player(), packet));
        });

        ServerPlayNetworking.registerGlobalReceiver(DeleteBuildC2SPacket.ID, (packet, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> ServerPacketHandler.handleDeleteBuild(server, context.player(), packet));
        });

        ServerPlayNetworking.registerGlobalReceiver(UploadImageChunkC2SPacket.ID, (packet, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> ServerPacketHandler.handleImageChunkUpload(server, context.player(), packet));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestImageC2SPacket.ID, (packet, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> ServerPacketHandler.handleImageRequest(server, context.player(), packet));
        });
    }
}