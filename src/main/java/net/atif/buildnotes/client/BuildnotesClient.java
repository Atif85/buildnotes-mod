package net.atif.buildnotes.client;

// Add these imports
import net.atif.buildnotes.data.PermissionLevel;
import net.atif.buildnotes.network.PacketIdentifiers;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.atif.buildnotes.data.TabType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

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

        // Register the client-side packet receiver
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.HANDSHAKE_S2C, this::handleHandshake);

        // Register disconnect event to clear server-side cache
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // We need to run this on the client thread
            client.execute(() -> {
                ClientSession.leaveServer();
                ClientCache.clear();
            });
        });
    }

    private void handleHandshake(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        // Read the permission level from the packet buffer
        PermissionLevel permission = buf.readEnumConstant(PermissionLevel.class);

        // This code runs on the main render thread, so it's safe to update our session
        client.execute(() -> {
            ClientSession.joinServer(permission);
        });
    }
}