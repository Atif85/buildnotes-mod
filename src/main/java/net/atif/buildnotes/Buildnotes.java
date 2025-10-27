package net.atif.buildnotes;

// Add these imports
import io.netty.buffer.Unpooled;
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.network.PacketIdentifiers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Buildnotes implements ModInitializer {
    public static final String MOD_ID = "buildnotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("BuildNotes Initialized!");

        // Register the server-side event for when a player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            if (!server.isDedicated()) return;

            // Determine the player's permission level.
            // For now, we'll say server operators (ops) can edit.
            PermissionLevel permission = server.getPlayerManager().isOperator(player.getGameProfile())
                    ? PermissionLevel.CAN_EDIT
                    : PermissionLevel.VIEW_ONLY;

            // Create a buffer to write our data into
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeEnumConstant(permission); // Write the permission level to the packet

            // Send the packet to the joining client
            ServerPlayNetworking.send(player, PacketIdentifiers.HANDSHAKE_S2C, buf);
            LOGGER.info("Sent handshake packet to " + player.getName().getString());
        });
    }
}