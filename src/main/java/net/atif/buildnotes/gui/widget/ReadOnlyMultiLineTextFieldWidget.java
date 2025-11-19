package net.atif.buildnotes.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

public class ReadOnlyMultiLineTextFieldWidget extends MultiLineTextFieldWidget {

    public ReadOnlyMultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText, int maxLines, boolean scrollingEnabled) {
        super(textRenderer, x, y, width, height, initialText, "", maxLines, scrollingEnabled);

        this.setCaretEnabled(false);
    }

    /**
     * Block all character input. This is a read-only field.
     */
    @Override
    public boolean charTyped(CharInput input) {
        return false;
    }

    /**
     * Block any programmatic text insertion.
     */
    @Override
    public void insertText(String text) {
        // Do nothing.
    }

    /**
     * Block all key presses that could modify the text.
     * Allows only navigation (arrows, home, end), copying, and selecting all.
     */
    @Override
    public boolean keyPressed(KeyInput input) {
        // These keys modify text, so we block them by "handling" the event (returning true)
        // but performing no action.
        int keyCode = input.getKeycode();
        if (input.isPaste() || input.isCut() || input.isEnter() ||
                keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            return true;
        }

        // For all other keys (which include navigation, copy, and select all),
        // we let the parent widget handle them as usual.
        return super.keyPressed(input);
    }
}