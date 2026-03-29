package net.atif.buildnotes.client;

import net.atif.buildnotes.data.ColorConfig;
import net.atif.buildnotes.data.ConfigManager;
import net.atif.buildnotes.gui.hud.PinnedNoteHud;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.atif.buildnotes.data.TabType;
import net.atif.buildnotes.network.ClientPacketHandler;
import net.atif.buildnotes.network.packet.s2c.*;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class BuildnotesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ColorConfig.loadColors();
        ConfigManager.load();
        KeyBinds.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBinds.openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    ColorConfig.loadColors();
                    ConfigManager.load();
                    client.setScreen(new MainScreen(TabType.NOTES));
                }
            }
        });

        // Add the pinned note HUD to render after all vanilla elements are drawn
        HudElementRegistry.addLast(
                Identifier.of("buildnotes", "pinned_hud"),
                PinnedNoteHud::render
        );

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

        ClientPlayNetworking.registerGlobalReceiver(UpdatePermissionS2CPacket.ID,
                (payload, context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> ClientPacketHandler.handleUpdatePermission(context.client(), payload));
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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientCache.clear();
            ClientImageTransferManager.clearFailedDownloads();
            ClientSession.leaveServer();
        });
    }
}