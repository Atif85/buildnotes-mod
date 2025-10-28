package net.atif.buildnotes;

import io.netty.buffer.Unpooled;
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.network.PacketIdentifiers;
import net.atif.buildnotes.network.ServerPacketHandler;
import net.atif.buildnotes.server.PermissionManager;
import net.atif.buildnotes.server.ServerDataManager;
import net.atif.buildnotes.server.command.BuildNotesCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
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

            // Create a buffer to write our data into
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeEnumConstant(permission); // Write the permission level to the packet

            // Send the packet to the joining client
            ServerPlayNetworking.send(player, PacketIdentifiers.HANDSHAKE_S2C, buf);
            LOGGER.info("Sent handshake packet to " + player.getName().getString());
        });

        // Register C2S packet handlers
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.REQUEST_DATA_C2S, ServerPacketHandler::handleRequestInitialData);
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SAVE_NOTE_C2S, ServerPacketHandler::handleSaveNote);
        // ADDED: Register handlers for other actions
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SAVE_BUILD_C2S, ServerPacketHandler::handleSaveBuild);
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.DELETE_NOTE_C2S, ServerPacketHandler::handleDeleteNote);
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.DELETE_BUILD_C2S, ServerPacketHandler::handleDeleteBuild);
    }
}