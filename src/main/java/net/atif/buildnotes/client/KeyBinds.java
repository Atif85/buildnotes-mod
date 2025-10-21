package net.atif.buildnotes.client;

import net.atif.buildnotes.Buildnotes;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBinds {

    public static KeyBinding openGuiKey;

    public static void register() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key."+ Buildnotes.MOD_ID +".opengui",
                InputUtil.Type.KEYSYM, // The type of input, KEYSYM for keyboard
                GLFW.GLFW_KEY_N, // The default key, N in this case
                "category." + Buildnotes.MOD_ID + ".main" // The category in the controls menu
        ));
    }
}