package net.atif.buildnotes.client;

import io.netty.buffer.Unpooled;
import net.atif.buildnotes.data.ColorConfig;
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.atif.buildnotes.data.TabType;
import net.atif.buildnotes.network.ClientPacketHandler;
import net.atif.buildnotes.network.PacketIdentifiers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

@Environment(EnvType.CLIENT)
public class BuildnotesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ColorConfig.loadColors();
        KeyBinds.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBinds.openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    Colors.reload();
                    client.setScreen(new MainScreen(TabType.NOTES));
                }
            }
        });

        // Register all your S2C packet handlers here
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.HANDSHAKE_S2C, this::handleHandshake);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.UPDATE_PERMISSION_S2C, ClientPacketHandler::handleUpdatePermission);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.INITIAL_SYNC_S2C, ClientPacketHandler::handleInitialSync);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.UPDATE_NOTE_S2C, ClientPacketHandler::handleUpdateNote);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.UPDATE_BUILD_S2C, ClientPacketHandler::handleUpdateBuild);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.DELETE_NOTE_S2C, ClientPacketHandler::handleDeleteNote);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.DELETE_BUILD_S2C, ClientPacketHandler::handleDeleteBuild);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.IMAGE_CHUNK_S2C, ClientPacketHandler::handleImageChunk);
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.IMAGE_NOT_FOUND_S2C, ClientPacketHandler::handleImageNotFound);

        // Register disconnect event to clear server-side cache
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientSession.leaveServer();
            ClientCache.clear();

            ClientImageTransferManager.clearFailedDownloads();
        }));
    }

    private void handleHandshake(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        PermissionLevel permission = buf.readEnumConstant(PermissionLevel.class);

        // This code runs on the main render thread, so it's safe to update our session
        client.execute(() -> {
            // Set the session state *here* after receiving the handshake
            ClientSession.joinServer(permission);
            // After joining, immediately request data from the server
            ClientPlayNetworking.send(PacketIdentifiers.REQUEST_DATA_C2S, new PacketByteBuf(Unpooled.buffer()));
        });
    }
}