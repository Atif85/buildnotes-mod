package net.atif.buildnotes.client;

import net.atif.buildnotes.data.TabType;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.atif.buildnotes.network.ClientPacketHandler;
import net.atif.buildnotes.network.ModPackets;
import net.atif.buildnotes.network.packet.s2c.*;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class BuildnotesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        KeyBinds.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBinds.openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MainScreen(TabType.NOTES));
                }
            }
        });

        // Register all S2C packet
        ClientPlayNetworking.registerGlobalReceiver(HandshakeS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleHandshake(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(InitialSyncS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleInitialSync(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(UpdateNoteS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleUpdateNote(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(UpdateBuildS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleUpdateBuild(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(DeleteNoteS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleDeleteNote(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(DeleteBuildS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleDeleteBuild(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(ImageChunkS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleImageChunk(client, packet));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(ImageNotFoundS2CPacket.ID,
                (packet, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleImageNotFound(client, packet));
                }
        );

        // Register disconnect event to clear server-side cache
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientSession.leaveServer();
            ClientCache.clear();

            ClientImageTransferManager.clearFailedDownloads();
        }));
    }
}