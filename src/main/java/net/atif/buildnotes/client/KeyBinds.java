package net.atif.buildnotes.client;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBinds {

    public static KeyMapping openGuiKey;

    private static final KeyMapping.Category MOD_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("buildnotes", "main"));

    public static void register() {

        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.buildnotes.opengui",
                InputConstants.Type.KEYSYM, // The type of input, KEYSYM for keyboard
                GLFW.GLFW_KEY_N, // The default key, N in this case
                MOD_CATEGORY
        ));
    }
}