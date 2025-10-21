package net.atif.buildnotes.client;

import net.atif.buildnotes.gui.screen.MainScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class BuildnotesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinds.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // We check if the key was just pressed
            while (KeyBinds.openGuiKey.wasPressed()) {
                // Make sure the player isn't in another screen before opening ours
                if (client.currentScreen == null) {
                    client.setScreen(new MainScreen());
                }
            }
        });
    }
}