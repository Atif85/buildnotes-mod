package net.atif.buildnotes.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

public class ReadOnlyMultiLineTextFieldWidget extends MultiLineTextFieldWidget {

    public ReadOnlyMultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText, int maxLines, boolean scrollingEnabled) {
        super(textRenderer, x, y, width, height, initialText, "", maxLines, scrollingEnabled);
    }

    /**
     * Override the render method to prevent the caret from ever being drawn.
     * We do this by temporarily setting 'focused' to false before calling the parent's render
     * method, and then restoring it immediately after.
     */
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        boolean originalFocus = this.focused;
        this.focused = false; // Trick the renderer into not drawing the caret
        super.render(matrices, mouseX, mouseY, delta);
        this.focused = originalFocus; // Restore the actual focus state
    }

    /**
     * Block all character input.
     */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    /**
     * Block all key presses except for navigation (for keyboard scrolling) and copying.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow copying text
        if (Screen.isCopy(keyCode)) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // Allow navigation keys (arrows, home, end, etc.) so the user can scroll with the keyboard.
        if (isNavigationKey(keyCode)) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // Block all other keys (text input, backspace, delete, paste, cut, etc.)
        return false;
    }

    private boolean isNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN ||
                keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT ||
                keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END ||
                keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN;
    }
}